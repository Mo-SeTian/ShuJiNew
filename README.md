# 书迹 ReadTrack

> 一个简洁优雅的 Android 阅读进度管理应用，帮助你追踪和管理阅读计划。

[![Android](https://img.shields.io/badge/Android-14%2B-brightgreen)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.x-blue)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Compose-Material%203-orange)](https://developer.android.com/compose)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.9.5-blue)](https://github.com/Mo-SeTian/ShuJiNew/releases)

## 下载

| 渠道 | 地址 |
|------|------|
| GitHub Releases | [最新版本](https://github.com/Mo-SeTian/ShuJiNew/releases/latest) |
| Gitee Releases（国内镜像） | [最新版本](https://gitee.com/mosetian/shu-ji-new/releases) |

## 功能特性

### 书籍管理
- 添加、编辑、删除书籍，支持设置封面
- 豆瓣 / Bing 封面搜索一键设置
- 支持按书名/作者搜索和筛选

### 阅读追踪
- 记录每日阅读进度（页数/章节）
- 可视化阅读趋势图表
- 阅读时间线展示

### 五大状态

| 状态 | 说明 |
|------|------|
| 想读 | 计划阅读的书籍 |
| 阅读中 | 正在阅读的书籍 |
| 已读 | 已完成的书籍 |
| 闲置 | 暂时搁置的书籍 |
| 放弃 | 决定不再阅读的书籍 |

### 统计中心
- 阅读趋势、书籍分布、数据统计
- 按书单分类统计

### 书单管理
- 创建自定义书单，批量管理书籍

### 数据备份
- 本地 ZIP 备份（含封面图片）
- WebDAV 云端自动同步
- 导入/导出预览、增量导入

### 主题切换
- 跟随系统 / 浅色 / 深色模式

### 应用更新
- 应用内检测新版本
- 支持 GitHub / Gitee 更新源切换

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
│   ├── ui/             # Compose 页面
│   └── viewmodel/      # ViewModels
├── remote/             # 更新检测、ReleaseInfo
├── worker/             # WorkManager 后台任务
├── util/               # 工具类
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
