package com.readtrack.domain.model

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.presentation.viewmodel.ProgressType
import kotlinx.serialization.Serializable

/**
 * 书单导出模型
 */
@Serializable
data class BookListExport(
    val id: Long,
    val name: String,
    val description: String? = null,
    val coverPath: String? = null,
    val coverBookId: Long? = null,
    val bookIds: List<Long> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 数据备份模型 - 用于导入导出
 */
@Serializable
data class DataBackup(
    val version: Int = 3,
    val exportTime: Long = System.currentTimeMillis(),
    val appVersion: String = "1.1.0",
    val books: List<BookExport> = emptyList(),
    val readingRecords: List<ReadingRecordExport> = emptyList(),
    val bookLists: List<BookListExport> = emptyList()
)

/**
 * 书籍导出模型
 */
@Serializable
data class BookExport(
    val id: Long,
    val title: String,
    val author: String? = null,
    val publisher: String? = null,
    val progressType: String,
    val totalPages: Double,
    val currentPage: Double,
    val totalChapters: Int,
    val currentChapter: Int,
    val coverPath: String? = null,
    val description: String? = null,
    val status: String,
    val rating: Float? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val lastReadAt: Long? = null
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
            rating = book.rating,
            createdAt = book.createdAt,
            updatedAt = book.updatedAt,
            lastReadAt = book.lastReadAt
        )
    }

    fun toEntity(): BookEntity = BookEntity(
        id = 0, // 导入时生成新ID
        title = title,
        author = author,
        publisher = publisher,
        progressType = ProgressType.valueOf(progressType),
        totalPages = totalPages,
        currentPage = currentPage,
        totalChapters = if (totalChapters > 0) totalChapters else null,
        currentChapter = currentChapter,
        coverPath = coverPath,
        description = description,
        status = BookStatus.valueOf(status),
        rating = rating,
        createdAt = createdAt,
        updatedAt = updatedAt,    // 保留原始时间，不覆盖
        lastReadAt = lastReadAt   // 保留原始时间
    )
}

/**
 * 阅读记录导出模型
 */
@Serializable
data class ReadingRecordExport(
    val id: Long,
    val bookId: Long?,        // 可空：图书删除后为 null
    val bookTitle: String,     // 用于匹配导入后的书籍
    val pagesRead: Double,
    val fromPage: Double,
    val toPage: Double,
    val date: Long,
    val note: String? = null
) {
    companion object {
        fun fromEntity(record: ReadingRecordEntity, bookTitle: String): ReadingRecordExport =
            ReadingRecordExport(
                id = record.id,
                bookId = record.bookId,
                bookTitle = bookTitle,
                pagesRead = record.pagesRead,
                fromPage = record.fromPage,
                toPage = record.toPage,
                date = record.date,
                note = record.note
            )
    }
}

/**
 * 导入前安全预览
 */
data class ImportPreview(
    val exportTime: Long,
    val appVersion: String,
    val backupBookCount: Int,
    val backupRecordCount: Int,
    val backupBookListCount: Int,
    val existingBookCount: Int,
    val existingRecordCount: Int,
    val existingBookListCount: Int,
    val duplicateBookCount: Int,
    val duplicateRecordCount: Int,
    val skippedOrphanRecordCount: Int,
    val appendBookCount: Int,
    val appendRecordCount: Int,
    val appendBookListCount: Int
)

fun buildImportPreview(
    backup: DataBackup,
    existingBooks: List<BookEntity>,
    existingRecords: List<ReadingRecordEntity>,
    existingBookLists: List<BookListEntity>
): ImportPreview {
    val existingBookByKey = existingBooks.associateBy { it.importIdentityKey() }
    val oldIdToResolvedBookId = mutableMapOf<Long, Long>()
    var duplicateBookCount = 0

    backup.books.forEach { bookExport ->
        val matched = existingBookByKey[bookExport.importIdentityKey()]
        if (matched != null) {
            duplicateBookCount++
            oldIdToResolvedBookId[bookExport.id] = matched.id
        } else {
            oldIdToResolvedBookId[bookExport.id] = bookExport.id
        }
    }

    val existingRecordKeys = existingRecords.mapNotNull { record ->
        record.bookId?.let { bookId -> recordIdentityKey(bookId, record.date) }
    }.toSet()

    var duplicateRecordCount = 0
    var skippedOrphanRecordCount = 0

    backup.readingRecords.forEach { recordExport ->
        val resolvedBookId = recordExport.bookId?.let(oldIdToResolvedBookId::get)
        if (resolvedBookId == null) {
            skippedOrphanRecordCount++
            return@forEach
        }

        if (recordIdentityKey(resolvedBookId, recordExport.date) in existingRecordKeys) {
            duplicateRecordCount++
        }
    }

    val appendBookCount = backup.books.size - duplicateBookCount
    val appendRecordCount = backup.readingRecords.size - duplicateRecordCount - skippedOrphanRecordCount

    return ImportPreview(
        exportTime = backup.exportTime,
        appVersion = backup.appVersion,
        backupBookCount = backup.books.size,
        backupRecordCount = backup.readingRecords.size,
        backupBookListCount = backup.bookLists.size,
        existingBookCount = existingBooks.size,
        existingRecordCount = existingRecords.size,
        existingBookListCount = existingBookLists.size,
        duplicateBookCount = duplicateBookCount,
        duplicateRecordCount = duplicateRecordCount,
        skippedOrphanRecordCount = skippedOrphanRecordCount,
        appendBookCount = appendBookCount.coerceAtLeast(0),
        appendRecordCount = appendRecordCount.coerceAtLeast(0),
        appendBookListCount = backup.bookLists.size
    )
}

private fun BookEntity.importIdentityKey(): String = "${title.trim()}::${author?.trim().orEmpty()}"

private fun BookExport.importIdentityKey(): String = "${title.trim()}::${author?.trim().orEmpty()}"

private fun recordIdentityKey(bookId: Long, date: Long): String = "$bookId::$date"

/**
 * 导入结果
 */
data class ImportResult(
    val booksImported: Int,
    val recordsImported: Int,
    val bookListsImported: Int = 0,
    val errors: List<String> = emptyList()
)
