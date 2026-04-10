package com.readtrack.data.backup

import android.content.Context
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.model.BookStatus
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class BackupData(
    val books: List<BookEntity>,
    val records: List<ReadingRecordEntity>,
    val exportTime: Long = System.currentTimeMillis(),
    val version: Int = 1
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val recordRepository: ReadingRecordRepository
) {
    suspend fun createBackup(): BackupData {
        val books = bookRepository.getAllBooks().first()
        val records = recordRepository.getAllRecords().first()
        return BackupData(books, records)
    }
    
    fun toJson(data: BackupData): String {
        val json = JSONObject()
        json.put("version", data.version)
        json.put("exportTime", data.exportTime)
        
        val booksArray = JSONArray()
        data.books.forEach { book ->
            val bookJson = JSONObject().apply {
                put("id", book.id)
                put("title", book.title)
                put("author", book.author)
                put("totalPages", book.totalPages)
                put("currentPage", book.currentPage)
                put("coverPath", book.coverPath)
                put("status", book.status.name)
                put("createdAt", book.createdAt)
                put("updatedAt", book.updatedAt)
                put("lastReadAt", book.lastReadAt)
            }
            booksArray.put(bookJson)
        }
        json.put("books", booksArray)
        
        val recordsArray = JSONArray()
        data.records.forEach { record ->
            val recordJson = JSONObject().apply {
                put("id", record.id)
                put("bookId", record.bookId)
                put("pagesRead", record.pagesRead)
                put("fromPage", record.fromPage)
                put("toPage", record.toPage)
                put("date", record.date)
                put("note", record.note)
            }
            recordsArray.put(recordJson)
        }
        json.put("records", recordsArray)
        
        return json.toString(2)
    }
    
    suspend fun fromJson(jsonString: String): BackupData {
        val json = JSONObject(jsonString)
        
        val books = mutableListOf<BookEntity>()
        val booksArray = json.getJSONArray("books")
        for (i in 0 until booksArray.length()) {
            val bookJson = booksArray.getJSONObject(i)
            books.add(BookEntity(
                id = bookJson.getLong("id"),
                title = bookJson.getString("title"),
                author = bookJson.optString("author").takeIf { it.isNotEmpty() },
                totalPages = bookJson.getDouble("totalPages"),
                currentPage = bookJson.getDouble("currentPage"),
                coverPath = bookJson.optString("coverPath").takeIf { it.isNotEmpty() },
                status = BookStatus.valueOf(bookJson.getString("status")),
                createdAt = bookJson.getLong("createdAt"),
                updatedAt = bookJson.getLong("updatedAt"),
                lastReadAt = if (bookJson.has("lastReadAt") && !bookJson.isNull("lastReadAt")) 
                    bookJson.getLong("lastReadAt") else null
            ))
        }
        
        val records = mutableListOf<ReadingRecordEntity>()
        val recordsArray = json.getJSONArray("records")
        for (i in 0 until recordsArray.length()) {
            val recordJson = recordsArray.getJSONObject(i)
            records.add(ReadingRecordEntity(
                id = recordJson.getLong("id"),
                bookId = recordJson.getLong("bookId"),
                pagesRead = recordJson.getDouble("pagesRead"),
                fromPage = recordJson.getDouble("fromPage"),
                toPage = recordJson.getDouble("toPage"),
                date = recordJson.getLong("date"),
                note = recordJson.optString("note").takeIf { it.isNotEmpty() }
            ))
        }
        
        return BackupData(books, records, json.getLong("exportTime"), json.optInt("version", 1))
    }
}
