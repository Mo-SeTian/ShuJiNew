package com.readtrack.presentation.ui.components

import androidx.compose.runtime.Immutable
import com.readtrack.data.local.entity.BookEntity

@Immutable
data class BookProgressUiModel(
    val progressPercent: Int,
    val progressFraction: Float,
    val progressLabel: String,
    val isChapterBased: Boolean
)

fun BookEntity.toBookProgressUiModel(): BookProgressUiModel {
    val chapterBased = (totalChapters ?: 0) > 0

    val progressPercent = when {
        chapterBased -> {
            val total = (totalChapters ?: 0).coerceAtLeast(1)
            ((currentChapter.toFloat() / total) * 100f).toInt()
        }
        totalPages > 0 -> ((currentPage.toFloat() / totalPages.toFloat()) * 100f).toInt()
        else -> 0
    }.coerceIn(0, 100)

    return BookProgressUiModel(
        progressPercent = progressPercent,
        progressFraction = (progressPercent / 100f).coerceIn(0f, 1f),
        progressLabel = if (chapterBased) {
            "$currentChapter/${totalChapters ?: 0} 章"
        } else {
            "${currentPage.toInt()}/${totalPages.toInt()} 页"
        },
        isChapterBased = chapterBased
    )
}
