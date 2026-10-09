---
description: MovielyKmp 项目铁律（每次会话必读）
globs:
alwaysApply: true
---

# MovielyKmp 项目铁律（每次会话必读）

## 必读文档（按需 Read，不要全读）
- **完整架构地图**：根目录 `AGENTS.md`（含模块清单/依赖图/源文件索引/任务速查表）
- **shared 模块**：`shared/AGENTS.md`
- **feature 模块（home / favorite / search / detail）**：`android/feature-home/AGENTS.md`、`android/feature-favorite/AGENTS.md`、`android/feature-search/AGENTS.md`、`android/feature-detail/AGENTS.md`
- **app 壳模块**：`android/app/AGENTS.md`
- **iOS 模块**：根 `AGENTS.md` 第 3 节「iOS 模块」索引（`ios/` 目录，非 Gradle 模块）

## 硬约束（修改代码前必查 AGENTS.md 第 5 节）
- Koin + KSP（不用 Hilt，KMP 不支持）
- Room 2.8.4 / OkHttp 4.12.0 锁定 / Ktor OkHttp engine（Android）/ Darwin engine（iOS）
- Ktor 3.0.3 锁定（3.1+ 客户端插件钩子 API 变化：AfterRender/AfterEncoding 为 3.1+ 才有，3.0.x 只有 SetupRequest/SendingRequest/Send）
- Gradle 9.5.0 + Java 25 (JBR) + Kotlin 2.0.20
- `ksp.incremental=false` / `kotlin.compiler.execution.strategy=in-process`
- `group = "com.moviely.kmp"`（与原工程 com.moviely 隔离）
- 壳工程永不写业务代码（只做装配 + 导航 + 启动）
- TMDB API Key 走 local.properties → BuildConfig → Koin `named("tmdb_api_key")`（v3 key→query 参数 / v4 token→Bearer 头，TmdbClient 按前缀 eyJ 自动识别）
- shared 的 data-api 不含业务端点（业务接口/DTO 归各 feature 自定义）
- 绝对不修改 c:\Users\dxw\Documents\dev\Learn\component-moviely\ 下任何文件
- **UI 路线已定（路线 A 平台原生）**：跨平台仅 shared/（数据层）；Android UI（Compose）一律放 android/ 目录，Gradle 模块名 :android:xxx；iOS UI（SwiftUI）放 ios/ 目录（预留）
- **文档与代码强制同步**：改包名/路径/文件归属（新增/删除/移动源文件）时，必须同步更新对应 AGENTS.md + 本文件「关键路径速查」
- **AI 发现文档与代码不符时**：先列出差异清单请用户审批，批准后统一修正文档，不静默修改

## 交互规范（节省 token）
1. 接到任务先读 `AGENTS.md` 第 6 节速查表定位文件
2. **不要 Glob/Grep 整个 codebase**，按速查表 Read 具体文件
3. 改代码前在 `AGENTS.md` 第 5 节查硬约束
4. 用户给文件路径就直接 Read，不要重复搜索
5. 简单任务无需读 AGENTS.md 全部，按需读相关章节

## 关键路径速查
- 壳入口：`android/app/src/main/java/com/moviely/kmp/app/{MainActivity,MovielyApp,MovielyNavHost}.kt`（appOverrideModule 定义在 MovielyApp.kt 内，无独立 di/ 文件）
- Android feature：`android/feature-home/`、`android/feature-favorite/`、`android/feature-search/`、`android/feature-detail/`（Gradle 模块名 :android:feature-xxx；新 feature 照此新增并同步 settings.gradle / app 依赖 / Koin / NavHost）
- iOS feature：`ios/feature-{home,search,favorite,detail}/`（非 Gradle 模块，Swift 文件，需 Mac + Xcode 编译）
- iOS app 壳：`ios/app/{MovielyApp,ContentView,Secrets,Components}.swift`
- 共享层：`shared/src/commonMain/kotlin/com/moviely/{common,data}/`
- iOS 桥接层：`shared/src/iosMain/kotlin/com/moviely/data/{IosKoin,FlowWrapper}.kt`
- 网络层：`shared/src/commonMain/kotlin/com/moviely/data/api/{core,tmdb}/`
- 数据库：`shared/src/commonMain/kotlin/com/moviely/data/db/`
- 构建配置：`settings.gradle.kts` / `build.gradle.kts` / `gradle.properties` / `gradle/libs.versions.toml`
