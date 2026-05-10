package com.readtrack.data.repository

import android.content.Context
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.BookListDao
import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.dao.TagDao
import com.readtrack.data.local.database.ReadTrackDatabase
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.BookListCrossRef
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.data.local.entity.RecordType
import com.readtrack.domain.model.BookExport
import com.readtrack.domain.model.BookListExport
import com.readtrack.domain.model.BookSnapshot
import com.readtrack.domain.model.BookTagExport
import com.readtrack.domain.model.DataBackup
import com.readtrack.domain.model.ImportPreview
import com.readtrack.domain.model.ImportResult
import com.readtrack.domain.model.ReadingRecordExport
import com.readtrack.domain.model.TagExport
import com.readtrack.domain.model.buildImportPreview
import com.readtrack.domain.repository.DataBackupRepository
import com.readtrack.domain.repository.TagRepository
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

/** 当前备份格式版本，导入低版本时执行迁移 */
private const val CURRENT_BACKUP_VERSION = 5

@Singleton
class DataBackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ReadTrackDatabase,
    private val bookDao: BookDao,
    private val recordDao: ReadingRecordDao,
    private val bookListDao: BookListDao,
    private val tagDao: TagDao,
    private val preferencesManager: PreferencesManager,
    private val coverStorageUtil: CoverStorageUtil,
    private val tagRepository: TagRepository
) : DataBackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** 临时目录：用于打包封面图片 */
    private val tempDir: File
        get() = File(context.cacheDir, "backup_temp").also { it.mkdirs() }

    /** 缓存 importFromZipForPreview 解析的备份，供 importFromZip 复用，避免二次解析 ZIP */
    private var pendingBackupFromPreview: DataBackup? = null

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
                    showInBooksPage = bookList.showInBooksPage,
                    createdAt = bookList.createdAt,
                    updatedAt = bookList.updatedAt
                )
            }

            // 导出标签（v5+）
            val allTags = tagDao.getAllTags().first()
            val tagExports = allTags.map { TagExport(name = it.name, color = it.color) }

            // 导出书籍-标签关联（批量查询替代逐书查询）
            val bookKeyMap = books.associate { it.id to importIdentityKey(it) }
            val crossRefs = tagDao.getAllBookTagCrossrefs()
            val tagIdToName = allTags.associate { it.id to it.name }
            val bookTagExports = crossRefs.mapNotNull { ref ->
                bookKeyMap[ref.bookId]?.let { key ->
                    tagIdToName[ref.tagId]?.let { name ->
                        BookTagExport(bookImportKey = key, tagName = name)
                    }
                }
            }

            // 导出用户设置
            val preferences = preferencesManager.exportPreferences()

            val backup = DataBackup(
                books = bookExports,
                readingRecords = records,
                bookLists = bookListExports,
                preferences = preferences,
                tags = tagExports,
                bookTags = bookTagExports
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
    override suspend fun exportToZip(): Result<File> {
        return try {
            val backupResult = exportAllData()
            if (backupResult.isFailure) return Result.failure(backupResult.exceptionOrNull()!!)

            val backup = backupResult.getOrThrow()

            // 收集需要打包的封面图片
            val coverPathsToInclude = mutableSetOf<String>()

            backup.books.forEach { book ->
                book.coverPath?.let { path ->
                    if (isLocalCoverPath(path)) coverPathsToInclude.add(path)
                }
            }
            backup.bookLists.forEach { list ->
                list.coverPath?.let { path ->
                    if (isLocalCoverPath(path)) coverPathsToInclude.add(path)
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
                val missingCovers = mutableListOf<String>()
                val includedCovers = mutableListOf<String>()
                coverPathsToInclude.forEach { coverPath ->
                    val coverFile = File(coverPath)
                    if (coverFile.exists()) {
                        zip.putNextEntry(ZipEntry("covers/${coverFile.name}"))
                        FileInputStream(coverFile).use { it.copyTo(zip) }
                        zip.closeEntry()
                        includedCovers.add(coverPath)
                    } else {
                        missingCovers.add(coverPath)
                    }
                }
                android.util.Log.d("DataBackup", "封面打包完成: 共 ${coverPathsToInclude.size} 个, 成功 ${includedCovers.size} 个, 缺失 ${missingCovers.size} 个")
                if (missingCovers.isNotEmpty()) {
                    android.util.Log.w("DataBackup", "以下封面文件缺失，已跳过: $missingCovers")
                }
            }

            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 导出纯 JSON（不含封面），用于兼容旧版 WebDAV 等场景
     */
    override fun getExportJson(): Flow<String> = flow {
        val backupResult = exportAllData()
        backupResult.getOrNull()?.let { backup ->
            emit(json.encodeToString(backup))
        }
    }

    // ─── 导入 ──────────────────────────────────────────────────────────────────

    override suspend fun importData(backup: DataBackup, clearExisting: Boolean): Result<ImportResult> {
        // 版本迁移：低版本备份自动升级字段
        val migrated = migrateBackup(backup)

        return try {
            val errors = mutableListOf<String>()
            var booksImported = 0
            var recordsImported = 0
            var bookListsImported = 0

            val oldIdToNewId = mutableMapOf<Long, Long>()
            val oldIdToNewBook = mutableMapOf<Long, BookEntity>()

            // 阶段 0：清空现有数据（如果需要）
            if (clearExisting) {
                recordDao.deleteAllRecords()
                bookDao.deleteAllBooks()
                bookListDao.deleteAllBookLists()
            }

            // 阶段 1：读取现有数据用于去重
            val existingBooks = if (!clearExisting) {
                bookDao.getAllBooks().first()
            } else {
                emptyList()
            }
            val existingKeys = existingBooks.map { importIdentityKey(it) }.toSet()

            // 阶段 2：导入书籍
            migrated.books.forEach { bookExport ->
                try {
                    val key = importIdentityKey(bookExport)
                    if (!clearExisting && key in existingKeys) {
                        val matched = existingBooks.first { importIdentityKey(it) == key }
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

            // 阶段 2.5（v5+）：导入标签和书籍-标签关联
            val importedTagNameToId = mutableMapOf<String, Long>()
            for (tagExport in migrated.tags) {
                try {
                    val tagId = tagRepository.createTag(tagExport.name, tagExport.color)
                    importedTagNameToId[tagExport.name] = tagId
                } catch (e: Exception) {
                    errors.add("导入标签「${tagExport.name}」失败: ${e.message}")
                }
            }
            val importKeyToBookId = oldIdToNewBook.values.associate { importIdentityKey(it) to it.id }
            for (bookTagExport in migrated.bookTags) {
                try {
                    val tagId = importedTagNameToId[bookTagExport.tagName] ?: continue
                    val bookId = importKeyToBookId[bookTagExport.bookImportKey] ?: continue
                    tagRepository.addTagToBook(tagId, bookId)
                } catch (e: Exception) {
                    errors.add("导入书籍标签关联失败: ${e.message}")
                }
            }

            // 阶段 3：读取现有记录用于去重
            val existingRecords = if (!clearExisting) {
                recordDao.getAllRecords().first()
            } else {
                emptyList()
            }
            val existingRecordKeys = existingRecords.mapNotNull { record ->
                record.bookId?.let { bookId -> "${bookId}::${record.date}" }
            }.toSet()

            // 阶段 4：导入阅读记录
            migrated.readingRecords.forEach { recordExport ->
                try {
                    val newBookId = recordExport.bookId?.let { oldIdToNewId[it] }
                    if (newBookId != null) {
                        val recordKey = "${newBookId}::${recordExport.date}"
                        if (!clearExisting && recordKey in existingRecordKeys) {
                            return@forEach
                        }
                    }

                    val newRecord = ReadingRecordEntity(
                        id = 0,
                        bookId = newBookId,
                        bookSnapshot = recordExport.bookSnapshot
                            ?: oldIdToNewBook[recordExport.bookId]?.let { BookSnapshot.from(it, it.status) },
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
                } catch (e: Exception) {
                    errors.add("导入阅读记录失败: ${e.message}")
                }
            }

            // 阶段 5：读取现有书单用于去重
            val existingBookListNames = if (!clearExisting) {
                bookListDao.getAllBookLists().first().map { it.name }.toSet()
            } else {
                emptySet()
            }

            // 阶段 6：导入书单
            migrated.bookLists.forEach { bookListExport ->
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
                        showInBooksPage = bookListExport.showInBooksPage,
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

            // 恢复用户设置（DataStore 不支持事务）
            if (migrated.preferences != null) {
                try {
                    preferencesManager.importPreferences(migrated.preferences)
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
    override suspend fun importFromZipForPreview(zipFile: File): Result<ImportPreview> {
        return try {
            val jsonContent: String
            ZipInputStream(FileInputStream(zipFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "data.json") {
                        jsonContent = zip.readBytes().toString(Charsets.UTF_8)
                        val backup = parseBackupFromJson(jsonContent)
                            ?: return Result.failure(IllegalStateException("ZIP 中 data.json 格式无效"))
                        pendingBackupFromPreview = backup
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
     * 从 ZIP 文件导入（包含封面图片自动解压到 covers 目录）。
     * 如果此前已调用 importFromZipForPreview 缓存了解析结果，则跳过 data.json 的二次解析。
     */
    override suspend fun importFromZip(zipFile: File, clearExisting: Boolean): Result<ImportResult> {
        return try {
            var backup = pendingBackupFromPreview
            pendingBackupFromPreview = null
            val extractedCoverFiles = mutableListOf<File>()
            val extractedCoverMap = mutableMapOf<String, String>()

            ZipInputStream(FileInputStream(zipFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "data.json" -> {
                            if (backup == null) {
                                val jsonContent = zip.readBytes().toString(Charsets.UTF_8)
                                backup = parseBackupFromJson(jsonContent)
                                    ?: return Result.failure(IllegalStateException("ZIP 中未找到有效的 data.json"))
                            }
                        }
                        entry.name.startsWith("covers/") && !entry.isDirectory -> {
                            val fileName = entry.name.removePrefix("covers/")
                            val destFile = resolveCoverFile(fileName)
                            FileOutputStream(destFile).use { output -> zip.copyTo(output) }
                            extractedCoverFiles.add(destFile)
                            extractedCoverMap[fileName] = destFile.absolutePath
                            android.util.Log.d("DataBackup", "封面已解压: ${destFile.absolutePath}")
                        }
                    }
                    entry = zip.nextEntry
                }
            }

            android.util.Log.d("DataBackup", "ZIP 解压完成: 共提取 ${extractedCoverFiles.size} 个封面文件")

            val finalBackup = backup ?: return Result.failure(IllegalStateException("ZIP 中未找到有效的 data.json"))

            val fixedBackup = fixCoverPathsForNewDevice(finalBackup, extractedCoverMap)
            if (fixedBackup != finalBackup) {
                android.util.Log.d("DataBackup", "封面路径已修复为新设备路径")
            }
            val result = importData(fixedBackup, clearExisting)
            zipFile.delete()
            result
        } catch (e: Exception) {
            pendingBackupFromPreview = null
            Result.failure(e)
        }
    }

    /**
     * 安全解析封面文件名，防止 ZIP 路径穿越攻击。
     * 拒绝包含 "/"、"\\"、".." 的文件名，并将结果限定在 covers 目录内。
     */
    private fun resolveCoverFile(fileName: String): File {
        val safeName = File(fileName).name  // 剥离任何路径前缀
        if (safeName != fileName || safeName.contains("..") || safeName.isEmpty()) {
            throw SecurityException("ZIP 中封面文件名不安全: $fileName")
        }
        return File(coverStorageUtil.coversDir, safeName)
    }

    /**
     * 修复跨设备恢复时的封面路径问题
     * ZIP 中只存储文件名（如 cover_uuid.jpg），但 data.json 中存储的是原设备的绝对路径
     * 这里将 data.json 中的封面路径替换为解压后的新设备路径
     */
    private fun fixCoverPathsForNewDevice(
        backup: DataBackup,
        extractedCoverMap: Map<String, String>
    ): DataBackup {
        if (extractedCoverMap.isEmpty()) return backup

        // 提取文件名到新路径的映射
        val fileNameToNewPath = extractedCoverMap.entries.associate { (fileName, newPath) ->
            fileName to newPath
        }

        // 修复书籍封面路径
        val fixedBooks = backup.books.map { bookExport ->
            val fixedCoverPath = bookExport.coverPath?.let { originalPath ->
                val fileName = File(originalPath).name
                fileNameToNewPath[fileName] ?: originalPath
            }
            bookExport.copy(coverPath = fixedCoverPath)
        }

        // 修复书单封面路径
        val fixedBookLists = backup.bookLists.map { listExport ->
            val fixedCoverPath = listExport.coverPath?.let { originalPath ->
                val fileName = File(originalPath).name
                fileNameToNewPath[fileName] ?: originalPath
            }
            listExport.copy(coverPath = fixedCoverPath)
        }

        return DataBackup(
            version = backup.version,
            exportTime = backup.exportTime,
            appVersion = backup.appVersion,
            books = fixedBooks,
            readingRecords = backup.readingRecords,
            bookLists = fixedBookLists,
            preferences = backup.preferences,
            tags = backup.tags,
            bookTags = backup.bookTags
        )
    }

    override suspend fun previewImport(backup: DataBackup) = runCatching {
        buildImportPreview(
            backup = migrateBackup(backup),
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

    // ─── 版本迁移 ──────────────────────────────────────────────────────────────

    /**
     * 按版本号逐步升级备份数据到当前版本格式。
     * 新增字段使用默认值，不会丢失数据。
     */
    private fun migrateBackup(backup: DataBackup): DataBackup {
        var current = backup
        if (current.version < 1) {
            // v1: 添加 bookLists 和 preferences 字段（使用默认值即可）
        }
        if (current.version < 2) {
            // v2: BookExport 添加 bookType 字段（默认 "NOVEL"）
        }
        if (current.version < 3) {
            // v3: ReadingRecordExport 添加 recordType 字段（默认 "NORMAL"）
        }
        if (current.version < 4) {
            // v4: ReadingRecordExport 添加 bookSnapshot 字段
        }
        if (current.version < 5) {
            // v5: 添加 tags 和 bookTags 字段（使用默认值即可，标签可选）
        }
        // 确保版本号更新到当前版本，后续导入不再重复迁移
        if (current.version < CURRENT_BACKUP_VERSION) {
            current = current.copy(version = CURRENT_BACKUP_VERSION)
        }
        return current
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private fun isLocalCoverPath(path: String): Boolean {
        return !path.startsWith("http://") && !path.startsWith("https://") &&
            !path.startsWith("emoji://") && !path.startsWith("color://") &&
            !path.startsWith("file://")
    }
}

private fun importIdentityKey(book: BookEntity): String = "${book.title.trim()}::${book.author?.trim().orEmpty()}"

private fun importIdentityKey(bookExport: BookExport): String = "${bookExport.title.trim()}::${bookExport.author?.trim().orEmpty()}"
