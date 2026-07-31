package com.readtrack.domain.repository

import com.readtrack.data.local.entity.ReadingRecordEntity
import kotlinx.coroutines.flow.Flow

interface ReadingRecordRepository {
    fun getRecordsByBookId(bookId: Long): Flow<List<ReadingRecordEntity>>
    fun getAllRecords(): Flow<List<ReadingRecordEntity>>
    fun getRecordsByYearRange(yearStart: Long, yearEnd: Long): Flow<List<ReadingRecordEntity>>
    fun getRecordsByDateRange(start: Long, end: Long): Flow<List<ReadingRecordEntity>>
    fun getTotalPagesReadSince(startTime: Long): Flow<Double?>
    suspend fun insertRecord(record: ReadingRecordEntity): Long
    suspend fun deleteRecord(record: ReadingRecordEntity)
}
