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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProgressType {
    PAGE,
    CHAPTER
}

data class AddBookUiState(
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val progressType: ProgressType = ProgressType.PAGE,
    val totalPages: String = "",
    val currentPage: String = "",
    val totalChapters: String = "",
    val currentChapter: String = "",
    val description: String = "",
    val coverUri: String? = null,
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

    // Store loaded book for update
    private var loadedBook: BookEntity? = null

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            try {
                bookRepository.getBookById(bookId).collect { book ->
                    book?.let {
                        loadedBook = it
                        _uiState.update { state ->
                            state.copy(
                                title = it.title,
                                author = it.author ?: "",
                                publisher = it.publisher ?: "",
                                progressType = if ((it.totalChapters ?: 0) > 0) ProgressType.CHAPTER else ProgressType.PAGE,
                                totalPages = if ((it.totalChapters ?: 0) == 0) it.totalPages.toInt().toString() else "",
                                currentPage = if ((it.totalChapters ?: 0) == 0) it.currentPage.toInt().toString() else "",
                                totalChapters = (it.totalChapters ?: 0).toString(),
                                currentChapter = it.currentChapter.toString(),
                                description = it.description ?: "",
                                coverUri = it.coverPath,
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

    fun updateProgressType(type: ProgressType) {
        _uiState.update { it.copy(progressType = type) }
    }

    fun updateTotalPages(pages: String) {
        _uiState.update { it.copy(totalPages = pages, errorMessage = null) }
    }

    fun updateCurrentPage(page: String) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun updateTotalChapters(chapters: String) {
        _uiState.update { it.copy(totalChapters = chapters.filter { c -> c.isDigit() }) }
    }

    fun updateCurrentChapter(chapter: String) {
        _uiState.update { it.copy(currentChapter = chapter.filter { c -> c.isDigit() }) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateCoverUri(coverPath: String?) {
        _uiState.update { it.copy(coverUri = coverPath) }
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

        // 直接使用用户选择的封面，不自动生成
        val finalCoverUri = state.coverUri?.takeIf { it.isNotBlank() }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                
                if (state.progressType == ProgressType.PAGE) {
                    val pages = state.totalPages.toDoubleOrNull()
                    if (pages == null || pages <= 0) {
                        _uiState.update { it.copy(isSaving = false, errorMessage = "请输入有效的页数（大于0）") }
                        return@launch
                    }
                    val currentPage = state.currentPage.toDoubleOrNull() ?: 0.0
                    
                    if (state.isEditing && loadedBook != null) {
                        val existingBook = loadedBook!!
                        val updatedBook = existingBook.copy(
                            title = state.title.trim(),
                            author = state.author.trim().takeIf { it.isNotBlank() },
                            publisher = state.publisher.trim().takeIf { it.isNotBlank() },
                            progressType = ProgressType.PAGE,
                            totalPages = pages,
                            currentPage = currentPage.coerceIn(0.0, pages),
                            totalChapters = null,
                            currentChapter = 0,
                            coverPath = finalCoverUri,
                            description = state.description.trim().takeIf { it.isNotBlank() },
                            status = state.status,
                            updatedAt = currentTime
                        )
                        bookRepository.updateBook(updatedBook)
                    } else {
                        val book = BookEntity(
                            title = state.title.trim(),
                            author = state.author.trim().takeIf { it.isNotBlank() },
                            publisher = state.publisher.trim().takeIf { it.isNotBlank() },
                            progressType = ProgressType.PAGE,
                            totalPages = pages,
                            currentPage = currentPage.coerceIn(0.0, pages),
                            totalChapters = null,
                            currentChapter = 0,
                            coverPath = finalCoverUri,
                            description = state.description.trim().takeIf { it.isNotBlank() },
                            status = state.status,
                            createdAt = currentTime,
                            updatedAt = currentTime,
                            lastReadAt = null
                        )
                        bookRepository.insertBook(book)
                    }
                } else {
                    val chapters = state.totalChapters.toIntOrNull()
                    if (chapters == null || chapters <= 0) {
                        _uiState.update { it.copy(isSaving = false, errorMessage = "请输入有效的章节数（大于0）") }
                        return@launch
                    }
                    val currentChapter = state.currentChapter.toIntOrNull() ?: 0
                    
                    if (state.isEditing && loadedBook != null) {
                        val existingBook = loadedBook!!
                        val updatedBook = existingBook.copy(
                            title = state.title.trim(),
                            author = state.author.trim().takeIf { it.isNotBlank() },
                            publisher = state.publisher.trim().takeIf { it.isNotBlank() },
                            progressType = ProgressType.CHAPTER,
                            totalPages = 0.0,
                            currentPage = 0.0,
                            totalChapters = chapters,
                            currentChapter = currentChapter.coerceIn(0, chapters),
                            coverPath = finalCoverUri,
                            description = state.description.trim().takeIf { it.isNotBlank() },
                            status = state.status,
                            updatedAt = currentTime
                        )
                        bookRepository.updateBook(updatedBook)
                    } else {
                        val book = BookEntity(
                            title = state.title.trim(),
                            author = state.author.trim().takeIf { it.isNotBlank() },
                            publisher = state.publisher.trim().takeIf { it.isNotBlank() },
                            progressType = ProgressType.CHAPTER,
                            totalPages = 0.0,
                            currentPage = 0.0,
                            totalChapters = chapters,
                            currentChapter = currentChapter.coerceIn(0, chapters),
                            coverPath = finalCoverUri,
                            description = state.description.trim().takeIf { it.isNotBlank() },
                            status = state.status,
                            createdAt = currentTime,
                            updatedAt = currentTime,
                            lastReadAt = null
                        )
                        bookRepository.insertBook(book)
                    }
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
        loadedBook = null
        _uiState.value = AddBookUiState()
    }
}
