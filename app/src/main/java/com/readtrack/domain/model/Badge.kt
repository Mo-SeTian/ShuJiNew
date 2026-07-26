package com.readtrack.domain.model

/**
 * 徽章分类。
 */
enum class BadgeCategory(val displayName: String) {
    QUANTITY("阅读数量"),
    STREAK("连续打卡"),
    PAGES("页数里程碑"),
    CHAPTERS("章节里程碑"),
    DEPTH("深度阅读"),
    HABIT("时段偏好"),
    SPECIAL("特殊成就")
}

/**
 * 徽章等级。
 */
enum class BadgeTier(val displayName: String, val colorHex: Long) {
    BRONZE("青铜", 0xFFCD7F32),
    SILVER("白银", 0xFFC0C0C0),
    GOLD("黄金", 0xFFFFD700),
    PLATINUM("白金", 0xFFE5E4E2),
    DIAMOND("钻石", 0xFFB9F2FF)
}

/**
 * 徽章元数据。全部内置在应用中，不依赖数据库。
 * 用户获得情况通过 [com.readtrack.data.local.entity.BadgeEntity] 存储。
 */
data class Badge(
    val id: String,
    val category: BadgeCategory,
    val tier: BadgeTier,
    val emoji: String,
    val title: String,
    val description: String,
    /** 达成阈值（用于 UI 展示进度） */
    val threshold: Int = 0
)

/**
 * 全部徽章目录。共 22 枚。
 */
object BadgeCatalog {

    val ALL: List<Badge> = listOf(
        // === 阅读数量（5） ===
        Badge("reader_first", BadgeCategory.QUANTITY, BadgeTier.BRONZE, "📖", "初次相遇", "读完第 1 本书", 1),
        Badge("reader_10", BadgeCategory.QUANTITY, BadgeTier.SILVER, "📚", "十里书香", "读完 10 本书", 10),
        Badge("reader_50", BadgeCategory.QUANTITY, BadgeTier.GOLD, "📑", "半百之约", "读完 50 本书", 50),
        Badge("reader_100", BadgeCategory.QUANTITY, BadgeTier.PLATINUM, "🏆", "百书争鸣", "读完 100 本书", 100),
        Badge("reader_300", BadgeCategory.QUANTITY, BadgeTier.DIAMOND, "👑", "千卷阅遍", "读完 300 本书", 300),

        // === 连续打卡（5） ===
        Badge("streak_3", BadgeCategory.STREAK, BadgeTier.BRONZE, "🔥", "起步三日", "连续打卡 3 天", 3),
        Badge("streak_7", BadgeCategory.STREAK, BadgeTier.SILVER, "🌟", "一周不断", "连续打卡 7 天", 7),
        Badge("streak_30", BadgeCategory.STREAK, BadgeTier.GOLD, "🌖", "月度坚持", "连续打卡 30 天", 30),
        Badge("streak_100", BadgeCategory.STREAK, BadgeTier.PLATINUM, "💯", "百日筑基", "连续打卡 100 天", 100),
        Badge("streak_365", BadgeCategory.STREAK, BadgeTier.DIAMOND, "🌈", "年度不辍", "连续打卡 365 天", 365),

        // === 页数（3） ===
        Badge("pages_1k", BadgeCategory.PAGES, BadgeTier.BRONZE, "📄", "千页翻阅", "累计阅读 1000 页", 1000),
        Badge("pages_10k", BadgeCategory.PAGES, BadgeTier.GOLD, "📜", "万页读者", "累计阅读 10000 页", 10000),
        Badge("pages_100k", BadgeCategory.PAGES, BadgeTier.DIAMOND, "📚", "十万页匠", "累计阅读 100000 页", 100000),

        // === 章节（2） ===
        Badge("chapters_500", BadgeCategory.CHAPTERS, BadgeTier.SILVER, "📑", "章节猎手", "累计阅读 500 章", 500),
        Badge("chapters_5000", BadgeCategory.CHAPTERS, BadgeTier.PLATINUM, "🔖", "章节大师", "累计阅读 5000 章", 5000),

        // === 深度阅读（2） ===
        Badge("deep_30", BadgeCategory.DEPTH, BadgeTier.SILVER, "⏳", "慢读时光", "单本书连续阅读超过 30 天", 30),
        Badge("deep_100", BadgeCategory.DEPTH, BadgeTier.GOLD, "🗿", "磨杵成针", "单本书连续阅读超过 100 天", 100),

        // === 时段（2） ===
        Badge("early_bird", BadgeCategory.HABIT, BadgeTier.SILVER, "🌅", "晨读之友", "在早上 5-9 点阅读 10 次", 10),
        Badge("night_owl", BadgeCategory.HABIT, BadgeTier.SILVER, "🌙", "夜读派", "在晚上 22 点后阅读 10 次", 10),

        // === 特殊（3） ===
        Badge("five_star", BadgeCategory.SPECIAL, BadgeTier.GOLD, "⭐", "五星品味", "有 5 本书获得 5 星评分", 5),
        Badge("diverse_reader", BadgeCategory.SPECIAL, BadgeTier.SILVER, "🎭", "涉猎多样", "读过全部 3 种书籍类型", 3),
        Badge("weekly_sprint", BadgeCategory.SPECIAL, BadgeTier.GOLD, "🚀", "一周冲刺", "单周内读完 3 本书", 3)
    )

    private val byId: Map<String, Badge> = ALL.associateBy { it.id }

    fun findById(id: String): Badge? = byId[id]

    fun byCategory(): Map<BadgeCategory, List<Badge>> = ALL.groupBy { it.category }
}
