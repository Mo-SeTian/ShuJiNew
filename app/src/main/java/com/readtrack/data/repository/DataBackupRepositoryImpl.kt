package com.readtrack.data.repository

import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.BookListDao
import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.entity.BookListCrossRef
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.model.BookExport
import com.readtrack.domain.model.BookListExport
import com.readtrack.domain.model.DataBackup
import com.readtrack.domain.model.ImportResult
import com.readtrack.domain.model.ReadingRecordExport
import com.readtrack.domain.model.buildImportPreview
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
    private val recordDao: ReadingRecordDao,
    private val bookListDao: BookListDao
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

            // 导出书单
            val allBookLists = bookListDao.getAllBookLists().first()
            val bookListExports = allBookLists.map { bookList ->
                // 获取书单内的书籍ID
                val booksInList = bookListDao.getBooksInBookList(bookList.id).first()
                BookListExport(
                    id = bookList.id,
                    name = bookList.name,
                    description = bookList.description,
                    coverPath = bookList.coverPath,
                    coverBookId = bookList.coverBookId,
                    bookIds = booksInList.map { it.id },
                    createdAt = bookList.createdAt,
                    updatedAt = bookList.updatedAt
                )
            }

            val backup = DataBackup(
                books = bookExports,
                readingRecords = records,
                bookLists = bookListExports
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
            var bookListsImported = 0

            // 创建旧书ID -> 新书ID 的映射
            val oldIdToNewId = mutableMapOf<Long, Long>()

            if (clearExisting) {
                // 整表清空，阅读记录外键级联删除
                recordDao.deleteAllRecords()
                bookDao.deleteAllBooks()
                // 清空书单（书单内的 cross_ref 由外键级联删除）
                bookListDao.deleteAllBookLists()
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

            // 导入书单
            backup.bookLists.forEach { bookListExport ->
                try {
                    val mappedCoverBookId = bookListExport.coverBookId?.let { oldId ->
                        oldIdToNewId[oldId]
                    }

                    val bookList = BookListEntity(
                        id = 0,
                        name = bookListExport.name,
                        description = bookListExport.description,
                        coverPath = bookListExport.coverPath,
                        coverBookId = mappedCoverBookId,
                        bookCount = 0,
                        createdAt = bookListExport.createdAt,
                        updatedAt = bookListExport.updatedAt
                    )
                    val newBookListId = bookListDao.insertBookList(bookList)
                    bookListsImported++

                    // 添加书单内书籍的关联
                    val validBookIds = bookListExport.bookIds.mapNotNull { oldBookId ->
                        oldIdToNewId[oldBookId]
                    }
                    if (validBookIds.isNotEmpty()) {
                        val crossRefs = validBookIds.map { BookListCrossRef(newBookListId, it) }
                        bookListDao.addBooksToList(crossRefs)
                        bookListDao.updateBookCount(newBookListId)
                    }
                } catch (e: Exception) {
                    errors.add("导入书单「${bookListExport.name}」失败: ${e.message}")
                }
            }

            Result.success(ImportResult(booksImported, recordsImported, bookListsImported, errors))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun previewImport(backup: DataBackup) = runCatching {
        buildImportPreview(
            backup = backup,
            existingBooks = bookDao.getAllBooks().first(),
            existingRecords = recordDao.getAllRecords().first(),
            existingBookLists = bookListDao.getAllBookLists().first()
        )
    }

    override fun getExportJson(): Flow<String> = flow {
        val backupResult = exportAllData()
        backupResult.getOrNull()?.let { backup ->
            emit(json.encodeToString(backup))
        }
    }

    override fun parseBackupFromJson(json: String): DataBackup? {
        return try {
            this.json.decodeFromString<DataBackup>(json)
        } catch (e: Exception) {
            null
        }
    }
}
