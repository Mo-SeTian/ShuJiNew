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
    val totalPages: String = "",
    val coverUri: Uri? = null,
    val status: BookStatus = BookStatus.WANT_TO_READ,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddBookViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBookUiState())
    val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, errorMessage = null) }
    }

    fun updateAuthor(author: String) {
        _uiState.update { it.copy(author = author) }
    }

    fun updateTotalPages(pages: String) {
        _uiState.update { it.copy(totalPages = pages, errorMessage = null) }
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

        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "书名不能为空") }
            return
        }

        val pages = state.totalPages.toDoubleOrNull()
        if (pages == null || pages <= 0) {
            _uiState.update { it.copy(errorMessage = "请输入有效的页数（大于0）") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                val book = BookEntity(
                    title = state.title.trim(),
                    author = state.author.trim().takeIf { it.isNotBlank() },
                    totalPages = pages,
                    currentPage = 0.0,
                    coverPath = state.coverUri?.toString(),
                    status = state.status,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    lastReadAt = null
                )
                
                bookRepository.insertBook(book)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSaving = false, 
                        errorMessage = "保存失败: ${e.message ?: "未知错误"}"
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
