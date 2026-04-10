# ReadTrack 📚

一个简洁优雅的 Android 阅读进度管理应用，帮助你追踪和管理阅读计划。

![Android](https://img.shields.io/badge/Android-16-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue)
![Compose](https://img.shields.io/badge/Compose-Material%203-orange)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

## ✨ 功能特性

### 📖 书籍管理
- 添加、编辑、删除书籍
- 支持设置书籍封面（从相册选择）
- 记录书籍基本信息（书名、作者、总页数）
- 搜索和筛选书籍

### 📊 阅读追踪
- 记录每日阅读进度
- 可视化阅读趋势图表
- 显示今日/本周/本月阅读量
- 计算日均阅读页数

### 🏷️ 状态管理
- **想读** - 计划阅读的书籍
- **阅读中** - 正在阅读的书籍
- **已读** - 已完成的书籍
- **闲置** - 暂时搁置的书籍
- **放弃** - 决定不再阅读的书籍

### 📈 统计中心
- 近7天阅读趋势图
- 书籍状态分布饼图
- 阅读数据统计

### 🌙 主题切换
- 跟随系统主题
- 浅色模式
- 深色模式

### 💾 数据管理
- 本地 SQLite 数据库存储
- 数据备份与恢复（JSON 格式）

## 🛠️ 技术栈

| 技术 | 版本 |
|------|------|
| Android SDK | 36 (Android 16) |
| Min SDK | 34 |
| Kotlin | 1.9.x |
| Jetpack Compose | BOM 2024.02 |
| Material Design | 3 |
| Room | 2.6.1 |
| Hilt | 2.51 |
| Coil | 2.6.0 |
| Navigation Compose | 2.7.7 |
| DataStore | 1.0.0 |

## 📁 项目结构

```
app/src/main/java/com/readtrack/
├── data/
│   ├── local/
│   │   ├── dao/           # Room DAOs
│   │   ├── database/      # 数据库和类型转换器
│   │   └── entity/        # 数据实体
│   ├── repository/        # Repository 实现
│   └── backup/           # 备份管理
├── di/                    # Hilt 依赖注入模块
├── domain/
│   ├── model/             # 领域模型
│   └── repository/       # Repository 接口
├── presentation/
│   ├── ui/
│   │   ├── components/    # 可复用 UI 组件
│   │   ├── home/          # 首页
│   │   ├── books/         # 书籍列表和详情
│   │   ├── addbook/       # 添加书籍
│   │   ├── stats/         # 统计页面
│   │   └── settings/      # 设置页面
│   ├── viewmodel/         # ViewModels
│   └── theme/             # 主题配置
└── util/                  # 工具类
```

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2024.1.1) 或更高版本
- JDK 17
- Android SDK 36

### 构建项目

```bash
# 克隆项目
git clone https://github.com/Mo-SeTian/ShuJiNew.git

# 进入项目目录
cd ShuJiNew

# 使用 Gradle Wrapper 构建 Debug APK
./gradlew assembleDebug

# APK 输出位置
# app/build/outputs/apk/debug/app-debug.apk
```

## 📥 下载 APK

从 GitHub Actions 构建产物下载最新的 Debug APK：

1. 进入 [Releases](https://github.com/Mo-SeTian/ShuJiNew/releases)
2. 或访问 Actions 页面查看构建记录
3. 下载 `app-debug.apk` 安装到设备

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

**Made with ❤️ and Jetpack Compose**
