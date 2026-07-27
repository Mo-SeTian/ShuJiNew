package com.readtrack.domain.badge

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BadgeCatalog
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.model.BookType
import com.readtrack.util.getStartOfDay
import java.util.Calendar

/**
 * 汇总用户数据用于徽章判定。所有徽章 checker 从这个快照读取，避免多次遍历同一份数据。
 */
data class BadgeCheckSnapshot(
    val books: List<BookEntity>,
    val records: List<ReadingRecordEntity>,
    /** 相对时间，测试时可注入固定值 */
    val now: Long = System.currentTimeMillis()
) {
    val finishedBookCount: Int = books.count { it.status == BookStatus.FINISHED }

    val currentStreak: Int by lazy { calculateStreak(records, now) }

    val totalPages: Double = records
        .filter { it.recordType == RecordType.NORMAL }
        .sumOf { it.pagesRead }

    val totalChapters: Int = records
        .filter { it.recordType == RecordType.NORMAL }
        .sumOf { it.chaptersRead ?: 0 }

    val morningReadCount: Int by lazy { countHourRange(records, 5, 9) }
    val nightReadCount: Int by lazy { countHourRange(records, 22, 24) + countHourRange(records, 0, 2) }

    val fiveStarCount: Int = books.count { (it.rating ?: 0f) >= 5.0f }

    val distinctBookTypes: Int = books.map { it.bookType }.distinct().size

    /** 某本书的第一条 NORMAL 记录到最后一条 NORMAL 记录跨越天数 */
    val longestDeepReadDays: Int by lazy {
        records
            .filter { it.recordType == RecordType.NORMAL && it.bookId != null }
            .groupBy { it.bookId!! }
            .values
            .maxOfOrNull { list ->
                val minDay = getStartOfDay(list.minOf { it.date })
                val maxDay = getStartOfDay(list.maxOf { it.date })
                ((maxDay - minDay) / DAY_MS).toInt() + 1
            } ?: 0
    }

    /** 单周(自然周)内 FINISHED 记录数最大值 */
    val maxFinishedInWeek: Int by lazy {
        records
            .filter { it.recordType == RecordType.STATUS_FINISHED }
            .groupBy { yearWeek(it.date) }
            .values
            .maxOfOrNull { it.size } ?: 0
    }

    /** 每个徽章当前的进度值，按 Badge.progressKey 索引 */
    val progressMap: Map<String, Int> by lazy {
        mapOf(
            BadgeCatalog.PROGRESS_FINISHED to finishedBookCount,
            BadgeCatalog.PROGRESS_STREAK to currentStreak,
            BadgeCatalog.PROGRESS_TOTAL_PAGES to totalPages.toInt(),
            BadgeCatalog.PROGRESS_TOTAL_CHAPTERS to totalChapters,
            BadgeCatalog.PROGRESS_DEEP_READ to longestDeepReadDays,
            BadgeCatalog.PROGRESS_MORNING to morningReadCount,
            BadgeCatalog.PROGRESS_NIGHT to nightReadCount,
            BadgeCatalog.PROGRESS_FIVE_STAR to fiveStarCount,
            BadgeCatalog.PROGRESS_DIVERSE to distinctBookTypes,
            BadgeCatalog.PROGRESS_WEEKLY to maxFinishedInWeek
        )
    }
}

private const val DAY_MS = 24L * 60L * 60L * 1000L

private fun countHourRange(records: List<ReadingRecordEntity>, startHourInclusive: Int, endHourExclusive: Int): Int {
    val cal = Calendar.getInstance()
    return records.count { record ->
        if (record.recordType != RecordType.NORMAL) return@count false
        cal.timeInMillis = record.date
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        hour in startHourInclusive until endHourExclusive
    }
}

private fun yearWeek(timestamp: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.WEEK_OF_YEAR)}"
}

/**
 * 连续打卡天数：从「今天或昨天」向前推算。
 * 与 HomeStateCalculator.calculateReadingStreak 语义一致。
 */
internal fun calculateStreak(records: List<ReadingRecordEntity>, now: Long): Int {
    val activeDays = records
        .filter { it.recordType == RecordType.NORMAL }
        .map { getStartOfDay(it.date) }
        .toSortedSet(reverseOrder())
    if (activeDays.isEmpty()) return 0

    val today = getStartOfDay(now)
    val yesterday = today - DAY_MS
    var cursor = when {
        activeDays.first() == today -> today
        activeDays.first() == yesterday -> yesterday
        else -> return 0
    }
    var streak = 0
    for (day in activeDays) {
        if (day == cursor) {
            streak++
            cursor -= DAY_MS
        } else if (day < cursor) {
            break
        }
    }
    return streak
}

/**
 * 计算目前应该达成的徽章 id 列表(不区分是否已获得)。
 */
fun evaluateBadges(snapshot: BadgeCheckSnapshot): Set<String> {
    val awarded = mutableSetOf<String>()

    // 数量 (8)
    if (snapshot.finishedBookCount >= 1) awarded += "reader_first"
    if (snapshot.finishedBookCount >= 3) awarded += "reader_3"
    if (snapshot.finishedBookCount >= 5) awarded += "reader_5"
    if (snapshot.finishedBookCount >= 10) awarded += "reader_10"
    if (snapshot.finishedBookCount >= 20) awarded += "reader_20"
    if (snapshot.finishedBookCount >= 50) awarded += "reader_50"
    if (snapshot.finishedBookCount >= 100) awarded += "reader_100"
    if (snapshot.finishedBookCount >= 300) awarded += "reader_300"

    // 连续 (7)
    if (snapshot.currentStreak >= 3) awarded += "streak_3"
    if (snapshot.currentStreak >= 7) awarded += "streak_7"
    if (snapshot.currentStreak >= 14) awarded += "streak_14"
    if (snapshot.currentStreak >= 30) awarded += "streak_30"
    if (snapshot.currentStreak >= 60) awarded += "streak_60"
    if (snapshot.currentStreak >= 100) awarded += "streak_100"
    if (snapshot.currentStreak >= 365) awarded += "streak_365"

    // 页数 (6)
    if (snapshot.totalPages >= 500) awarded += "pages_500"
    if (snapshot.totalPages >= 1000) awarded += "pages_1k"
    if (snapshot.totalPages >= 5000) awarded += "pages_5k"
    if (snapshot.totalPages >= 10000) awarded += "pages_10k"
    if (snapshot.totalPages >= 50000) awarded += "pages_50k"
    if (snapshot.totalPages >= 100000) awarded += "pages_100k"

    // 章节 (4)
    if (snapshot.totalChapters >= 100) awarded += "chapters_100"
    if (snapshot.totalChapters >= 500) awarded += "chapters_500"
    if (snapshot.totalChapters >= 1000) awarded += "chapters_1000"
    if (snapshot.totalChapters >= 5000) awarded += "chapters_5000"

    // 深度
    if (snapshot.longestDeepReadDays >= 30) awarded += "deep_30"
    if (snapshot.longestDeepReadDays >= 100) awarded += "deep_100"

    // 时段
    if (snapshot.morningReadCount >= 10) awarded += "early_bird"
    if (snapshot.nightReadCount >= 10) awarded += "night_owl"

    // 特殊
    if (snapshot.fiveStarCount >= 5) awarded += "five_star"
    if (snapshot.distinctBookTypes >= BookType.entries.size) awarded += "diverse_reader"
    if (snapshot.maxFinishedInWeek >= 3) awarded += "weekly_sprint"

    return awarded
}
