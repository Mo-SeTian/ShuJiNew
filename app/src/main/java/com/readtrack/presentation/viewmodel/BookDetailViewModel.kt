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
    val allTags: List<TagEntity> = emptyList()
) {
    val recentRecords: List<ReadingRecordEntity>
        get() = readingRecords.sortedByDescending { it.date }.take(10)
}

/** 趋势图数据点 */
data class TrendPoint(
    val dateLabel: String,    // 显示用如 "6/12"
    val dateMs: Long,        // 排序用
    val cumulative: Double    // 累计阅读量
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
                    BookDetailUiState(
                        book = book,
                        readingRecords = records,
                        isLoading = false,
                        trendData = trendData,
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
        }.mapValues { (_, dayRecords) -> dayRecords.sumOf { it.pagesRead } }

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

    fun updateStatus(status: BookStatus) {
        val currentBook = _uiState.value.book ?: return
        viewModelScope.launch {
            try {
                val recordType = when (status) {
                    BookStatus.READING -> RecordType.STATUS_READING
                    BookStatus.FINISHED -> RecordType.STATUS_FINISHED
                    BookStatus.ABANDONED -> RecordType.STATUS_DROPPED
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
