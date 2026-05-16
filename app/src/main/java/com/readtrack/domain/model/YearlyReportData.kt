package com.readtrack.domain.model

import com.readtrack.data.local.entity.BookEntity

data class YearlyReportData(
    val year: Int,
    val totalBooksRead: Int,
    val finishedBooks: Int,
    val totalPages: Double,
    val totalChapters: Double,
    val averageRating: Float,
    val monthlyPages: List<Float>,
    val monthlyChapters: List<Float>,
    val favoriteBook: BookEntity?,
    val thickestBook: BookEntity?,
    val longestBook: BookEntity?,
    val topGenre: String,
    val maxStreakDays: Int,
    val activeDays: Int,
    val favoriteMonth: Int,
    val availableYears: List<Int>
)
