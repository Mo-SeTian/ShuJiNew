package com.readtrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "badges")
data class BadgeEntity(
    /** 徽章 id，见 [com.readtrack.domain.model.BadgeCatalog] */
    @PrimaryKey
    val id: String,
    val earnedAt: Long = System.currentTimeMillis()
)
