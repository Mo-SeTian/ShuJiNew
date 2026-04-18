package com.readtrack.domain.model

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportPreviewTest {

    @Test
    fun buildImportPreview_countsDuplicatesNewItemsAndSkippedOrphanRecords() {
        val backup = DataBackup(
            exportTime = 1_710_000_000_000L,
            appVersion = "1.5.0",
            books = listOf(
                BookExport(id = 1, title = "三体", author = "刘慈欣", progressType = "PAGE", totalPages = 302.0, currentPage = 120.0, totalChapters = 0, currentChapter = 0, status = "READING", createdAt = 1L, updatedAt = 2L),
                BookExport(id = 2, title = "活着", author = "余华", progressType = "PAGE", totalPages = 191.0, currentPage = 20.0, totalChapters = 0, currentChapter = 0, status = "WANT_TO_READ", createdAt = 3L, updatedAt = 4L)
            ),
            readingRecords = listOf(
                ReadingRecordExport(id = 1, bookId = 1, bookTitle = "三体", pagesRead = 10.0, fromPage = 110.0, toPage = 120.0, date = 1000L),
                ReadingRecordExport(id = 2, bookId = 2, bookTitle = "活着", pagesRead = 20.0, fromPage = 1.0, toPage = 21.0, date = 2000L),
                ReadingRecordExport(id = 3, bookId = null, bookTitle = "孤立记录", pagesRead = 5.0, fromPage = 1.0, toPage = 6.0, date = 3000L)
            ),
            bookLists = listOf(
                BookListExport(id = 1, name = "科幻", bookIds = listOf(1), createdAt = 10L, updatedAt = 11L),
                BookListExport(id = 2, name = "文学", bookIds = listOf(2), createdAt = 12L, updatedAt = 13L)
            )
        )
        val existingBooks = listOf(
            BookEntity(id = 10, title = "三体", author = "刘慈欣")
        )
        val existingRecords = listOf(
            ReadingRecordEntity(id = 100, bookId = 10, pagesRead = 10.0, fromPage = 110.0, toPage = 120.0, date = 1000L)
        )
        val existingBookLists = listOf(
            BookListEntity(id = 20, name = "已有书单")
        )

        val preview = buildImportPreview(
            backup = backup,
            existingBooks = existingBooks,
            existingRecords = existingRecords,
            existingBookLists = existingBookLists
        )

        assertEquals(2, preview.backupBookCount)
        assertEquals(3, preview.backupRecordCount)
        assertEquals(2, preview.backupBookListCount)
        assertEquals(1, preview.existingBookCount)
        assertEquals(1, preview.existingRecordCount)
        assertEquals(1, preview.existingBookListCount)
        assertEquals(1, preview.duplicateBookCount)
        assertEquals(1, preview.duplicateRecordCount)
        assertEquals(1, preview.skippedOrphanRecordCount)
        assertEquals(1, preview.appendBookCount)
        assertEquals(1, preview.appendRecordCount)
        assertEquals(2, preview.appendBookListCount)
        assertEquals("1.5.0", preview.appVersion)
        assertEquals(1_710_000_000_000L, preview.exportTime)
    }

    @Test
    fun buildImportPreview_matchesBlankAuthorAsSameBook() {
        val backup = DataBackup(
            books = listOf(
                BookExport(id = 1, title = "原则", author = null, progressType = "PAGE", totalPages = 100.0, currentPage = 0.0, totalChapters = 0, currentChapter = 0, status = "WANT_TO_READ", createdAt = 1L, updatedAt = 2L)
            )
        )
        val existingBooks = listOf(
            BookEntity(id = 8, title = "原则", author = "")
        )

        val preview = buildImportPreview(
            backup = backup,
            existingBooks = existingBooks,
            existingRecords = emptyList(),
            existingBookLists = emptyList()
        )

        assertEquals(1, preview.duplicateBookCount)
        assertEquals(0, preview.appendBookCount)
    }
}
