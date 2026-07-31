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

    @Query("SELECT * FROM reading_records WHERE date >= :yearStart AND date < :yearEnd ORDER BY date DESC")
    fun getRecordsByYearRange(yearStart: Long, yearEnd: Long): Flow<List<ReadingRecordEntity>>

    @Query("SELECT * FROM reading_records WHERE date >= :start AND date < :end ORDER BY date DESC")
    fun getRecordsByDateRange(start: Long, end: Long): Flow<List<ReadingRecordEntity>>

    /** 指定时间之后是否存在普通阅读记录（用于每日提醒"已读跳过"判断） */
    @Query("SELECT EXISTS(SELECT 1 FROM reading_records WHERE date >= :start AND recordType = 'NORMAL' LIMIT 1)")
    suspend fun hasNormalRecordSince(start: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ReadingRecordEntity): Long

    @Delete
    suspend fun deleteRecord(record: ReadingRecordEntity)

    @Query("DELETE FROM reading_records WHERE bookId = :bookId")
    suspend fun deleteRecordsByBookId(bookId: Long)

    @Query("DELETE FROM reading_records")
    suspend fun deleteAllRecords()

    @Query("SELECT * FROM reading_records WHERE bookId = :bookId ORDER BY date DESC")
    suspend fun getRecordsByBookIdOnce(bookId: Long): List<ReadingRecordEntity>
}
