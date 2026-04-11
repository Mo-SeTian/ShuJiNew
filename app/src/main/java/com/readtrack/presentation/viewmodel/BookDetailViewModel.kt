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
    val isLoading: Boolean = true,
    val errorMessage: String? = null
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

    private val _deleteSuccess = MutableSharedFlow<Boolean>()
    val deleteSuccess: SharedFlow<Boolean> = _deleteSuccess.asSharedFlow()

    init {
        loadBookDetail()
    }

    private fun loadBookDetail() {
        viewModelScope.launch {
            try {
                combine(
                    bookRepository.getBookById(bookId).catch { emit(null) },
                    recordRepository.getRecordsByBookId(bookId).catch { emit(emptyList()) }
                ) { book, records ->
                    BookDetailUiState(
                        book = book,
                        readingRecords = records,
                        isLoading = false
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = BookDetailUiState(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun updateStatus(newStatus: BookStatus) {
        viewModelScope.launch {
            try {
                _uiState.value.book?.let { book ->
                    val updatedBook = book.copy(
                        status = newStatus,
                        currentPage = if (newStatus == BookStatus.FINISHED) book.totalPages else book.currentPage,
                        updatedAt = System.currentTimeMillis()
                    )
                    bookRepository.updateBook(updatedBook)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "更新状态失败: ${e.message}") }
            }
        }
    }

    fun addReadingRecord(pagesRead: Double) {
        viewModelScope.launch {
            try {
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

                    val updatedBook = book.copy(
                        currentPage = toPage,
                        lastReadAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    bookRepository.updateBook(updatedBook)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "添加记录失败: ${e.message}") }
            }
        }
    }

    fun deleteBook() {
        viewModelScope.launch {
            try {
                bookRepository.deleteBook(bookId)
                _deleteSuccess.emit(true)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "删除失败: ${e.message}") }
            }
        }
    }
}
