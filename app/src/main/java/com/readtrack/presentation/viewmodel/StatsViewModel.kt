package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.local.StatsUnit
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
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
    val statsUnit: StatsUnit = StatsUnit.CHAPTER,
    // 按偏好过滤后的显示值
    val todayValue: Double = 0.0,
    val weekValue: Double = 0.0,
    val monthValue: Double = 0.0,
    val totalValue: Double = 0.0,
    val weeklyReadingData: List<DailyReading> = emptyList(),
    val recentRecords: List<ReadingRecordEntity> = emptyList(),
    val recentRecordsWithBooks: List<RecordWithBook> = emptyList(),
    val isLoading: Boolean = true
)

data class DailyReading(
    val date: Long,
    val pages: Double,
    val chapters: Double,
    val dayOfWeek: String
)

data class RecordWithBook(
    val record: ReadingRecordEntity,
    val book: BookEntity?
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val recordRepository: ReadingRecordRepository,
    private val preferencesManager: PreferencesManager
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
                recordRepository.getAllRecords().catch { emit(emptyList()) },
                preferencesManager.statsUnit
            ) { books, records, statsUnit ->
                Triple(books, records, statsUnit)
            }.collect { (books, records, statsUnit) ->
                val state = buildStatsUiState(books, records, statsUnit)
                _uiState.value = state
            }
        }
    }

    private fun buildStatsUiState(
        books: List<BookEntity>,
        records: List<ReadingRecordEntity>,
        statsUnit: StatsUnit
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

        // 按天分组的 records（用于 weekly 桶）
        val recordsByDay = records.groupBy { record ->
            weeklyBuckets.keys.firstOrNull { dayStart ->
                record.date in dayStart until (dayStart + ONE_DAY_MILLIS)
            }
        }

        records.filter { it.recordType == RecordType.NORMAL }.forEach { record ->
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

        // weekly 桶只统计普通阅读记录（状态变更记录 pagesRead=0，不影响结果但避免无谓计算）
        recordsByDay.forEach { (dayStart, dayRecords) ->
            if (dayStart != null) {
                weeklyBuckets[dayStart] = dayRecords
                    .filter { it.recordType == RecordType.NORMAL }
                    .sumOf { it.pagesRead }
            }
        }

        // 按偏好计算今天的 value（章节桶也需要按书籍类型分别累加）
        val recordsByDayChapter = recordsByDay.mapValues { (_, recs) ->
            recs.filter { booksMap[it.bookId]?.progressType == ProgressType.CHAPTER }.sumOf { it.pagesRead }
        }
        val recordsByDayPage = recordsByDay.mapValues { (_, recs) ->
            recs.filter { booksMap[it.bookId]?.progressType != ProgressType.CHAPTER }.sumOf { it.pagesRead }
        }

        // 单次遍历获取 booksByStatus
        val booksByStatus = books.groupBy { it.status }.mapValues { it.value.size }

        // recentRecords 取最近10条
        val recentRecords = records.sortedByDescending { it.date }.take(10)
        val recordsWithBooks = recentRecords.map { record ->
            RecordWithBook(record = record, book = booksMap[record.bookId])
        }

        return StatsUiState(
            totalBooks = books.size,
            booksByStatus = booksByStatus,
            statsUnit = statsUnit,
            todayValue = if (statsUnit == StatsUnit.CHAPTER) todayChapters else todayPages,
            weekValue = if (statsUnit == StatsUnit.CHAPTER) weekChapters else weekPages,
            monthValue = if (statsUnit == StatsUnit.CHAPTER) monthChapters else monthPages,
            totalValue = if (statsUnit == StatsUnit.CHAPTER) totalChapters else totalPages,
            weeklyReadingData = weeklyBuckets.map { (date, _) ->
                DailyReading(
                    date = date,
                    pages = recordsByDayPage[date] ?: 0.0,
                    chapters = recordsByDayChapter[date] ?: 0.0,
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