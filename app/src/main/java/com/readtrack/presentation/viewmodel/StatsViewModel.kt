package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class StatsUiState(
    val totalBooks: Int = 0,
    val booksByStatus: Map<BookStatus, Int> = emptyMap(),
    val totalPagesRead: Double = 0.0,
    val totalChaptersRead: Double = 0.0,
    val todayPages: Double = 0.0,
    val todayChapters: Double = 0.0,
    val weekPages: Double = 0.0,
    val weekChapters: Double = 0.0,
    val monthPages: Double = 0.0,
    val monthChapters: Double = 0.0,
    val averagePagesPerDay: Double = 0.0,
    val weeklyReadingData: List<DailyReading> = emptyList(),
    val recentRecords: List<ReadingRecordEntity> = emptyList(),
    val recentRecordsWithBooks: List<RecordWithBook> = emptyList(),
    val isLoading: Boolean = true
)

data class DailyReading(
    val date: Long,
    val pages: Double,
    val dayOfWeek: String
)

// 阅读记录+书籍信息
data class RecordWithBook(
    val record: ReadingRecordEntity,
    val book: BookEntity?
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val recordRepository: ReadingRecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            
            // Start of today
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfToday = calendar.timeInMillis
            
            // Start of week
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            val startOfWeek = calendar.timeInMillis
            
            // Start of month
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val startOfMonth = calendar.timeInMillis
            
            // Start of 7 days ago
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_MONTH, -7)
            val sevenDaysAgo = calendar.timeInMillis

            combine(
                bookRepository.getAllBooks(),
                recordRepository.getAllRecords()
            ) { books, records ->
                val booksByStatus = BookStatus.entries.associateWith { status ->
                    books.count { it.status == status }
                }
                
                // Create book lookup map
                val booksMap = books.associateBy { it.id }
                
                // Calculate pages/chapters by time period
                val now = System.currentTimeMillis()
                
                // Today's records
                val todayRecords = records.filter { it.date >= startOfToday }
                val todayPages = todayRecords.filter { 
                    booksMap[it.bookId]?.progressType != ProgressType.CHAPTER 
                }.sumOf { it.pagesRead }
                val todayChapters = todayRecords.filter { 
                    booksMap[it.bookId]?.progressType == ProgressType.CHAPTER 
                }.sumOf { it.pagesRead }
                
                // This week's records
                val weekRecords = records.filter { it.date >= startOfWeek }
                val weekPages = weekRecords.filter { 
                    booksMap[it.bookId]?.progressType != ProgressType.CHAPTER 
                }.sumOf { it.pagesRead }
                val weekChapters = weekRecords.filter { 
                    booksMap[it.bookId]?.progressType == ProgressType.CHAPTER 
                }.sumOf { it.pagesRead }
                
                // This month's records
                val monthRecords = records.filter { it.date >= startOfMonth }
                val monthPages = monthRecords.filter { 
                    booksMap[it.bookId]?.progressType != ProgressType.CHAPTER 
                }.sumOf { it.pagesRead }
                val monthChapters = monthRecords.filter { 
                    booksMap[it.bookId]?.progressType == ProgressType.CHAPTER 
                }.sumOf { it.pagesRead }
                
                // Total
                val totalPages = records.filter { 
                    booksMap[it.bookId]?.progressType != ProgressType.CHAPTER 
                }.sumOf { it.pagesRead }
                val totalChapters = records.filter { 
                    booksMap[it.bookId]?.progressType == ProgressType.CHAPTER 
                }.sumOf { it.pagesRead }
                
                // Combine records with book info
                val recordsWithBooks = records.take(10).map { record ->
                    RecordWithBook(
                        record = record,
                        book = booksMap[record.bookId]
                    )
                }
                
                val weeklyData = generateWeeklyData(records, sevenDaysAgo)
                val avgPerDay = if (records.isNotEmpty()) {
                    val oldestRecord = records.minByOrNull { it.date }?.date ?: System.currentTimeMillis()
                    val daysSinceOldest = ((System.currentTimeMillis() - oldestRecord) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
                    records.sumOf { it.pagesRead } / daysSinceOldest
                } else 0.0

                StatsUiState(
                    totalBooks = books.size,
                    booksByStatus = booksByStatus,
                    totalPagesRead = totalPages,
                    totalChaptersRead = totalChapters,
                    todayPages = todayPages,
                    todayChapters = todayChapters,
                    weekPages = weekPages,
                    weekChapters = weekChapters,
                    monthPages = monthPages,
                    monthChapters = monthChapters,
                    averagePagesPerDay = avgPerDay,
                    weeklyReadingData = weeklyData,
                    recentRecords = records.take(10),
                    recentRecordsWithBooks = recordsWithBooks,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun generateWeeklyData(records: List<ReadingRecordEntity>, startTime: Long): List<DailyReading> {
        val calendar = Calendar.getInstance()
        val result = mutableListOf<DailyReading>()
        
        val dayNames = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        
        calendar.timeInMillis = startTime
        for (i in 0..6) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dayStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val dayEnd = calendar.timeInMillis
            
            val dayPages = records
                .filter { it.date in dayStart until dayEnd }
                .sumOf { it.pagesRead }
            
            result.add(DailyReading(
                date = dayStart,
                pages = dayPages,
                dayOfWeek = dayNames[Calendar.getInstance().apply { timeInMillis = dayStart }.get(Calendar.DAY_OF_WEEK) - 1]
            ))
        }
        
        return result
    }
}
