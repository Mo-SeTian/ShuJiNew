package com.readtrack.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.model.ProgressType
import com.readtrack.domain.model.YearlyReportData
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import com.readtrack.util.TimeConstants.ONE_DAY_MILLIS
import com.readtrack.util.getStartOfDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class YearlyReportViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val recordRepository: ReadingRecordRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))

    private val availableYears = MutableStateFlow(listOf(Calendar.getInstance().get(Calendar.YEAR)))

    init {
        viewModelScope.launch {
            recordRepository.getAllRecords().collect { records ->
                availableYears.value = buildAvailableYears(records)
            }
        }
    }

    val uiState: StateFlow<YearlyReportData?> = selectedYear
        .flatMapLatest { year ->
            val (yearStart, yearEnd) = yearBoundaries(year)
            combine(
                bookRepository.getAllBooks(),
                recordRepository.getRecordsByYearRange(yearStart, yearEnd),
                availableYears
            ) { books, records, years ->
                buildReport(year, books, records, years)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun selectYear(year: Int) {
        selectedYear.value = year
    }

    val currentYear: Int get() = selectedYear.value

    private fun yearBoundaries(year: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val yearStart = calendar.timeInMillis
        calendar.set(year + 1, Calendar.JANUARY, 1, 0, 0, 0)
        val yearEnd = calendar.timeInMillis
        return yearStart to yearEnd
    }

    private fun buildReport(
        year: Int,
        books: List<BookEntity>,
        records: List<ReadingRecordEntity>,
        years: List<Int>
    ): YearlyReportData {
        val (yearStart, yearEnd) = yearBoundaries(year)
        val booksMap = books.associateBy { it.id }
        val normalRecords = records.filter { it.recordType == RecordType.NORMAL }

        val bookIdsWithRecords = normalRecords.mapNotNull { it.bookId }.toSet()
        val yearBooks = books.filter { it.id in bookIdsWithRecords || it.createdAt in yearStart until yearEnd }

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
        val thickestBook = yearBooks.maxByOrNull { book ->
            if (book.progressType == ProgressType.CHAPTER) (book.totalChapters ?: 0).toDouble()
            else book.totalPages
        }
        val longestBook = yearBooks
            .filter { it.lastReadAt != null && it.lastReadAt > 0 && it.createdAt > 0 }
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

        // 最爱作者
        val authorCounts = yearBooks.mapNotNull { it.author?.takeIf { a -> a.isNotBlank() } }
            .groupBy { it }.mapValues { it.value.size }
        val favoriteAuthor = authorCounts.maxByOrNull { it.value }?.key

        // 最常阅读星期几（1=Sun..7=Sat → 转换）
        val dowCounts = normalRecords.groupBy { record ->
            Calendar.getInstance().apply { timeInMillis = record.date }.get(Calendar.DAY_OF_WEEK)
        }
        val favoriteDow = dowCounts.maxByOrNull { it.value.size }?.key ?: 1

        // 读得最快的书（日均页数最多）
        val fastestBook = yearBooks.filter { it.lastReadAt != null && it.lastReadAt > 0 && it.createdAt > 0 }
            .maxByOrNull { book ->
                val bookRecords = normalRecords.filter { it.bookId == book.id }
                val days = ((book.lastReadAt ?: 0L) - book.createdAt) / ONE_DAY_MILLIS + 1
                if (days > 0) bookRecords.sumOf {
                    if (book.progressType == ProgressType.CHAPTER) (it.chaptersRead ?: 0).toDouble()
                    else it.pagesRead
                } / days else 0.0
            }

        // 状态分布
        val statusDistribution = BookStatus.entries.associate { status ->
            status.displayName to yearBooks.count { it.status == status }
        }

        // 新增书籍数
        val newBooksCount = yearBooks.count { it.createdAt in yearStart until yearEnd }

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
            fastestBook = fastestBook,
            topGenre = topGenre,
            favoriteAuthor = favoriteAuthor,
            maxStreakDays = maxStreakDays,
            activeDays = activeDays.size,
            favoriteMonth = favoriteMonth,
            favoriteDayOfWeek = favoriteDow,
            totalRecords = normalRecords.size,
            newBooksCount = newBooksCount,
            statusDistribution = statusDistribution,
            availableYears = years
        )
    }

    private fun getMonth(timestamp: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = timestamp }
        return c.get(Calendar.MONTH)
    }

    private fun buildAvailableYears(records: List<ReadingRecordEntity>): List<Int> {
        if (records.isEmpty()) return listOf(Calendar.getInstance().get(Calendar.YEAR))
        val years = HashSet<Int>()
        records.forEach { record ->
            val c = Calendar.getInstance().apply { timeInMillis = record.date }
            years.add(c.get(Calendar.YEAR))
        }
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        years.add(currentYear)
        return years.sortedDescending()
    }

    companion object {

        fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("yyyy年M月", Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }

        private fun calculateMaxStreak(dates: List<Long>): Int {
            if (dates.isEmpty()) return 0
            val sorted = dates.map(::getStartOfDay).distinct().sortedDescending()
            if (sorted.isEmpty()) return 0

            var maxStreak = 1
            var currentStreak = 1

            for (i in 1 until sorted.size) {
                if (sorted[i] == sorted[i - 1] - ONE_DAY_MILLIS) {
                    currentStreak++
                } else {
                    maxStreak = maxOf(maxStreak, currentStreak)
                    currentStreak = 1
                }
            }
            maxStreak = maxOf(maxStreak, currentStreak)
            return maxStreak
        }
    }
}
