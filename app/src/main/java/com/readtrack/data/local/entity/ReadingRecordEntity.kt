package com.readtrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 阅读记录类型
 * - NORMAL: 普通阅读记录（翻阅了 X 页/章节）
 * - STATUS_ADDED: 添加图书
 * - STATUS_READING: 开始阅读
 * - STATUS_FINISHED: 读完
 * - STATUS_DROPPED: 抛弃
 */
enum class RecordType {
    NORMAL,
    STATUS_ADDED,
    STATUS_READING,
    STATUS_FINISHED,
    STATUS_DROPPED
}

@Entity(
    tableName = "reading_records",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["date"]),
        Index(value = ["bookId", "date"])
    ]
)
data class ReadingRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val pagesRead: Double,
    val fromPage: Double,
    val toPage: Double,
    val date: Long = System.currentTimeMillis(),
    val note: String? = null,
    val recordType: RecordType = RecordType.NORMAL
)
