package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val todayPages: Double = 0.0,
    val todayChapters: Double = 0.0,
    val streakDays: Int = 0,
    val totalReadingTime: Double = 0.0,
    val totalBooks: Int = 0,
    val readingBooks: Int = 0,
    val finishedBooks: Int = 0,
    val recentBooks: List<BookEntity> = emptyList(),
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
        observeHomeState()
    }

    private fun observeHomeState() {
        viewModelScope.launch {
            combine(
                bookRepository.getAllBooks().catch { emit(emptyList()) },
                recordRepository.getAllRecords().catch { emit(emptyList()) }
            ) { books, records ->
                val chapterBookIds = books.asSequence()
                    .filter { it.progressType == ProgressType.CHAPTER }
                    .map { it.id }
                    .toHashSet()

                val startOfToday = startOfTodayMillis()
                var todayPages = 0.0
                var todayChapters = 0.0
                var totalReadingTime = 0.0

                records.forEach { record ->
                    totalReadingTime += record.pagesRead
                    if (record.date >= startOfToday) {
                        if (record.bookId in chapterBookIds) {
                            todayChapters += record.pagesRead
                        } else {
                            todayPages += record.pagesRead
                        }
                    }
                }

                val statusCounts = BookStatus.entries.associateWith { status ->
                    books.count { it.status == status }
                }

                HomeUiState(
                    todayPages = todayPages,
                    todayChapters = todayChapters,
                    streakDays = calculateStreak(records.map { it.date }),
                    totalReadingTime = totalReadingTime,
                    totalBooks = books.size,
                    readingBooks = books.count { it.status == BookStatus.READING },
                    finishedBooks = books.count { it.status == BookStatus.FINISHED },
                    recentBooks = books.filter { it.status == BookStatus.READING },
                    statusCounts = statusCounts,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun startOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun calculateStreak(dates: List<Long>): Int {
        if (dates.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val sortedDates = dates.map { date ->
            calendar.timeInMillis = date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }.distinct().sortedDescending()

        if (sortedDates.isEmpty()) return 0

        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val today = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = calendar.timeInMillis

        if (sortedDates[0] != today && sortedDates[0] != yesterday) return 0

        var streak = 1
        var currentDate = sortedDates[0]

        for (i in 1 until sortedDates.size) {
            calendar.timeInMillis = currentDate
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val expectedPrevDate = calendar.timeInMillis

            if (sortedDates[i] == expectedPrevDate) {
                streak++
                currentDate = expectedPrevDate
            } else {
                break
            }
        }

        return streak
    }
}