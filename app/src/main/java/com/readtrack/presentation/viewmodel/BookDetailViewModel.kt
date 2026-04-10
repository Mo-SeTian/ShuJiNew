package com.readtrack.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailUiState(
    val book: BookEntity? = null,
    val readingRecords: List<ReadingRecordEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val recordRepository: ReadingRecordRepository
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: 0L

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    init {
        loadBookDetail()
    }

    private fun loadBookDetail() {
        viewModelScope.launch {
            combine(
                bookRepository.getBookById(bookId),
                recordRepository.getRecordsByBookId(bookId)
            ) { book, records ->
                BookDetailUiState(
                    book = book,
                    readingRecords = records,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun updateStatus(newStatus: BookStatus) {
        viewModelScope.launch {
            _uiState.value.book?.let { book ->
                val updatedBook = book.copy(
                    status = newStatus,
                    currentPage = if (newStatus == BookStatus.FINISHED) book.totalPages else book.currentPage,
                    updatedAt = System.currentTimeMillis()
                )
                bookRepository.updateBook(updatedBook)
            }
        }
    }

    fun addReadingRecord(pagesRead: Double) {
        viewModelScope.launch {
            _uiState.value.book?.let { book ->
                val fromPage = book.currentPage
                val toPage = (book.currentPage + pagesRead).coerceAtMost(book.totalPages)
                val actualPagesRead = toPage - fromPage

                val record = ReadingRecordEntity(
                    bookId = book.id,
                    pagesRead = actualPagesRead,
                    fromPage = fromPage,
                    toPage = toPage,
                    date = System.currentTimeMillis()
                )
                recordRepository.insertRecord(record)

                // Update book's current page
                val updatedBook = book.copy(
                    currentPage = toPage,
                    lastReadAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                bookRepository.updateBook(updatedBook)
            }
        }
    }

    fun deleteBook() {
        viewModelScope.launch {
            bookRepository.deleteBook(bookId)
        }
    }
}
