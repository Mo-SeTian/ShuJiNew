package com.readtrack.domain.model

enum class BookStatus(val displayName: String) {
    WANT_TO_READ("想读"),     // 想读 - 绿色 #4CAF50
    READING("在读"),         // 阅读中 - 橙色 #FF9800
    FINISHED("已读"),        // 已读 - 蓝色 #2196F3
    ON_HOLD("闲置"),         // 闲置 - 灰色 #9E9E9E
    ABANDONED("放弃")        // 放弃 - 红色 #F44336
}
