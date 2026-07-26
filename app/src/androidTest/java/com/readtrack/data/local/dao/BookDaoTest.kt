package com.readtrack.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.readtrack.data.local.database.ReadTrackDatabase
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.BookStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDaoTest {

    private lateinit var db: ReadTrackDatabase
    private lateinit var dao: BookDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, ReadTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.bookDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndFetchBook() = runTest {
        val id = dao.insertBook(BookEntity(title = "深入理解Kotlin", author = "郭霖"))
        val book = dao.getBookByIdOnce(id)
        assertNotNull(book)
        assertEquals("深入理解Kotlin", book?.title)
    }

    @Test
    fun filterByStatus() = runTest {
        dao.insertBook(BookEntity(title = "A", status = BookStatus.READING))
        dao.insertBook(BookEntity(title = "B", status = BookStatus.WANT_TO_READ))
        dao.insertBook(BookEntity(title = "C", status = BookStatus.READING))

        val reading = dao.getBooksByStatus(BookStatus.READING).first()
        assertEquals(2, reading.size)
    }

    @Test
    fun searchByTitle() = runTest {
        dao.insertBook(BookEntity(title = "追风筝的人", author = "胡赛尼"))
        dao.insertBook(BookEntity(title = "百年孤独", author = "马尔克斯"))

        val results = dao.searchBooks("孤独").first()
        assertEquals(1, results.size)
        assertEquals("百年孤独", results.first().title)
    }

    @Test
    fun deleteBook() = runTest {
        val id = dao.insertBook(BookEntity(title = "测试"))
        dao.deleteBookById(id)
        assertEquals(null, dao.getBookByIdOnce(id))
    }
}
