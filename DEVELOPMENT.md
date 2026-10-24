# 开发文档（DEVELOPMENT.md）

本文档面向开发者与 AI 协作开发，描述 `D:\app-word_dictionary` 的架构设计、模块详解、
状态管理、性能优化与编码规范。开发前必读；与代码同 commit 同步更新。

架构与规范完全套用 `D:\app`（WeatherWatch）并适配词典应用，详见 [README.md](./README.md)。

---

## 目录

- [架构设计](#架构设计)
- [数据层详解](#数据层详解)
- [ViewModel 层详解](#viewmodel-层详解)
- [UI 层详解](#ui-层详解)
- [状态流转](#状态流转)
- [数据模型表](#数据模型表)
- [性能优化指南](#性能优化指南)
- [编码规范](#编码规范)
- [命名规范](#命名规范)
- [依赖管理](#依赖管理)
- [构建与发布](#构建与发布)
- [常见问题](#常见问题)

---

## 架构设计

三层架构（MVVM + Repository），与 D:\app 同构：

```
UI 层（Compose）       ViewModel 层            Data 层
Screens/Components ──► WordViewModel ──► DictionaryRepository 接口
   ▲  StateFlow           │                      │
   └──────────────────────┘         LocalDictionaryRepository（实现）
```

- **UI 层**：Screens / Components / 顶层 NavHost（`WordAppScreen.kt`），只依赖 ViewModel，
  不直接接触数据层
- **ViewModel 层**：状态管理、业务编排（搜索防抖、背诵会话）、协程调度；依赖 Repository **接口**
- **Data 层**：数据获取（assets 解析）、内存索引、异常封装；实现 Repository 接口
- **单向数据流**：UI 事件 → ViewModel → `StateFlow<UiState>` → UI 重组
- **Hilt 依赖注入**：`@HiltAndroidApp` + `@AndroidEntryPoint` + `@HiltViewModel` + `di/DataModule.kt`
  （Provides / Binds 分离）

## 数据层详解

| 文件 | 职责 |
|------|------|
| `data/model/WordEntry.kt` | `WordBook`/`WordEntry` 数据模型（`@Immutable` + `@Serializable`） |
| `data/DictionaryRepository.kt` | 仓库接口：`searchWords` / `getAllWords` / `getWord` |
| `data/LocalDictionaryRepository.kt` | assets `words.json` 加载与解析（单例缓存 + 内存过滤） |
| `data/WrongBookRepository.kt` | 错题集仓库接口：`loadWrongIds` / `addWrong` / `removeWrong` / `clearWrong` |
| `data/LocalWrongBookRepository.kt` | 错题集 SharedPreferences 实现（StringSet 持久化） |
| `data/WordExceptions.kt` | 业务异常：`WordDataLoadException`（词库加载失败） |

要点：
- `LocalDictionaryRepository` 用 `by lazy` 单例缓存词库；`Json { ignoreUnknownKeys = true }`
  共享反序列化器；assets 读取失败统一抛 `WordDataLoadException`
- 搜索规则：英文包含/前缀（不区分大小写）+ 中文释义包含；空查询返回全部
- 词条 id 使用英文词条文本（唯一稳定键），`WordEntry.displayLine` 为「词性+释义」展示合并串

## ViewModel 层详解

| 文件 | 职责 |
|------|------|
| `ui/WordViewModel.kt` | `@HiltViewModel`，注入 `DictionaryRepository` + `WrongBookRepository` + IO 调度器 |

状态：
- `uiState: StateFlow<WordUiState>`：主状态（Loading / Success / Error），
  `Success` 同时携带 `allWords`（背诵数据源）、`displayWords`（搜索过滤后列表）
  与 `isLoading`（词库是否仍在后台加载；为 true 时背诵/错题入口禁用）
- `studyState: StateFlow<StudyState>`：背诵会话状态
  （词序 / 当前索引 / 翻面 / 认识与不认识集合 / 顺序）
- `wrongIds: StateFlow<Set<String>>`：错题集词条 id 集合（持久化，跨会话保留）
- `searchQuery: StateFlow<String>`：搜索关键字

业务逻辑：
- `loadWords()`：词库后台加载（IO 调度器）；加载期间保持主页面骨架
  （`Success(isLoading = true)`），完成后切换完整数据，失败映射为 `Error(DATA_LOAD_FAILED)`；
  重试时同样先回到主页面骨架再加载
- `updateSearchQuery()` / `clearSearch()`：200ms 防抖刷新 `displayWords`
- `startStudy(order)`：随机（`shuffled()` 一次成型，会话内顺序固定）；词库未就绪（`isLoading`）时忽略
- `startWrongStudy()`：以当前错题为数据源开始复习会话（顺序，`source = WRONG_BOOK`）
- `clearWrong()`：清空错题集（持久化 + 内存状态）
- `flipCard()` / `markCurrent(known)` / `skipNext()` / `resetStudy()`：背诵会话编排；
  全部答完（`reviewed >= words.size`）进入完成态，不再前进；
  `markCurrent(false)` 同步把词条 id 写入错题集（`WrongBookRepository.addWrong`）；
  `markCurrent(true)` 在错题复习会话（`source == WRONG_BOOK`）中同步移出错题集（已学会）

## UI 层详解

| 文件 | 职责 |
|------|------|
| `WordAppScreen.kt` | 顶层 NavHost：`list` / `detail/{word}` / `study` / `wrongbook`；普通 NavHost（规避 API 36 SwipeDismissableNavHost 首屏黑屏缺陷） |
| `screens/WordListScreen.kt` | 词库列表：标题/总数、错题集/随机背诵入口、搜索框、词条行；TransformingLazyColumn + snap 表圈滚动 + 稳定 key；词库后台加载中显示页面骨架（入口禁用 + 词条区加载提示） |
| `screens/WordDetailScreen.kt` | 单词详情：英文/词性/释义/派生链 + 开始背诵入口 |
| `screens/StudyScreen.kt` | 翻卡背诵：3D 翻转动画、认识/不认识、表圈驱动、梗概覆盖层（表圈滚动）、结束统计 |
| `screens/WrongBookScreen.kt` | 错题集：错题回顾列表（表圈滚动）+ 开始复习/清空错题 + 空状态 |
| `screens/LoadingScreen.kt` / `ErrorScreen.kt` | 加载/错误态 |
| `components/DictionaryIcons.kt` | Canvas 矢量图标（返回/搜索/词库/翻面/认识/不认识/梗概） |
| `theme/Theme.kt` | Wear M3 主题：`dynamicColorScheme` 优先 + 品牌色兜底（原型配色） |

背诵屏交互（对齐 HTML 交互原型「背单词 MVP」）：
- 点击卡片正面 → 翻面显示释义
- 背面「认识/不认识」→ 标记并自动前进；「不认识」同步记入错题集
- 转表圈（`onRotaryScrollEvent`，需 `focusRequester` 持有焦点）→ 未翻面先翻面显示释义，已翻面再跳过（不记录）
- 「梗概」→ 记得/没记得清单（表圈滚动）+ 清空更改
- 全部答完 → 结束统计 + 再来一组

错题集交互：
- 列表页「错题集」入口 → 错题回顾列表（TransformingLazyColumn + snap 表圈滚动）
- 标记「不认识」自动写入 SharedPreferences 持久化；点击错题词条进入详情
- 「开始复习」→ 以当前错题为数据源进入背诵会话（`StudySource.WRONG_BOOK`），
  标记「认识」自动移出错题集；「清空错题」→ 清空持久化与内存错题集

## 状态流转

```
启动 → Success(空词库, isLoading=true)：主页面骨架立即渲染
  ├─ loadWords()（后台 IO）
  │    ├─ 成功 → Success(allWords, displayWords, isLoading=false)
  │    ├─ 失败 → Error(DATA_LOAD_FAILED) → 重试 → 回到主页面骨架再加载
  │    └─ 加载中 → 背诵/错题入口禁用，词条区域显示加载提示
  ├─ 搜索 → updateSearchQuery(防抖) → Success(displayWords 过滤)
  ├─ 点词条 → detail/{word}（详情）
  ├─ 错题集 → wrongbook（wrongIds 过滤 allWords）
  └─ 随机背诵 → startStudy(SHUFFLED) → study（StudyState 会话）

StudyState:
  未翻面（正面单词） → flipCard → 翻面（背面释义）
  翻面 → markCurrent(true/false) → 记录并前进（翻回正面）；false 同步写入错题集；
         错题复习（source=WRONG_BOOK）时 true 同步移出错题集
  未翻面 → skipNext（表圈第一次）→ 翻面；已翻面 → skipNext（表圈第二次）→ 前进不记录
  reviewed == words.size → 完成态 → resetStudy（再来一组）/ 返回
```

## 数据模型表

`WordBook`（words.json 顶层对象）

| 字段 | 类型 | 说明 |
|------|------|------|
| total | Int | 词条总数（1269） |
| generatedAt | String | 生成日期 |
| source | String | 源文件名 |
| words | List\<WordEntry\> | 词条列表 |

`WordEntry`

| 字段 | 类型 | 说明 |
|------|------|------|
| word | String | 英文（单词或短语，词条 id） |
| pos | String | 词性标注（可为空） |
| meaning | String | 中文释义（①②③ 原样保留） |
| wordForms | List\<String\> | 词性变化/派生词链（可为空） |
| raw | String | 源文件原始行（容错保留） |

`StudyState`

| 字段 | 类型 | 说明 |
|------|------|------|
| words | List\<WordEntry\> | 会话词序 |
| index | Int | 当前词索引 |
| flipped | Boolean | 是否翻面 |
| known / unknown | Set\<String\> | 已标记词条 id 集合 |
| order | StudyOrder | SEQUENTIAL / SHUFFLED |

## 性能优化指南

- 数据类一律 `@Immutable`；列表 item 提供稳定 `key`
- 单一 `StateFlow` 合并主状态；一次刷新一轮重组
- 图标用 Canvas 矢量绘制（`drawWithCache` 缓存），**禁止 emoji**
- 搜索防抖（200ms）+ 内存过滤，无 IO 阻塞
- 静态共享 `Json` 反序列化器；词库单例缓存
- 深色 `windowBackground` 防冷启动白屏；Manifest 声明主题
- 启动先渲染主页面骨架，词库在 IO 后台加载完成后填充词条列表（`Success.isLoading` 态），消除整屏加载等待

## 编码规范

- Kotlin/Compose：`@Immutable` 数据类、稳定 lambda（`remember` 包裹）、稳定 key
- 异常：数据层返回/抛出业务异常，UI 用 `WordUiState.ErrorType` 枚举，不直接暴露异常文本
- 命名：ViewModel 以 `ViewModel` 结尾、UI 状态以 `UiState` 结尾、屏幕级 Composable 以 `Screen` 结尾、
  Repository 接口直接命名/实现加前缀（`LocalDictionaryRepository`）
- 包名：`com.galaxy.worddictionary`

## 命名规范

| 类型 | 规则 | 示例 |
|------|------|------|
| ViewModel | 以 `ViewModel` 结尾 | `WordViewModel` |
| UI 状态 | 以 `UiState` 结尾 | `WordUiState` |
| 屏幕级 Composable | 以 `Screen` 结尾 | `WordListScreen` |
| Repository 接口 | 直接命名 | `DictionaryRepository` |
| Repository 实现 | 加前缀 | `LocalDictionaryRepository` |

## 依赖管理

- 统一在 `gradle/libs.versions.toml` 声明版本与依赖；升级依赖只改该文件
- 版本矩阵复用 D:\app 已验证组合：AGP 8.13.2 / Kotlin 2.2.21 / KSP 2.2.21-2.0.5 /
  Compose BOM 2025.09.00 / Wear Compose 1.6.2 / Navigation 2.9.0 / Hilt 2.57.2 /
  kotlinx-serialization 1.9.0
- 仓库：阿里云 Maven 镜像（settings.gradle.kts），官方源兜底

## 构建与发布

命令行构建（必须 JDK 17）：

```powershell
$env:JAVA_HOME = "D:\jdk17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:assembleDebug
```

- debug：不混淆，直接安装调试
- release：R8 混淆 + 资源压缩（`proguard-rules.pro` 保留序列化元数据与模型类）
- APK 产物：`app/build/outputs/apk/debug/app-debug.apk`

## 常见问题

| 问题 | 处理 |
|------|------|
| API 36 首屏黑屏 | 已使用普通 NavHost（见 WordAppScreen 注释），勿换回 SwipeDismissableNavHost |
| 词库不显示/加载失败 | 检查 `app/src/main/assets/words.json` 是否存在；重新执行 `python tools\parse_vocabulary.py` |
| 转表圈无效 | 背诵屏需先触摸/点击获取焦点（`focusRequester.requestFocus()` 在进入时已请求） |
| 依赖下载慢/失败 | 阿里云镜像已配置；检查网络后重试 |
