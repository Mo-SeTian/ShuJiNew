package com.readtrack.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookSnapshot
import com.readtrack.domain.model.ProgressType
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import com.readtrack.util.PerformanceTrace
import com.readtrack.widget.WidgetUpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val recordRepository: ReadingRecordRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val TAG = "HomeViewModel"

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
                preferencesManager.statsUnit,
                preferencesManager.homeComponentOrder
            ) { books, records, statsUnit, componentOrder ->
                buildHomeUiState(books, records, statsUnit, componentOrder)
            }
                .debounce(300L)
                .collect { state ->
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
                val now = System.currentTimeMillis()
                val record: ReadingRecordEntity
                val updatedBook: BookEntity
                if (isChapterBased) {
                    val maxChapter = book.totalChapters ?: 0
                    val fromChapter = book.currentChapter
                    // 防止进度回退，同时限制在总章数内（未设置总章数则不设上限）
                    val toChapter = if (maxChapter > 0) {
                        newChapter.coerceIn(fromChapter, maxChapter)
                    } else {
                        newChapter.coerceAtLeast(fromChapter)
                    }
                    val chaptersRead = toChapter - fromChapter
                    record = ReadingRecordEntity(
                        bookId = bookId,
                        bookSnapshot = BookSnapshot.from(book, book.status),
                        pagesRead = chaptersRead.toDouble(),
                        fromPage = fromChapter.toDouble(),
                        toPage = toChapter.toDouble(),
                        chaptersRead = chaptersRead,
                        recordType = RecordType.NORMAL,
                        date = now
                    )
                    updatedBook = book.copy(
                        currentChapter = toChapter,
                        lastReadAt = now,
                        updatedAt = now
                    )
                } else {
                    val fromPage = book.currentPage
                    // 防止进度回退，同时限制在总页数内
                    val toPage = newPage.coerceIn(fromPage, book.totalPages)
                    record = ReadingRecordEntity(
                        bookId = bookId,
                        bookSnapshot = BookSnapshot.from(book, book.status),
                        pagesRead = toPage - fromPage,
                        fromPage = fromPage,
                        toPage = toPage,
                        recordType = RecordType.NORMAL,
                        date = now
                    )
                    updatedBook = book.copy(
                        currentPage = toPage,
                        lastReadAt = now,
                        updatedAt = now
                    )
                }
                bookRepository.insertRecordAndUpdateBook(record, updatedBook)
                WidgetUpdateHelper.triggerUpdate(context)
            } catch (e: Exception) {
                Log.e(TAG, "快速记录失败: bookId=$bookId, page=$newPage", e)
            }
        }
    }

    fun updateComponentOrder(order: List<String>) {
        viewModelScope.launch {
            preferencesManager.setHomeComponentOrder(order)
        }
    }
}
