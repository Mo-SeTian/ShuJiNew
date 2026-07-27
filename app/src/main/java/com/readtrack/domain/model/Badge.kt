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
    HABIT("时段习惯"),
    SPECIAL("特殊成就")
}

/**
 * 徽章等级。
 */
enum class BadgeTier(val displayName: String, val colorHex: Long, val starCount: Int) {
    BRONZE("青铜", 0xFFCD853F, 1),
    SILVER("白银", 0xFFA8B8C8, 2),
    GOLD("黄金", 0xFFE8B730, 3),
    PLATINUM("白金", 0xFFA0D2DB, 4),
    DIAMOND("钻石", 0xFF9B59B6, 5)
}

/**
 * 徽章元数据。全部内置在应用中。
 * [progressKey] 对应 [BadgeCheckSnapshot] 中的统计字段，用于计算当前进度。
 */
data class Badge(
    val id: String,
    val category: BadgeCategory,
    val tier: BadgeTier,
    val icon: String,
    val title: String,
    val description: String,
    val threshold: Int,
    /** 进度统计键，见 BadgeCheckSnapshot.progressMap */
    val progressKey: String,
    /** 进度的单位标签，如 "本"/"天"/"页"/"章"/"次" */
    val unit: String,
    /** 获得徽章时展示的趣味评语 */
    val quote: String
)

/**
 * 全部徽章目录。共 22 枚。
 */
object BadgeCatalog {

    // 进度键常量
    const val PROGRESS_FINISHED = "finishedBookCount"
    const val PROGRESS_STREAK = "currentStreak"
    const val PROGRESS_TOTAL_PAGES = "totalPages"
    const val PROGRESS_TOTAL_CHAPTERS = "totalChapters"
    const val PROGRESS_DEEP_READ = "longestDeepReadDays"
    const val PROGRESS_MORNING = "morningReadCount"
    const val PROGRESS_NIGHT = "nightReadCount"
    const val PROGRESS_FIVE_STAR = "fiveStarCount"
    const val PROGRESS_DIVERSE = "distinctBookTypes"
    const val PROGRESS_WEEKLY = "maxFinishedInWeek"

