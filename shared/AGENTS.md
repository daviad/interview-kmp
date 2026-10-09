# shared 模块 - AI 导航文档

> **模块定位**：KMP 共享中枢。三源集结构（commonMain / androidMain / iosMain）。
> domain + data 全部在此模块，占全工程 75% 代码。feature 模块和壳通过 `project(":shared")` 依赖。

---

## 1. 源集结构

```
shared/src/
├── commonMain/          ← 跨平台共享（Ktor / Room / Coroutines / Serialization，纯 Kotlin）
│   └── kotlin/com/moviely/
│       ├── common/data/Constants.kt
│       ├── common/domain/
│       │   ├── model/{Movie, MovieDetail, Cast}.kt
│       │   ├── Result.kt
│       │   └── repository/MovieRepository.kt
│       └── data/
│           ├── api/core/
│           │   ├── PlatformEngine.kt          (expect)
│           │   └── HttpClientFactory.kt
│           ├── api/tmdb/
│           │   ├── TmdbClient.kt
│           │   └── TmdbDtos.kt
│           ├── db/
│           │   ├── FavoriteEntity.kt
│           │   ├── FavoriteDao.kt
│           │   └── MovielyDatabase.kt         (expect Builder)
│           ├── repository/MovieRepositoryImpl.kt
│           └── SharedKoinModule.kt
│
├── androidMain/         ← Android actual 实现
│   └── kotlin/com/moviely/data/
│       ├── api/core/PlatformEngine.kt         (actual: OkHttp)
│       └── db/MovielyDatabase.kt              (actual: Room.databaseBuilder)
│
└── iosMain/             ← iOS actual（仅占位，无 Mac 不验证编译）
    └── kotlin/com/moviely/data/
        ├── api/core/PlatformEngine.kt         (actual: Darwin)
        └── db/MovielyDatabase.kt              (actual: Room iOS builder)
```

## 2. expect/actual 对应关系表

| expect（commonMain） | actual androidMain | actual iosMain | 说明 |
|---|---|---|---|
| `PlatformEngineFactory.create()` | OkHttp 引擎（支持系统代理） | Darwin 引擎（底层 NSURLSession） | Ktor HTTP 引擎按平台选择 |
| `MovielyDatabaseBuilder.build()` | `Room.databaseBuilder(context, ...)` | `Room.databaseBuilder<...>()`（iOS native SQLite） | Room 数据库实例化需平台 API |

## 3. Koin module 暴露服务清单

`SharedKoinModule.kt` 中的 `sharedModule`：

| 服务 | Qualifier | Scope | 说明 |
|---|---|---|---|
| `PlatformEngineFactory` | — | single | HTTP 引擎工厂 |
| `HttpClient` | — | single | Ktor 客户端（含 ContentNegotiation + HttpTimeout + Logging） |
| `String` (apiKey) | `named("tmdb_api_key")` | single | ⚠️ shared 中为占位值，由 androidApp override；v3 key 或 v4 token 均可（TmdbClient 自动识别认证方式） |
| `String` (baseUrl) | `named("tmdb_base_url")` | single | Constants.TMDB_BASE_URL |
| `TmdbClient` | — | single | 通用 TMDB 客户端 |
| `MovieRepositoryImpl` bind `MovieRepository` | — | single | Repository 实现 |

## 4. 依赖关系

- **对外依赖**：kotlinx-coroutines-core / ktor-client-core / ktor-content-negotiation / ktor-serialization-json / room-runtime / kotlinx-serialization-json / koin-core
- **androidMain 额外**：ktor-client-okhttp / okhttp / okhttp-logging
- **iosMain 额外**：ktor-client-darwin
- **对内**：self-contained，不依赖 feature 或 androidApp

## 5. 常见任务速查

| 任务 | 文件 |
|---|---|
| 改 TMDB 常量 | `commonMain/.../common/data/Constants.kt` |
| 加领域模型字段 | `commonMain/.../common/domain/model/Movie.kt`（或 MovieDetail/Cast） |
| 加 TMDB 通用端点（如配置/语言列表） | `commonMain/.../data/api/tmdb/TmdbClient.kt` 加方法 |
| 改网络配置（超时/拦截器/日志级别） | `commonMain/.../data/api/core/HttpClientFactory.kt` |
| 加 Room 表 | `commonMain/.../data/db/` 新增 Entity + Dao + 在 MovielyDatabase 注册 |
| 加 Koin 服务 | `commonMain/.../data/SharedKoinModule.kt` |
| 改 Android 网络引擎 | `androidMain/.../data/api/core/PlatformEngine.kt` |
| 改 iOS 网络引擎 | `iosMain/.../data/api/core/PlatformEngine.kt` |

## 6. 硬约束

| # | 约束 |
|---|---|
| 1 | KSP 必须配置 `kspAndroid`（`kspCommonMainMetadata` 在 KMP 模块不生效，实际用 `kspAndroid` 生成 `_Impl`） |
| 2 | Room schema 目录：`shared/schemas/` |
| 3 | Ktor 超时锁定 15s/30s/60s，装在 HttpClient 层（不是 Engine 层） |
| 4 | commonMain 不可 import 任何 `android.*` 或 `androidx.*`（Room/Koin 除外，它们 KMP 兼容） |
| 5 | TmdbClient 是业务无关的通用客户端，不加 feature 专属端点 |
| 6 | iosMain 代码仅占位，不验证编译（当前无 Mac 环境） |
