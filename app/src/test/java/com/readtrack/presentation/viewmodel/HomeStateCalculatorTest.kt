package com.readtrack.presentation.viewmodel

import com.readtrack.data.local.StatsUnit
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.model.BookStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStateCalculatorTest {

    @Test
    fun calculateReadingStreak_countsTodayAndConsecutiveDays() {
        val now = 1_710_000_000_000L
        val day = 24L * 60L * 60L * 1000L

        val streak = calculateReadingStreak(
            dates = listOf(now, now - day, now - day * 2, now - day * 4),
            now = now
        )

        assertEquals(3, streak)
    }

    @Test
    fun buildHomeUiState_limitsAndSortsRecentReadingBooks() {
        val books = listOf(
            BookEntity(id = 1, title = "A", status = BookStatus.READING, lastReadAt = 300, updatedAt = 100),
            BookEntity(id = 2, title = "B", status = BookStatus.READING, lastReadAt = 500, updatedAt = 200),
            BookEntity(id = 3, title = "C", status = BookStatus.READING, lastReadAt = 400, updatedAt = 300),
            BookEntity(id = 4, title = "D", status = BookStatus.READING, lastReadAt = 100, updatedAt = 400),
            BookEntity(id = 5, title = "E", status = BookStatus.FINISHED, lastReadAt = 900, updatedAt = 500)
        )

        val state = buildHomeUiState(
            books = books,
            records = emptyList(),
            now = 1_710_000_000_000L
        )

        assertEquals(listOf(2L, 3L, 1L), state.recentBooks.map { it.id })
        assertEquals(4, state.readingBooks)
        assertEquals(1, state.finishedBooks)
        assertEquals(5, state.totalBooks)
    }

    @Test
    fun buildHomeUiState_splitsTodayPagesAndChapters() {
        val now = 1_710_000_000_000L
        val books = listOf(
            BookEntity(id = 1, title = "PageBook", status = BookStatus.READING, progressType = ProgressType.PAGE),
            BookEntity(id = 2, title = "ChapterBook", status = BookStatus.READING, progressType = ProgressType.CHAPTER, totalChapters = 20)
        )
        val records = listOf(
            ReadingRecordEntity(id = 1, bookId = 1, pagesRead = 12.0, fromPage = 1.0, toPage = 13.0, date = now),
            ReadingRecordEntity(id = 2, bookId = 2, pagesRead = 3.0, fromPage = 1.0, toPage = 4.0, date = now)
        )

        val state = buildHomeUiState(
            books = books,
            records = records,
            statsUnit = StatsUnit.PAGE,
            now = now
        )

        assertEquals(12.0, state.todayPages, 0.001)
        assertEquals(3.0, state.todayChapters, 0.001)
        assertEquals(15.0, state.totalReadingTime, 0.001)
    }
}
