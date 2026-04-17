package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.remote.BingImageResult
import com.readtrack.data.remote.BingImageSearchService
import com.readtrack.data.remote.BookSearchResult
import com.readtrack.data.remote.DoubanSearchService
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.repository.BookRepository
import com.readtrack.util.PerformanceTrace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
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
    val errorMessage: String? = null,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<BookSearchResult> = emptyList(),
    val searchError: String? = null,
    val showSearchDialog: Boolean = false,
    val doubanCookie: String = "",
    // 网络图片搜索状态
    val showImageSearchDialog: Boolean = false,
    val imageSearchQuery: String = "",
    val imageSearchResults: List<BingImageResult> = emptyList(),
    val isImageSearching: Boolean = false,
    val imageSearchError: String? = null,
    val selectedImageUrl: String? = null  // 长按预览时用的 URL
)

@HiltViewModel
class AddBookViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val doubanSearchService: DoubanSearchService,
    private val bingImageSearchService: BingImageSearchService,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBookUiState())
    val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

    private var loadedBook: BookEntity? = null
    private var searchJob: Job? = null
    private var imageSearchJob: Job? = null
    private var lastSearchQuery: String = ""

    init {
        viewModelScope.launch {
            val cookie = preferencesManager.doubanCookie.first()
            _uiState.update { it.copy(doubanCookie = cookie) }
        }
    }

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
                _uiState.update { it.copy(errorMessage = "加载失败: ${e.message}") }
            }
        }
    }

    fun updateTitle(title: String) = _uiState.update { it.copy(title = title) }
    fun updateAuthor(author: String) = _uiState.update { it.copy(author = author) }
    fun updatePublisher(publisher: String) = _uiState.update { it.copy(publisher = publisher) }
    fun updateProgressType(progressType: ProgressType) = _uiState.update { it.copy(progressType = progressType) }
    fun updateTotalPages(pages: String) = _uiState.update { it.copy(totalPages = pages) }
    fun updateCurrentPage(page: String) = _uiState.update { it.copy(currentPage = page) }
    fun updateTotalChapters(chapters: String) = _uiState.update { it.copy(totalChapters = chapters) }
    fun updateCurrentChapter(chapter: String) = _uiState.update { it.copy(currentChapter = chapter) }
    fun updateDescription(description: String) = _uiState.update { it.copy(description = description) }
    fun updateCoverUri(uri: String?) = _uiState.update { it.copy(coverUri = uri) }
    fun updateStatus(status: BookStatus) = _uiState.update { it.copy(status = status) }

    fun showSearchDialog() {
        searchJob?.cancel()
        lastSearchQuery = ""
        _uiState.update {
            it.copy(
                showSearchDialog = true,
                searchQuery = "",
                searchResults = emptyList(),
                searchError = null,
                isSearching = false
            )
        }
    }

    fun hideSearchDialog() {
        searchJob?.cancel()
        lastSearchQuery = ""
        _uiState.update {
            it.copy(
                showSearchDialog = false,
                searchQuery = "",
                searchResults = emptyList(),
                searchError = null,
                isSearching = false
            )
        }
    }

    // ─── 网络图片搜索（替代原网络导入URL输入） ───

    fun showImageSearchDialog(initialQuery: String = "") {
        imageSearchJob?.cancel()
        _uiState.update {
            it.copy(
                showImageSearchDialog = true,
                imageSearchQuery = initialQuery,
                imageSearchResults = emptyList(),
                imageSearchError = null,
                isImageSearching = false,
                selectedImageUrl = null
            )
        }
        // 如果已有初始搜索词（如书名），自动搜索
        if (initialQuery.isNotBlank()) {
            searchImages(initialQuery)
        }
    }

    fun hideImageSearchDialog() {
        imageSearchJob?.cancel()
        _uiState.update {
            it.copy(
                showImageSearchDialog = false,
                imageSearchQuery = "",
                imageSearchResults = emptyList(),
                imageSearchError = null,
                isImageSearching = false,
                selectedImageUrl = null
            )
        }
    }

    fun updateImageSearchQuery(query: String) {
        _uiState.update { it.copy(imageSearchQuery = query) }
        imageSearchJob?.cancel()

        if (query.length >= 2) {
            imageSearchJob = viewModelScope.launch {
                delay(400)
                searchImages(query)
            }
        } else {
            _uiState.update {
                it.copy(
                    imageSearchResults = emptyList(),
                    imageSearchError = null,
                    isImageSearching = false
                )
            }
        }
    }

    private fun searchImages(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isImageSearching = true, imageSearchError = null) }

            bingImageSearchService.searchImages(query)
                .onSuccess { results ->
                    _uiState.update {
                        it.copy(
                            isImageSearching = false,
                            imageSearchResults = results.filter { img -> img.isValid() }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isImageSearching = false,
                            imageSearchError = "图片搜索失败: ${error.message}"
                        )
                    }
                }
        }
    }

    /**
     * 长按预览图片
     */
    fun previewImage(imageUrl: String) {
        _uiState.update { it.copy(selectedImageUrl = imageUrl) }
    }

    /**
     * 关闭图片预览
     */
    fun clearPreview() {
        _uiState.update { it.copy(selectedImageUrl = null) }
    }

    /**
     * 选择图片作为封面（从搜索结果中选择）
     */
    fun selectImage(result: BingImageResult) {
        // 优先用高清大图，其次缩略图
        val url = result.fullUrl.ifBlank { result.thumbnailUrl }
        updateCoverUri(url)
        hideImageSearchDialog()
    }

    fun updateSearchQuery(query: String) {
        val normalizedQuery = normalizeSearchQuery(query)
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (normalizedQuery.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(350)
                if (normalizedQuery != lastSearchQuery) {
                    searchBooks(normalizedQuery)
                }
            }
        } else {
            lastSearchQuery = ""
            _uiState.update { it.copy(searchResults = emptyList(), searchError = null, isSearching = false) }
        }
    }

    fun searchBooks(query: String) {
        val normalizedQuery = normalizeSearchQuery(query)
        if (normalizedQuery.isBlank()) return

        lastSearchQuery = normalizedQuery

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null) }

            val latestCookie = preferencesManager.doubanCookie.first()
            _uiState.update { it.copy(doubanCookie = latestCookie) }

            PerformanceTrace.measure("cover.search:$normalizedQuery") {
                doubanSearchService.searchBooks(normalizedQuery, latestCookie)
                    .onSuccess { results ->
                        if (normalizedQuery == lastSearchQuery) {
                            _uiState.update {
                                it.copy(isSearching = false, searchResults = results)
                            }
                        }
                    }
                    .onFailure { error ->
                        if (normalizedQuery == lastSearchQuery) {
                            _uiState.update {
                                it.copy(
                                    isSearching = false,
                                    searchError = "搜索失败: ${error.message}"
                                )
                            }
                        }
                    }
            }
        }
    }

    fun fillFromSearchResult(result: BookSearchResult) {
        searchJob?.cancel()
        lastSearchQuery = ""
        _uiState.update { state ->
            state.copy(
                title = result.title,
                author = result.author ?: "",
                publisher = result.publisher ?: "",
                coverUri = result.coverUrl,
                description = result.description ?: "",
                totalPages = result.pageCount?.toString() ?: "",
                showSearchDialog = false,
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false,
                searchError = null
            )
        }
    }

    fun saveBook() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入书名") }
            return
        }

        val totalPages = if (state.progressType == ProgressType.PAGE) state.totalPages.toDoubleOrNull() ?: 0.0 else 0.0
        val currentPage = if (state.progressType == ProgressType.PAGE) state.currentPage.toDoubleOrNull() ?: 0.0 else 0.0
        val totalChapters = if (state.progressType == ProgressType.CHAPTER) state.totalChapters.toIntOrNull() ?: 0 else 0
        val currentChapter = if (state.progressType == ProgressType.CHAPTER) state.currentChapter.toIntOrNull() ?: 0 else 0

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val now = System.currentTimeMillis()
                if (state.isEditing && loadedBook != null) {
                    val updatedBook = loadedBook!!.copy(
                        title = state.title,
                        author = state.author.ifBlank { null },
                        publisher = state.publisher.ifBlank { null },
                        progressType = state.progressType,
                        totalPages = totalPages,
                        currentPage = currentPage,
                        totalChapters = if (totalChapters > 0) totalChapters else null,
                        currentChapter = currentChapter,
                        description = state.description.ifBlank { null },
                        coverPath = state.coverUri,
                        status = state.status,
                        updatedAt = now
                    )
                    bookRepository.updateBook(updatedBook)
                } else {
                    val newBook = BookEntity(
                        title = state.title,
                        author = state.author.ifBlank { null },
                        publisher = state.publisher.ifBlank { null },
                        progressType = state.progressType,
                        totalPages = totalPages,
                        currentPage = currentPage,
                        totalChapters = if (totalChapters > 0) totalChapters else null,
                        currentChapter = currentChapter,
                        description = state.description.ifBlank { null },
                        coverPath = state.coverUri,
                        status = state.status,
                        createdAt = now,
                        updatedAt = now,
                        lastReadAt = null
                    )
                    bookRepository.insertBookWithStatus(newBook)
                }
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "保存失败: ${e.message}") }
            }
        }
    }
    
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    
    fun resetState() {
        loadedBook = null
        _uiState.value = AddBookUiState()
    }
}