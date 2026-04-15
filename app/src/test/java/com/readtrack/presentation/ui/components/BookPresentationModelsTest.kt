package com.readtrack.presentation.ui.components

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.presentation.viewmodel.ProgressType
import org.junit.Assert.assertEquals
import org.junit.Test

class BookPresentationModelsTest {

    @Test
    fun toBookProgressUiModel_usesChapterProgressWhenAvailable() {
        val book = BookEntity(
            title = "章节书",
            progressType = ProgressType.CHAPTER,
            totalChapters = 20,
            currentChapter = 5
        )

        val model = book.toBookProgressUiModel()

        assertEquals(25, model.progressPercent)
        assertEquals("5/20 章", model.progressLabel)
    }

    @Test
    fun toBookProgressUiModel_usesPageProgressForPageBooks() {
        val book = BookEntity(
            title = "页码书",
            totalPages = 300.0,
            currentPage = 150.0
        )

        val model = book.toBookProgressUiModel()

        assertEquals(50, model.progressPercent)
        assertEquals("150/300 页", model.progressLabel)
    }
}
