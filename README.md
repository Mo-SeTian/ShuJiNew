# ReadTrack 📚

一款简洁美观的书籍阅读进度管理应用，助你追踪阅读进度、养成阅读习惯。

![Android](https://img.shields.io/badge/Android-16%20(API%2036)-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.02-purple)
![License](https://img.shields.io/badge/License-MIT-yellow)

## ✨ 功能特性

### 📚 书籍管理
- 添加/编辑/删除书籍
- 支持从相册选择封面或拍照获取
- 5种书籍状态分类：
  - 🟢 想读 - 计划阅读的书籍
  - 🟠 阅读中 - 正在阅读的书籍
  - 🔵 已读 - 已完成的书籍
  - ⚪ 闲置 - 暂停阅读的书籍
  - 🔴 放弃 - 决定放弃的书籍

### 📊 阅读进度追踪
- 记录每日阅读页数（支持小数）
- 自动计算阅读进度百分比
- 查看阅读历史记录
- 进度趋势可视化

### 📈 数据统计
- 今日/本周阅读统计
- 连续阅读天数追踪
- 书籍状态分布饼图
- 阅读趋势折线图

### 🎨 Material Design 3
- 动态颜色主题（从壁纸提取）
- 完美的深色模式支持
- 响应式布局（支持折叠屏）
- 流畅的动画效果

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| 平台 | Android 16 (API 36) |
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Clean Architecture |
| 数据库 | Room + Kotlin Flow |
| 依赖注入 | Hilt |
| 图片加载 | Coil |
| 图表 | Vico |
| 导航 | Navigation Compose |

## 📱 截图

> 截图待添加

## 🚀 开始使用

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34+

### 克隆项目
```bash
git clone https://github.com/Mo-SeTian/ShuJiNew.git
cd ShuJiNew
```

### 在 Android Studio 中打开
1. 打开 Android Studio
2. 选择 "Open an existing project"
3. 选择 ShuJiNew 文件夹
4. 等待 Gradle 同步完成

### 运行应用
1. 连接 Android 设备或启动模拟器
2. 点击 Run 按钮 (▶️)
3. 应用将自动安装并启动

### 构建 APK
```bash
# 在项目目录下执行
./gradlew assembleDebug

# APK 输出位置
# app/build/outputs/apk/debug/app-debug.apk
```

## 📂 项目结构

```
app/src/main/java/com/readtrack/
├── data/                      # 数据层
│   ├── local/                # 本地存储
│   │   ├── dao/             # Data Access Objects
│   │   ├── entity/          # Room 实体
│   │   └── database/         # 数据库配置
│   └── repository/          # Repository 实现
├── domain/                   # 领域层
│   ├── model/               # 领域模型
│   └── repository/          # Repository 接口
├── presentation/            # 展示层
│   ├── ui/                 # Compose UI
│   │   ├── theme/          # 主题配置
│   │   ├── components/     # 可复用组件
│   │   ├── home/          # 首页
│   │   ├── books/         # 书籍列表/详情
│   │   ├── stats/         # 统计页面
│   │   ├── settings/      # 设置页面
│   │   └── addbook/       # 添加书籍
│   └── viewmodel/         # ViewModels
├── di/                     # 依赖注入模块
└── util/                  # 工具类
```

## 🎯 开发计划

- [x] 项目基础架构搭建
- [x] Room 数据库配置
- [x] 书籍管理 CRUD
- [x] 阅读进度追踪
- [x] 状态系统实现
- [x] 底部导航栏
- [ ] 统计图表完善
- [ ] 数据导入/导出
- [ ] 阅读提醒功能

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

Made with ❤️ by [Mo-SeTian](https://github.com/Mo-SeTian)
