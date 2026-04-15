package com.readtrack.domain.model

import com.readtrack.presentation.viewmodel.ProgressType

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
    val status: BookStatus = BookStatus.WANT_TO_READ
)
