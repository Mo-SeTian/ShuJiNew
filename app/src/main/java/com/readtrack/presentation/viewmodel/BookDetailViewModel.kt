package com.readtrack.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.TagEntity
import com.readtrack.R
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookSnapshot
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.model.ProgressType
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import com.readtrack.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.readtrack.util.TimeConstants
import com.readtrack.util.getEndOfDay
import com.readtrack.util.getStartOfDay
import com.readtrack.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.max

data class BookDetailUiState(
    val book: BookEntity? = null,
    val readingRecords: List<ReadingRecordEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** 阅读趋势数据：按日期累计阅读量（排除状态变更记录） */
    val trendData: List<TrendPoint> = emptyList(),
    /** 当前书籍的标签 */
    val tags: List<TagEntity> = emptyList(),
    /** 所有可选标签 */
    val allTags: List<TagEntity> = emptyList(),
    /** 阅读热力图数据 */
    val heatmapMonths: List<HeatmapMonth> = emptyList(),
    /** 已读书籍阅读时间线 */
    val readingPeriods: List<ReadingPeriod> = emptyList()
) {
    val recentRecords: List<ReadingRecordEntity>
        get() = readingRecords.sortedByDescending { it.date }.take(10)
}

/** 趋势图数据点 */
data class TrendPoint(
    val dateLabel: String,
    val dateMs: Long,
    val cumulative: Double
)

/** 热力图单日单书数据 */
data class DayBookBreakdown(
    val bookTitle: String,
    val chaptersRead: Double = 0.0,
    val pagesRead: Double = 0.0
)

/** 热力图单日数据 */
data class HeatmapDay(
    val dateMs: Long,
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    val dayOfWeek: Int,
    val chaptersRead: Double,
    val pagesRead: Double,
    val bookBreakdowns: List<DayBookBreakdown> = emptyList()
)

/** 热力图单月数据 */
data class HeatmapMonth(
    val year: Int,
    val month: Int,
    val label: String,
    val days: List<HeatmapDay>,
    val totalValue: Double
)

