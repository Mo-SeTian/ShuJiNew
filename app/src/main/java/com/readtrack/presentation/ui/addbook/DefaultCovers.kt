package com.readtrack.presentation.ui.addbook

/**
 * 内置默认封面库 - 使用纯色背景+emoji图标，无需网络
 * 覆盖各种书籍类型，美观可靠
 */
object DefaultCovers {
    
    /**
     * 预设封面列表
     * 每个封面使用 颜色+emoji+文字 的组合
     */
    val covers = listOf(
        // ========== 文学小说类 ==========
        CoverItem(colorHex = "FF6B6B", emoji = "📖", title = "文学"),
        CoverItem(colorHex = "EE5A5A", emoji = "📚", title = "小说"),
        CoverItem(colorHex = "D63031", emoji = "📕", title = "经典"),
        CoverItem(colorHex = "E84393", emoji = "💝", title = "言情"),
        CoverItem(colorHex = "FD79A8", emoji = "🌸", title = "浪漫"),
        
        // ========== 科幻玄幻类 ==========
        CoverItem(colorHex = "0984E3", emoji = "🚀", title = "科幻"),
        CoverItem(colorHex = "74B9FF", emoji = "🌌", title = "星际"),
        CoverItem(colorHex = "00CEC9", emoji = "⚡", title = "玄幻"),
        CoverItem(colorHex = "6C5CE7", emoji = "🔮", title = "奇幻"),
        CoverItem(colorHex = "A29BFE", emoji = "🧙", title = "魔幻"),
        
        // ========== 悬疑推理类 ==========
        CoverItem(colorHex = "2D3436", emoji = "🔍", title = "推理"),
        CoverItem(colorHex = "636E72", emoji = "❓", title = "悬疑"),
        CoverItem(colorHex = "B2BEC3", emoji = "🕵️", title = "侦探"),
        CoverItem(colorHex = "DFE6E9", emoji = "🎭", title = "惊悚"),
        CoverItem(colorHex = "FDCB6E", emoji = "🔎", title = "破案"),
        
        // ========== 历史社科类 ==========
        CoverItem(colorHex = "D4A574", emoji = "🏛️", title = "历史"),
        CoverItem(colorHex = "B8860B", emoji = "📜", title = "古籍"),
        CoverItem(colorHex = "8B4513", emoji = "⚔️", title = "战争"),
        CoverItem(colorHex = "CD853F", emoji = "🌍", title = "社科"),
        CoverItem(colorHex = "DEB887", emoji = "🏺", title = "考古"),
        
        // ========== 商业经济类 ==========
        CoverItem(colorHex = "00B894", emoji = "💼", title = "商业"),
        CoverItem(colorHex = "00CEC9", emoji = "📈", title = "经济"),
        CoverItem(colorHex = "55A3FF", emoji = "💰", title = "理财"),
        CoverItem(colorHex = "0984E3", emoji = "📊", title = "管理"),
        CoverItem(colorHex = "74B9FF", emoji = "🎯", title = "营销"),
        
        // ========== 科技编程类 ==========
        CoverItem(colorHex = "00D2D3", emoji = "💻", title = "编程"),
        CoverItem(colorHex = "01A3A4", emoji = "🖥️", title = "电脑"),
        CoverItem(colorHex = "0ABDE3", emoji = "📱", title = "移动"),
        CoverItem(colorHex = "5F27CD", emoji = "🤖", title = "AI"),
        CoverItem(colorHex = "48DBFB", emoji = "🔧", title = "技术"),
        
        // ========== 生活休闲类 ==========
        CoverItem(colorHex = "FF9FF3", emoji = "🍳", title = "美食"),
        CoverItem(colorHex = "FECA57", emoji = "✈️", title = "旅行"),
        CoverItem(colorHex = "FF6B6B", emoji = "🏠", title = "家居"),
        CoverItem(colorHex = "54A0FF", emoji = "🎨", title = "手工"),
        CoverItem(colorHex = "5F27CD", emoji = "🎮", title = "游戏"),
        
        // ========== 儿童教育类 ==========
        CoverItem(colorHex = "FF9F43", emoji = "🎈", title = "儿童"),
        CoverItem(colorHex = "FECA57", emoji = "🧸", title = "绘本"),
        CoverItem(colorHex = "1DD1A1", emoji = "🎓", title = "教育"),
        CoverItem(colorHex = "00D2D3", emoji = "📝", title = "教辅"),
        CoverItem(colorHex = "54A0FF", emoji = "🔢", title = "科普"),
        
        // ========== 艺术设计类 ==========
        CoverItem(colorHex = "FD79A8", emoji = "🎨", title = "绘画"),
        CoverItem(colorHex = "E84393", emoji = "📷", title = "摄影"),
        CoverItem(colorHex = "FF6B6B", emoji = "🎵", title = "音乐"),
        CoverItem(colorHex = "A29BFE", emoji = "✏️", title = "设计"),
        CoverItem(colorHex = "DFE6E9", emoji = "🏛️", title = "艺术"),
        
        // ========== 心理哲学类 ==========
        CoverItem(colorHex = "636E72", emoji = "🧠", title = "心理"),
        CoverItem(colorHex = "B2BEC3", emoji = "📖", title = "哲学"),
        CoverItem(colorHex = "2D3436", emoji = "💭", title = "思想"),
        CoverItem(colorHex = "D63031", emoji = "❤️", title = "情感"),
        CoverItem(colorHex = "E17055", emoji = "🤝", title = "社交"),
    )
    
    /**
     * 纯色选项 - 多种颜色供选择
     */
    val solidColors = listOf(
        // 暖色系
        "FF6B6B", // 珊瑚红
        "FF9F43", // 橙色
        "FECA57", // 黄色
        "FFEEAD", // 浅黄
        
        // 绿色系
        "1DD1A1", // 薄荷绿
        "00B894", // 青绿
        "55EFC4", // 浅绿
        "006266", // 深绿
        
        // 蓝色系
        "54A0FF", // 天蓝
        "0984E3", // 蓝色
        "00D2D3", // 青色
        "0C2461", // 深蓝
        
        // 紫色系
        "A29BFE", // 淡紫
        "6C5CE7", // 紫色
        "FD79A8", // 粉红
        "E84393", // 品红
        
        // 中性色
        "DFE6E9", // 浅灰
        "B2BEC3", // 灰色
        "636E72", // 深灰
        "2D3436", // 炭灰
    )
}

data class CoverItem(
    val colorHex: String,
    val emoji: String,
    val title: String
) {
    val id: String get() = "emoji_$colorHex"
    
    val url: String get() = "emoji://$colorHex|$emoji|$title"
}
