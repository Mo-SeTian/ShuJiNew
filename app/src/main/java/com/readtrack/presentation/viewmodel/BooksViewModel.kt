package com.readtrack.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.BookListCrossRef
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.data.local.entity.TagEntity
import com.readtrack.domain.model.BookSnapshot
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.model.ProgressType
import com.readtrack.domain.repository.BookListRepository
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.TagRepository
import com.readtrack.util.PerformanceTrace
import com.readtrack.widget.WidgetUpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
    val selectedStatuses: Set<BookStatus> = emptySet(),
    val searchQuery: String = "",
    val sortOrder: BookSortOrder = BookSortOrder.default(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val allTags: List<TagEntity> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val bookTagMap: Map<Long, List<TagEntity>> = emptyMap(),
    val allBookLists: List<BookListEntity> = emptyList(),
    val selectedBookListIds: Set<Long> = emptySet()
)

@OptIn(FlowPreview::class)
@HiltViewModel
class BooksViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val tagRepository: TagRepository,
    private val bookListRepository: BookListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BooksUiState())
    val uiState: StateFlow<BooksUiState> = _uiState.asStateFlow()

    private val selectedStatusesFlow = MutableStateFlow<Set<BookStatus>>(emptySet())
    private val searchQueryFlow = MutableStateFlow("")
    private val sortOrderFlow = MutableStateFlow(BookSortOrder.default())
    private val selectedTagIdsFlow = MutableStateFlow<Set<Long>>(emptySet())
    private val taggedBookIdsFlow = MutableStateFlow<Set<Long>>(emptySet())
    private val selectedBookListIdsFlow = MutableStateFlow<Set<Long>>(emptySet())
    private val allCrossRefsFlow = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())
    private var bookListDefaultsApplied = false

    init {
        // 默认状态筛选为「在读」
        selectedStatusesFlow.value = setOf(BookStatus.READING)
        loadBooks()
        loadTags()
        loadBookListData()
    }

    private fun loadBooks() {
        viewModelScope.launch {
            val firstCombine = combine(
                bookRepository.getAllBooks().distinctUntilChanged(),
                selectedStatusesFlow,
                searchQueryFlow
                    .debounce(250)
                    .distinctUntilChanged(),
                sortOrderFlow,
                selectedTagIdsFlow
            ) { books, selectedStatuses, rawQuery, sortOrder, selectedTagIds ->
                FirstCombineResult(books, selectedStatuses, rawQuery, sortOrder, selectedTagIds)
            }
            val secondCombine = combine(
                firstCombine,
                selectedBookListIdsFlow,
                allCrossRefsFlow
            ) { first, selectedBookListIds, crossRefs ->
                Triple(first, selectedBookListIds, crossRefs)
            }
            combine(
                secondCombine,
                taggedBookIdsFlow
            ) { (first, selectedBookListIds, crossRefs), taggedBookIds ->
                PerformanceTrace.measure("books.filter") {
                    var filteredBooks = filterBooks(
                        BooksFilterInput(
                            books = first.books,
                            statuses = first.selectedStatuses,
                            query = first.rawQuery,
                            sortOrder = first.sortOrder
                        )
                    )
                    // 按标签筛选：显示包含任一选中标签的书籍（OR 逻辑）
                    if (first.selectedTagIds.isNotEmpty()) {
                        filteredBooks = filteredBooks.filter { it.id in taggedBookIds }
                    }
                    // 按书单筛选：仅展示属于选中书单的书籍
                    if (selectedBookListIds.isNotEmpty()) {
                        filteredBooks = filteredBooks.filter { book ->
                            val bookListIds = crossRefs[book.id]
                            bookListIds != null && bookListIds.any { it in selectedBookListIds }
                        }
                    }
                    BooksUiState(
                        books = first.books,
                        filteredBooks = filteredBooks,
                        selectedStatuses = first.selectedStatuses,
                        searchQuery = first.rawQuery,
                        sortOrder = first.sortOrder,
                        isLoading = false,
                        errorMessage = null,
                        allTags = _uiState.value.allTags,
                        selectedTagIds = first.selectedTagIds,
                        bookTagMap = _uiState.value.bookTagMap,
                        allBookLists = _uiState.value.allBookLists,
                        selectedBookListIds = selectedBookListIds
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

    private data class FirstCombineResult(
        val books: List<BookEntity>,
        val selectedStatuses: Set<BookStatus>,
        val rawQuery: String,
        val sortOrder: BookSortOrder,
        val selectedTagIds: Set<Long>
    )

    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getAllTags().collect { tags ->
                _uiState.update { it.copy(allTags = tags) }
            }
        }
        viewModelScope.launch {
            tagRepository.getAllTagCrossRefsFlow().collect { crossRefs ->
                val tags = _uiState.value.allTags
                val tagById = tags.associateBy { it.id }
                val map = crossRefs.groupBy({ it.bookId }, { it.tagId })
                    .mapValues { (_, tagIds) -> tagIds.mapNotNull { tagById[it] } }
                _uiState.update { it.copy(bookTagMap = map) }
            }
        }
    }

    private fun loadBookListData() {
        viewModelScope.launch {
            bookListRepository.getAllBookLists().collect { bookLists ->
                _uiState.update { it.copy(allBookLists = bookLists) }
                val allIds = bookLists.map { it.id }.toSet()

                if (!bookListDefaultsApplied && bookLists.isNotEmpty()) {
                    // 默认不筛选书单，展示全部书籍
                    bookListDefaultsApplied = true
                } else {
                    // 删除的书单自动清理
                    val current = selectedBookListIdsFlow.value
                    if (current.any { it !in allIds }) {
                        selectedBookListIdsFlow.value = current.filter { it in allIds }.toSet()
                    }
                }
            }
        }
        viewModelScope.launch {
            bookListRepository.getAllCrossRefs().collect { crossRefs ->
                allCrossRefsFlow.value = crossRefs
                    .groupBy { it.bookId }
                    .mapValues { (_, refs) -> refs.map { it.bookListId }.toSet() }
            }
        }
    }

    fun toggleStatusFilter(status: BookStatus) {
        selectedStatusesFlow.value = if (status in selectedStatusesFlow.value) {
            selectedStatusesFlow.value - status
        } else {
            selectedStatusesFlow.value + status
        }
    }

    fun clearStatusFilters() {
        selectedStatusesFlow.value = emptySet()
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

    fun toggleTagFilter(tagId: Long) {
        val newSet = if (tagId in selectedTagIdsFlow.value) {
            selectedTagIdsFlow.value - tagId
        } else {
            selectedTagIdsFlow.value + tagId
        }
        selectedTagIdsFlow.value = newSet
        viewModelScope.launch {
            taggedBookIdsFlow.value = if (newSet.isNotEmpty()) {
                val bookIdsPerTag = newSet.map { tagRepository.getBookIdsWithTag(it).first().toSet() }
                bookIdsPerTag.reduce { acc, ids -> acc.intersect(ids) }
            } else {
                emptySet()
            }
        }
    }

    fun clearTagFilters() {
        selectedTagIdsFlow.value = emptySet()
        taggedBookIdsFlow.value = emptySet()
    }

    fun toggleBookListFilter(bookListId: Long) {
        val newSet = if (bookListId in selectedBookListIdsFlow.value) {
            selectedBookListIdsFlow.value - bookListId
        } else {
            selectedBookListIdsFlow.value + bookListId
        }
        selectedBookListIdsFlow.value = newSet
    }

    fun clearBookListFilters() {
        selectedBookListIdsFlow.value = emptySet()
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
                WidgetUpdateHelper.triggerUpdate(context)
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
