# 书迹 ReadTrack

> 一个简洁优雅的 Android 阅读进度管理应用，帮助你追踪和管理阅读计划。

[![Android](https://img.shields.io/badge/Android-14%2B-brightgreen)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.x-blue)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Compose-Material%203-orange)](https://developer.android.com/compose)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)
[![Version](https://img.shields.io/badge/Version-2.6.0-blue)](https://github.com/Mo-SeTian/ShuJiNew/releases)

## 下载

| 渠道 | 地址 |
|------|------|
| GitHub Releases | [最新版本](https://github.com/Mo-SeTian/ShuJiNew/releases/latest) |
| Gitee Releases（国内镜像） | [最新版本](https://gitee.com/mosetian/ShuJiNew/releases) |

## 功能特性

### 书籍管理
- 添加、编辑、删除书籍，支持设置封面（本地/URL/Emoji/纯色）
- 豆瓣搜索一键填充书籍信息（书名、作者、出版社、封面）
- 支持按书名/作者搜索、按状态/类型/书单/标签多条件筛选
- 自定义排序（最近阅读/最近添加/书名/评分）

### 阅读追踪
- 双模式进度：页数模式 / 章节模式
- 快速记录进度（首页卡片 / 桌面小组件）
- 阅读热力图（GitHub 风格月度日历，点击格子查看当日详情）
- 7 天阅读趋势折线图
- 阅读时间线：展示在读→已读/闲置/放弃的完整周期，支持多周期

### 五大状态

| 状态 | 说明 |
|------|------|
| 想读 | 计划阅读的书籍 |
| 在读 | 正在阅读的书籍 |
| 已读 | 已完成的书籍 |
| 闲置 | 暂时搁置的书籍 |
| 放弃 | 决定不再阅读的书籍 |

### 标签系统
- 自定义标签创建/删除，支持颜色标记
- 书籍多标签关联，标签筛选

### 统计中心
- 今日/本周/本月/累计阅读量
- 近 7 天阅读趋势柱状图（点击查看日明细）
- 书籍状态分布可视化
- 阅读历史时间线，支持时间范围筛选

### 年度阅读报告
- Spotify Wrapped 风格年度总结
- 年度数据总结、月度趋势折线图、趣味排行榜、阅读习惯画像
- 支持年份切换，生成图片分享

### 书单收藏夹
- 创建/编辑/删除书单，批量添加书籍
- 书单封面自动/手动设置
- 书单筛选

### 桌面小组件
- 添加桌面小部件，绑定指定书籍
- 显示封面、书名、作者、进度
- 点击快捷记录阅读进度

### 分享成就
- 本周概览 / 月度概览（年月可选）生成精美图片
- 年度报告长图分享
- 阅读时间线分享
- 通过系统分享面板发送到社交平台

### 数据备份
- 本地 ZIP 备份（含封面图片）
- JSON 纯数据导出
- CSV 表格导出全部书籍信息
- WebDAV 云端自动同步（每日/每周）
- 导入预览、清空导入/追加导入

### 主题切换
- 跟随系统 / 浅色 / 深色模式
- Material You 动态取色（Android 12+）

### 应用更新
- 应用内检测新版本
- 支持 GitHub / Gitee 更新源切换，离线可切换

### 隐私保护
- 使用友盟统计，仅收集匿名活跃数据
- 不收集个人信息、书籍内容或阅读记录

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin 1.9.x |
| 平台 | Android (Min SDK 34, Target SDK 36) |
| UI | Jetpack Compose + Material Design 3 |
| 架构 | MVVM + Clean Architecture |
| DI | Hilt 2.51 (KSP) |
| 数据库 | Room 2.6.1 |
| 网络 | OkHttp 4.12.0 |
| 图片 | Coil 2.6.0 |
| 导航 | Navigation Compose 2.7.7 |
| 序列化 | Kotlin Serialization 1.6.2 |
| 存储 | DataStore Preferences 1.0.0 |
| 后台 | WorkManager 2.9.0 |
| 统计 | 友盟 U-App (common 9.9.1 + asms 1.8.7.2) |
| 构建 | Gradle 8.6 + AGP 8.3.0 + KSP |

## 项目结构

```
app/src/main/java/com/readtrack/
├── data/
│   ├── local/          # Room DAOs、Database、Entity
│   ├── remote/         # 豆瓣/Bing/WebDAV/更新检测
│   └── repository/     # Repository 实现
├── di/                 # Hilt 依赖注入
├── domain/
│   ├── model/          # 领域模型、备份模型
│   └── repository/     # Repository 接口
├── presentation/
│   ├── ui/
│   │   ├── addbook/    # 添加/编辑书籍
│   │   ├── booklist/   # 书单
│   │   ├── books/      # 书籍列表、详情、阅读记录
│   │   ├── components/ # 通用组件（热力图、趋势图、卡片等）
│   │   ├── home/       # 首页仪表盘
│   │   ├── readinghistory/ # 阅读历史
│   │   ├── settings/   # 设置、备份、关于、小组件
│   │   ├── share/      # 分享卡片和截图工具
│   │   ├── stats/      # 统计、年度报告
│   │   ├── timeline/   # 时间线
│   │   └── theme/      # 颜色、主题、字体
│   └── viewmodel/      # ViewModels
├── remote/             # 更新检测
├── util/               # 工具类（CSV导出、封面存储等）
├── widget/             # 桌面小组件
├── worker/             # WorkManager 后台任务
├── MainActivity.kt
└── ReadTrackApp.kt
```

## 构建

```bash
git clone https://github.com/Mo-SeTian/ShuJiNew.git
cd ShuJiNew
./gradlew assembleRelease
```

APK 输出：`app/build/outputs/apk/release/app-release.apk`

## 许可证

MIT License
