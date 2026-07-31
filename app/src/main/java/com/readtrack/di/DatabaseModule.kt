package com.readtrack.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.readtrack.data.local.dao.BadgeDao
import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.BookListDao
import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.dao.TagDao
import com.readtrack.data.local.database.ReadTrackDatabase
import com.readtrack.data.local.PreferencesManager
import com.readtrack.data.repository.BadgeRepositoryImpl
import com.readtrack.data.repository.BookListRepositoryImpl
import com.readtrack.data.repository.BookRepositoryImpl
import com.readtrack.data.repository.DataBackupRepositoryImpl
import com.readtrack.data.repository.ReadingRecordRepositoryImpl
import com.readtrack.data.repository.TagRepositoryImpl
import com.readtrack.domain.badge.BadgeCheckScheduler
import com.readtrack.domain.repository.BadgeRepository
import com.readtrack.domain.repository.BookListRepository
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.DataBackupRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import com.readtrack.domain.repository.TagRepository
import com.readtrack.util.CoverStorageUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReadTrackDatabase {
        return Room.databaseBuilder(
            context,
            ReadTrackDatabase::class.java,
            "readtrack_database"
        )
            .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * 从版本 9 迁移到 10：为 books 表新增 bookType 列（默认 'NOVEL'）
     */
    private val MIGRATION_9_10 = Migration(9, 10) { db ->
        db.execSQL("ALTER TABLE books ADD COLUMN bookType TEXT NOT NULL DEFAULT 'NOVEL'")
    }

    /**
     * 从版本 10 迁移到 11：新增 tags 表和 book_tag_cross_ref 表
     */
    private val MIGRATION_10_11 = Migration(10, 11) { db ->
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                color INTEGER,
                createdAt INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS book_tag_cross_ref (
                tagId INTEGER NOT NULL,
                bookId INTEGER NOT NULL,
                addedAt INTEGER NOT NULL,
                PRIMARY KEY (tagId, bookId),
                FOREIGN KEY (tagId) REFERENCES tags(id) ON DELETE CASCADE,
                FOREIGN KEY (bookId) REFERENCES books(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_book_tag_cross_ref_tagId ON book_tag_cross_ref(tagId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_book_tag_cross_ref_bookId ON book_tag_cross_ref(bookId)")
    }

    /**
     * 从版本 11 迁移到 12：重建 tags.name 索引为 UNIQUE，防止同名标签
     */
    private val MIGRATION_11_12 = Migration(11, 12) { db ->
        db.execSQL("DROP INDEX IF EXISTS index_tags_name")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name)")
    }

    /**
     * 从版本 12 迁移到 13：为 book_lists 表新增 showInBooksPage 列（默认 1/true）
     */
    private val MIGRATION_12_13 = Migration(12, 13) { db ->
        db.execSQL("ALTER TABLE book_lists ADD COLUMN showInBooksPage INTEGER NOT NULL DEFAULT 1")
    }

    /**
     * 从版本 13 迁移到 14：新增 badges 表存储已获得的成就徽章
     */
    private val MIGRATION_13_14 = Migration(13, 14) { db ->
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS badges (
                id TEXT PRIMARY KEY NOT NULL,
                earnedAt INTEGER NOT NULL
            )
        """)
    }

    @Provides
    @Singleton
    fun provideBookDao(database: ReadTrackDatabase): BookDao {
        return database.bookDao()
    }

    @Provides
    @Singleton
    fun provideReadingRecordDao(database: ReadTrackDatabase): ReadingRecordDao {
        return database.readingRecordDao()
    }

    @Provides
    @Singleton
    fun provideBookListDao(database: ReadTrackDatabase): BookListDao {
        return database.bookListDao()
    }

    @Provides
    @Singleton
    fun provideTagDao(database: ReadTrackDatabase): TagDao {
        return database.tagDao()
    }

    @Provides
    @Singleton
    fun provideBadgeDao(database: ReadTrackDatabase): BadgeDao {
        return database.badgeDao()
    }

    @Provides
    @Singleton
    fun provideBookRepository(
        bookDao: BookDao,
        readingRecordDao: ReadingRecordDao,
        database: ReadTrackDatabase,
        badgeCheckScheduler: BadgeCheckScheduler
    ): BookRepository {
        return BookRepositoryImpl(bookDao, readingRecordDao, database, badgeCheckScheduler)
    }

    @Provides
    @Singleton
    fun provideBookListRepository(
        bookListDao: BookListDao,
        bookDao: BookDao,
        database: ReadTrackDatabase
    ): BookListRepository {
        return BookListRepositoryImpl(bookListDao, bookDao, database)
    }

    @Provides
    @Singleton
    fun provideReadingRecordRepository(
        readingRecordDao: ReadingRecordDao,
        badgeCheckScheduler: BadgeCheckScheduler
    ): ReadingRecordRepository {
        return ReadingRecordRepositoryImpl(readingRecordDao, badgeCheckScheduler)
    }

    @Provides
    @Singleton
    fun provideTagRepository(tagDao: TagDao): TagRepository {
        return TagRepositoryImpl(tagDao)
    }

    @Provides
    @Singleton
    fun provideBadgeRepository(
        badgeDao: BadgeDao,
        bookDao: BookDao,
        readingRecordDao: ReadingRecordDao
    ): BadgeRepository {
        return BadgeRepositoryImpl(badgeDao, bookDao, readingRecordDao)
    }

    @Provides
    @Singleton
    fun provideDataBackupRepository(
        @ApplicationContext context: Context,
        database: ReadTrackDatabase,
        bookDao: BookDao,
        readingRecordDao: ReadingRecordDao,
        bookListDao: BookListDao,
        tagDao: TagDao,
        preferencesManager: PreferencesManager,
        coverStorageUtil: CoverStorageUtil,
        tagRepository: TagRepository,
        badgeDao: BadgeDao,
        badgeRepository: BadgeRepository
    ): DataBackupRepository {
        return DataBackupRepositoryImpl(
            context = context,
            database = database,
            bookDao = bookDao,
            recordDao = readingRecordDao,
            bookListDao = bookListDao,
            tagDao = tagDao,
            preferencesManager = preferencesManager,
            coverStorageUtil = coverStorageUtil,
            tagRepository = tagRepository,
            badgeDao = badgeDao,
            badgeRepository = badgeRepository
        )
    }
}
