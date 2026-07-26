package com.readtrack.data.repository

import com.readtrack.data.local.dao.BadgeDao
import com.readtrack.data.local.dao.BookDao
import com.readtrack.data.local.dao.ReadingRecordDao
import com.readtrack.data.local.entity.BadgeEntity
import com.readtrack.domain.badge.BadgeCheckSnapshot
import com.readtrack.domain.badge.evaluateBadges
import com.readtrack.domain.model.Badge
import com.readtrack.domain.model.BadgeCatalog
import com.readtrack.domain.repository.BadgeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BadgeRepositoryImpl @Inject constructor(
    private val badgeDao: BadgeDao,
    private val bookDao: BookDao,
    private val recordDao: ReadingRecordDao
) : BadgeRepository {

    // 防止多次触发时并发写入导致的重复判定
    private val awardMutex = Mutex()

    private val _newBadgeEvents = MutableSharedFlow<Badge>(extraBufferCapacity = 8)
    override val newBadgeEvents: SharedFlow<Badge> = _newBadgeEvents.asSharedFlow()

    override fun observeEarnedBadges(): Flow<List<BadgeEntity>> = badgeDao.getAllBadges()

    override fun observeRecentBadges(limit: Int): Flow<List<BadgeEntity>> = badgeDao.getRecentBadges(limit)

    override suspend fun checkAndAward(): List<Badge> = awardMutex.withLock {
        val books = bookDao.getAllBooks().first()
        val records = recordDao.getAllRecords().first()
        val snapshot = BadgeCheckSnapshot(books = books, records = records)

        val eligible = evaluateBadges(snapshot)
        val already = badgeDao.getEarnedIds().toSet()
        val newIds = eligible - already
        if (newIds.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        val awarded = mutableListOf<Badge>()
        newIds.forEach { id ->
            val meta = BadgeCatalog.findById(id) ?: return@forEach
            badgeDao.insert(BadgeEntity(id = id, earnedAt = now))
            awarded.add(meta)
        }
        awarded.forEach { _newBadgeEvents.tryEmit(it) }
        return awarded
    }
}
