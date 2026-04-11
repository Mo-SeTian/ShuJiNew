package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BooksUiState(
    val books: List<BookEntity> = emptyList(),
    val filteredBooks: List<BookEntity> = emptyList(),
    val selectedStatus: BookStatus? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class BooksViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BooksUiState())
    val uiState: StateFlow<BooksUiState> = _uiState.asStateFlow()

    init {
        loadBooks()
    }

    private fun loadBooks() {
        viewModelScope.launch {
            bookRepository.getAllBooks()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "加载失败: ${e.message}") }
                }
                .collect { books ->
                    _uiState.update { state ->
                        state.copy(
                            books = books,
                            filteredBooks = filterBooks(books, state.selectedStatus, state.searchQuery),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun setStatusFilter(status: BookStatus?) {
        _uiState.update { state ->
            state.copy(
                selectedStatus = status,
                filteredBooks = filterBooks(state.books, status, state.searchQuery)
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredBooks = filterBooks(state.books, state.selectedStatus, query)
            )
        }
    }

    private fun filterBooks(
        books: List<BookEntity>,
        status: BookStatus?,
        query: String
    ): List<BookEntity> {
        return books.filter { book ->
            val matchesStatus = status == null || book.status == status
            val matchesQuery = query.isBlank() ||
                    book.title.contains(query, ignoreCase = true) ||
                    (book.author?.contains(query, ignoreCase = true) == true)
            matchesStatus && matchesQuery
        }
    }

    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            try {
                bookRepository.deleteBook(bookId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "删除失败: ${e.message}") }
            }
        }
    }
}
