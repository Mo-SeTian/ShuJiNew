package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookSnapshot
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.viewmodel.ProgressType
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
    val sortOrder: BookSortOrder = BookSortOrder.default(),
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
    private val sortOrderFlow = MutableStateFlow(BookSortOrder.default())

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
                    .distinctUntilChanged(),
                sortOrderFlow
            ) { books, selectedStatus, rawQuery, sortOrder ->
                PerformanceTrace.measure("books.filter") {
                    val filteredBooks = filterBooks(
                        BooksFilterInput(
                            books = books,
                            status = selectedStatus,
                            query = rawQuery,
                            sortOrder = sortOrder
                        )
                    )
                    BooksUiState(
                        books = books,
                        filteredBooks = filteredBooks,
                        selectedStatus = selectedStatus,
                        searchQuery = rawQuery,
                        sortOrder = sortOrder,
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
                        "books.render ready total=${state.books.size} filtered=${state.filteredBooks.size} query='${state.searchQuery}' sort=${state.sortOrder.name}"
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
        // 不在这里更新 searchQuery！combine 里用 searchQueryFlow.debounce(250).distinctUntilChanged()
        // 来驱动 filteredBooks 和 searchQuery 的更新，避免每次按键都触发 recomposition
    }

    fun setSortOrder(sortOrder: BookSortOrder) {
        sortOrderFlow.value = sortOrder
        _uiState.update { it.copy(sortOrder = sortOrder) }
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

    /**
     * 快速记录阅读进度（从书籍列表直接记录）
     */
    fun quickRecord(bookId: Long, newPage: Double, newChapter: Int) {
        viewModelScope.launch {
            try {
                val book = _uiState.value.books.find { it.id == bookId } ?: return@launch
                val isChapterBased = book.progressType == ProgressType.CHAPTER
                val (fromPage, fromChapter) = if (isChapterBased) {
                    book.currentChapter.toDouble() to book.currentChapter
                } else {
                    book.currentPage to 0
                }
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
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "记录失败: ${e.message}") }
            }
        }
    }

    /**
     * 快速标记书籍为读完
     */
    fun quickFinish(bookId: Long) {
        viewModelScope.launch {
            try {
                bookRepository.updateBookStatus(bookId, BookStatus.FINISHED, RecordType.STATUS_FINISHED)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "标记失败: ${e.message}") }
            }
        }
    }

    /**
     * 根据 ID 获取书籍（用于弹窗显示书籍信息）
     */
    fun getBookById(bookId: Long): BookEntity? {
        return _uiState.value.books.find { it.id == bookId }
    }
}
