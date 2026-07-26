package com.readtrack.domain.repository

import com.readtrack.data.local.entity.BadgeEntity
import com.readtrack.domain.model.Badge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface BadgeRepository {
    fun observeEarnedBadges(): Flow<List<BadgeEntity>>
    fun observeRecentBadges(limit: Int): Flow<List<BadgeEntity>>

    /** 新解锁徽章的实时事件流，供 UI 层展示弹窗 */
    val newBadgeEvents: SharedFlow<Badge>

    /**
     * 重新计算所有徽章达成情况，插入本次新解锁的徽章。
     * @return 本次新解锁的徽章元数据列表（可能为空）
     */
    suspend fun checkAndAward(): List<Badge>
}
