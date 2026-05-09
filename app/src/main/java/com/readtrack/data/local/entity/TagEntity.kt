package com.readtrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** 标签颜色（可选），存储 Color.toArgb() 的 Long 值 */
    val color: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
