package com.readtrack.data.repository

import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.repository.ReadingRecordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingRecordRepositoryImpl @Inject constructor(
    private val readingRecordDao: ReadingRecordDao
) : ReadingRecordRepository {

    override fun getRecordsByBookId(bookId: Long): Flow<List<ReadingRecordEntity>> =
        readingRecordDao.getRecordsByBookId(bookId)

    override fun getAllRecords(): Flow<List<ReadingRecordEntity>> =
        readingRecordDao.getAllRecords()

    override fun getTotalPagesReadSince(startTime: Long): Flow<Double?> =
        readingRecordDao.getTotalPagesReadSince(startTime)

    override suspend fun insertRecord(record: ReadingRecordEntity): Long =
        readingRecordDao.insertRecord(record)

    override suspend fun deleteRecord(record: ReadingRecordEntity) =
        readingRecordDao.deleteRecord(record)
}
