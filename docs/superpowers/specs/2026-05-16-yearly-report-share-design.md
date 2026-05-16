# 年度阅读报告 + 分享阅读成就 设计文档

## 概述

在统计页新增年度阅读报告入口，展示全年阅读数据（Spotify Wrapped 风格）。支持将年度报告和日常阅读成就生成为图片，通过系统分享面板发送到社交平台。

## 架构

### 新增文件（8 个）

| 文件 | 说明 |
|------|------|
| `presentation/ui/stats/YearlyReportScreen.kt` | 年度报告全屏页面 |
| `presentation/ui/share/ShareCard.kt` | 分享卡片通用布局组件 |
| `presentation/ui/share/YearlyReportCard.kt` | 年度报告长卡片（4 模块拼接） |
| `presentation/ui/share/DailyAchievementCard.kt` | 日常成就卡片（今日/本周） |
| `presentation/ui/components/MonthlyTrendChart.kt` | 12 月月度趋势折线图 Canvas 组件 |
| `presentation/viewmodel/YearlyReportViewModel.kt` | 年度报告 ViewModel |
| `util/ShareBitmapHelper.kt` | Compose → Bitmap 截图 + 系统分享 |
| `domain/model/YearlyReportData.kt` | 年度报告数据模型 |

### 修改文件（4 个）

| 文件 | 改动 |
|------|------|
| `Navigation.kt` | 新增 `YearlyReport(year: Int)` 路由；NavHost 添加 composable |
| `StatsScreen.kt` | 顶部加年度报告横幅入口；统计卡片旁加分享按钮 |
| `ReadingRecordDao.kt` | 新增 `getRecordsByYearRange(yearStart, yearEnd)` 查询 |
| `MainActivity.kt` | 注册 ShareBitmapHelper 所需的 FileProvider（如尚未配置） |

### 不涉及

- 不新增持久化配置（DataStore 无改动）
- 不动备份/恢复逻辑
- 零新第三方依赖

## 数据流

```
YearlyReportViewModel
  ├── bookRepository.getAllBooks()
  ├── readingRecordDao.getRecordsByYearRange(yearStart, yearEnd)
  └── preferencesManager.statsUnit

  ↓ combine → map → YearlyReportData

YearlyReportScreen
  ├── 滚动区：Hero + 4 个数据模块卡片
  └── 固定底部：「分享图片」按钮
        ↓ ShareBitmapHelper.captureAndShare()
              Compose → Bitmap → PNG → FileProvider → Intent.ACTION_SEND
```

## 年度报告页面

### 入口

统计页顶部横幅卡片，显示「2025 年度阅读报告」，左右箭头切换年份。点击进入全屏报告页。

### 页面布局（自上而下滚动）

```
┌──────────────────────────────┐
│  ← 返回    2025 年度报告  [分享] │  固定顶部栏
├──────────────────────────────┤
│  Hero 区（渐变背景 + 年份）    │
│  "你的阅读，有迹可循"          │
├──────────────────────────────┤
│  卡片1: 年度数据总结 (2x2 网格) │
│  共读 X 本书  |  X 页/章     │
│  读完 X 本    |  平均 X 星   │
├──────────────────────────────┤
│  卡片2: 月度阅读趋势          │
│  12 月折线图 (Canvas 绘制)    │
├──────────────────────────────┤
│  卡片3: 趣味排行榜            │
│  最爱之书 · 最厚之书          │
│  陪伴最久之书                │
├──────────────────────────────┤
│  卡片4: 阅读习惯画像          │
│  最爱类型 · 最长连续天数      │
│  活跃天数 · 最爱月份          │
└──────────────────────────────┘
```

- 年份可切换（仅显示有数据的年份）
- 无数据的年份灰色不可选
- Hero 区使用渐变背景，可选封面拼贴

## 分享功能

### 两种分享卡片

**年度报告长图**：将 YearlyReportScreen 的 4 个模块渲染为一张竖长 PNG。
**日常成就卡片**：今日阅读（方形紧凑卡）、本周概览（长条卡），从首页/统计页卡片旁分享按钮触发。

### 截图分享流程

1. 将目标 Composable 渲染到 Bitmap（通过 ComposeView + drawToBitmap）
2. Bitmap 存为 PNG 到 `context.cacheDir/share/`
3. 通过 `FileProvider` 生成 `content://` URI
4. `Intent.ACTION_SEND` (MIME `image/png`) 调起系统分享面板

## 数据查询

### 新增 DAO

```kotlin
@Query("SELECT * FROM reading_records WHERE date >= :yearStart AND date < :yearEnd ORDER BY date DESC")
fun getRecordsByYearRange(yearStart: Long, yearEnd: Long): Flow<List<ReadingRecordEntity>>
```

### YearlyReportData

```kotlin
data class YearlyReportData(
    val year: Int,
    val totalBooks: Int,           // 当年有过记录的书籍数
    val finishedBooks: Int,        // 当年读完的书
    val totalPages: Double,        // 总页数
    val totalChapters: Int,        // 总章节
    val averageRating: Float,      // 平均评分
    val monthlyPages: List<Float>, // 12 个月每月页数
    val favoriteBook: BookEntity?, // 最高评分书
    val thickestBook: BookEntity?, // 最厚书
    val longestBook: BookEntity?,  // 陪伴最久之书
    val topGenre: String,          // 最爱类型
    val maxStreakDays: Int,        // 年度最长连续天数
    val activeDays: Int,           // 活跃天数
    val favoriteMonth: Int,        // 阅读量最大月份 (1-12)
)
```

### 年份列表

从全部记录中取最早年份到当前年份，无记录的年份灰显不可选。

## 测试策略

- `YearlyReportData` 计算逻辑提取为纯函数，可单元测试
- `MonthlyTrendChart` Canvas 绘制验证编译期无误（Canvas 难以自动化截图对比）
- `ShareBitmapHelper` 在真机验证分享流程
