package com.readtrack.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.TagEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookSnapshot
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.model.ProgressType
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import com.readtrack.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

/** 热力图单日数据 */
data class HeatmapDay(
    val dateMs: Long,
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    val dayOfWeek: Int,
    val chaptersRead: Double,
    val pagesRead: Double
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
        val dayMs = 24L * 60 * 60 * 1000

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
        val normalRecords = records.filter { it.recordType == RecordType.NORMAL }
        if (normalRecords.isEmpty()) return emptyList()

        val dayMs = 24L * 60 * 60 * 1000
        val calendar = Calendar.getInstance()

        // 按天聚合
        val dailyMap = linkedMapOf<Long, HeatmapDay>()
        normalRecords.forEach { record ->
            calendar.timeInMillis = record.date
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val dow = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dayStart = calendar.timeInMillis

            val isChapter = record.bookSnapshot?.progressType == ProgressType.CHAPTER
        val amount = if (isChapter) (record.chaptersRead ?: 0).toDouble() else record.pagesRead
        val existing = dailyMap[dayStart]
        if (existing != null) {
            dailyMap[dayStart] = existing.copy(
                chaptersRead = existing.chaptersRead + if (isChapter) amount else 0.0,
                pagesRead = existing.pagesRead + if (!isChapter) amount else 0.0
            )
        } else {
            dailyMap[dayStart] = HeatmapDay(
                dateMs = dayStart,
                year = year,
                month = month,
                dayOfMonth = day,
                dayOfWeek = dow,
                chaptersRead = if (isChapter) amount else 0.0,
                pagesRead = if (!isChapter) amount else 0.0
            )
        }
        }

        // 填充缺失日（最早记录日到今天）
        val earliest = dailyMap.keys.min()
        calendar.timeInMillis = earliest
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var currentDay = earliest
        while (currentDay <= todayStart) {
            if (!dailyMap.containsKey(currentDay)) {
                calendar.timeInMillis = currentDay
                dailyMap[currentDay] = HeatmapDay(
                    dateMs = currentDay,
                    year = calendar.get(Calendar.YEAR),
                    month = calendar.get(Calendar.MONTH),
                    dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
                    dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
                    chaptersRead = 0.0,
                    pagesRead = 0.0
                )
            }
            currentDay += dayMs
        }

        // 按月分组，按日期排序
        val sortedDays = dailyMap.values.sortedBy { it.dateMs }
        return sortedDays.groupBy { it.year to it.month }.map { (key, days) ->
            val (year, month) = key
            HeatmapMonth(
                year = year,
                month = month,
                label = "${year}年${month + 1}月",
                days = days,
                totalValue = days.sumOf { it.pagesRead + it.chaptersRead }
            )
        }.sortedWith(compareBy({ it.year }, { it.month }))
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

        val statusRecords = records.filter {
            it.recordType == startType || it.recordType in endTypes
        }.sortedBy { it.date }

        val periods = mutableListOf<ReadingPeriod>()
        val readingStack = ArrayDeque<ReadingRecordEntity>()

        for (record in statusRecords) {
            when (record.recordType) {
                startType -> {
                    if (readingStack.isNotEmpty()) {
                        val previous = readingStack.removeFirst()
                        periods.add(buildPeriod(previous, record, normalRecords))
                    }
                    readingStack.addLast(record)
                }
                in endTypes -> {
                    if (readingStack.isNotEmpty()) {
                        val start = readingStack.removeFirst()
                        periods.add(buildPeriod(start, record, normalRecords))
                    }
                }
                else -> {}
            }
        }

        // 剩余的未结束周期 → 至今（在读中）
        if (readingStack.isNotEmpty()) {
            val start = readingStack.removeFirst()
            val now = System.currentTimeMillis()
            periods.add(
                buildPeriod(start, null, normalRecords).copy(
                    endDate = now,
                    endLabel = "至今",
                    isOpenEnded = true
                )
            )
        }

        return periods.sortedByDescending { it.startDate }
    }

    private fun buildPeriod(
        startRecord: ReadingRecordEntity,
        endRecord: ReadingRecordEntity?,
        normalRecords: List<ReadingRecordEntity>
    ): ReadingPeriod {
        val startDate = startRecord.date
        val endDate = endRecord?.date ?: System.currentTimeMillis()
        val periodRecords = normalRecords.filter { it.date in startDate..endDate }
        // 按 ProgressType 分开统计，避免章节书的 pagesRead 和 chaptersRead 重复累加
        val totalPages = periodRecords.filter { it.bookSnapshot?.progressType != ProgressType.CHAPTER }.sumOf { it.pagesRead }
        val totalChapters = periodRecords.filter { it.bookSnapshot?.progressType == ProgressType.CHAPTER }.sumOf { (it.chaptersRead ?: 0).toDouble() }
        // 活跃天数：区间内有实际阅读记录的不同天数
        val activeDays = periodRecords.map { getStartOfDay(it.date) }.distinct().count().coerceAtLeast(1)
        return ReadingPeriod(
            startDate = startDate,
            endDate = endDate,
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
            _uiState.update { it.copy(errorMessage = "不能将书籍状态修改回「想读」，这会导致阅读时间线错乱。如有需要，请删除书籍后重新添加。") }
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
     * 添加阅读记录（更新进度）
     * 使用原子操作 insertRecordAndUpdateBook 保证记录和书籍同步更新
     * @param pages 输入的页数
     * @param isIncrement true=增量模式（当前进度+输入值），false=直接模式（直接设置到输入值）
     */
    fun addReadingRecord(pages: Double, isIncrement: Boolean = true) {
        val currentBook = _uiState.value.book ?: return
        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                val fromPage = currentBook.currentPage
                val toPage = if (isIncrement) {
                    (fromPage + pages).coerceAtMost(currentBook.totalPages)
                } else {
                    pages.coerceIn(0.0, currentBook.totalPages)
                }
                val pagesActuallyRead = if (isIncrement) pages else (toPage - fromPage).coerceAtLeast(0.0)

                val record = ReadingRecordEntity(
                    bookId = currentBook.id,
                    bookSnapshot = BookSnapshot.from(currentBook, currentBook.status),
                    pagesRead = pagesActuallyRead,
                    fromPage = fromPage,
                    toPage = toPage,
                    date = currentTime
                )
                val updatedBook = currentBook.copy(
                    currentPage = toPage,
                    lastReadAt = currentTime,
                    updatedAt = currentTime
                )
                // 原子操作：记录插入 + 书籍更新在同一个事务中
                bookRepository.insertRecordAndUpdateBook(record, updatedBook)
                WidgetUpdateHelper.triggerUpdate(context)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "添加记录失败: ${e.message}") }
            }
        }
    }

    /**
     * 添加章节进度
     * 使用原子操作 insertRecordAndUpdateBook 保证记录和书籍同步更新
     * @param chapters 输入的章节数
     * @param isIncrement true=增量模式（当前章节+输入值），false=直接模式（直接设置到输入值）
     */
    fun addChapterProgress(chapters: Int, isIncrement: Boolean = true) {
        val currentBook = _uiState.value.book ?: return
        if (currentBook.progressType != ProgressType.CHAPTER) return

        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                val fromChapter = currentBook.currentChapter
                val maxChapter = currentBook.totalChapters ?: 0
                val toChapter = if (isIncrement) {
                    (fromChapter + chapters).coerceAtMost(maxChapter)
                } else {
                    chapters.coerceIn(0, maxChapter)
                }
                val chaptersActuallyRead = if (isIncrement) chapters else (toChapter - fromChapter).coerceAtLeast(0)

                val record = ReadingRecordEntity(
                    bookId = currentBook.id,
                    bookSnapshot = BookSnapshot.from(currentBook, currentBook.status),
                    pagesRead = chaptersActuallyRead.toDouble(),
                    fromPage = fromChapter.toDouble(),
                    toPage = toChapter.toDouble(),
                    chaptersRead = chaptersActuallyRead,
                    date = currentTime
                )
                val updatedBook = currentBook.copy(
                    currentChapter = toChapter,
                    lastReadAt = currentTime,    // 修复：章节模式同样需要更新 lastReadAt
                    updatedAt = currentTime
                )
                // 原子操作：记录插入 + 书籍更新在同一个事务中
                bookRepository.insertRecordAndUpdateBook(record, updatedBook)
                WidgetUpdateHelper.triggerUpdate(context)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "添加记录失败: ${e.message}") }
            }
        }
    }

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
