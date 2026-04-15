package com.readtrack.di

import android.content.Context
import androidx.room.Room
import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.database.ReadTrackDatabase
import com.readtrack.data.repository.BookRepositoryImpl
import com.readtrack.data.repository.DataBackupRepositoryImpl
import com.readtrack.data.repository.ReadingRecordRepositoryImpl
import com.readtrack.domain.repository.BookRepository
import com.readtrack.domain.repository.DataBackupRepository
import com.readtrack.domain.repository.ReadingRecordRepository
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
            .fallbackToDestructiveMigration()
            .build()
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
    fun provideBookRepository(
        bookDao: BookDao,
        readingRecordDao: ReadingRecordDao,
        database: ReadTrackDatabase
    ): BookRepository {
        return BookRepositoryImpl(bookDao, readingRecordDao, database)
    }

    @Provides
    @Singleton
    fun provideReadingRecordRepository(readingRecordDao: ReadingRecordDao): ReadingRecordRepository {
        return ReadingRecordRepositoryImpl(readingRecordDao)
    }

    @Provides
    @Singleton
    fun provideDataBackupRepository(
        bookDao: BookDao,
        readingRecordDao: ReadingRecordDao
    ): DataBackupRepository {
        return DataBackupRepositoryImpl(bookDao, readingRecordDao)
    }
}
