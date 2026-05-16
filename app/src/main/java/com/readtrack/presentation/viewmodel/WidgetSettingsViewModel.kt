package com.readtrack.presentation.viewmodel

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.repository.BookRepository
import com.readtrack.widget.ReadingWidgetProvider
import com.readtrack.widget.WidgetUpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WidgetBookBinding(
    val appWidgetId: Int,
    val selectedBookId: Long?,
    val selectedBook: BookEntity?
)

data class WidgetSettingsUiState(
    val widgets: List<WidgetBookBinding> = emptyList(),
    val books: List<BookEntity> = emptyList(),
    val searchQuery: String = ""
) {
    val filteredBooks: List<BookEntity>
        get() {
            val query = searchQuery.trim()
            if (query.isEmpty()) return books
            return books.filter { book ->
                book.title.contains(query, ignoreCase = true) ||
                    book.author?.contains(query, ignoreCase = true) == true
            }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WidgetSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val widgetIds = MutableStateFlow(loadWidgetIds())
    private val searchQuery = MutableStateFlow("")

    val uiState = combine(
        widgetIds.flatMapLatest { ids ->
            preferencesManager.widgetBookIds(ids).map { bindings -> ids to bindings }
        },
        bookRepository.getAllBooks(),
        searchQuery
    ) { (ids, bindings), books, query ->
        val widgets = ids.map { appWidgetId ->
            val selectedBookId = bindings[appWidgetId]
            WidgetBookBinding(
                appWidgetId = appWidgetId,
                selectedBookId = selectedBookId,
                selectedBook = books.firstOrNull { it.id == selectedBookId }
            )
        }
        WidgetSettingsUiState(
            widgets = widgets,
            books = books,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WidgetSettingsUiState()
    )

    fun refreshWidgets() {
        val ids = loadWidgetIds()
        widgetIds.value = ids
        pruneStaleEntries(ids.toSet())
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setWidgetBook(appWidgetId: Int, bookId: Long) {
        viewModelScope.launch {
            preferencesManager.setWidgetBookId(appWidgetId, bookId)
            WidgetUpdateHelper.triggerUpdate(context)
        }
    }

    fun clearWidgetBook(appWidgetId: Int) {
        viewModelScope.launch {
            preferencesManager.clearWidgetBookId(appWidgetId)
            WidgetUpdateHelper.triggerUpdate(context)
        }
    }

    private fun loadWidgetIds(): IntArray {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, ReadingWidgetProvider::class.java)
        return appWidgetManager.getAppWidgetIds(componentName)
    }

    private fun pruneStaleEntries(existingIds: Set<Int>) {
        viewModelScope.launch {
            preferencesManager.pruneStaleWidgetEntries(existingIds)
        }
    }
}
