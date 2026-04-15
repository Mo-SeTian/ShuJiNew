package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import com.readtrack.util.PerformanceTrace
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.util.Calendar
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
            combine(
                bookRepository.getAllBooks().catch { emit(emptyList()) },
                recordRepository.getAllRecords().catch { emit(emptyList()) }
            ) { books, records ->
                PerformanceTrace.measure("stats.build") {
                    buildStatsUiState(books, records)
                }
            }
                .distinctUntilChanged()
                .flowOn(Dispatchers.Default)
                .collect { state ->
                    _uiState.value = state
                    PerformanceTrace.mark(
                        "stats.ready books=${state.totalBooks} records=${state.recentRecordsWithBooks.size}"
                    )
                }
        }
    }

    private fun buildStatsUiState(
        books: List<BookEntity>,
        records: List<ReadingRecordEntity>
    ): StatsUiState {
        val now = System.currentTimeMillis()
        val boundaries = createTimeBoundaries(now)
        val booksMap = books.associateBy { it.id }
        val weeklyBuckets = boundaries.weeklyBuckets.toMutableMap()

        var todayPages = 0.0
        var todayChapters = 0.0
        var weekPages = 0.0
        var weekChapters = 0.0
        var monthPages = 0.0
        var monthChapters = 0.0
        var totalPages = 0.0
        var totalChapters = 0.0
        var totalReadingTime = 0.0

        // 按天分组的 records（用于 weekly 桶）
        val recordsByDay = records.groupBy { record ->
            weeklyBuckets.keys.firstOrNull { dayStart ->
                record.date in dayStart until (dayStart + ONE_DAY_MILLIS)
            }
        }

        records.forEach { record ->
            totalReadingTime += record.pagesRead
            val isChapterBook = booksMap[record.bookId]?.progressType == ProgressType.CHAPTER
            val value = record.pagesRead

            if (isChapterBook) {
                totalChapters += value
                if (record.date >= boundaries.startOfMonth) monthChapters += value
                if (record.date >= boundaries.startOfWeek) weekChapters += value
                if (record.date >= boundaries.startOfToday) todayChapters += value
            } else {
                totalPages += value
                if (record.date >= boundaries.startOfMonth) monthPages += value
                if (record.date >= boundaries.startOfWeek) weekPages += value
                if (record.date >= boundaries.startOfToday) todayPages += value
            }
        }

        // weekly 桶累加（一次性分组查找）
        recordsByDay.forEach { (dayStart, dayRecords) ->
            if (dayStart != null) {
                weeklyBuckets[dayStart] = dayRecords.sumOf { it.pagesRead }
            }
        }

        // 单次遍历获取 booksByStatus
        val booksByStatus = books.groupBy { it.status }.mapValues { it.value.size }

        // recentRecords 取最近10条（不需要全排序）
        val recentRecords = records
            .sortedByDescending { it.date }
            .take(10)
        val recordsWithBooks = recentRecords.map { record ->
            RecordWithBook(record = record, book = booksMap[record.bookId])
        }

        val averagePagesPerDay = if (totalReadingTime > 0 && records.isNotEmpty()) {
            val oldestRecord = records.minOf { it.date }
            val daysSinceOldest = ((now - oldestRecord) / ONE_DAY_MILLIS).toInt().coerceAtLeast(1)
            totalReadingTime / daysSinceOldest
        } else {
            0.0
        }

        return StatsUiState(
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
            averagePagesPerDay = averagePagesPerDay,
            weeklyReadingData = weeklyBuckets.map { (date, pages) ->
                DailyReading(
                    date = date,
                    pages = pages,
                    dayOfWeek = dayLabel(date)
                )
            },
            recentRecords = recentRecords,
            recentRecordsWithBooks = recordsWithBooks,
            isLoading = false
        )
    }

    private fun createTimeBoundaries(now: Long): TimeBoundaries {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.timeInMillis

        // 自然周起始（周一/周日，取决于 locale 的 firstDayOfWeek）
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek())
        val startOfWeek = calendar.timeInMillis

        calendar.timeInMillis = startOfToday
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = calendar.timeInMillis

        // 近7天趋势桶：从今天-6天开始向后遍历7天
        // 严格以今天为终点，保证今天的数据落在今天的桶里
        calendar.timeInMillis = startOfToday
        calendar.add(Calendar.DAY_OF_MONTH, -6)
        val weeklyBuckets = linkedMapOf<Long, Double>()
        repeat(7) {
            weeklyBuckets[calendar.timeInMillis] = 0.0
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return TimeBoundaries(
            startOfToday = startOfToday,
            startOfWeek = startOfWeek,
            startOfMonth = startOfMonth,
            weeklyBuckets = weeklyBuckets
        )
    }

    private fun dayLabel(timeMillis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            Calendar.SUNDAY -> "周日"
            else -> ""
        }
    }

    private data class TimeBoundaries(
        val startOfToday: Long,
        val startOfWeek: Long,
        val startOfMonth: Long,
        val weeklyBuckets: LinkedHashMap<Long, Double>
    )

    private companion object {
        private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}