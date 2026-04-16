package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.local.StatsUnit
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookSnapshot
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.viewmodel.ProgressType
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import com.readtrack.util.PerformanceTrace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val statsUnit: StatsUnit = StatsUnit.CHAPTER,
    val todayValue: Double = 0.0,
    val streakDays: Int = 0,
    val totalReadingTime: Double = 0.0,
    // 预计算好的格式化字符串，避免 UI 层每次 recomposition 都重算
    val totalReadingTimeLabel: String = "0 小时 0 分钟",
    val totalBooks: Int = 0,
    val readingBooks: Int = 0,
    val finishedBooks: Int = 0,
    val recentBooks: List<BookEntity> = emptyList(),
    val statusCounts: Map<BookStatus, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val recordRepository: ReadingRecordRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeHomeState()
    }

    private fun observeHomeState() {
        viewModelScope.launch {
            combine(
                bookRepository.getAllBooks().catch { emit(emptyList()) },
                recordRepository.getAllRecords().catch { emit(emptyList()) },
                preferencesManager.statsUnit
            ) { books, records, statsUnit ->
                Triple(books, records, statsUnit)
            }.collect { (books, records, statsUnit) ->
                val state = buildHomeUiState(books, records, statsUnit)
                _uiState.value = state
                PerformanceTrace.mark(
                    "home.ready total=${state.totalBooks} recent=${state.recentBooks.size} streak=${state.streakDays}"
                )
            }
        }
    }

    fun quickRecord(bookId: Long, newPage: Double, newChapter: Int) {
        viewModelScope.launch {
            try {
                val book = _uiState.value.recentBooks.find { it.id == bookId } ?: return@launch
                val isChapterBased = book.progressType == ProgressType.CHAPTER
                val fromPage = if (isChapterBased) book.currentChapter.toDouble() else book.currentPage
                val record = ReadingRecordEntity(
                    bookId = bookId,
                    bookSnapshot = BookSnapshot(
                        id = book.id,
                        title = book.title,
                        author = book.author,
                        coverPath = book.coverPath,
                        progressType = book.progressType,
                        status = book.status
                    ),
                    pagesRead = if (isChapterBased) 0.0 else (newPage - book.currentPage).coerceAtLeast(0.0),
                    fromPage = book.currentPage,
                    toPage = if (isChapterBased) 0.0 else newPage.coerceAtMost(book.totalPages),
                    recordType = RecordType.NORMAL,
                    date = System.currentTimeMillis()
                )
                val updatedBook = book.copy(
                    currentPage = if (isChapterBased) book.currentPage else newPage,
                    currentChapter = if (isChapterBased) newChapter else book.currentChapter,
                    lastReadAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                bookRepository.insertRecordAndUpdateBook(record, updatedBook)
            } catch (_: Exception) {
                // 静默失败，最近阅读卡片不需要显示错误
            }
        }
    }
}
