package com.readtrack.presentation.ui.addbook

/**
 * 内置默认封面库 - 预设精美的书籍封面，无需网络
 */
object DefaultCovers {
    
    val covers = listOf(
        // 文学小说类
        CoverItem("https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=300&h=450&fit=crop", "文学", "book_literature_1"),
        CoverItem("https://images.unsplash.com/photo-1512820790803-83ca734da794?w=300&h=450&fit=crop", "小说", "book_novel_1"),
        CoverItem("https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=300&h=450&fit=crop", "阅读", "book_reading_1"),
        CoverItem("https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=300&h=450&fit=crop", "书架", "book_shelf_1"),
        CoverItem("https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=300&h=450&fit=crop", "经典", "book_classic_1"),
        
        // 科技类
        CoverItem("https://images.unsplash.com/photo-1518770660439-4636190af475?w=300&h=450&fit=crop", "科技", "book_tech_1"),
        CoverItem("https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=300&h=450&fit=crop", "编程", "book_code_1"),
        CoverItem("https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=300&h=450&fit=crop", "电脑", "book_computer_1"),
        
        // 历史类
        CoverItem("https://images.unsplash.com/photo-1461360370896-922624d12a74?w=300&h=450&fit=crop", "历史", "book_history_1"),
        CoverItem("https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=300&h=450&fit=crop", "古籍", "book_ancient_1"),
        
        // 艺术类
        CoverItem("https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=300&h=450&fit=crop", "艺术", "book_art_1"),
        CoverItem("https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?w=300&h=450&fit=crop", "绘画", "book_painting_1"),
        
        // 商业类
        CoverItem("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&h=450&fit=crop", "商业", "book_business_1"),
        CoverItem("https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=300&h=450&fit=crop", "办公", "book_office_1"),
        
        // 生活类
        CoverItem("https://images.unsplash.com/photo-1490818387583-1baba5e638af?w=300&h=450&fit=crop", "生活", "book_life_1"),
        CoverItem("https://images.unsplash.com/photo-1553636224-3b7ee2e4befa?w=300&h=450&fit=crop", "美食", "book_food_1"),
        CoverItem("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=300&h=450&fit=crop", "旅行", "book_travel_1"),
        
        // 儿童类
        CoverItem("https://images.unsplash.com/photo-1532012197267-da84d127e765?w=300&h=450&fit=crop", "儿童", "book_kids_1"),
        CoverItem("https://images.unsplash.com/photo-1544717305-2782549b5136?w=300&h=450&fit=crop", "绘本", "book_picture_1"),
        
        // 学术类
        CoverItem("https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=300&h=450&fit=crop", "学习", "book_study_1"),
        CoverItem("https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=300&h=450&fit=crop", "教育", "book_edu_1"),
        
        // 悬疑类
        CoverItem("https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=300&h=450&fit=crop", "悬疑", "book_mystery_1"),
        CoverItem("https://images.unsplash.com/photo-1509266272358-7701da638078?w=300&h=450&fit=crop", "推理", "book_detective_1"),
        
        // 杂志类
        CoverItem("https://images.unsplash.com/photo-1517180102446-f3ece451e9d8?w=300&h=450&fit=crop", "杂志", "book_magazine_1"),
        
        // 默认封面 - 纯色
        CoverItem("default://orange", "橙色", "color_orange"),
        CoverItem("default://blue", "蓝色", "color_blue"),
        CoverItem("default://green", "绿色", "color_green"),
        CoverItem("default://purple", "紫色", "color_purple"),
        CoverItem("default://red", "红色", "color_red"),
    )
}

data class CoverItem(
    val url: String,
    val category: String,
    val id: String
)
