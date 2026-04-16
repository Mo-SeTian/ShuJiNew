package com.readtrack.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.BookListDao
import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.entity.BookEntity
import com.readtrack.data.local.entity.BookListCrossRef
import com.readtrack.data.local.entity.BookListEntity
import com.readtrack.data.local.entity.ReadingRecordEntity

@Database(
    entities = [
        BookEntity::class,
        ReadingRecordEntity::class,
        BookListEntity::class,
        BookListCrossRef::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ReadTrackDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun readingRecordDao(): ReadingRecordDao
    abstract fun bookListDao(): BookListDao
}
