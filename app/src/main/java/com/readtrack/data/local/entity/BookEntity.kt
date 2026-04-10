package com.readtrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.readtrack.domain.model.BookStatus

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String? = null,
    val totalPages: Double,
    val currentPage: Double = 0.0,
    val coverPath: String? = null,
    val status: BookStatus = BookStatus.WANT_TO_READ,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long? = null
)
