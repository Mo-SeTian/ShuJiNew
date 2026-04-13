package com.readtrack.domain.model

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity

/**
 * 数据备份模型 - 用于导入导出
 */
data class DataBackup(
    val version: Int = 1,
    val exportTime: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val books: List<BookExport>,
    val readingRecords: List<ReadingRecordExport>
)

/**
 * 书籍导出模型
 */
data class BookExport(
    val id: Long,
    val title: String,
    val author: String?,
    val publisher: String?,
    val progressType: String,
    val totalPages: Double,
    val currentPage: Double,
    val totalChapters: Int,
    val currentChapter: Int,
    val coverPath: String?,
    val description: String?,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun fromEntity(book: BookEntity): BookExport = BookExport(
            id = book.id,
            title = book.title,
            author = book.author,
            publisher = book.publisher,
            progressType = book.progressType.name,
            totalPages = book.totalPages,
            currentPage = book.currentPage,
            totalChapters = book.totalChapters ?: 0,
            currentChapter = book.currentChapter,
            coverPath = book.coverPath,
            description = book.description,
            status = book.status.name,
            createdAt = book.createdAt,
            updatedAt = book.updatedAt
        )
    }
    
    fun toEntity(): BookEntity = BookEntity(
        id = 0, // 导入时生成新ID
        title = title,
        author = author,
        publisher = publisher,
        progressType = com.readtrack.domain.model.ProgressType.valueOf(progressType),
        totalPages = totalPages,
        currentPage = currentPage,
        totalChapters = if (totalChapters > 0) totalChapters else null,
        currentChapter = currentChapter,
        coverPath = coverPath,
        description = description,
        status = com.readtrack.domain.model.BookStatus.valueOf(status),
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * 阅读记录导出模型
 */
data class ReadingRecordExport(
    val id: Long,
    val bookId: Long, // 导出时用外部ID引用
    val bookTitle: String, // 用于匹配导入后的书籍
    val pagesRead: Double,
    val startPage: Double,
    val endPage: Double,
    val duration: Long,
    val note: String?,
    val recordDate: Long,
    val createdAt: Long
) {
    companion object {
        fun fromEntity(record: ReadingRecordEntity, bookTitle: String): ReadingRecordExport = 
            ReadingRecordExport(
                id = record.id,
                bookId = record.bookId,
                bookTitle = bookTitle,
                pagesRead = record.pagesRead,
                startPage = record.startPage,
                endPage = record.endPage,
                duration = record.duration,
                note = record.note,
                recordDate = record.recordDate,
                createdAt = record.createdAt
            )
    }
}
