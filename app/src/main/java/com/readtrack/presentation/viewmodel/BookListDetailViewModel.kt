package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.domain.repository.BookListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookListDetailUiState(
    val bookList: BookListEntity? = null,
    val books: List<BookEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class BookListDetailViewModel @Inject constructor(
    private val bookListRepository: BookListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookListDetailUiState())
    val uiState: StateFlow<BookListDetailUiState> = _uiState.asStateFlow()

    private var currentBookListId: Long = -1

    fun loadBookList(bookListId: Long) {
        if (bookListId == currentBookListId) return
        currentBookListId = bookListId

        viewModelScope.launch {
            combine(
                bookListRepository.getBookListById(bookListId),
                bookListRepository.getBooksInBookList(bookListId)
            ) { bookList, books ->
                BookListDetailUiState(
                    bookList = bookList,
                    books = books,
                    isLoading = false
                )
            }
                .flowOn(Dispatchers.Default)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "加载失败: ${e.message}") }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun removeBookFromList(bookId: Long) {
        viewModelScope.launch {
            try {
                bookListRepository.removeBookFromList(currentBookListId, bookId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "移除失败: ${e.message}") }
            }
        }
    }

    fun clearBookList() {
        viewModelScope.launch {
            try {
                bookListRepository.clearBookList(currentBookListId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "清空失败: ${e.message}") }
            }
        }
    }

    /**
     * 使用指定书籍的封面
     */
    fun updateCover(bookId: Long) {
        viewModelScope.launch {
            try {
                val bookList = _uiState.value.bookList ?: return@launch
                val book = _uiState.value.books.find { it.id == bookId } ?: return@launch
                bookListRepository.updateBookList(
                    bookList.copy(
                        coverPath = book.coverPath,
                        coverBookId = bookId,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "更新封面失败: ${e.message}") }
            }
        }
    }

    /**
     * 自动选择第一本有封面的书作为封面
     */
    fun updateCoverAuto() {
        viewModelScope.launch {
            try {
                val bookList = _uiState.value.bookList ?: return@launch
                val coverBook = _uiState.value.books.firstOrNull { !it.coverPath.isNullOrBlank() }
                if (coverBook != null) {
                    bookListRepository.updateBookList(
                        bookList.copy(
                            coverPath = coverBook.coverPath,
                            coverBookId = coverBook.id,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "更新封面失败: ${e.message}") }
            }
        }
    }

    /**
     * 移除自定义封面（将使用自动封面逻辑）
     */
    fun removeCover() {
        viewModelScope.launch {
            try {
                val bookList = _uiState.value.bookList ?: return@launch
                bookListRepository.updateBookList(
                    bookList.copy(
                        coverPath = null,
                        coverBookId = null,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "移除封面失败: ${e.message}") }
            }
        }
    }
}
