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
) {
    val recentRecords: List<ReadingRecordEntity>
        get() = readingRecords.sortedByDescending { it.date }.take(10)
}

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val recordRepository: ReadingRecordRepository,
    private val savedStateHandle: SavedStateHandle
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

    fun updateStatus(status: BookStatus) {
        val currentBook = _uiState.value.book ?: return
        viewModelScope.launch {
            try {
                val updatedBook = currentBook.copy(
                    status = status,
                    updatedAt = System.currentTimeMillis()
                )
                bookRepository.updateBook(updatedBook)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "更新状态失败: ${e.message}") }
            }
        }
    }

    fun addReadingRecord(pagesRead: Double) {
        val currentBook = _uiState.value.book ?: return
        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                val fromPage = currentBook.currentPage
                val toPage = (fromPage + pagesRead).coerceAtMost(currentBook.totalPages)
                
                val record = ReadingRecordEntity(
                    bookId = currentBook.id,
                    pagesRead = pagesRead,
                    fromPage = fromPage,
                    toPage = toPage,
                    date = currentTime
                )
                recordRepository.insertRecord(record)
                
                // Update book's current page and last read time
                val updatedBook = currentBook.copy(
                    currentPage = toPage,
                    lastReadAt = currentTime,
                    updatedAt = currentTime
                )
                bookRepository.updateBook(updatedBook)
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
