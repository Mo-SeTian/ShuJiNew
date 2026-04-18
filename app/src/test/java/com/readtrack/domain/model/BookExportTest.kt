package com.readtrack.domain.model

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.viewmodel.ProgressType
import org.junit.Assert.assertEquals
import org.junit.Test

class BookExportTest {

    @Test
    fun toEntity_preserves_currentChapter_when_totalChapters_gt_zero() {
        // Chapter-mode book: read 10 of 50 chapters
        val export = BookExport(
            id = 1, title = "Test Book", progressType = "CHAPTER",
            totalPages = 0.0, currentPage = 0.0,
            totalChapters = 50, currentChapter = 10,
            status = "READING", createdAt = 1L, updatedAt = 2L
        )
        val entity = export.toEntity()
        assertEquals(10, entity.currentChapter)
        assertEquals(50, entity.totalChapters)
        assertEquals(ProgressType.CHAPTER, entity.progressType)
    }

    @Test
    fun toEntity_uses_0_when_currentChapter_is_null_and_totalChapters_gt_0() {
        // Chapter-mode book: read 0 of 50 chapters
        val export = BookExport(
            id = 1, title = "Test Book", progressType = "CHAPTER",
            totalPages = 0.0, currentPage = 0.0,
            totalChapters = 50, currentChapter = null,
            status = "READING", createdAt = 1L, updatedAt = 2L
        )
        val entity = export.toEntity()
        assertEquals(0, entity.currentChapter)
    }

    @Test
    fun toEntity_uses_0_when_totalChapters_is_null_page_mode_book() {
        // Page-mode book: currentChapter should always be 0
        val export = BookExport(
            id = 1, title = "Test Book", progressType = "PAGE",
            totalPages = 302.0, currentPage = 120.0,
            totalChapters = null, currentChapter = null,
            status = "READING", createdAt = 1L, updatedAt = 2L
        )
        val entity = export.toEntity()
        assertEquals(0, entity.currentChapter)
        assertEquals(null, entity.totalChapters)
        assertEquals(ProgressType.PAGE, entity.progressType)
    }

    @Test
    fun toEntity_roundtrip_preserves_chapter_data() {
        // Simulate a book entity as it exists in the database
        val originalEntity = BookEntity(
            id = 0,
            title = "三体",
            author = "刘慈欣",
            progressType = ProgressType.CHAPTER,
            totalPages = 0.0,
            currentPage = 0.0,
            totalChapters = 50,
            currentChapter = 30,
            status = BookStatus.READING,
            createdAt = 1_000_000L,
            updatedAt = 2_000_000L,
            lastReadAt = 2_500_000L
        )

        // Export to backup format
        val export = BookExport.fromEntity(originalEntity)

        // Verify export data
        assertEquals(50, export.totalChapters)
        assertEquals(30, export.currentChapter)
        assertEquals("CHAPTER", export.progressType)

        // Import back
        val restoredEntity = export.toEntity()

        // Verify restored data
        assertEquals(30, restoredEntity.currentChapter)
        assertEquals(50, restoredEntity.totalChapters)
        assertEquals(ProgressType.CHAPTER, restoredEntity.progressType)
        assertEquals("三体", restoredEntity.title)
    }

    @Test
    fun toEntity_currentChapter_is_0_when_totalChapters_is_0() {
        // Edge case: totalChapters explicitly set to 0 in backup
        // This represents a PAGE-mode book (chapter mode never activated)
        // currentChapter should be 0 (the database default)
        val export = BookExport(
            id = 1, title = "Test Book", progressType = "PAGE",
            totalPages = 100.0, currentPage = 50.0,
            totalChapters = 0, currentChapter = 5,
            status = "READING", createdAt = 1L, updatedAt = 2L
        )
        val entity = export.toEntity()
        // totalChapters = 0 means chapter mode not active → currentChapter should be 0
        assertEquals(0, entity.currentChapter)
        assertEquals(0, entity.totalChapters)
    }

    @Test
    fun toEntity_preserves_chapter_data_in_normal_scenario() {
        // Normal chapter-based book with 10 chapters read
        val export = BookExport(
            id = 1, title = "Test Book", progressType = "CHAPTER",
            totalPages = 0.0, currentPage = 0.0,
            totalChapters = 50, currentChapter = 10,
            status = "READING", createdAt = 1L, updatedAt = 2L
        )
        val entity = export.toEntity()
        assertEquals(10, entity.currentChapter)
        assertEquals(50, entity.totalChapters)
    }
}
