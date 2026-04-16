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
    val coverPath: String? = null,        // 可选：书单封面（取第一本的封面或用户上传）
    val bookCount: Int = 0,                // 缓存：书单内书籍数量，用于列表快速展示
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
