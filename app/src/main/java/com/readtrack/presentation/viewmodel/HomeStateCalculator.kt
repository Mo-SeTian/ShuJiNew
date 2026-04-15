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
    val chapterBookIds = books.asSequence()
        .filter { it.progressType == ProgressType.CHAPTER }
        .map { it.id }
        .toHashSet()

    val startOfToday = getStartOfDay(now)
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

    val statusCounts = mutableMapOf<BookStatus, Int>()
    var readingBooks = 0
    var finishedBooks = 0

    BookStatus.entries.forEach { status -> statusCounts[status] = 0 }
    books.forEach { book ->
        statusCounts[book.status] = (statusCounts[book.status] ?: 0) + 1
        if (book.status == BookStatus.READING) readingBooks++
        if (book.status == BookStatus.FINISHED) finishedBooks++
    }

    return HomeUiState(
        todayPages = todayPages,
        todayChapters = todayChapters,
        streakDays = calculateReadingStreak(records.map { it.date }, now),
        totalReadingTime = totalReadingTime,
        totalBooks = books.size,
        readingBooks = readingBooks,
        finishedBooks = finishedBooks,
        recentBooks = books.asSequence()
            .filter { it.status == BookStatus.READING }
            .sortedWith(compareByDescending<BookEntity> { it.lastReadAt ?: 0L }.thenByDescending { it.updatedAt })
            .take(3)
            .toList(),
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
