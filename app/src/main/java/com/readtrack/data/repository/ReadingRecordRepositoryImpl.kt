package com.readtrack.data.repository

import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.badge.BadgeCheckScheduler
import com.readtrack.domain.repository.ReadingRecordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingRecordRepositoryImpl @Inject constructor(
    private val readingRecordDao: ReadingRecordDao,
    private val badgeCheckScheduler: BadgeCheckScheduler
) : ReadingRecordRepository {

    override fun getRecordsByBookId(bookId: Long): Flow<List<ReadingRecordEntity>> =
        readingRecordDao.getRecordsByBookId(bookId)

    override fun getAllRecords(): Flow<List<ReadingRecordEntity>> =
        readingRecordDao.getAllRecords()

    override fun getRecordsByYearRange(yearStart: Long, yearEnd: Long): Flow<List<ReadingRecordEntity>> =
        readingRecordDao.getRecordsByYearRange(yearStart, yearEnd)

    override fun getRecordsByDateRange(start: Long, end: Long): Flow<List<ReadingRecordEntity>> =
        readingRecordDao.getRecordsByDateRange(start, end)

    override suspend fun hasNormalRecordSince(startTime: Long): Boolean =
        readingRecordDao.hasNormalRecordSince(startTime)

    override suspend fun insertRecord(record: ReadingRecordEntity): Long {
        val id = readingRecordDao.insertRecord(record)
        badgeCheckScheduler.schedule()
        return id
    }

    override suspend fun deleteRecord(record: ReadingRecordEntity) {
        readingRecordDao.deleteRecord(record)
        badgeCheckScheduler.schedule()
    }
}
