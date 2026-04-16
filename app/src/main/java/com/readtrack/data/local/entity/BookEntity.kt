package com.readtrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.viewmodel.ProgressType

@Entity(
    tableName = "books",
    indices = [
        Index(value = ["status", "updatedAt"]),
        Index(value = ["updatedAt"]),
        Index(value = ["lastReadAt"]),
        Index(value = ["title"]),
        Index(value = ["author"]),
        Index(value = ["rating"])
    ]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String? = null,
    val publisher: String? = null,
    val description: String? = null,
    val progressType: ProgressType = ProgressType.PAGE,
    val totalPages: Double = 0.0,
    val currentPage: Double = 0.0,
    val totalChapters: Int? = null,
    val currentChapter: Int = 0,
    val coverPath: String? = null,
    val status: BookStatus = BookStatus.WANT_TO_READ,
    val rating: Float? = null,        // 0-5 星评分，null 表示未评分
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long? = null
)
