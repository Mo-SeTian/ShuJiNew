package com.readtrack.data.repository

import android.content.Context
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.BookListDao
import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.BookListCrossRef
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookExport
import com.readtrack.domain.model.BookListExport
import com.readtrack.domain.model.BookSnapshot
import com.readtrack.domain.model.DataBackup
import com.readtrack.domain.model.ImportPreview
import com.readtrack.domain.model.ImportResult
import com.readtrack.domain.model.PreferencesExport
import com.readtrack.domain.model.ReadingRecordExport
import com.readtrack.domain.model.buildImportPreview
import com.readtrack.domain.repository.DataBackupRepository
import com.readtrack.util.CoverStorageUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataBackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val recordDao: ReadingRecordDao,
    private val bookListDao: BookListDao,
    private val preferencesManager: PreferencesManager,
    private val coverStorageUtil: CoverStorageUtil
) : DataBackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** 临时目录：用于打包封面图片 */
    private val tempDir: File
        get() = File(context.cacheDir, "backup_temp").also { it.mkdirs() }

    // ─── 导出 ──────────────────────────────────────────────────────────────────

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

            // 导出用户设置
            val preferences = preferencesManager.exportPreferences()

            val backup = DataBackup(
                books = bookExports,
                readingRecords = records,
                bookLists = bookListExports,
                preferences = preferences
            )

            Result.success(backup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 导出为 ZIP 文件（包含 JSON 数据文件 + 封面图片目录）
     * @return ZIP 文件路径
     */
    suspend fun exportToZip(): Result<File> {
        return try {
            val backupResult = exportAllData()
            if (backupResult.isFailure) return Result.failure(backupResult.exceptionOrNull()!!)

            val backup = backupResult.getOrThrow()

            // 收集需要打包的封面图片
            val coverPathsToInclude = mutableSetOf<String>()

            backup.books.forEach { book ->
                book.coverPath?.let { path ->
                    if (!path.startsWith("http://") && !path.startsWith("https://") &&
                        !path.startsWith("emoji://") && !path.startsWith("color://")) {
                        coverPathsToInclude.add(path)
                    }
                }
            }
            backup.bookLists.forEach { list ->
                list.coverPath?.let { path ->
                    if (!path.startsWith("http://") && !path.startsWith("https://") &&
                        !path.startsWith("emoji://") && !path.startsWith("color://")) {
                        coverPathsToInclude.add(path)
                    }
                }
            }

            // 打包 ZIP
            val zipFile = File(tempDir, "readtrack_backup_${System.currentTimeMillis()}.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
                // 写入 data.json
                val jsonBytes = json.encodeToString(backup).toByteArray(Charsets.UTF_8)
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write(jsonBytes)
                zip.closeEntry()

                // 写入封面图片
                coverPathsToInclude.forEach { coverPath ->
                    val coverFile = File(coverPath)
                    if (coverFile.exists()) {
                        zip.putNextEntry(ZipEntry("covers/${coverFile.name}"))
                        FileInputStream(coverFile).use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }

            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 导出纯 JSON（不含封面），用于 WebDAV 自动备份等场景
     */
    override fun getExportJson(): Flow<String> = flow {
        val backupResult = exportAllData()
        backupResult.getOrNull()?.let { backup ->
            emit(json.encodeToString(backup))
        }
    }

    // ─── 导入 ──────────────────────────────────────────────────────────────────

    override suspend fun importData(backup: DataBackup, clearExisting: Boolean): Result<ImportResult> {
        return try {
            val errors = mutableListOf<String>()
            var booksImported = 0
            var recordsImported = 0
            var bookListsImported = 0

            val oldIdToNewId = mutableMapOf<Long, Long>()
            val oldIdToNewBook = mutableMapOf<Long, BookEntity>()

            if (clearExisting) {
                recordDao.deleteAllRecords()
                bookDao.deleteAllBooks()
                bookListDao.deleteAllBookLists()
            }

            val existingBooks = if (!clearExisting) {
                bookDao.getAllBooks().first()
            } else {
                emptyList()
            }
            val existingKeys = existingBooks.map { "${it.title}::${it.author ?: ""}" }.toSet()

            // 导入书籍
            backup.books.forEach { bookExport ->
                try {
                    val key = "${bookExport.title}::${bookExport.author ?: ""}"
                    if (!clearExisting && key in existingKeys) {
                        val matched = existingBooks.first { "${it.title}::${it.author ?: ""}" == key }
                        oldIdToNewId[bookExport.id] = matched.id
                        oldIdToNewBook[bookExport.id] = matched
                        return@forEach
                    }

                    val newBook = bookExport.toEntity()
                    val newId = bookDao.insertBook(newBook)
                    oldIdToNewId[bookExport.id] = newId
                    oldIdToNewBook[bookExport.id] = newBook.copy(id = newId)
                    booksImported++
                } catch (e: Exception) {
                    errors.add("导入书籍《${bookExport.title}》失败: ${e.message}")
                }
            }

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
                        val recordKey = "${newBookId}::${recordExport.date}"
                        if (!clearExisting && recordKey in existingRecordKeys) {
                            return@forEach
                        }

                        val newRecord = ReadingRecordEntity(
                            id = 0,
                            bookId = newBookId,
                            bookSnapshot = oldIdToNewBook[recordExport.bookId]?.let { BookSnapshot.from(it, it.status) },
                            pagesRead = recordExport.pagesRead,
                            fromPage = recordExport.fromPage,
                            toPage = recordExport.toPage,
                            chaptersRead = recordExport.chaptersRead,
                            date = recordExport.date,
                            note = recordExport.note,
                            recordType = try { RecordType.valueOf(recordExport.recordType) } catch (e: Exception) { RecordType.NORMAL }
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
            val existingBookListNames = if (!clearExisting) {
                bookListDao.getAllBookLists().first().map { it.name }.toSet()
            } else {
                emptySet()
            }

            backup.bookLists.forEach { bookListExport ->
                try {
                    if (!clearExisting && bookListExport.name in existingBookListNames) {
                        return@forEach
                    }

                    val mappedCoverBookId = bookListExport.coverBookId?.let { oldId -> oldIdToNewId[oldId] }

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

                    val validBookIds = bookListExport.bookIds.mapNotNull { oldBookId -> oldIdToNewId[oldBookId] }
                    if (validBookIds.isNotEmpty()) {
                        val crossRefs = validBookIds.map { BookListCrossRef(newBookListId, it) }
                        bookListDao.addBooksToList(crossRefs)
                        bookListDao.updateBookCount(newBookListId)
                    }
                } catch (e: Exception) {
                    errors.add("导入书单「${bookListExport.name}」失败: ${e.message}")
                }
            }

            // 恢复用户设置（始终追加模式，不覆盖运行时状态）
            if (backup.preferences != null) {
                try {
                    preferencesManager.importPreferences(backup.preferences)
                } catch (e: Exception) {
                    errors.add("恢复用户设置失败: ${e.message}")
                }
            }

            Result.success(ImportResult(booksImported, recordsImported, bookListsImported, errors))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从 ZIP 文件提取 data.json 并预览（不解压封面，不执行导入）
     */
    suspend fun importFromZipForPreview(zipFile: File): Result<ImportPreview> {
        return try {
            val jsonContent: String
            ZipInputStream(FileInputStream(zipFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "data.json") {
                        jsonContent = zip.readBytes().toString(Charsets.UTF_8)
                        val backup = parseBackupFromJson(jsonContent)
                            ?: return Result.failure(IllegalStateException("ZIP 中 data.json 格式无效"))
                        return previewImport(backup)
                    }
                    entry = zip.nextEntry
                }
            }
            Result.failure(IllegalStateException("ZIP 中未找到 data.json"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从 ZIP 文件导入（包含封面图片自动解压到 covers 目录）
     */
    suspend fun importFromZip(zipFile: File, clearExisting: Boolean): Result<ImportResult> {
        return try {
            var jsonContent: String = ""
            val extractedCoverFiles = mutableListOf<File>()

            ZipInputStream(FileInputStream(zipFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "data.json" -> {
                            jsonContent = zip.readBytes().toString(Charsets.UTF_8)
                        }
                        entry.name.startsWith("covers/") && !entry.isDirectory -> {
                            val fileName = entry.name.removePrefix("covers/")
                            val destFile = File(coverStorageUtil.coversDir, fileName)
                            FileOutputStream(destFile).use { output -> zip.copyTo(output) }
                            extractedCoverFiles.add(destFile)
                        }
                    }
                    entry = zip.nextEntry
                }
            }

            val backup = parseBackupFromJson(jsonContent)
                ?: return Result.failure(IllegalStateException("ZIP 中未找到有效的 data.json"))

            // 导入数据（coverPath 已经是解压后的本地路径，可直接使用）
            val result = importData(backup, clearExisting)

            // 清理临时 ZIP
            zipFile.delete()

            result
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

    override fun parseBackupFromJson(json: String): DataBackup? {
        return try {
            this.json.decodeFromString<DataBackup>(json)
        } catch (e: Exception) {
            null
        }
    }
}
