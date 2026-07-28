package com.readtrack.presentation.viewmodel

import com.readtrack.data.local.StatsUnit
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.model.ProgressType
import com.readtrack.util.TimeConstants.ONE_DAY_MILLIS
import com.readtrack.util.getStartOfDay
import java.util.Calendar

data class HomeUiState(
    val statsUnit: StatsUnit = StatsUnit.CHAPTER,
    val componentOrder: List<String> = emptyList(),
    val todayValue: Double = 0.0,
    val todayPages: Double = 0.0,
    val todayChapters: Double = 0.0,
    val weeklyValue: Double = 0.0,
    val monthlyValue: Double = 0.0,
    val activeDaysThisWeek: Int = 0,
    val streakDays: Int = 0,
    val totalReadingTime: Double = 0.0,
    val totalReadingTimeLabel: String = "0 小时 0 分钟",
    val totalBooks: Int = 0,
    val readingBooks: Int = 0,
    val finishedBooks: Int = 0,
    val completionRate: Int = 0,
    val recentBooks: List<BookEntity> = emptyList(),
    val latestReadingBookTitle: String? = null,
    val statusCounts: Map<BookStatus, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

internal fun buildHomeUiState(
    books: List<BookEntity>,
    records: List<ReadingRecordEntity>,
    statsUnit: StatsUnit = StatsUnit.CHAPTER,
    componentOrder: List<String> = emptyList(),
    now: Long = System.currentTimeMillis()
): HomeUiState {
    val startOfToday = getStartOfDay(now)
    val calendar = Calendar.getInstance().apply { timeInMillis = now }
    // 日历周（周一~周日），与 StatsViewModel 对齐
    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
    val startOfWeek = getStartOfDay(calendar.timeInMillis)
    // 日历月
    calendar.timeInMillis = now
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val startOfMonth = getStartOfDay(calendar.timeInMillis)

    var todayPages = 0.0
    var todayChapters = 0.0
    var weeklyPages = 0.0
    var weeklyChapters = 0.0
    var monthlyPages = 0.0
    var monthlyChapters = 0.0
    var totalReadingTime = 0.0

    val booksMap = books.associateBy { it.id }
    val statusCounts = LinkedHashMap<BookStatus, Int>(BookStatus.entries.size)
    var readingBooks = 0
    var finishedBooks = 0

    val recentBooks = books.asSequence()
        .filter { it.status == BookStatus.READING && (it.lastReadAt ?: 0L) > 0L }
        .sortedByDescending { it.lastReadAt ?: 0L }
        .take(3)
        .toList()

    books.forEach { book ->
        statusCounts[book.status] = (statusCounts[book.status] ?: 0) + 1
        when (book.status) {
            BookStatus.READING -> readingBooks++
            BookStatus.FINISHED -> finishedBooks++
            else -> Unit
        }
    }
    BookStatus.entries.forEach { status -> statusCounts.putIfAbsent(status, 0) }

    val normalRecords = ArrayList<ReadingRecordEntity>(records.size)
    val activeDaysThisWeek = HashSet<Long>()
    records.forEach { record ->
        if (record.recordType != RecordType.NORMAL) return@forEach
        normalRecords.add(record)

        // 快照优先，其次按当前书籍类型；与 StatsViewModel.isChapterBook 保持一致
        val progressType = record.bookSnapshot?.progressType
            ?: (record.bookId?.let { booksMap[it]?.progressType })
        val isChapterBook = progressType == ProgressType.CHAPTER
        val value = if (isChapterBook) (record.chaptersRead ?: 0).toDouble() else record.pagesRead
        totalReadingTime += value

        if (record.date >= startOfMonth) {
            if (isChapterBook) {
                monthlyChapters += value
            } else {
                monthlyPages += value
            }
        }
        if (record.date >= startOfWeek) {
            if (isChapterBook) {
                weeklyChapters += value
            } else {
                weeklyPages += value
            }
            activeDaysThisWeek.add(getStartOfDay(record.date))
        }
        if (record.date >= startOfToday) {
            if (isChapterBook) {
                todayChapters += value
            } else {
                todayPages += value
            }
        }
    }

    val todayValue = if (statsUnit == StatsUnit.CHAPTER) todayChapters else todayPages
    val weeklyValue = if (statsUnit == StatsUnit.CHAPTER) weeklyChapters else weeklyPages
    val monthlyValue = if (statsUnit == StatsUnit.CHAPTER) monthlyChapters else monthlyPages
    val completionRate = if (books.isNotEmpty()) {
        ((finishedBooks * 100.0) / books.size).toInt()
    } else {
        0
    }

    return HomeUiState(
        statsUnit = statsUnit,
        componentOrder = componentOrder,
        todayValue = todayValue,
        todayPages = todayPages,
        todayChapters = todayChapters,
        weeklyValue = weeklyValue,
        monthlyValue = monthlyValue,
        activeDaysThisWeek = activeDaysThisWeek.size,
        streakDays = calculateReadingStreak(normalRecords.map { it.date }, now),
        totalReadingTime = totalReadingTime,
        totalReadingTimeLabel = if (statsUnit == StatsUnit.PAGE) formatReadingTime(totalReadingTime) else "",
        totalBooks = books.size,
        readingBooks = readingBooks,
        finishedBooks = finishedBooks,
        completionRate = completionRate,
        recentBooks = recentBooks,
        latestReadingBookTitle = recentBooks.firstOrNull()?.title,
        statusCounts = statusCounts,
        isLoading = false,
        errorMessage = null
    )
}

internal fun calculateReadingStreak(
    dates: List<Long>,
    now: Long = System.currentTimeMillis()
): Int {
    if (dates.isEmpty()) return 0

    val sortedDates = dates
        .map(::getStartOfDay)
        .distinct()
        // DB 查询已按时间排序（reading_records.date DESC），直接倒序遍历即可
        // 不再额外 sort，节省 O(n log n)

    if (sortedDates.isEmpty()) return 0

    val today = getStartOfDay(now)
    val yesterday = today - ONE_DAY_MILLIS
    if (sortedDates.first() != today && sortedDates.first() != yesterday) return 0

    var streak = 1
    var currentDate = sortedDates.first()

    for (index in 1 until sortedDates.size) {
        val expectedPreviousDate = currentDate - ONE_DAY_MILLIS
        if (sortedDates[index] == expectedPreviousDate) {
            streak++
            currentDate = expectedPreviousDate
        } else {
            break
        }
    }

    return streak
}

private fun formatReadingTime(totalMinutes: Double): String {
    val hours = (totalMinutes / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()
    return "$hours 小时 $minutes 分钟"
}
