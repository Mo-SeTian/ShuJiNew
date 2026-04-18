package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookSnapshot
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import com.readtrack.util.PerformanceTrace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

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
                preferencesManager.statsUnit,
                preferencesManager.homeComponentOrder
            ) { books, records, statsUnit, componentOrder ->
                buildHomeUiState(books, records, statsUnit, componentOrder)
            }.collect { state ->
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
                val record = ReadingRecordEntity(
                    bookId = bookId,
                    bookSnapshot = BookSnapshot.from(book, book.status),
                    pagesRead = if (isChapterBased) 0.0 else (newPage - book.currentPage).coerceAtLeast(0.0),
                    fromPage = book.currentPage,
                    toPage = if (isChapterBased) 0.0 else newPage.coerceAtMost(book.totalPages),
                    chaptersRead = if (isChapterBased) (newChapter - book.currentChapter).coerceAtLeast(0) else null,
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
                // 快速记录失败时静默忽略，避免打断首页使用
            }
        }
    }

    fun updateComponentOrder(order: List<String>) {
        viewModelScope.launch {
            preferencesManager.setHomeComponentOrder(order)
        }
    }
}
