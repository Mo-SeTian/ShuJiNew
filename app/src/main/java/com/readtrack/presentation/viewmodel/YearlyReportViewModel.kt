package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.local.StatsUnit
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.model.BookType
import com.readtrack.domain.model.ProgressType
import com.readtrack.domain.model.YearlyReportData
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import com.readtrack.util.getStartOfDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class YearlyReportViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val recordRepository: ReadingRecordRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))

    val uiState: StateFlow<YearlyReportData?> = combine(
        selectedYear,
        preferencesManager.statsUnit
    ) { year, _ -> year }.combine(bookRepository.getAllBooks()) { year, books -> year to books }
        .combine(recordRepository.getAllRecords()) { (year, books), records ->
            buildReport(year, books, records)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun selectYear(year: Int) {
        selectedYear.value = year
    }

    val currentYear: Int get() = selectedYear.value

    private fun buildReport(
        year: Int,
        books: List<BookEntity>,
        records: List<ReadingRecordEntity>
    ): YearlyReportData {
        val calendar = Calendar.getInstance()
        calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val yearStart = calendar.timeInMillis

        calendar.set(year + 1, Calendar.JANUARY, 1, 0, 0, 0)
        val yearEnd = calendar.timeInMillis

        val yearRecords = records.filter { it.date in yearStart until yearEnd }
        val normalRecords = yearRecords.filter { it.recordType == RecordType.NORMAL }
        val booksMap = books.associateBy { it.id }

        val bookIdsWithRecords = normalRecords.mapNotNull { it.bookId }.toSet()
        val yearBooks = books.filter { it.id in bookIdsWithRecords || (it.createdAt in yearStart until yearEnd) }

        val finishedInYear = yearBooks.count { it.status == BookStatus.FINISHED }

        var totalPages = 0.0
        var totalChapters = 0.0
        val monthlyPages = FloatArray(12)
        val monthlyChapters = FloatArray(12)
        val activeDays = HashSet<Long>()
        val streakDates = ArrayList<Long>()

        normalRecords.forEach { record ->
            val progressType = record.bookSnapshot?.progressType
                ?: record.bookId?.let { booksMap[it]?.progressType }
            val isChapterBook = progressType == ProgressType.CHAPTER

            if (isChapterBook) {
                val v = (record.chaptersRead ?: 0).toDouble()
                totalChapters += v
                val month = getMonth(record.date)
                monthlyChapters[month] += v.toFloat()
            } else {
                totalPages += record.pagesRead
                val month = getMonth(record.date)
                monthlyPages[month] += record.pagesRead.toFloat()
            }

            activeDays.add(getStartOfDay(record.date))
            streakDates.add(record.date)
        }

        val totalBooksRead = yearBooks.size
        val ratedBooks = yearBooks.filter { it.rating != null && it.rating!! > 0 }
        val averageRating = if (ratedBooks.isNotEmpty()) ratedBooks.map { it.rating!! }.average().toFloat() else 0f

        val favoriteBook = ratedBooks.maxByOrNull { it.rating ?: 0f }
        val thickestBook = yearBooks.maxByOrNull { it.totalPages }
        val longestBook = yearBooks.filter { it.lastReadAt != null && it.createdAt > 0 }
            .maxByOrNull { (it.lastReadAt ?: 0L) - it.createdAt }

        val genreCounts = yearBooks.groupBy { it.bookType }
        val topGenre = genreCounts.maxByOrNull { it.value.size }?.key?.displayName ?: "未知"

        val maxStreakDays = if (streakDates.isNotEmpty()) {
            calculateMaxStreak(streakDates)
        } else 0

        val favoriteMonth = if (normalRecords.isNotEmpty()) {
            val merged = FloatArray(12) { monthlyPages[it] + monthlyChapters[it].toDouble().toFloat() }
            merged.indices.maxByOrNull { merged[it] } ?: 0
        } else 0

        val availableYears = buildAvailableYears(records)

        return YearlyReportData(
            year = year,
            totalBooksRead = totalBooksRead,
            finishedBooks = finishedInYear,
            totalPages = totalPages,
            totalChapters = totalChapters,
            averageRating = averageRating,
            monthlyPages = monthlyPages.toList(),
            monthlyChapters = monthlyChapters.toList(),
            favoriteBook = favoriteBook,
            thickestBook = thickestBook,
            longestBook = longestBook,
            topGenre = topGenre,
            maxStreakDays = maxStreakDays,
            activeDays = activeDays.size,
            favoriteMonth = favoriteMonth,
            availableYears = availableYears
        )
    }

    private fun getMonth(timestamp: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = timestamp }
        return c.get(Calendar.MONTH) // 0-11
    }

    private fun buildAvailableYears(records: List<ReadingRecordEntity>): List<Int> {
        if (records.isEmpty()) return listOf(Calendar.getInstance().get(Calendar.YEAR))
        val years = HashSet<Int>()
        records.forEach {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            years.add(c.get(Calendar.YEAR))
        }
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        years.add(currentYear)
        return years.sortedDescending()
    }

    companion object {
        private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L

        private fun calculateMaxStreak(dates: List<Long>): Int {
            if (dates.isEmpty()) return 0
            val sorted = dates.map(::getStartOfDay).distinct()
            if (sorted.isEmpty()) return 0

            var maxStreak = 1
            var currentStreak = 1
            var currentDate = sorted[0]

            for (i in 1 until sorted.size) {
                if (sorted[i] == currentDate - ONE_DAY_MILLIS) {
                    currentStreak++
                } else {
                    maxStreak = maxOf(maxStreak, currentStreak)
                    currentStreak = 1
                }
                currentDate = sorted[i]
            }
            maxStreak = maxOf(maxStreak, currentStreak)
            return maxStreak
        }
    }
}
