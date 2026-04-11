package com.readtrack.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddBookUiState(
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val totalPages: String = "",
    val currentPage: String = "",
    val description: String = "",
    val coverUri: Uri? = null,
    val status: BookStatus = BookStatus.WANT_TO_READ,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isEditing: Boolean = false,
    val editingBookId: Long? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AddBookViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBookUiState())
    val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            try {
                bookRepository.getBookById(bookId).collect { book ->
                    book?.let {
                        _uiState.update { state ->
                            state.copy(
                                title = it.title,
                                author = it.author ?: "",
                                publisher = it.publisher ?: "",
                                totalPages = it.totalPages.toInt().toString(),
                                currentPage = it.currentPage.toInt().toString(),
                                description = it.description ?: "",
                                coverUri = it.coverPath?.let { path -> Uri.parse(path) },
                                status = it.status,
                                isEditing = true,
                                editingBookId = bookId
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "加载书籍失败: ${e.message}") }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, errorMessage = null) }
    }

    fun updateAuthor(author: String) {
        _uiState.update { it.copy(author = author) }
    }

    fun updatePublisher(publisher: String) {
        _uiState.update { it.copy(publisher = publisher) }
    }

    fun updateTotalPages(pages: String) {
        _uiState.update { it.copy(totalPages = pages, errorMessage = null) }
    }

    fun updateCurrentPage(page: String) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateCoverUri(uri: Uri?) {
        _uiState.update { it.copy(coverUri = uri) }
    }

    fun updateStatus(status: BookStatus) {
        _uiState.update { it.copy(status = status) }
    }

    fun saveBook() {
        val state = _uiState.value

        // Validation
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入书名") }
            return
        }

        val pages = state.totalPages.toDoubleOrNull()
        if (pages == null || pages <= 0) {
            _uiState.update { it.copy(errorMessage = "请输入有效的页数（大于0）") }
            return
        }

        val currentPage = state.currentPage.toDoubleOrNull() ?: 0.0

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                
                if (state.isEditing && state.editingBookId != null) {
                    // Update existing book
                    val existingBook = bookRepository.getBookByIdSync(state.editingBookId)
                    existingBook?.let { existing ->
                        val updatedBook = existing.copy(
                            title = state.title.trim(),
                            author = state.author.trim().takeIf { it.isNotBlank() },
                            publisher = state.publisher.trim().takeIf { it.isNotBlank() },
                            totalPages = pages,
                            currentPage = currentPage.coerceIn(0.0, pages),
                            coverPath = state.coverUri?.toString(),
                            description = state.description.trim().takeIf { it.isNotBlank() },
                            status = state.status,
                            updatedAt = currentTime
                        )
                        bookRepository.updateBook(updatedBook)
                    }
                } else {
                    // Create new book
                    val book = BookEntity(
                        title = state.title.trim(),
                        author = state.author.trim().takeIf { it.isNotBlank() },
                        publisher = state.publisher.trim().takeIf { it.isNotBlank() },
                        totalPages = pages,
                        currentPage = currentPage.coerceIn(0.0, pages),
                        coverPath = state.coverUri?.toString(),
                        description = state.description.trim().takeIf { it.isNotBlank() },
                        status = state.status,
                        createdAt = currentTime,
                        updatedAt = currentTime,
                        lastReadAt = null
                    )
                    bookRepository.insertBook(book)
                }
                
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSaving = false, 
                        errorMessage = "保存失败: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun resetState() {
        _uiState.value = AddBookUiState()
    }
}
