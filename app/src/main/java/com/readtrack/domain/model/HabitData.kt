package com.readtrack.domain.model

/** 时间段定义。startHour 含, endHour 不含。 */
enum class TimeSlot(val label: String, val startHour: Int, val endHour: Int, val colorHex: Long) {
    DAWN("凌晨", 0, 6, 0xFF37474F),
    MORNING("早晨", 6, 9, 0xFFFF8F00),
    LATE_MORNING("上午", 9, 12, 0xFF42A5F5),
    AFTERNOON("下午", 12, 18, 0xFFFF7043),
    EVENING("晚上", 18, 22, 0xFF7E57C2),
    NIGHT("深夜", 22, 24, 0xFF1A237E)
}

data class TimeSlotDistribution(
    val slot: TimeSlot,
    val recordCount: Int,
    val percentage: Float
)

data class DayOfWeekActivity(
    val dayIndex: Int,        // 1=周一 … 7=周日
    val dayLabel: String,
    val activeDays: Int,
    val isMostActive: Boolean
)

data class TypePreference(
    val bookType: String,     // "NOVEL" / "COMIC" / "AUDIOBOOK"
    val typeLabel: String,
    val pagesRead: Double,
    val percentage: Float,
    val bookCount: Int
)

data class ReadingSpeed(
    val avgPagesPerSession: Double,
    val avgChaptersPerSession: Double,
    val favoriteTimeLabel: String,
    val favoriteDayLabel: String
)

data class HabitDashboardData(
    val totalActiveDays: Int,
    val streakDays: Int,
    val favoriteTimeLabel: String,
    val totalRecords: Int,
    val timeDistribution: List<TimeSlotDistribution>,
    val weeklyActivity: List<DayOfWeekActivity>,
    val typePreference: List<TypePreference>,
    val readingSpeed: ReadingSpeed
)