/** 阅读周期（适用于已读、在读、闲置、放弃的书籍） */
data class ReadingPeriod(
    val startDate: Long,
    val endDate: Long,
    val startLabel: String,
    val endLabel: String,
    val totalPagesRead: Double,
    val totalChaptersRead: Double,
    val activeDays: Int,
    val pagesPerDay: Double,
    val chaptersPerDay: Double,
    val isOpenEnded: Boolean = false  // 在读中，未结束
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val recordRepository: ReadingRecordRepository,
    private val tagRepository: TagRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: 0L

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    private val _deleteSuccess = MutableSharedFlow<Boolean>()
    val deleteSuccess: SharedFlow<Boolean> = _deleteSuccess.asSharedFlow()

    init {
        loadBookDetail()
        loadTags()
    }

    private fun loadBookDetail() {
        viewModelScope.launch {
            try {
                combine(
                    bookRepository.getBookById(bookId).catch { emit(null) },
                    recordRepository.getRecordsByBookId(bookId).catch { emit(emptyList()) }
                ) { book, records ->
                    val trendData = computeTrendData(records)
                    val heatmapMonths = computeHeatmapData(records)
                    val readingPeriods = if (book?.status != null && book.status != BookStatus.WANT_TO_READ)
                        computeReadingPeriods(records) else emptyList()
                    BookDetailUiState(
                        book = book,
                        readingRecords = records,
                        isLoading = false,
                        trendData = trendData,
                        heatmapMonths = heatmapMonths,
                        readingPeriods = readingPeriods,
                        tags = _uiState.value.tags,
                        allTags = _uiState.value.allTags
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = BookDetailUiState(isLoading = false, errorMessage = e.message)
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getTagsForBook(bookId).collect { tags ->
                _uiState.update { it.copy(tags = tags) }
            }
        }
        viewModelScope.launch {
            tagRepository.getAllTags().collect { allTags ->
                _uiState.update { it.copy(allTags = allTags) }
            }
        }
    }

    fun addTag(tagId: Long) {
        viewModelScope.launch {
            tagRepository.addTagToBook(tagId, bookId)
        }
    }

    fun removeTag(tagId: Long) {
        viewModelScope.launch {
            tagRepository.removeTagFromBook(tagId, bookId)
        }
    }

    fun createTagAndAdd(name: String) {
        viewModelScope.launch {
            val tagId = tagRepository.createTag(name.trim())
            tagRepository.addTagToBook(tagId, bookId)
        }
    }

    /**
     * 计算最近 7 天阅读趋势：
     * - 今天往前推 6 天，共 7 天
     * - 每天一个数据点，为该天当前书的 NORMAL 记录阅读量
     * - 无记录的天显示 0
     */
    private fun computeTrendData(records: List<ReadingRecordEntity>): List<TrendPoint> {
        val normalRecords = records.filter { it.recordType == RecordType.NORMAL }
        val dateFormatter = SimpleDateFormat("M/d", Locale.CHINESE)
        val dayMs = ONE_DAY_MILLIS

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStartMs = calendar.timeInMillis

        // 按天聚合阅读量
        val dailyPages = normalRecords.groupBy { record ->
            calendar.timeInMillis = record.date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }.mapValues { (_, dayRecords) -> dayRecords.sumOf { r ->
                if (r.bookSnapshot?.progressType == ProgressType.CHAPTER) (r.chaptersRead ?: 0).toDouble()
                else r.pagesRead
            } }

        // 生成最近 7 天数据点（从远到近）
        val result = mutableListOf<TrendPoint>()
        for (dayOffset in 6 downTo 0) {
            val dayStartMs = todayStartMs - dayOffset * dayMs
            val amount = dailyPages[dayStartMs] ?: 0.0
            result.add(
                TrendPoint(
                    dateLabel = dateFormatter.format(Date(dayStartMs)),
                    dateMs = dayStartMs,
                    cumulative = amount
                )
            )
        }
        return result
    }

    private fun computeHeatmapData(records: List<ReadingRecordEntity>): List<HeatmapMonth> {
        return buildHeatmapMonths(records)
    }

    private fun computeReadingPeriods(records: List<ReadingRecordEntity>): List<ReadingPeriod> {
        val endTypes = setOf(RecordType.STATUS_FINISHED, RecordType.STATUS_DROPPED)
        val normalRecords = records.filter { it.recordType == RecordType.NORMAL }

        val hasReadingRecord = records.any { it.recordType == RecordType.STATUS_READING }
        val hasNormalRecords = normalRecords.isNotEmpty()

        // 确定起点类型：优先 STATUS_READING；若无但存在 NORMAL 阅读量，回退到 STATUS_ADDED
        val startType = if (hasReadingRecord || !hasNormalRecords)
            RecordType.STATUS_READING
        else
            RecordType.STATUS_ADDED

        // 从新到旧遍历，自然按阅读记录顺序输出（新→旧，上→下）
        val statusRecords = records.filter {
            it.recordType == startType || it.recordType in endTypes
        }.sortedByDescending { it.date }

        val periods = mutableListOf<ReadingPeriod>()
        val endStack = ArrayDeque<ReadingRecordEntity>() // 等待配对的结束记录

        for (record in statusRecords) {
            when (record.recordType) {
                startType -> {
                    if (endStack.isNotEmpty()) {
                        val end = endStack.removeFirst()
                        periods.add(buildPeriod(record, end, normalRecords))
                    } else {
                        // 无配对的结束记录 → 至今（在读中）
                        periods.add(
                            buildPeriod(record, null, normalRecords).copy(
                                endDate = System.currentTimeMillis(),
                                endLabel = "至今",
                                isOpenEnded = true
                            )
                        )
                    }
                }
                in endTypes -> {
                    endStack.addLast(record)
                }
                else -> {}
            }
        }

        // 剩余的结束记录无开始配对（书籍添加时直接是结束状态），忽略
        return periods
    }

    private fun buildPeriod(
        startRecord: ReadingRecordEntity,
        endRecord: ReadingRecordEntity?,
        normalRecords: List<ReadingRecordEntity>
    ): ReadingPeriod {
        val startDate = startRecord.date
        val endDate = endRecord?.date ?: System.currentTimeMillis()
        // 起止归一化到零点；上界用 getEndOfDay 保证当天记录不被过滤
        val startDay = getStartOfDay(startDate)
        val endDay = getEndOfDay(endDate)
        val periodRecords = normalRecords.filter { it.date in startDay..endDay }
        // 按 ProgressType 分开统计，避免章节书的 pagesRead 和 chaptersRead 重复累加
        val totalPages = periodRecords.filter { it.bookSnapshot?.progressType != ProgressType.CHAPTER }.sumOf { it.pagesRead }
        val totalChapters = periodRecords.filter { it.bookSnapshot?.progressType == ProgressType.CHAPTER }.sumOf { (it.chaptersRead ?: 0).toDouble() }
        // 活跃天数：区间内有实际阅读记录的不同天数
        val activeDays = periodRecords.map { getStartOfDay(it.date) }.distinct().count().coerceAtLeast(1)
        return ReadingPeriod(
            startDate = startDay,
            endDate = endDay,
            startLabel = recordTypeLabel(startRecord.recordType, startRecord.bookSnapshot?.status),
            endLabel = if (endRecord != null) recordTypeLabel(endRecord.recordType, endRecord.bookSnapshot?.status) else "至今",
            totalPagesRead = totalPages,
            totalChaptersRead = totalChapters,
            activeDays = activeDays,
            pagesPerDay = totalPages / activeDays,
            chaptersPerDay = totalChapters / activeDays
        )
    }

    private fun recordTypeLabel(type: RecordType, status: BookStatus?): String = when (type) {
        RecordType.STATUS_READING -> "在读"
        RecordType.STATUS_ADDED -> "添加"
        RecordType.STATUS_FINISHED -> "已读"
        RecordType.STATUS_DROPPED -> when (status) {
            BookStatus.ON_HOLD -> "闲置"
            BookStatus.ABANDONED -> "放弃"
            else -> "暂停"
        }
        else -> ""
    }

    companion object {
        private const val ONE_DAY_MILLIS = 24L * 60 * 60 * 1000L
    }

    fun updateStatus(status: BookStatus) {
        val currentBook = _uiState.value.book ?: return

        // 不允许改回想读（会导致时间线缺少开始或未闭合）
        if (status == BookStatus.WANT_TO_READ && currentBook.status != BookStatus.WANT_TO_READ) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.error_status_change_forbidden)) }
            return
        }

        viewModelScope.launch {
            try {
                val recordType = when (status) {
                    BookStatus.READING -> RecordType.STATUS_READING
                    BookStatus.FINISHED -> RecordType.STATUS_FINISHED
                    BookStatus.ON_HOLD, BookStatus.ABANDONED -> RecordType.STATUS_DROPPED
                    else -> return@launch
                }
                bookRepository.updateBookStatus(currentBook.id, status, recordType)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "更新状态失败: ${e.message}") }
            }
        }
    }

    /**
     * 添加阅读进度（页数模式 / 章节模式统一入口）
     * 使用原子操作 insertRecordAndUpdateBook 保证记录和书籍同步更新
     * @param amount 输入的数值（页数模式为 Double，章节模式为 Int.toDouble()）
     * @param isIncrement true=增量模式，false=直接模式
     */
    fun addProgress(amount: Double, isIncrement: Boolean = true) {
        val currentBook = _uiState.value.book ?: return
        val isChapterBased = currentBook.progressType == ProgressType.CHAPTER

        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()

                val record: ReadingRecordEntity
                val updatedBook: BookEntity

                if (isChapterBased) {
                    val chapters = amount.toInt()
                    val fromChapter = currentBook.currentChapter
                    val maxChapter = currentBook.totalChapters ?: 0
                    val toChapter = if (maxChapter > 0) {
                        if (isIncrement) {
                            (fromChapter + chapters).coerceAtMost(maxChapter)
                        } else {
                            chapters.coerceIn(0, maxChapter)
                        }
                    } else {
                        // 未设置总章数时不设上限，避免 coerceIn(0,0) 抛异常
                        (fromChapter + chapters).coerceAtLeast(0)
                    }
                    // 实际阅读量 = 终点 - 起点（clamp 后），避免超量记录虚增统计
                    val chaptersActuallyRead = (toChapter - fromChapter).coerceAtLeast(0)

                    record = ReadingRecordEntity(
                        bookId = currentBook.id,
                        bookSnapshot = BookSnapshot.from(currentBook, currentBook.status),
                        pagesRead = chaptersActuallyRead.toDouble(),
                        fromPage = fromChapter.toDouble(),
                        toPage = toChapter.toDouble(),
                        chaptersRead = chaptersActuallyRead,
                        date = currentTime
                    )
                    updatedBook = currentBook.copy(
                        currentChapter = toChapter,
                        lastReadAt = currentTime,
                        updatedAt = currentTime
                    )
                } else {
                    val fromPage = currentBook.currentPage
                    val toPage = if (isIncrement) {
                        (fromPage + amount).coerceAtMost(currentBook.totalPages)
                    } else {
                        amount.coerceIn(0.0, currentBook.totalPages)
                    }
                    val pagesActuallyRead = (toPage - fromPage).coerceAtLeast(0.0)

                    record = ReadingRecordEntity(
                        bookId = currentBook.id,
                        bookSnapshot = BookSnapshot.from(currentBook, currentBook.status),
                        pagesRead = pagesActuallyRead,
                        fromPage = fromPage,
                        toPage = toPage,
                        date = currentTime
                    )
                    updatedBook = currentBook.copy(
                        currentPage = toPage,
                        lastReadAt = currentTime,
                        updatedAt = currentTime
                    )
                }

                bookRepository.insertRecordAndUpdateBook(record, updatedBook)
                WidgetUpdateHelper.triggerUpdate(context)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "添加记录失败: ${e.message}") }
            }
        }
    }

    /** @deprecated 请使用 addProgress(amount, isIncrement) */
    fun addReadingRecord(pages: Double, isIncrement: Boolean = true) = addProgress(pages, isIncrement)

    /** @deprecated 请使用 addProgress(amount.toDouble(), isIncrement) */
    fun addChapterProgress(chapters: Int, isIncrement: Boolean = true) = addProgress(chapters.toDouble(), isIncrement)

    fun deleteBook() {
        val currentBook = _uiState.value.book ?: return
        viewModelScope.launch {
            try {
                bookRepository.deleteBook(currentBook.id)
                _deleteSuccess.emit(true)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "删除失败: ${e.message}") }
            }
        }
    }

    /**
     * 删除单条阅读记录，同时重算书籍进度
     */
    fun deleteReadingRecord(record: ReadingRecordEntity) {
        viewModelScope.launch {
            try {
                bookRepository.deleteRecordAndRecalculateBook(record)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "删除记录失败: ${e.message}") }
            }
        }
    }

    /**
     * 更新单条阅读记录，同时重算书籍进度
     */
    fun updateReadingRecord(record: ReadingRecordEntity) {
        viewModelScope.launch {
            try {
                bookRepository.updateRecordAndRecalculateBook(record)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "更新记录失败: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 更新书籍评分
     * @param rating 0-5 星，传入 null 表示清除评分
     */
    fun updateRating(rating: Float?) {
        val currentBook = _uiState.value.book ?: return
        viewModelScope.launch {
            try {
                val updatedBook = currentBook.copy(
                    rating = rating,
                    updatedAt = System.currentTimeMillis()
                )
                bookRepository.updateBook(updatedBook)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "更新评分失败: ${e.message}") }
            }
        }
    }
}
