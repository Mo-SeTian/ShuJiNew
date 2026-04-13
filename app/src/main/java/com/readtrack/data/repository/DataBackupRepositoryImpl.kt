package com.readtrack.data.repository

import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.model.BookExport
import com.readtrack.domain.model.DataBackup
import com.readtrack.domain.model.ImportResult
import com.readtrack.domain.model.ReadingRecordExport
import com.readtrack.domain.repository.DataBackupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataBackupRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val recordDao: ReadingRecordDao
) : DataBackupRepository {

    override suspend fun exportAllData(): Result<DataBackup> {
        return try {
            val books = bookDao.getAllBooks().first()
            val bookIdToTitle = books.associate { it.id to it.title }
            
            val records = recordDao.getAllRecords().first().map { record ->
                ReadingRecordExport.fromEntity(record, bookIdToTitle[record.bookId] ?: "Unknown")
            }
            
            val bookExports = books.map { BookExport.fromEntity(it) }
            
            val backup = DataBackup(
                books = bookExports,
                readingRecords = records
            )
            
            Result.success(backup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importData(backup: DataBackup, clearExisting: Boolean): Result<ImportResult> {
        return try {
            val errors = mutableListOf<String>()
            var booksImported = 0
            var recordsImported = 0
            
            // 创建书名到旧ID的映射
            val oldIdToNewId = mutableMapOf<Long, Long>()
            
            if (clearExisting) {
                // 清空现有数据
                bookDao.getAllBooks().first().forEach { book ->
                    bookDao.deleteBook(book.id)
                }
            }
            
            // 导入书籍
            backup.books.forEach { bookExport ->
                try {
                    val newBook = bookExport.toEntity()
                    val newId = bookDao.insertBook(newBook)
                    oldIdToNewId[bookExport.id] = newId
                    booksImported++
                } catch (e: Exception) {
                    errors.add("导入书籍《${bookExport.title}》失败: ${e.message}")
                }
            }
            
            // 导入阅读记录
            backup.readingRecords.forEach { recordExport ->
                try {
                    val newBookId = oldIdToNewId[recordExport.bookId]
                    if (newBookId != null) {
                        val newRecord = ReadingRecordEntity(
                            id = 0,
                            bookId = newBookId,
                            pagesRead = recordExport.pagesRead,
                            startPage = recordExport.startPage,
                            endPage = recordExport.endPage,
                            duration = recordExport.duration,
                            note = recordExport.note,
                            recordDate = recordExport.recordDate,
                            createdAt = recordExport.createdAt
                        )
                        recordDao.insertRecord(newRecord)
                        recordsImported++
                    } else {
                        errors.add("未找到书籍《${recordExport.bookTitle}》，其阅读记录被跳过")
                    }
                } catch (e: Exception) {
                    errors.add("导入阅读记录失败: ${e.message}")
                }
            }
            
            Result.success(ImportResult(booksImported, recordsImported, errors))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getExportJson(): Flow<String> = flow {
        val backupResult = exportAllData()
        backupResult.getOrNull()?.let { backup ->
            val json = JSONObject().apply {
                put("version", backup.version)
                put("exportTime", backup.exportTime)
                put("appVersion", backup.appVersion)
                
                val booksArray = JSONArray()
                backup.books.forEach { book ->
                    booksArray.put(JSONObject().apply {
                        put("id", book.id)
                        put("title", book.title)
                        put("author", book.author ?: JSONObject.NULL)
                        put("publisher", book.publisher ?: JSONObject.NULL)
                        put("progressType", book.progressType)
                        put("totalPages", book.totalPages)
                        put("currentPage", book.currentPage)
                        put("totalChapters", book.totalChapters)
                        put("currentChapter", book.currentChapter)
                        put("coverPath", book.coverPath ?: JSONObject.NULL)
                        put("description", book.description ?: JSONObject.NULL)
                        put("status", book.status)
                        put("createdAt", book.createdAt)
                        put("updatedAt", book.updatedAt)
                    })
                }
                put("books", booksArray)
                
                val recordsArray = JSONArray()
                backup.readingRecords.forEach { record ->
                    recordsArray.put(JSONObject().apply {
                        put("id", record.id)
                        put("bookId", record.bookId)
                        put("bookTitle", record.bookTitle)
                        put("pagesRead", record.pagesRead)
                        put("startPage", record.startPage)
                        put("endPage", record.endPage)
                        put("duration", record.duration)
                        put("note", record.note ?: JSONObject.NULL)
                        put("recordDate", record.recordDate)
                        put("createdAt", record.createdAt)
                    })
                }
                put("readingRecords", recordsArray)
            }
            emit(json.toString(2))
        }
    }
}
