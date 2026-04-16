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

data class AddToBookListUiState(
    val allBookLists: List<BookListEntity> = emptyList(),
    /** key = bookListId, value = true if ALL current books are in this list */
    val bookIdsInList: Map<Long, Boolean> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AddToBookListViewModel @Inject constructor(
    private val bookListRepository: BookListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddToBookListUiState())
    val uiState: StateFlow<AddToBookListUiState> = _uiState.asStateFlow()

    /** 当前选中的书籍ID列表（批量模式），单书时为 [bookId] */
    private var currentBookIds: List<Long> = emptyList()

    private val _loadTrigger = MutableStateFlow(0L)

    /**
     * 加载多个书籍的书单状态（批量模式）
     */
    fun loadBookListsForBooks(bookIds: List<Long>) {
        currentBookIds = bookIds
        viewModelScope.launch {
            _loadTrigger.update { it + 1 }
            _loadTrigger
                .flatMapLatest {
                    bookListRepository.getAllBookLists()
                }
                .flatMapLatest { allLists ->
                    if (allLists.isEmpty()) {
                        flowOf(AddToBookListUiState(allBookLists = emptyList(), isLoading = false))
                    } else {
                        // For each list, check membership
                        val flows = allLists.map { list ->
                            bookListRepository.getBooksInBookList(list.id).map { booksInList ->
                                list to bookIds.all { bookId -> booksInList.any { it.id == bookId } }
                            }
                        }
                        @Suppress("UNCHECKED_CAST")
                        combine(flows) { results ->
                            val pairs = results.map { it as Pair<com.readtrack.data.local.entity.BookListEntity, Boolean> }
                            val membership = pairs.associate { it.first.id to it.second }
                            AddToBookListUiState(
                                allBookLists = allLists,
                                bookIdsInList = membership,
                                isLoading = false
                            )
                        }
                    }
                }
                .flowOn(Dispatchers.Default)
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    /**
     * 加载单个书籍的书单状态（向后兼容）
     */
    fun loadBookListsForBook(bookId: Long) {
        loadBookListsForBooks(listOf(bookId))
    }

    /**
     * 切换书单状态（批量模式：用 currentBookIds）
     */
    fun toggleBookListMembership(bookListId: Long) {
        viewModelScope.launch {
            val isCurrentlyInList = _uiState.value.bookIdsInList[bookListId] == true
            if (isCurrentlyInList) {
                currentBookIds.forEach { bookId ->
                    bookListRepository.removeBookFromList(bookListId, bookId)
                }
            } else {
                bookListRepository.addBooksToList(bookListId, currentBookIds)
            }
        }
    }

    /**
     * 切换书单状态（单书模式）
     */
    fun toggleBookListMembership(bookId: Long, bookListId: Long) {
        currentBookIds = listOf(bookId)
        viewModelScope.launch {
            val isCurrentlyInList = _uiState.value.bookIdsInList[bookListId] == true
            if (isCurrentlyInList) {
                bookListRepository.removeBookFromList(bookListId, bookId)
            } else {
                bookListRepository.addBookToList(bookListId, bookId)
            }
        }
    }

    /**
     * 创建新书单并加入多本书（批量模式）
     */
    fun createBookListAndAddBooks(name: String, description: String?) {
        viewModelScope.launch {
            try {
                val newListId = bookListRepository.createBookList(name, description)
                bookListRepository.addBooksToList(newListId, currentBookIds)
            } catch (e: Exception) {
                // silently fail
            }
        }
    }

    /**
     * 创建新书单并加入单本书（向后兼容）
     */
    fun createBookListAndAddBook(bookId: Long, name: String, description: String?) {
        currentBookIds = listOf(bookId)
        createBookListAndAddBooks(name, description)
    }
}
