package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class HomeUiState(
    val todayPages: Double = 0.0,
    val streakDays: Int = 0,
    val readingBooks: List<BookEntity> = emptyList(),
    val statusCounts: Map<BookStatus, Int> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val recordRepository: ReadingRecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Get today's reading pages
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = calendar.timeInMillis

            combine(
                recordRepository.getTotalPagesReadOnDate(startOfDay, endOfDay),
                bookRepository.getBooksByStatus(BookStatus.READING),
                bookRepository.getAllBooks()
            ) { todayPages, readingBooks, allBooks ->
                val statusCounts = BookStatus.entries.associateWith { status ->
                    allBooks.count { it.status == status }
                }
                HomeUiState(
                    todayPages = todayPages ?: 0.0,
                    streakDays = calculateStreak(),
                    readingBooks = readingBooks,
                    statusCounts = statusCounts,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun calculateStreak(): Int {
        // Simplified streak calculation - counts consecutive days with reading
        return 1 // Placeholder
    }
}
