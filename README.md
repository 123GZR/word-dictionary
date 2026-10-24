# Galaxy Watch 高考高频词背单词 App

为 Galaxy Watch 4 Classic 打造的高考高频词汇词典应用（1269 词，离线可用），
支持词库浏览/搜索、单词详情、翻卡背诵（随机）、错题集与「我的梗概」回顾。

基于 **Kotlin + Jetpack Compose + Wear OS** 技术栈，采用 MVVM + Repository 架构，
完全套用 `D:\app`（WeatherWatch）的开发规范与程序架构。

---

## 目录

- [环境要求](#环境要求)
- [首次打开](#首次打开)
- [调试链路（无 WLAN）](#调试链路无-wlan)
- [项目结构](#项目结构)
- [架构概览](#架构概览)
- [核心特性](#核心特性)
- [数据源与词库](#数据源与词库)
- [性能优化](#性能优化)
- [测试](#测试)
- [里程碑进度](#里程碑进度)
- [开发文档](#开发文档)

---

## 环境要求

| 组件 | 版本 |
|------|------|
| Android Studio | 2025.x 或更新 |
| JDK | 17（Temurin，位于 `D:\jdk17`；命令行构建必须显式设置 `JAVA_HOME`） |
| Android SDK Platform | API 36（compileSdk/targetSdk = 36，minSdk = 30） |
| Gradle | 8.13（自动下载） |
| Kotlin | 2.2.21 |
| AGP | 8.13.2 |
| Compose BOM | 2025.09.00（Compose 1.9.x，与 Wear Compose 1.6.2 声明基准对齐） |
| Wear Compose | 1.6.2（material3 / foundation） |
| Navigation | navigation-compose 2.9.0（NavHost，API 36 首屏黑屏修复方案） |
| Hilt | 2.57.2（KSP 编译） |
| KSP | 2.2.21-2.0.5 |

> ⚠️ **命令行构建必须使用 JDK 17**：Gradle 8.13 不支持在更高版本 JDK（如 Studio 自带 JBR 25）上运行。
> 每次执行 Gradle 命令前：
> ```powershell
> $env:JAVA_HOME = "D:\jdk17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
> ```

依赖版本统一管理在 `gradle/libs.versions.toml`，升级依赖请修改该文件。

---

## 首次打开

1. 用 Android Studio 打开 `D:\app-word_dictionary` 目录
2. Studio 会自动检测 `gradle-wrapper.properties`，并下载 Gradle 8.13
   - 如果提示 `gradle-wrapper.jar` 缺失，在 Studio 弹窗中选择 "Use Gradle Wrapper" 即可自动补全
3. SDK Manager 勾选 API 36 + Build-Tools（AGP 首次同步会自动安装）
4. 等待首次 Sync 完成

> 项目已配置阿里云 Maven 镜像，国内网络环境下依赖下载通常无需额外配置。

---

## 调试链路（无 WLAN）

电脑只负责编译，APK 通过手机端 WearOS 工具箱无线安装到手表：

1. 手机开移动热点，手表连热点
2. 手表：设置 → 关于手表 → 连续点"软件版本"7 次开启开发者模式
3. 手表：设置 → 开发者选项 → 开启 ADB 调试 + 无线调试
4. 手机端打开 WearOS 工具箱，输入配对码连接手表
5. 在 Android Studio 中 `Build → Build APK(s)` 生成 `app-debug.apk`
6. 把 APK 文件传到手机（微信/QQ/USB 均可）
7. 在 WearOS 工具箱中选 APK 安装

---

## 项目结构

```
app-word_dictionary/                       # 工程根目录
├── README.md / DEVELOPMENT.md / AGENTS.md # 规范文档三件套（套用 D:\app 结构）
├── settings.gradle.kts / build.gradle.kts / gradle.properties
├── gradle/
│   ├── wrapper/                           # Gradle 8.13 wrapper
│   └── libs.versions.toml                 # 统一版本矩阵
├── tools/parse_vocabulary.py              # 词库解析脚本（txt → words.json）
├── docs/词库解析报告.md                    # 词库解析统计与规则说明
└── app/                                   # 主应用模块
    ├── build.gradle.kts / proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml        # Wear OS 声明、MainActivity
        │   ├── assets/words.json          # 结构化词库（1269 条，由脚本生成）
        │   ├── kotlin/com/galaxy/worddictionary/
        │   │   ├── WordDictionaryApplication.kt # Application 入口（@HiltAndroidApp）
        │   │   ├── MainActivity.kt              # 主 Activity（@AndroidEntryPoint）
        │   │   ├── data/                        # 数据层
        │   │   │   ├── model/WordEntry.kt       # 词条模型（@Immutable + @Serializable）
        │   │   │   ├── DictionaryRepository.kt  # 词库仓库接口
        │   │   │   ├── LocalDictionaryRepository.kt # assets JSON 实现（单例缓存）
        │   │   │   ├── WrongBookRepository.kt   # 错题集仓库接口
        │   │   │   ├── LocalWrongBookRepository.kt # 错题集 SharedPreferences 实现
        │   │   │   └── WordExceptions.kt        # 词库业务异常
        │   │   ├── di/DataModule.kt             # Hilt 装配（Provides / Binds）
        │   │   └── ui/                          # UI 层
        │   │       ├── WordAppScreen.kt         # 顶层导航根（NavHost）
        │   │       ├── WordViewModel.kt         # ViewModel + UiState + 背诵会话
        │   │       ├── theme/Theme.kt           # Wear M3 主题（原型配色）
        │   │       ├── components/DictionaryIcons.kt # Canvas 矢量图标（禁 emoji）
        │   │       └── screens/
        │   │           ├── WordListScreen.kt    # 词库浏览/搜索/错题集与随机背诵入口
        │   │           ├── WordDetailScreen.kt  # 单词详情
        │   │           ├── StudyScreen.kt       # 翻卡背诵 + 梗概（MVP 核心）
        │   │           ├── WrongBookScreen.kt   # 错题集（表圈滚动）
        │   │           ├── LoadingScreen.kt
        │   │           └── ErrorScreen.kt
        │   └── res/
        │       ├── values/{strings,colors,themes}.xml
        │       ├── drawable/ic_launcher_foreground.xml
        │       └── mipmap-anydpi-v26/ic_launcher{,_round}.xml
```

---

## 架构概览

项目采用 **MVVM + Repository** 架构（与 D:\app 同构）：

```
┌─────────────────────────────────────────────┐
│                  UI Layer                    │
│  Screens / Components / WordAppScreen        │
│              (Jetpack Compose)               │
└──────────────────┬──────────────────────────┘
                   │ StateFlow (collectAsState)
                   ▼
┌─────────────────────────────────────────────┐
│              ViewModel Layer                 │
│             WordViewModel                    │
│    (状态管理、搜索防抖、背诵会话编排)         │
└──────────────────┬──────────────────────────┘
                   │ DictionaryRepository 接口
                   ▼
┌─────────────────────────────────────────────┐
│               Data Layer                     │
│         LocalDictionaryRepository            │
│     (assets JSON 解析、内存索引、Result)      │
└─────────────────────────────────────────────┘
```

**数据流方向：** 单向数据流
- Repository → ViewModel（词库数据）
- ViewModel → UI（`StateFlow<WordUiState>` / `StateFlow<StudyState>`）
- UI → ViewModel（事件回调：`onQueryChange` / `onMark` 等）

详细架构说明请参考 [DEVELOPMENT.md](./DEVELOPMENT.md)。

---

## 核心特性

- ⌚ **Wear OS 原生体验**：Wear Compose Material 3 + Compose Navigation（NavHost），适配圆形表盘
- ⚡ **秒开主页面**：启动立即渲染主页面骨架（标题/入口/搜索框），词库在后台加载完成后填充词条列表
- 📖 **词库浏览**：1269 条高考高频词/短语，TransformingLazyColumn 列表 + 表圈 snap 滚动
- 🔍 **搜索**：英文包含/前缀 + 中文释义包含，200ms 防抖
- 🃏 **翻卡背诵**：点击翻面显示释义，「认识/不认识」标记自动前进；长单词自动缩小字号
- 🌀 **表圈驱动**：背诵时转表圈先翻面看释义、再转跳过；梗概与错题集列表支持表圈滚动
- 📕 **错题集**：背诵标记「不认识」的单词自动记录并持久化；可复习（标记「认识」自动移出）或清空
- 📊 **我的梗概**：记得/没记得清单回顾，可清空更改重新开始
- ✅ **结束统计**：全部答完后展示认识/不认识统计与「再来一组」
- 🎨 **Canvas 矢量图标**：纯绘制图标，无字体依赖，性能更好
- ⚡ **Compose Strong Skipping**：Kotlin 2.0 编译器默认开启
- 📊 **单一状态流**：主状态合并为单一 StateFlow，减少重组次数

---

## 数据源与词库

词库来源：`高频词汇总.txt`（高考高频词背诵汇总，扫描版 38 页整理，共 1269 条）。

- 生成脚本：`tools/parse_vocabulary.py`
- 产物：`app/src/main/assets/words.json`（1269 条全部结构化成功，无 raw 保留、无重复）
- 解析规则（详见 `docs/词库解析报告.md`）：
  - 英文与中文释义以制表符分隔
  - 词性标注提取：`v.`/`n.`/`a.`/`ad.`/`prep.`/`conj.`/`pron.`/`num.`/`art.` 及组合（`n./v.`），兼容 `adj.`/`adv.`/`vt.`/`vi.` 与中文标注（介词）等
  - 多义词 ①②③ 标注原样保留；`→` 派生词链拆入 `wordForms`（本版源文件无 → 链）
  - 每条记录保留 `raw` 原始行，可追溯、不丢数据
- 重新生成词库：
  ```powershell
  python tools\parse_vocabulary.py
  ```

---

## 性能优化（P0，已完成）

**组合/重组层**
- 数据模型加 `@Immutable`，Compose 编译器可跳过未变化子树的重组
- 主状态合并为单一 `StateFlow<WordUiState>`，一次刷新只触发一轮重组
- 列表 item 使用稳定 `key`（word 文本），刷新时只重组变化行
- Compose strong skipping 由 Kotlin 2.0.20 编译器默认开启（无需显式配置）

**绘制层**
- 图标由 emoji 改为 Canvas 矢量绘制，消除手表字体回退扫描卡顿
- 图标使用 `drawWithCache` 缓存绘制指令，滚动/重组时直接复用

**启动层**
- 主题声明深色 `windowBackground`，冷启动到首帧期间窗口不白屏闪烁
- 移除 Activity 中冗余的重复 `setTheme` 调用
- 启动先渲染主页面骨架，词库在 IO 后台加载完成后填充词条列表，避免整屏「词库加载中…」等待

**数据层**
- 词库单例缓存（首次加载后常驻内存），搜索为内存过滤，无 IO 阻塞
- 共享 `Json` 反序列化器，解析失败快速暴露（错误页 + 重试）

---

## 测试

> ⚠️ 与 D:\app 一致，本项目暂不要求编写单元测试（2026-10）。
> 构建脚本中的测试依赖（JUnit / MockK / Turbine / coroutines-test）保留，
> 将来恢复测试时直接在 `app/src/test/` 添加即可。

---

## 里程碑进度

- [x] M1 工程骨架（Gradle + Wear 模块 + git，照搬 D:\app 规范）
- [x] M2 词库数据层（1269 条结构化 + Repository）
- [x] M3 词库浏览与详情 UI（列表 + 搜索 + 详情）
- [x] M4 背诵模式 MVP（翻卡/认识/不认识/表圈跳过/梗概/结束统计）
- [x] M5 构建验证与文档交付（:app:assembleDebug 通过）
- [x] M6 错题集与表圈交互升级（错题持久化/错题列表与复习/清空/梗概表圈滚动/先看释义再跳过/换词闪现修复）

---

## 开发文档

更详细的开发指南请参考：

| 文档 | 说明 |
|------|------|
| [DEVELOPMENT.md](./DEVELOPMENT.md) | 架构设计、模块详解、编码规范、构建发布 |
| [AGENTS.md](./AGENTS.md) | AI 协作规范、文档更新要求、提交规范（AI 必读） |

> ⚠️ **AI 开发者注意**：每次新增或修改功能后，必须同步更新相关文档。
> 具体规则请阅读 [AGENTS.md](./AGENTS.md)。
