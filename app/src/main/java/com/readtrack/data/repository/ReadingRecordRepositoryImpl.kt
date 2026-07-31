package com.readtrack.data.repository

import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.entity.ReadingRecordEntity
import com.readtrack.domain.repository.BadgeRepository
import com.readtrack.domain.repository.ReadingRecordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingRecordRepositoryImpl @Inject constructor(
    private val readingRecordDao: ReadingRecordDao,
    private val badgeRepository: BadgeRepository
) : ReadingRecordRepository {

    // 独立后台作用域，避免调用方 scope 取消影响徽章计算
    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getRecordsByBookId(bookId: Long): Flow<List<ReadingRecordEntity>> =
        readingRecordDao.getRecordsByBookId(bookId)

    override fun getAllRecords(): Flow<List<ReadingRecordEntity>> =
        readingRecordDao.getAllRecords()

    override fun getRecordsByYearRange(yearStart: Long, yearEnd: Long): Flow<List<ReadingRecordEntity>> =
        readingRecordDao.getRecordsByYearRange(yearStart, yearEnd)

    override fun getRecordsByDateRange(start: Long, end: Long): Flow<List<ReadingRecordEntity>> =
        readingRecordDao.getRecordsByDateRange(start, end)

    override fun getTotalPagesReadSince(startTime: Long): Flow<Double?> =
        readingRecordDao.getTotalPagesReadSince(startTime)

    override suspend fun insertRecord(record: ReadingRecordEntity): Long {
        val id = readingRecordDao.insertRecord(record)
        triggerBadgeCheck()
        return id
    }

    override suspend fun deleteRecord(record: ReadingRecordEntity) {
        readingRecordDao.deleteRecord(record)
        triggerBadgeCheck()
    }

    private fun triggerBadgeCheck() {
        bgScope.launch {
            runCatching { badgeRepository.checkAndAward() }
                .onFailure { android.util.Log.w("ReadingRecordRepository", "徽章检查失败", it) }
        }
    }
}
