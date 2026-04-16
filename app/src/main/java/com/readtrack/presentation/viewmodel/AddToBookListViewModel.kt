package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.domain.repository.BookListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddToBookListUiState(
    val allBookLists: List<BookListEntity> = emptyList(),
    /** key = bookListId, value = true if the current book is in this list */
    val bookIdsInList: Map<Long, Boolean> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AddToBookListViewModel @Inject constructor(
    private val bookListRepository: BookListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddToBookListUiState())
    val uiState: StateFlow<AddToBookListUiState> = _uiState.asStateFlow()

    private var currentBookId: Long = -1

    fun loadBookListsForBook(bookId: Long) {
        currentBookId = bookId
        viewModelScope.launch {
            combine(
                bookListRepository.getAllBookLists(),
                bookListRepository.getBookListsForBook(bookId)
            ) { allLists, listsContainingBook ->
                val membershipMap = allLists.associate { it.id to (listsContainingBook.any { bl -> bl.id == it.id }) }
                AddToBookListUiState(
                    allBookLists = allLists,
                    bookIdsInList = membershipMap,
                    isLoading = false
                )
            }
                .flowOn(Dispatchers.Default)
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun toggleBookListMembership(bookId: Long, bookListId: Long) {
        viewModelScope.launch {
            val isCurrentlyInList = _uiState.value.bookIdsInList[bookListId] == true
            if (isCurrentlyInList) {
                bookListRepository.removeBookFromList(bookListId, bookId)
            } else {
                bookListRepository.addBookToList(bookListId, bookId)
            }
            // The Flow will automatically update the UI state
        }
    }

    fun createBookListAndAddBook(bookId: Long, name: String, description: String?) {
        viewModelScope.launch {
            try {
                val newListId = bookListRepository.createBookList(name, description)
                bookListRepository.addBookToList(newListId, bookId)
            } catch (e: Exception) {
                // silently fail - dialog will refresh on next interaction
            }
        }
    }
}
