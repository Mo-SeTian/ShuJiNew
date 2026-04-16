package com.readtrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 书单与书籍的多对多关联表
 * 一本书可以属于多个书单，一个书单可以包含多本书。
 */
@Entity(
    tableName = "book_list_cross_ref",
    primaryKeys = ["bookListId", "bookId"],
    foreignKeys = [
        ForeignKey(
            entity = BookListEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookListId"],
            onDelete = ForeignKey.CASCADE   // 删除书单时移除关联
        ),
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE   // 删除书籍时移除关联
        )
    ],
    indices = [
        Index(value = ["bookListId"]),
        Index(value = ["bookId"])
    ]
)
data class BookListCrossRef(
    val bookListId: Long,
    val bookId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
