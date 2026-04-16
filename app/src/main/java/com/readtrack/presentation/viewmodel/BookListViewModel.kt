package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.domain.repository.BookListRepository
import com.readtrack.util.PerformanceTrace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookListUiState(
    val bookLists: List<BookListEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class BookListDetailUiState(
    val bookList: BookListEntity? = null,
    val books: List<BookEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class BookListViewModel @Inject constructor(
    private val bookListRepository: BookListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookListUiState())
    val uiState: StateFlow<BookListUiState> = _uiState.asStateFlow()

    init {
        loadBookLists()
    }

    private fun loadBookLists() {
        viewModelScope.launch {
            bookListRepository.getAllBookLists()
                .flowOn(Dispatchers.Default)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "加载失败: ${e.message}") }
                }
                .collect { bookLists ->
                    _uiState.update { it.copy(bookLists = bookLists, isLoading = false) }
                }
        }
    }

    fun createBookList(name: String, description: String? = null) {
        viewModelScope.launch {
            try {
                bookListRepository.createBookList(name, description)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "创建失败: ${e.message}") }
            }
        }
    }

    fun updateBookList(bookList: BookListEntity) {
        viewModelScope.launch {
            try {
                bookListRepository.updateBookList(bookList)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "更新失败: ${e.message}") }
            }
        }
    }

    fun deleteBookList(id: Long) {
        viewModelScope.launch {
            try {
                bookListRepository.deleteBookList(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "删除失败: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

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
}
