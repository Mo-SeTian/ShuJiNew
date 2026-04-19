package com.readtrack.domain.model

import com.readtrack.data.local.entity.BookEntity
import com.readtrack.domain.model.ProgressType

/**
 * 写入阅读记录时对图书信息的"快照"。
 * 删除图书后，记录仍可通过此快照显示书名、封面、进度类型等信息。
 */
@kotlinx.serialization.Serializable
data class BookSnapshot(
    val id: Long,
    val title: String,
    val author: String? = null,
    val coverPath: String? = null,
    val progressType: ProgressType = ProgressType.PAGE,
    val status: BookStatus = BookStatus.WANT_TO_READ,
    val totalChapters: Int? = null,
    val currentChapter: Int? = null
) {
    companion object {
        fun from(book: BookEntity, status: BookStatus): BookSnapshot = BookSnapshot(
            id = book.id,
            title = book.title,
            author = book.author,
            coverPath = book.coverPath,
            progressType = book.progressType,
            status = status,
            totalChapters = book.totalChapters,
            currentChapter = book.currentChapter
        )
    }
}
