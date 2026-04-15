package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.repository.BookRepository
import com.readtrack.util.PerformanceTrace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
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

@OptIn(FlowPreview::class)
@HiltViewModel
class BooksViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BooksUiState())
    val uiState: StateFlow<BooksUiState> = _uiState.asStateFlow()

    private val selectedStatusFlow = MutableStateFlow<BookStatus?>(null)
    private val searchQueryFlow = MutableStateFlow("")

    init {
        loadBooks()
    }

    private fun loadBooks() {
        viewModelScope.launch {
            combine(
                bookRepository.getAllBooks().distinctUntilChanged(),
                selectedStatusFlow,
                searchQueryFlow
                    .debounce(250)
                    .distinctUntilChanged()
            ) { books, selectedStatus, rawQuery ->
                PerformanceTrace.measure("books.filter") {
                    val filteredBooks = filterBooks(
                        BooksFilterInput(
                            books = books,
                            status = selectedStatus,
                            query = rawQuery
                        )
                    )
                    BooksUiState(
                        books = books,
                        filteredBooks = filteredBooks,
                        selectedStatus = selectedStatus,
                        searchQuery = rawQuery,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
                .distinctUntilChanged()
                .flowOn(Dispatchers.Default)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "加载失败: ${e.message}")
                    }
                }
                .collect { state ->
                    _uiState.value = state
                    PerformanceTrace.mark(
                        "books.render ready total=${state.books.size} filtered=${state.filteredBooks.size} query='${state.searchQuery}'"
                    )
                }
        }
    }

    fun setStatusFilter(status: BookStatus?) {
        selectedStatusFlow.value = status
        _uiState.update { it.copy(selectedStatus = status) }
    }

    fun setSearchQuery(query: String) {
        searchQueryFlow.value = normalizeSearchQuery(query)
        _uiState.update { it.copy(searchQuery = query) }
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
