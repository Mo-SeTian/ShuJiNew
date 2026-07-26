package com.readtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.readtrack.data.local.entity.BadgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {

    @Query("SELECT * FROM badges ORDER BY earnedAt DESC")
    fun getAllBadges(): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges ORDER BY earnedAt DESC LIMIT :limit")
    fun getRecentBadges(limit: Int): Flow<List<BadgeEntity>>

    @Query("SELECT id FROM badges")
    suspend fun getEarnedIds(): List<String>

    @Query("SELECT * FROM badges WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): BadgeEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(badge: BadgeEntity): Long

    @Query("DELETE FROM badges")
    suspend fun deleteAll()
}
