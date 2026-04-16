package com.readtrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 书单收藏夹实体
 * 用户可创建书单（如"本月待读"、"科幻经典"），将书籍添加到书单中。
 */
@Entity(
    tableName = "book_lists",
    indices = [
        Index(value = ["updatedAt"])
    ]
)
data class BookListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    /** 书单封面图片路径，为 null 时自动取第一本（coverBookId 对应书籍）的封面 */
    val coverPath: String? = null,
    /** 封面来源书籍ID（封面自动更新时参考），可由用户自定义覆盖 coverPath */
    val coverBookId: Long? = null,
    val bookCount: Int = 0,                // 缓存：书单内书籍数量，用于列表快速展示
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