    val ALL: List<Badge> = listOf(
        // === 阅读数量（5） ===
        Badge("reader_first", BadgeCategory.QUANTITY, BadgeTier.BRONZE,
            icon = "📖",
            title = "初次相遇",
            description = "在书迹中读完第 1 本书，开启你的阅读之旅",
            threshold = 1, progressKey = PROGRESS_FINISHED, unit = "本",
            quote = "每一段旅程都始于翻开第一页"),
        Badge("reader_10", BadgeCategory.QUANTITY, BadgeTier.SILVER,
            icon = "📚",
            title = "十里书香",
            description = "累计读完 10 本书，阅读已成为生活的一部分",
            threshold = 10, progressKey = PROGRESS_FINISHED, unit = "本",
            quote = "十日一读易，十年一读难。你已迈出坚实的第一步"),
        Badge("reader_50", BadgeCategory.QUANTITY, BadgeTier.GOLD,
            icon = "🏛️",
            title = "半百书阁",
            description = "累计读完 50 本书，家中的书架已蔚为壮观",
            threshold = 50, progressKey = PROGRESS_FINISHED, unit = "本",
            quote = "五十本书，五十个世界。你的眼界已超越大多数人"),
        Badge("reader_100", BadgeCategory.QUANTITY, BadgeTier.PLATINUM,
            icon = "🏆",
            title = "百书争鸣",
            description = "累计读完 100 本书，百种思想在脑中碰撞",
            threshold = 100, progressKey = PROGRESS_FINISHED, unit = "本",
            quote = "读万卷书，行万里路。你已完成了万卷的百分之一"),
        Badge("reader_300", BadgeCategory.QUANTITY, BadgeTier.DIAMOND,
            icon = "👑",
            title = "千卷阅遍",
            description = "累计读完 300 本书，知识的殿堂为你敞开",
            threshold = 300, progressKey = PROGRESS_FINISHED, unit = "本",
            quote = "三百卷藏胸中，谈吐自有丘壑"),

        // === 连续打卡（5） ===
        Badge("streak_3", BadgeCategory.STREAK, BadgeTier.BRONZE,
            icon = "🔥",
            title = "星星之火",
            description = "连续 3 天有阅读记录，好习惯开始萌芽",
            threshold = 3, progressKey = PROGRESS_STREAK, unit = "天",
            quote = "三日不读，便觉语言无味。你已点燃阅读之火"),
        Badge("streak_7", BadgeCategory.STREAK, BadgeTier.SILVER,
            icon = "🌟",
            title = "一周之约",
            description = "连续 7 天（一整周）每天阅读，自律即自由",
            threshold = 7, progressKey = PROGRESS_STREAK, unit = "天",
            quote = "七天，一个循环。你已证明阅读不是一时兴起"),
        Badge("streak_30", BadgeCategory.STREAK, BadgeTier.GOLD,
            icon = "🌙",
            title = "月度修行",
            description = "连续 30 天（一个月）不间断阅读，习惯已融入血液",
            threshold = 30, progressKey = PROGRESS_STREAK, unit = "天",
            quote = "三十天，足以让任何行为成为本能"),
        Badge("streak_100", BadgeCategory.STREAK, BadgeTier.PLATINUM,
            icon = "💎",
            title = "百日筑基",
            description = "连续 100 天每天阅读，毅力堪比职业运动员",
            threshold = 100, progressKey = PROGRESS_STREAK, unit = "天",
            quote = "百日坚持，世间难事不过如此"),
        Badge("streak_365", BadgeCategory.STREAK, BadgeTier.DIAMOND,
            icon = "🌈",
            title = "一年之书",
            description = "连续 365 天（一整年）每天阅读，四季更迭，书不离手",
            threshold = 365, progressKey = PROGRESS_STREAK, unit = "天",
            quote = "一年四季，日日书香。此等恒心，万中无一"),

        // === 页数（3） ===
        Badge("pages_1k", BadgeCategory.PAGES, BadgeTier.BRONZE,
            icon = "📄",
            title = "千页之初",
            description = "累计阅读 1,000 页，相当于 3~5 本中等厚度的书",
            threshold = 1000, progressKey = PROGRESS_TOTAL_PAGES, unit = "页",
            quote = "千里之行，始于足下。千页之读，始于指尖"),
        Badge("pages_10k", BadgeCategory.PAGES, BadgeTier.GOLD,
            icon = "📜",
            title = "万页书香",
            description = "累计阅读 10,000 页，堆起来比你还高",
            threshold = 10000, progressKey = PROGRESS_TOTAL_PAGES, unit = "页",
            quote = "万页翻过，指尖的茧是知识最真实的印记"),
        Badge("pages_100k", BadgeCategory.PAGES, BadgeTier.DIAMOND,
            icon = "📚",
            title = "十万页匠",
            description = "累计阅读 100,000 页，相当于一套百科全书",
            threshold = 100000, progressKey = PROGRESS_TOTAL_PAGES, unit = "页",
            quote = "十万页，足以重塑一个人的灵魂"),

        // === 章节（2） ===
        Badge("chapters_500", BadgeCategory.CHAPTERS, BadgeTier.SILVER,
            icon = "🔖",
            title = "章节猎手",
            description = "累计阅读 500 章（网络小说约 2~3 部的体量）",
            threshold = 500, progressKey = PROGRESS_TOTAL_CHAPTERS, unit = "章",
            quote = "五百章，已是数量可观的阅读投入"),
        Badge("chapters_5000", BadgeCategory.CHAPTERS, BadgeTier.PLATINUM,
            icon = "🏅",
            title = "万章大师",
            description = "累计阅读 5,000 章，追更追到天荒地老",
            threshold = 5000, progressKey = PROGRESS_TOTAL_CHAPTERS, unit = "章",
            quote = "五千章，不是谁都能坚持的。你是真正的章节猎人"),

        // === 深度阅读（2） ===
        Badge("deep_30", BadgeCategory.DEPTH, BadgeTier.SILVER,
            icon = "⏳",
            title = "慢读时光",
            description = "有一本书陪伴了你超过 30 天，细水长流的阅读最深情",
            threshold = 30, progressKey = PROGRESS_DEEP_READ, unit = "天",
            quote = "慢下来，才能真正读懂一本书"),
        Badge("deep_100", BadgeCategory.DEPTH, BadgeTier.GOLD,
            icon = "🗿",
            title = "磨杵成针",
            description = "有一本书陪伴了你超过 100 天，你和这本书已成挚友",
            threshold = 100, progressKey = PROGRESS_DEEP_READ, unit = "天",
            quote = "百天共读，这本书已是你生命的一部分"),

        // === 阅读习惯（2） ===
        Badge("early_bird", BadgeCategory.HABIT, BadgeTier.SILVER,
            icon = "🌅",
            title = "晨光读者",
            description = "在清晨 5:00~9:00 之间记录了 10 次阅读，一日之计在于晨",
            threshold = 10, progressKey = PROGRESS_MORNING, unit = "次",
            quote = "迎着朝阳翻开书页，是给一天最好的开始"),
        Badge("night_owl", BadgeCategory.HABIT, BadgeTier.SILVER,
            icon = "🦉",
            title = "夜读猫头鹰",
            description = "在深夜 22:00 之后记录了 10 次阅读，万籁俱寂时最适合读书",
            threshold = 10, progressKey = PROGRESS_NIGHT, unit = "次",
            quote = "夜深人静，一盏灯一本书，便是全世界"),

        // === 特殊成就（3） ===
        Badge("five_star", BadgeCategory.SPECIAL, BadgeTier.GOLD,
            icon = "⭐",
            title = "慧眼识珠",
            description = "为 5 本书打出了五星满分评价，好品味值得被看见",
            threshold = 5, progressKey = PROGRESS_FIVE_STAR, unit = "本",
            quote = "五本书，五颗星，你的审美从未掉线"),
        Badge("diverse_reader", BadgeCategory.SPECIAL, BadgeTier.SILVER,
            icon = "🎭",
            title = "博览群书",
            description = "读过全部 3 种类型（小说、漫画、有声书），阅读口味不设限",
            threshold = 3, progressKey = PROGRESS_DIVERSE, unit = "种",
            quote = "不定义自己，不局限视野。这才是真正的阅读者"),
        Badge("weekly_sprint", BadgeCategory.SPECIAL, BadgeTier.GOLD,
            icon = "⚡",
            title = "阅读冲刺",
            description = "在一周之内完成了 3 本书，效率拉满的阅读马拉松",
            threshold = 3, progressKey = PROGRESS_WEEKLY, unit = "本",
            quote = "一周三本，你不是在读书，你是在燃烧")
    )

    private val byId: Map<String, Badge> = ALL.associateBy { it.id }

    fun findById(id: String): Badge? = byId[id]

    fun byCategory(): Map<BadgeCategory, List<Badge>> = ALL.groupBy { it.category }
}
