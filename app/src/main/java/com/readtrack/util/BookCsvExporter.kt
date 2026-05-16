package com.readtrack.util

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.model.ProgressType

object BookCsvExporter {

    fun exportToCsv(books: List<BookEntity>): String {
        val sb = StringBuilder()
        sb.append("﻿") // BOM for Excel UTF-8 compatibility
        sb.appendLine("标题,作者,状态,类型,评分,总页数/章节,当前进度,进度百分比")

        for (book in books) {
            val title = escapeCsv(book.title)
            val author = escapeCsv(book.author ?: "")
            val status = statusLabel(book.status)
            val type = book.bookType.displayName
            val rating = if (book.rating != null && book.rating!! > 0) "${book.rating}" else "未评分"
            val totalStr = if (book.progressType == ProgressType.CHAPTER)
                "共 ${book.totalChapters ?: 0} 章" else "共 ${book.totalPages.toInt()} 页"
            val currentStr = if (book.progressType == ProgressType.CHAPTER)
                "第 ${book.currentChapter} 章" else "${book.currentPage.toInt()} 页"
            val progressPercent = when {
                book.progressType == ProgressType.CHAPTER && (book.totalChapters ?: 0) > 0 ->
                    "${(book.currentChapter * 100 / (book.totalChapters ?: 1))}%"
                book.totalPages > 0 ->
                    "${(book.currentPage * 100 / book.totalPages).toInt()}%"
                else -> "0%"
            }

            sb.appendLine("$title,$author,$status,$type,$rating,$totalStr,$currentStr,$progressPercent")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }

    private fun statusLabel(status: BookStatus): String = when (status) {
        BookStatus.WANT_TO_READ -> "想读"
        BookStatus.READING -> "在读"
        BookStatus.FINISHED -> "已读"
        BookStatus.ON_HOLD -> "闲置"
        BookStatus.ABANDONED -> "放弃"
    }
}
