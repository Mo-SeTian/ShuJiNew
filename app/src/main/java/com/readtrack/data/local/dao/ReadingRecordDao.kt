package com.readtrack.data.local.dao

import androidx.room.*
import com.readtrack.data.local.entity.ReadingRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingRecordDao {
    @Query("SELECT * FROM reading_records WHERE bookId = :bookId ORDER BY date DESC")
    fun getRecordsByBookId(bookId: Long): Flow<List<ReadingRecordEntity>>

    @Query("SELECT * FROM reading_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<ReadingRecordEntity>>

    @Query("SELECT * FROM reading_records WHERE date >= :startOfDay AND date < :endOfDay")
    fun getRecordsByDate(startOfDay: Long, endOfDay: Long): Flow<List<ReadingRecordEntity>>

    @Query("SELECT SUM(pagesRead) FROM reading_records WHERE date >= :startOfDay AND date < :endOfDay")
    fun getTotalPagesReadOnDate(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(pagesRead) FROM reading_records WHERE date >= :startTime")
    fun getTotalPagesReadSince(startTime: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ReadingRecordEntity): Long

    @Delete
    suspend fun deleteRecord(record: ReadingRecordEntity)

    @Query("DELETE FROM reading_records WHERE bookId = :bookId")
    suspend fun deleteRecordsByBookId(bookId: Long)
}
