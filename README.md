# ReadTrack 📚

> 一个简洁优雅的 Android 阅读进度管理应用，帮助你追踪和管理阅读计划。

[![Android](https://img.shields.io/badge/Android-16-brightgreen)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Compose-Material%203-orange)](https://developer.android.com/compose)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.7.0-blue)](app/build.gradle.kts)

## 📱 应用截图

> 截图待添加

## ✨ 功能特性

### 📖 书籍管理
- 添加、编辑、删除书籍
- 支持设置书籍封面（从相册选择或网络搜索）
- 记录书籍基本信息（书名、作者、出版社、总页数）
- 支持按书名/作者搜索和筛选书籍
- 批量管理书籍

### 📊 阅读追踪
- 记录每日阅读进度（页数/章节）
- 可视化阅读趋势图表
- 显示今日/本周/本月阅读量
- 计算日均阅读页数
- 阅读时间线展示

### 🏷️ 状态管理
五种阅读状态，清晰管理阅读计划：

| 状态 | 说明 |
|------|------|
| 📚 想读 | 计划阅读的书籍 |
| 📖 阅读中 | 正在阅读的书籍 |
| ✅ 已读 | 已完成的书籍 |
| 💤 闲置 | 暂时搁置的书籍 |
| ❌ 放弃 | 决定不再阅读的书籍 |

### 📈 统计中心
- 近7天阅读趋势图
- 书籍状态分布饼图
- 阅读数据统计（总阅读量、阅读时长等）
- 按书单分类统计

### 📋 书单管理
- 创建自定义书单
- 拖拽添加/移除书籍
- 自定义书单封面
- 书单内书籍批量操作

### 🌙 主题切换
- 跟随系统主题
- 浅色模式
- 深色模式

### 💾 数据备份
- 本地 ZIP 备份（包含数据 + 封面图片）
- WebDAV 云端自动同步
- 导入/导出预览功能
- 增量导入模式

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| **语言** | Kotlin 1.9.22 |
| **平台** | Android (Min SDK 34, Target SDK 36) |
| **UI框架** | Jetpack Compose + Material Design 3 |
| **架构** | MVVM + Clean Architecture |
| **依赖注入** | Hilt 2.51 |
| **数据库** | Room 2.6.1 |
| **网络** | OkHttp 4.12.0 |
| **图片加载** | Coil 2.6.0 |
| **导航** | Navigation Compose 2.7.7 |
| **序列化** | Kotlin Serialization 1.6.2 |
| **本地存储** | DataStore Preferences 1.0.0 |
| **后台任务** | WorkManager 2.9.0 |
| **构建工具** | Gradle 8.6 + AGP 8.3.0 + KSP |

## 📁 项目结构

```
app/src/main/java/com/readtrack/
├── data/                        # 数据层
│   ├── local/                   # 本地存储
│   │   ├── dao/                 # Room DAOs
│   │   ├── database/            # 数据库和类型转换器
│   │   └── entity/             # 数据实体
│   ├── remote/                  # 远程服务（豆瓣/Bing/WebDAV）
│   └── repository/             # Repository 实现
├── di/                          # Hilt 依赖注入模块
├── domain/                      # 领域层
│   ├── model/                  # 领域模型
│   └── repository/             # Repository 接口
├── presentation/                # 表现层
│   ├── ui/
│   │   ├── components/         # 可复用 UI 组件
│   │   ├── home/              # 首页
│   │   ├── books/             # 书籍列表和详情
│   │   ├── addbook/           # 添加书籍
│   │   ├── booklist/         # 书单管理
│   │   ├── stats/            # 统计页面
│   │   ├── timeline/         # 阅读时间线
│   │   ├── settings/         # 设置页面
│   │   └── theme/            # 主题配置
│   └── viewmodel/            # ViewModels
├── worker/                      # WorkManager 后台任务
├── util/                        # 工具类
├── MainActivity.kt
└── ReadTrackApp.kt            # Application 类
```

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2024.1.1) 或更高版本
- JDK 17
- Android SDK 36
- Android 14 (API 34) 或更高版本设备

### 构建项目

```bash
# 克隆项目
git clone https://github.com/Mo-SeTian/ShuJiNew.git

# 进入项目目录
cd ShuJiNew

# 使用 Gradle Wrapper 构建 Debug APK
./gradlew assembleDebug

# APK 输出位置
app/build/outputs/apk/debug/app-debug.apk
```

### 安装测试

```bash
# 将 APK 安装到已连接的设备
./gradlew installDebug
```

## 📥 下载 APK

从 GitHub Actions 构建产物下载最新的 Debug APK：

1. 进入 [Releases](https://github.com/Mo-SeTian/ShuJiNew/releases)
2. 或访问 [Actions](https://github.com/Mo-SeTian/ShuJiNew/actions) 页面查看构建记录
3. 下载 `app-debug.apk` 安装到设备

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

**Made with ❤️ and Jetpack Compose**