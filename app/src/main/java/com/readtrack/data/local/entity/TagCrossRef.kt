package com.readtrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "book_tag_cross_ref",
    primaryKeys = ["tagId", "bookId"],
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tagId"]), Index(value = ["bookId"])]
)
data class TagCrossRef(
    val tagId: Long,
    val bookId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
