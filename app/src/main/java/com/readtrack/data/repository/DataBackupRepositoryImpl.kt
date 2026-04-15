package com.readtrack.data.repository

import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.model.BookExport
import com.readtrack.domain.model.DataBackup
import com.readtrack.domain.model.ImportResult
import com.readtrack.domain.model.ReadingRecordExport
import com.readtrack.domain.repository.DataBackupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataBackupRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val recordDao: ReadingRecordDao
) : DataBackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

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

            // 创建旧书ID -> 新书ID 的映射
            val oldIdToNewId = mutableMapOf<Long, Long>()

            if (clearExisting) {
                // 整表清空，阅读记录外键级联删除
                recordDao.deleteAllRecords()
                bookDao.deleteAllBooks()
            }

            // 追加导入时：查询现有书籍用于去重（按 title + author 匹配）
            val existingBooks = if (!clearExisting) {
                bookDao.getAllBooks().first()
            } else {
                emptyList()
            }
            val existingKeys = existingBooks.map { "${it.title}::${it.author ?: ""}" }.toSet()

            // 导入书籍
            backup.books.forEach { bookExport ->
                try {
                    // 追加导入去重：若已存在相同 title + author 的书，跳过
                    val key = "${bookExport.title}::${bookExport.author ?: ""}"
                    if (!clearExisting && key in existingKeys) {
                        // 匹配到已有书籍，建立旧ID到新ID的映射（指向已有书籍）
                        val matched = existingBooks.first { "${it.title}::${it.author ?: ""}" == key }
                        oldIdToNewId[bookExport.id] = matched.id
                        return@forEach
                    }

                    val newBook = bookExport.toEntity()
                    val newId = bookDao.insertBook(newBook)
                    oldIdToNewId[bookExport.id] = newId
                    booksImported++
                } catch (e: Exception) {
                    errors.add("导入书籍《${bookExport.title}》失败: ${e.message}")
                }
            }

            // 追加导入时：查询现有阅读记录用于去重（按 bookId+date 匹配）
            val existingRecords = if (!clearExisting) {
                recordDao.getAllRecords().first()
            } else {
                emptyList()
            }
            val existingRecordKeys = existingRecords.map { "${it.bookId}::${it.date}" }.toSet()

            // 导入阅读记录
            backup.readingRecords.forEach { recordExport ->
                try {
                    val newBookId = oldIdToNewId[recordExport.bookId]
                    if (newBookId != null) {
                        // 追加导入去重：若已存在相同 bookId+date 的记录，跳过
                        val recordKey = "${newBookId}::${recordExport.date}"
                        if (!clearExisting && recordKey in existingRecordKeys) {
                            return@forEach
                        }

                        val newRecord = ReadingRecordEntity(
                            id = 0,
                            bookId = newBookId,
                            pagesRead = recordExport.pagesRead,
                            fromPage = recordExport.fromPage,
                            toPage = recordExport.toPage,
                            date = recordExport.date,
                            note = recordExport.note
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
            emit(json.encodeToString(backup))
        }
    }

    override fun parseBackupFromJson(jsonString: String): DataBackup? {
        return try {
            json.decodeFromString<DataBackup>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
}
