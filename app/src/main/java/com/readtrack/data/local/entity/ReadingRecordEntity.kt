package com.readtrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.readtrack.domain.model.BookSnapshot

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
            onDelete = ForeignKey.SET_NULL
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
    val bookId: Long?,   // 可空：图书删除后保留记录但断开关联
    /** 写入记录时对图书信息的快照，删除图书后仍可通过此字段显示书名、封面等 */
    val bookSnapshot: BookSnapshot? = null,
    /** 阅读页数（页面模式） */
    val pagesRead: Double,
    val fromPage: Double,
    val toPage: Double,
    /** 阅读章节数（章节模式），与 pagesRead 共存，UI 根据 progressType 选取对应字段 */
    val chaptersRead: Int? = null,
    val date: Long = System.currentTimeMillis(),
    val note: String? = null,
    val recordType: RecordType = RecordType.NORMAL
)
