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
    val isLoading: Boolean = true,
    val errorMessage: String? = null
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
            try {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.timeInMillis

                combine(
                    recordRepository.getTotalPagesReadOnDate(startOfDay, endOfDay)
                        .catch { emit(0.0) },
                    bookRepository.getBooksByStatus(BookStatus.READING)
                        .catch { emit(emptyList()) },
                    bookRepository.getAllBooks()
                        .catch { emit(emptyList()) },
                    recordRepository.getAllRecords()
                        .catch { emit(emptyList()) }
                ) { todayPages, readingBooks, allBooks, allRecords ->
                    val statusCounts = BookStatus.entries.associateWith { status ->
                        allBooks.count { it.status == status }
                    }
                    
                    val streak = calculateReadingStreak(allRecords.map { it.date })
                    
                    HomeUiState(
                        todayPages = todayPages ?: 0.0,
                        streakDays = streak,
                        readingBooks = readingBooks,
                        statusCounts = statusCounts,
                        isLoading = false
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState(isLoading = false, errorMessage = e.message)
            }
        }
    }

    private fun calculateReadingStreak(recordDates: List<Long>): Int {
        if (recordDates.isEmpty()) return 0
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val today = calendar.timeInMillis
        val yesterday = today - 24 * 60 * 60 * 1000
        
        val daysWithRecords = recordDates.map { date ->
            calendar.timeInMillis = date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }.distinct().sortedDescending()
        
        if (daysWithRecords.isEmpty()) return 0
        
        val mostRecentDay = daysWithRecords.first()
        if (mostRecentDay < yesterday) return 0
        
        var streak = 0
        var currentDay = if (mostRecentDay == today) today else yesterday
        
        for (day in daysWithRecords) {
            if (day == currentDay) {
                streak++
                currentDay -= 24 * 60 * 60 * 1000
            } else if (day < currentDay) {
                break
            }
        }
        
        return streak
    }
}
