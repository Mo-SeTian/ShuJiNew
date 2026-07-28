package com.readtrack.presentation.viewmodel

import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.ProgressType
import java.util.Calendar

private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L

/**
 * 将阅读记录按天聚合为热力图数据，按月分组。
 * BookDetailViewModel 和 MonthlyStatsViewModel 共用。
 */
fun buildHeatmapMonths(records: List<ReadingRecordEntity>): List<HeatmapMonth> {
    val normalRecords = records.filter { it.recordType == RecordType.NORMAL }
    if (normalRecords.isEmpty()) return emptyList()

    val calendar = Calendar.getInstance()

    // 按天聚合（总额 + 单书明细）
    val dailyMap = linkedMapOf<Long, HeatmapDay>()
    val dailyBookMap = mutableMapOf<Long, MutableMap<String, DayBookBreakdown>>()

    normalRecords.forEach { record ->
        calendar.timeInMillis = record.date
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dow = calendar.get(Calendar.DAY_OF_WEEK)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val dayStart = calendar.timeInMillis

        val isChapter = record.bookSnapshot?.progressType == ProgressType.CHAPTER
        val amount = if (isChapter) (record.chaptersRead ?: 0).toDouble() else record.pagesRead
        val existing = dailyMap[dayStart]
        if (existing != null) {
            dailyMap[dayStart] = existing.copy(
                chaptersRead = existing.chaptersRead + if (isChapter) amount else 0.0,
                pagesRead = existing.pagesRead + if (!isChapter) amount else 0.0
            )
        } else {
            dailyMap[dayStart] = HeatmapDay(
                dateMs = dayStart,
                year = year, month = month, dayOfMonth = day, dayOfWeek = dow,
                chaptersRead = if (isChapter) amount else 0.0,
                pagesRead = if (!isChapter) amount else 0.0
            )
        }

        // 单书明细
        val title = record.bookSnapshot?.title ?: "未知书籍" // snapshot 为历史快照，书名变更时不回溯更新
        val bookMap = dailyBookMap.getOrPut(dayStart) { mutableMapOf() }
        val existingBook = bookMap[title]
        if (existingBook != null) {
            bookMap[title] = existingBook.copy(
                chaptersRead = existingBook.chaptersRead + if (isChapter) amount else 0.0,
                pagesRead = existingBook.pagesRead + if (!isChapter) amount else 0.0
            )
        } else {
            bookMap[title] = DayBookBreakdown(
                bookTitle = title,
                chaptersRead = if (isChapter) amount else 0.0,
                pagesRead = if (!isChapter) amount else 0.0
            )
        }
    }

    // 将单书明细写入对应 HeatmapDay
    dailyBookMap.forEach { (dayStart, bookMap) ->
        dailyMap[dayStart]?.let { entry ->
            dailyMap[dayStart] = entry.copy(bookBreakdowns = bookMap.values.toList())
        }
    }

    // 填充缺失日（最早记录日到今天）
    val earliest = dailyMap.keys.min()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    var currentDay = earliest
    while (currentDay <= todayStart) {
        if (!dailyMap.containsKey(currentDay)) {
            calendar.timeInMillis = currentDay
            dailyMap[currentDay] = HeatmapDay(
                dateMs = currentDay,
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH),
                dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
                dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
                chaptersRead = 0.0, pagesRead = 0.0
            )
        }
        currentDay += ONE_DAY_MILLIS
    }

    // 按月分组
    val sortedDays = dailyMap.values.sortedBy { it.dateMs }
    return sortedDays.groupBy { it.year to it.month }.map { (key, days) ->
        val (year, month) = key
        HeatmapMonth(
            year = year, month = month,
            label = "${year}年${month + 1}月",
            days = days,
            totalValue = days.sumOf { it.pagesRead + it.chaptersRead }
        )
    }.sortedWith(compareBy({ it.year }, { it.month }))
}
