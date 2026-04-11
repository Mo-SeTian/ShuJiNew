package com.readtrack.presentation.ui.addbook

/**
 * 内置封面库 - 使用 Emoji + 纯色组合，无需网络
 * 覆盖各种书籍类型，美观实用
 */
object DefaultCovers {
    
    /**
     * 预设封面列表 - 纯色背景 + Emoji + 分类标签
     */
    val covers = listOf(
        // 文学小说类
        CoverItem(colorHex = "FF6B6B", emoji = "📖", title = "文学"),
        CoverItem(colorHex = "EE5A5A", emoji = "📚", title = "小说"),
        CoverItem(colorHex = "D63031", emoji = "📕", title = "经典"),
        CoverItem(colorHex = "E84393", emoji = "💝", title = "言情"),
        CoverItem(colorHex = "FD79A8", emoji = "🌸", title = "浪漫"),
        CoverItem(colorHex = "FF7675", emoji = "📗", title = "散文"),
        
        // 科幻玄幻类
        CoverItem(colorHex = "0984E3", emoji = "🚀", title = "科幻"),
        CoverItem(colorHex = "74B9FF", emoji = "🌌", title = "星际"),
        CoverItem(colorHex = "00CEC9", emoji = "⚡", title = "玄幻"),
        CoverItem(colorHex = "6C5CE7", emoji = "🔮", title = "奇幻"),
        CoverItem(colorHex = "A29BFE", emoji = "🧙", title = "魔幻"),
        CoverItem(colorHex = "5F27CD", emoji = "👽", title = "太空"),
        
        // 悬疑推理类
        CoverItem(colorHex = "2D3436", emoji = "🔍", title = "推理"),
        CoverItem(colorHex = "636E72", emoji = "❓", title = "悬疑"),
        CoverItem(colorHex = "B2BEC3", emoji = "🕵️", title = "侦探"),
        CoverItem(colorHex = "DFE6E9", emoji = "🎭", title = "惊悚"),
        CoverItem(colorHex = "FDCB6E", emoji = "🔎", title = "破案"),
        
        // 历史社科类
        CoverItem(colorHex = "D4A574", emoji = "🏛️", title = "历史"),
        CoverItem(colorHex = "B8860B", emoji = "📜", title = "古籍"),
        CoverItem(colorHex = "8B4513", emoji = "⚔️", title = "战争"),
        CoverItem(colorHex = "CD853F", emoji = "🌍", title = "社科"),
        CoverItem(colorHex = "DEB887", emoji = "🏺", title = "考古"),
        
        // 商业经济类
        CoverItem(colorHex = "00B894", emoji = "💼", title = "商业"),
        CoverItem(colorHex = "00CEC9", emoji = "📈", title = "经济"),
        CoverItem(colorHex = "55A3FF", emoji = "💰", title = "理财"),
        CoverItem(colorHex = "0984E3", emoji = "📊", title = "管理"),
        CoverItem(colorHex = "74B9FF", emoji = "🎯", title = "营销"),
        
        // 科技编程类
        CoverItem(colorHex = "00D2D3", emoji = "💻", title = "编程"),
        CoverItem(colorHex = "01A3A4", emoji = "🖥️", title = "电脑"),
        CoverItem(colorHex = "0ABDE3", emoji = "📱", title = "移动"),
        CoverItem(colorHex = "5F27CD", emoji = "🤖", title = "AI"),
        CoverItem(colorHex = "48DBFB", emoji = "🔧", title = "技术"),
        
        // 生活休闲类
        CoverItem(colorHex = "FF9FF3", emoji = "🍳", title = "美食"),
        CoverItem(colorHex = "FECA57", emoji = "✈️", title = "旅行"),
        CoverItem(colorHex = "FF6B6B", emoji = "🏠", title = "家居"),
        CoverItem(colorHex = "54A0FF", emoji = "🎨", title = "手工"),
        CoverItem(colorHex = "5F27CD", emoji = "🎮", title = "游戏"),
        
        // 儿童教育类
        CoverItem(colorHex = "FF9F43", emoji = "🎈", title = "儿童"),
        CoverItem(colorHex = "FECA57", emoji = "🧸", title = "绘本"),
        CoverItem(colorHex = "1DD1A1", emoji = "🎓", title = "教育"),
        CoverItem(colorHex = "00D2D3", emoji = "📝", title = "教辅"),
        CoverItem(colorHex = "54A0FF", emoji = "🔢", title = "科普"),
        
        // 艺术设计类
        CoverItem(colorHex = "FD79A8", emoji = "🎨", title = "绘画"),
        CoverItem(colorHex = "E84393", emoji = "📷", title = "摄影"),
        CoverItem(colorHex = "FF6B6B", emoji = "🎵", title = "音乐"),
        CoverItem(colorHex = "A29BFE", emoji = "✏️", title = "设计"),
        CoverItem(colorHex = "6C5CE7", emoji = "🎬", title = "电影"),
        
        // 心理哲学类
        CoverItem(colorHex = "636E72", emoji = "🧠", title = "心理"),
        CoverItem(colorHex = "B2BEC3", emoji = "📖", title = "哲学"),
        CoverItem(colorHex = "D63031", emoji = "❤️", title = "情感"),
        CoverItem(colorHex = "FDCB6E", emoji = "💡", title = "励志"),
    )
    
    /**
     * 纯色选项 - 30种精选颜色
     */
    val solidColors = listOf(
        "FF6B6B", "FF9F43", "FECA57", "FFEEAD", // 暖色系
        "1DD1A1", "00B894", "55EFC4", "00D2D3", // 绿色系
        "54A0FF", "0984E3", "0ABDE3", "48DBFB", // 蓝色系
        "A29BFE", "6C5CE7", "FD79A8", "E84393", // 紫色系
        "DFE6E9", "B2BEC3", "636E72", "2D3436", // 灰色系
        "D4A574", "B8860B", "8B4513", "CD853F", // 棕色系
        "FFFFFF", "FDCB6E", "FFEAA7", "FAB1A0", // 浅色系
        "E17055", "D63031", "FF7675", "FF6348"  // 橙红色系
    )
}

data class CoverItem(
    val colorHex: String,
    val emoji: String,
    val title: String
) {
    val id: String get() = "emoji_${colorHex}_$emoji"
    
    // 生成封面数据URI（纯色+emoji，无需网络）
    val url: String get() = "emoji://$colorHex|$emoji|$title"
}
