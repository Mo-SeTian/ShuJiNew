package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import com.readtrack.presentation.viewmodel.ProgressType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
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
        loadBooks()
        loadReadingBooks()
        loadReadingRecords()
    }

    private fun loadBooks() {
        viewModelScope.launch {
            bookRepository.getAllBooks()
                .catch { emit(emptyList()) }
                .collect { books ->
                    val statusCounts = BookStatus.entries.associateWith { status ->
                        books.count { it.status == status }
                    }
                    _uiState.update { 
                        it.copy(
                            statusCounts = statusCounts,
                            totalBooks = books.size,
                            readingBooks = books.count { it.status == BookStatus.READING },
                            finishedBooks = books.count { it.status == BookStatus.FINISHED },
                            isLoading = false
                        ) 
                    }
                }
        }
    }

    private fun loadReadingBooks() {
        viewModelScope.launch {
            bookRepository.getBooksByStatus(BookStatus.READING)
                .catch { emit(emptyList()) }
                .collect { books ->
                    _uiState.update { it.copy(recentBooks = books) }
                }
        }
    }

    private fun loadReadingRecords() {
        viewModelScope.launch {
            // Get all books for reference
            bookRepository.getAllBooks()
                .catch { emit(emptyList()) }
                .collect { books ->
                    val booksMap = books.associateBy { it.id }
                    val chapterBooks = booksMap.filterValues { it.progressType == ProgressType.CHAPTER }.keys
                    
                    // Get all records and calculate stats
                    recordRepository.getAllRecords()
                        .catch { emit(emptyList()) }
                        .collect { records ->
                            val now = System.currentTimeMillis()
                            val startOfToday = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            
                            // Today's stats
                            val todayRecords = records.filter { it.date >= startOfToday }
                            val todayPages = todayRecords.filter { it.bookId !in chapterBooks }.sumOf { it.pagesRead }
                            val todayChapters = todayRecords.filter { it.bookId in chapterBooks }.sumOf { it.pagesRead }
                            
                            // Calculate streak and total time
                            val streak = calculateStreak(records.map { it.date })
                            val totalTime = records.sumOf { it.pagesRead }
                            
                            _uiState.update { 
                                it.copy(
                                    todayPages = todayPages,
                                    todayChapters = todayChapters,
                                    streakDays = streak,
                                    totalReadingTime = totalTime
                                ) 
                            }
                        }
                }
        }
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
        
        // Check if the most recent read is today or yesterday
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
