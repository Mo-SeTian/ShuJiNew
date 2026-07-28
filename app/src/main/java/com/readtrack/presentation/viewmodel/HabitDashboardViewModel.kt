package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookType
import com.readtrack.domain.model.DayOfWeekActivity
import com.readtrack.domain.model.ProgressType
import com.readtrack.domain.model.HabitDashboardData
import com.readtrack.domain.model.ReadingSpeed
import com.readtrack.domain.model.TimeSlot
import com.readtrack.domain.model.TimeSlotDistribution
import com.readtrack.domain.model.TypePreference
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import com.readtrack.util.getStartOfDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HabitDashboardUiState(
    val data: HabitDashboardData? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HabitDashboardViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val recordRepository: ReadingRecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitDashboardUiState())
    val uiState: StateFlow<HabitDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                bookRepository.getAllBooks().catch { emit(emptyList()) },
                recordRepository.getAllRecords().catch { emit(emptyList()) }
            ) { books, records -> buildHabitData(books, records) }
                .collect { data ->
                    _uiState.value = HabitDashboardUiState(data = data, isLoading = false)
                }
        }
    }

    private fun buildHabitData(
        books: List<BookEntity>,
        records: List<ReadingRecordEntity>
    ): HabitDashboardData? {
        val normalRecords = records.filter { it.recordType == RecordType.NORMAL }
        if (normalRecords.isEmpty()) return null

        val booksMap = books.associateBy { it.id }

        // 1. 时段分布
        val timeDist = buildTimeDistribution(normalRecords)

        // 2. 每周活跃
        val weeklyActivity = buildWeeklyActivity(normalRecords)

        // 3. 类型偏好
        val typePreference = buildTypePreference(normalRecords, booksMap)

        // 4. 阅读速度
        val readingSpeed = buildReadingSpeed(normalRecords, timeDist, weeklyActivity)

        // 5. 摘要
        val activeDays = normalRecords.map { getStartOfDay(it.date) }.distinct().size
        val streak = calculateReadingStreak(
            normalRecords.map { it.date },
            System.currentTimeMillis()
        )
        val favTime = timeDist.maxByOrNull { it.recordCount }?.slot?.label ?: "未知"

        return HabitDashboardData(
            totalActiveDays = activeDays,
            streakDays = streak,
            favoriteTimeLabel = "${favTime}型",
            totalRecords = normalRecords.size,
            timeDistribution = timeDist,
            weeklyActivity = weeklyActivity,
            typePreference = typePreference,
            readingSpeed = readingSpeed
        )
    }

    private fun buildTimeDistribution(records: List<ReadingRecordEntity>): List<TimeSlotDistribution> {
        val cal = Calendar.getInstance()
        val counts = mutableMapOf<TimeSlot, Int>()
        TimeSlot.entries.forEach { counts[it] = 0 }

        records.forEach { record ->
            cal.timeInMillis = record.date
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val slot = TimeSlot.entries.firstOrNull { hour in it.startHour until it.endHour }
                ?: TimeSlot.AFTERNOON
            counts[slot] = (counts[slot] ?: 0) + 1
        }

        val total = counts.values.sum().coerceAtLeast(1)
        return TimeSlot.entries.map { slot ->
            val count = counts[slot] ?: 0
            TimeSlotDistribution(
                slot = slot,
                recordCount = count,
                percentage = count.toFloat() / total
            )
        }
    }

    private fun buildWeeklyActivity(records: List<ReadingRecordEntity>): List<DayOfWeekActivity> {
        val cal = Calendar.getInstance()
        // dayIndex: 1=周一 … 7=周日
        val activeDateSet = mutableMapOf<Int, MutableSet<Long>>()
        (1..7).forEach { activeDateSet[it] = mutableSetOf() }

        records.forEach { record ->
            val day = getStartOfDay(record.date)
            cal.timeInMillis = record.date
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, …, 7=Saturday
            val index = when (dayOfWeek) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                else -> 7 // SUNDAY
            }
            activeDateSet[index]?.add(day)
        }

        val maxDays = activeDateSet.values.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
        val labels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        return (1..7).map { index ->
            val days = activeDateSet[index]?.size ?: 0
            DayOfWeekActivity(
                dayIndex = index,
                dayLabel = labels[index - 1],
                activeDays = days,
                isMostActive = days == maxDays && days > 0
            )
        }
    }

    private fun buildTypePreference(
        records: List<ReadingRecordEntity>,
        booksMap: Map<Long, BookEntity>
    ): List<TypePreference> {
        val pagesByType = mutableMapOf<String, Double>()
        val bookIdsByType = mutableMapOf<String, MutableSet<Long>>()

        records.forEach { record ->
            val bookType = record.bookSnapshot?.bookType
                ?: record.bookId?.let { booksMap[it]?.bookType?.name }
                ?: BookType.NOVEL.name
            val isChapter = record.bookSnapshot?.progressType == ProgressType.CHAPTER
            val amount = if (isChapter) (record.chaptersRead ?: 0).toDouble() else record.pagesRead
            pagesByType[bookType] = (pagesByType[bookType] ?: 0.0) + amount
            // null bookId 不参与去重计数
            record.bookId?.let { bookIdsByType.getOrPut(bookType) { mutableSetOf() }.add(it) }
        }

        val totalPages = pagesByType.values.sum().coerceAtLeast(1.0)
        val typeLabelMap = mapOf("NOVEL" to "小说", "COMIC" to "漫画", "AUDIOBOOK" to "有声书")

        return pagesByType.entries
            .sortedByDescending { it.value }
            .map { (type, pages) ->
                TypePreference(
                    bookType = type,
                    typeLabel = typeLabelMap[type] ?: type,
                    pagesRead = pages,
                    percentage = (pages / totalPages).toFloat(),
                    bookCount = bookIdsByType[type]?.size ?: 0
                )
            }
    }

    private fun buildReadingSpeed(
        records: List<ReadingRecordEntity>,
        timeDist: List<TimeSlotDistribution>,
        weeklyActivity: List<DayOfWeekActivity>
    ): ReadingSpeed {
        val pageRecords = records.filter { it.bookSnapshot?.progressType != ProgressType.CHAPTER && it.pagesRead > 0 }
        val chapterRecords = records.filter { it.bookSnapshot?.progressType == ProgressType.CHAPTER && (it.chaptersRead ?: 0) > 0 }

        val avgPages = if (pageRecords.isNotEmpty())
            pageRecords.sumOf { it.pagesRead } / pageRecords.size else 0.0
        val avgChapters = if (chapterRecords.isNotEmpty())
            chapterRecords.sumOf { it.chaptersRead ?: 0 }.toDouble() / chapterRecords.size else 0.0

        val favTime = timeDist.maxByOrNull { it.recordCount }?.slot?.label ?: "未知"
        val favDay = weeklyActivity.maxByOrNull { it.activeDays }?.dayLabel ?: "未知"

        return ReadingSpeed(
            avgPagesPerSession = avgPages,
            avgChaptersPerSession = avgChapters,
            favoriteTimeLabel = "${favTime}型",
            favoriteDayLabel = favDay
        )
    }
}
