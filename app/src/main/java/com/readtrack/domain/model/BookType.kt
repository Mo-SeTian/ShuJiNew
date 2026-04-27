package com.readtrack.domain.model

import kotlinx.serialization.Serializable

/**
 * 书籍类型：漫画、小说、有声书
 */
@Serializable
enum class BookType(val displayName: String) {
    COMIC("漫画"),
    NOVEL("小说"),
    AUDIOBOOK("有声书")
}
