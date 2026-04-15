package com.readtrack.presentation.viewmodel

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.util.getStartOfDay

internal fun buildHomeUiState(
    books: List<BookEntity>,
    records: List<ReadingRecordEntity>,
    now: Long = System.currentTimeMillis()
): HomeUiState {
    val startOfToday = getStartOfDay(now)
    var todayPages = 0.0
    var todayChapters = 0.0
    var totalReadingTime = 0.0

    // 合并遍历：一边构建 chapterBookIds，一边统计 statusCounts，一边找最近在读
    val chapterBookIds = mutableSetOf<Long>()
    val statusCounts = mutableMapOf<BookStatus, Int>()
    var readingBooks = 0
    var finishedBooks = 0
    var latestReadingBook: BookEntity? = null

    books.forEach { book ->
        if (book.progressType == ProgressType.CHAPTER) chapterBookIds.add(book.id)
        statusCounts[book.status] = (statusCounts[book.status] ?: 0) + 1
        if (book.status == BookStatus.READING) readingBooks++
        if (book.status == BookStatus.FINISHED) finishedBooks++
        // 记录最近翻阅的在读书籍（用于 recentBooks 排序）
        if (book.status == BookStatus.READING) {
            val bookTime = book.lastReadAt ?: 0L
            val latestTime = latestReadingBook?.lastReadAt ?: 0L
            if (bookTime > latestTime) latestReadingBook = book
        }
    }
    BookStatus.entries.forEach { status -> statusCounts.putIfAbsent(status, 0) }

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

    // 最近在读：直接复用上面遍历时找出的最大 lastReadAt 书籍，最多取3本
    // 先按 lastReadAt 降序（从大到小），取前3本即最近翻阅的
    val recentBooks = books.asSequence()
        .filter { it.status == BookStatus.READING && it.lastReadAt != null && it.lastReadAt > 0L }
        .sortedByDescending { it.lastReadAt }
        .take(3)
        .toList()

    // 格式化阅读时长，避免 UI 层每次 recomposition 都重算
    val totalReadingTimeLabel = formatReadingTime(totalReadingTime)

    return HomeUiState(
        todayPages = todayPages,
        todayChapters = todayChapters,
        streakDays = calculateReadingStreak(records.map { it.date }, now),
        totalReadingTime = totalReadingTime,
        totalReadingTimeLabel = totalReadingTimeLabel,
        totalBooks = books.size,
        readingBooks = readingBooks,
        finishedBooks = finishedBooks,
        recentBooks = recentBooks,
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
        .sortedDescending()

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

private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L

private fun formatReadingTime(totalMinutes: Double): String {
    val hours = (totalMinutes / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()
    return "$hours 小时 $minutes 分钟"
}
