# MovielyKmp - AI 导航文档

> **工程定位**：从 component-moviely（纯 Android + Hilt）迁移而来的 KMP 独立工程。
> 零侵入原工程——不修改 `c:\Users\dxw\Documents\dev\Learn\component-moviely\` 下任何文件。
> DI 从 Hilt 切换为 Koin，数据层（domain + data）KMP 化，UI 保留 Jetpack Compose。

---

## 1. 模块清单

| 模块 | 类型 | 职责 | AGENTS.md |
|---|---|---|---|
| `:shared` | KMP (Android Lib + iOS) | 跨平台共享层：领域模型 / Repository 接口与实现 / Ktor 网络层 / Room 数据库 | [shared/AGENTS.md](shared/AGENTS.md) |
| `:android:feature-home`<br>（android/feature-home/） | Android Library | 首页 MVI（ViewModel + Compose Screen + HomeApi 端点） | [android/feature-home/AGENTS.md](android/feature-home/AGENTS.md) |
| `:android:feature-favorite`<br>（android/feature-favorite/） | Android Library | 收藏列表 MVI（订阅 MovieRepository 的 Room Flow） | [android/feature-favorite/AGENTS.md](android/feature-favorite/AGENTS.md) |
| `:android:feature-search`<br>（android/feature-search/） | Android Library | 搜索 MVI（MovieRepository.searchMovies suspend） | [android/feature-search/AGENTS.md](android/feature-search/AGENTS.md) |
| `:android:feature-detail`<br>（android/feature-detail/） | Android Library | 电影详情 MVI（getMovieDetail suspend + isFavorite Flow 收藏联动） | [android/feature-detail/AGENTS.md](android/feature-detail/AGENTS.md) |
| `:android:app`<br>（android/app/） | Android Application | 薄壳 App：Koin 装配 + NavHost 导航 + 启动 | [android/app/AGENTS.md](android/app/AGENTS.md) |

## 2. KMP 源集依赖图

```
                    android/app (:android:app，纯 Android 壳)
                    ├── implementation(project(":shared"))
                    ├── implementation(project(":android:feature-home"))
                    └── Koin startKoin { modules(sharedModule, homeModule, appOverride) }
                            │
                    android/feature-home | feature-favorite | feature-search（纯 Android UI Library）
                    ├── implementation(project(":shared"))
                    └── home 走自管 HomeApiClient（TmdbClient）；favorite/search 走 MovieRepository（domain 接口）
                            │
                    shared (KMP: commonMain + androidMain + iosMain)
                    ├── commonMain  ← 75% 代码（domain + data-api + data-db + data-repository + Koin module）
                    ├── androidMain ← OkHttp Engine / Room Android Builder
                    └── iosMain     ← Darwin Engine / Room iOS Builder（仅占位，无 Mac 不编译）
```

## 3. 关键源文件索引

### shared 模块（KMP 共享层）

| 文件 | 职责 |
|---|---|
| `shared/src/commonMain/.../common/data/Constants.kt` | TMDB 基础常量（BASE_URL / IMAGE_BASE_URL / DEFAULT_LANGUAGE） |
| `shared/src/commonMain/.../common/domain/model/Movie.kt` | 领域模型：电影列表项 |
| `shared/src/commonMain/.../common/domain/model/MovieDetail.kt` | 领域模型：电影详情 |
| `shared/src/commonMain/.../common/domain/model/Cast.kt` | 领域模型：演员 |
| `shared/src/commonMain/.../common/domain/Result.kt` | 统一结果封装（Success / Error / Loading） |
| `shared/src/commonMain/.../common/domain/repository/MovieRepository.kt` | Repository 接口（纯 Kotlin interface） |
| `shared/src/commonMain/.../data/api/core/PlatformEngine.kt` | expect 声明：平台 HTTP 引擎工厂 |
| `shared/src/commonMain/.../data/api/core/HttpClientFactory.kt` | Ktor HttpClient 工厂（ContentNegotiation + HttpTimeout + Logging） |
| `shared/src/commonMain/.../data/api/tmdb/TmdbClient.kt` | 通用 TMDB 客户端（baseUrl + apiKey/v4token + reified GET；认证自动识别：JWT→Bearer 头，v3 key→query 参数） |
| `shared/src/commonMain/.../data/api/tmdb/TmdbDtos.kt` | TMDB 通用 DTO（@Serializable） |
| `shared/src/commonMain/.../data/db/FavoriteEntity.kt` | Room Entity：收藏电影 |
| `shared/src/commonMain/.../data/db/FavoriteDao.kt` | Room DAO：收藏 CRUD |
| `shared/src/commonMain/.../data/db/MovielyDatabase.kt` | Room Database 定义 + expect Builder |
| `shared/src/commonMain/.../data/repository/MovieRepositoryImpl.kt` | Repository 实现（DTO→Domain 映射） |
| `shared/src/commonMain/.../data/SharedKoinModule.kt` | shared 层 Koin module（TmdbClient / HttpClient / Repository 绑定） |
| `shared/src/androidMain/.../data/api/core/PlatformEngine.kt` | actual：OkHttp 引擎 |
| `shared/src/androidMain/.../data/db/MovielyDatabase.kt` | actual：Room Android Builder（需 Context） |
| `shared/src/iosMain/.../data/api/core/PlatformEngine.kt` | actual：Darwin 引擎（仅占位） |
| `shared/src/iosMain/.../data/db/MovielyDatabase.kt` | actual：Room iOS Builder（仅占位） |
| `shared/src/iosMain/.../data/IosKoin.kt` | iOS Koin 启动器（startKoinIos + IosApp 依赖访问器） |
| `shared/src/iosMain/.../data/FlowWrapper.kt` | Kotlin Flow → Swift 回调桥接（subscribe/cancel） |

### feature-home 模块

| 文件 | 职责 |
|---|---|
| `android/feature-home/src/main/.../HomeViewModel.kt` | MVI ViewModel（StateFlow + Channel，去掉 @HiltViewModel） |
| `android/feature-home/src/main/.../HomeUiState.kt` | UI 状态 sealed class（Loading / Success / Error） |
| `android/feature-home/src/main/.../HomeScreen.kt` | Compose 首页屏幕（koinViewModel() 替代 hiltViewModel()） |
| `android/feature-home/src/main/.../data/HomeApi.kt` | 首页业务端点接口 |
| `android/feature-home/src/main/.../data/HomeDtos.kt` | 首页专属 DTO（@Serializable） |
| `android/feature-home/src/main/.../data/HomeApiClient.kt` | HomeApi 实现（注入 TmdbClient） |
| `android/feature-home/src/main/.../di/HomeKoinModule.kt` | Koin module（HomeApiClient + HomeViewModel 绑定） |

### feature-favorite 模块

| 文件 | 职责 |
|---|---|
| `android/feature-favorite/src/main/.../FavoriteViewModel.kt` | 收藏 VM：订阅 MovieRepository.getFavoriteMovies(): Flow（Room 自动驱动）+ removeFavorite |
| `android/feature-favorite/src/main/.../FavoriteUiState.kt` | FavoriteUiState / FavoriteIntent（MVI） |
| `android/feature-favorite/src/main/.../FavoriteScreen.kt` | 收藏列表 Compose（TopAppBar 返回 + 空态引导 + 删除按钮） |
| `android/feature-favorite/src/main/.../di/FavoriteKoinModule.kt` | favoriteKoinModule（viewModelOf） |

### feature-search 模块

| 文件 | 职责 |
|---|---|
| `android/feature-search/src/main/.../SearchViewModel.kt` | 搜索 VM：QueryChanged 只更输入框，Search 调 MovieRepository.searchMovies() |
| `android/feature-search/src/main/.../SearchUiState.kt` | SearchUiState(Idle/Loading/Success/Error) / SearchIntent（MVI） |
| `android/feature-search/src/main/.../SearchScreen.kt` | 搜索页 Compose（OutlinedTextField + IME Search + 结果列表） |
| `android/feature-search/src/main/.../di/SearchKoinModule.kt` | searchKoinModule（viewModelOf） |

### feature-detail 模块

| 文件 | 职责 |
|---|---|
| `android/feature-detail/src/main/.../DetailViewModel.kt` | 详情 VM：suspend 拉详情 + 订阅 isFavorite Flow + ToggleFavorite 写 Room；movieId 由 Koin 参数注入 |
| `android/feature-detail/src/main/.../DetailUiState.kt` | DetailUiState / DetailIntent（MVI） |
| `android/feature-detail/src/main/.../DetailScreen.kt` | 详情页 Compose（背景大图/海报信息/收藏按钮/演员表/相似电影） |
| `android/feature-detail/src/main/.../di/DetailKoinModule.kt` | detailKoinModule（viewModel { params -> DetailViewModel(get(), params.get()) }） |

### app 壳模块

| 文件 | 职责 |
|---|---|
| `android/app/src/main/.../MovielyApp.kt` | Application（startKoin 装配所有 module）+ appOverrideModule（TMDB API Key override / FavoriteDao，定义于同文件底部） |
| `android/app/src/main/.../MainActivity.kt` | 唯一 Activity（setContent { MovielyTheme { MovielyNavHost() } }） |
| `android/app/src/main/.../MovielyNavHost.kt` | NavHost 路由表：home / search / favorites / detail/{movieId}，feature 跳转在此接线 |

### iOS 模块（ios/ 目录，SwiftUI + 非 Gradle 模块）

> 非 Gradle 模块——纯 Swift 文件，由 Xcode 工程引用 shared 框架。
> 当前无 Mac 环境，代码已写好但不编译。

| 文件 | 职责 |
|---|---|
| `ios/app/MovielyApp.swift` | @main App 入口（startKoinIos + TabView 导航）+ AppDependencies 访问器 |
| `ios/app/ContentView.swift` | TabView 三 Tab（Home / Search / Favorites），NavigationStack push Detail |
| `ios/app/Secrets.swift` | TMDB API Key 配置（对标 Android local.properties，⚠️ 应 gitignore） |
| `ios/app/Components.swift` | 共享 UI 组件（MoviePosterCard / MovieRowCard / ErrorBanner） |
| `ios/feature-home/HomeView.swift` | 首页 SwiftUI（@Observable ViewModel + ScrollView 横向+纵向混排） |
| `ios/feature-search/SearchView.swift` | 搜索页 SwiftUI（TextField + async/await suspend） |
| `ios/feature-favorite/FavoriteView.swift` | 收藏页 SwiftUI（FlowWrapper 订阅 Room Flow + List onDelete 删除） |
| `ios/feature-detail/DetailView.swift` | 详情页 SwiftUI（suspend 拉详情 + Flow 订阅收藏 + 演员表 + 相似电影） |

## 4. 核心机制速查

### 4.1 Koin DI 装配链路

```
android/app/.../MovielyApp.kt → startKoin {
    modules(
        sharedModule,       ← shared 层提供（TmdbClient / HttpClient / Repository）
        homeKoinModule,      ← feature-home（HomeApiClient / HomeViewModel）
        favoriteKoinModule,  ← feature-favorite（FavoriteViewModel）
        searchKoinModule,    ← feature-search（SearchViewModel）
        detailKoinModule,    ← feature-detail（DetailViewModel，movieId 走 Koin parametersOf）
        appOverrideModule    ← app 壳 override（TMDB_API_KEY / FavoriteDao），定义于 MovielyApp.kt 文件底部
    )
}
```

### 4.2 TMDB API Key 数据链路

```
local.properties (TMDB_API_KEY=xxx)
  → android/app/build.gradle.kts 读取 → buildConfigField
  → BuildConfig.TMDB_API_KEY
  → Koin single(named("tmdb_api_key")) { BuildConfig.TMDB_API_KEY }
  → TmdbClient 构造参数 apiKey
```

### 4.3 MVI 数据流

```
HomeScreen(koinViewModel<HomeViewModel>())
  → viewModel.sendIntent(HomeIntent.Refresh)
  → viewModel 收到 intent → 调用 HomeApi.getPopularMovies()
  → HomeApi → TmdbClient.get<HomeResponse>("movie/popular")
  → StateFlow<HomeUiState> 更新 → Compose 自动 recompose
  → Channel<HomeEffect> 发送一次性事件（如 Snackbar）
```

## 5. 硬约束

| # | 约束 | 原因 |
|---|---|---|
| 1 | **绝对不碰** `c:\Users\dxw\Documents\dev\Learn\component-moviely\` 下任何文件 | 零侵入原工程 |
| 2 | Gradle 9.5.0 + Kotlin 2.0.20 + AGP 8.5.2 | Java 25 兼容 |
| 3 | `ksp.incremental=false` / `kotlin.compiler.execution.strategy=in-process` | Windows KSP daemon 稳定性 |
| 4 | OkHttp 4.12.0 锁定（Android Ktor 引擎底层） | 避免 metadata mismatch |
| 5 | Room 2.8.4，KSP 跑 `kspAndroid`（`kspCommonMainMetadata` 在 KMP 不生效） | Room KMP 注解处理 |
| 6 | `group = "com.moviely.kmp"`（不是 `com.moviely`） | 避免与原工程坐标冲突 |
| 7 | 禁用所有 Hilt 注解，DI 全部用 Koin DSL | Hilt 不支持 KMP |
| 8 | TMDB API Key 不硬编码：local.properties → BuildConfig → Koin @Named | 安全 |
| 9 | Ktor 超时 15s/30s/60s，装在 HttpClient（commonMain）层 | 调试快速失败 |
| 10 | 壳工程永不写业务代码，只做装配 + 导航 + 启动 | 架构原则 |
| 11 | Ktor 3.0.3 锁定 | 3.1+ 客户端插件钩子 API 变化（AfterRender/AfterEncoding 为 3.1+ 才有） |
| 12 | 改包名/路径/文件归属时，必须同步更新对应 AGENTS.md + project-essentials.md 关键路径速查；发现文档与代码不符先列清单请审批 | 文档与代码同步 |
| 13 | UI 路线已定（路线 A 平台原生）：Android UI 进 android/，iOS UI（SwiftUI）进 ios/，跨平台仅 shared；新 Android feature 放 android/ 目录，模块名 :android:xxx | 目录与职责清晰，避免二次搬家 |

## 6. 常见任务速查表

| 任务关键词 | 去哪个文件 |
|---|---|
| 改 TMDB 常量（baseUrl / imageUrl / language） | `shared/.../common/data/Constants.kt` |
| 改领域模型（Movie / MovieDetail / Cast） | `shared/.../common/domain/model/` |
| 加 TMDB 通用端点 | `shared/.../data/api/tmdb/TmdbClient.kt` |
| 加首页业务端点 | `android/feature-home/.../data/HomeApi.kt` + `HomeApiClient.kt` |
| 改首页 UI | `android/feature-home/.../HomeScreen.kt` |
| 改首页 MVI 逻辑 | `android/feature-home/.../HomeViewModel.kt` + `HomeUiState.kt` |
| 改收藏功能 | `android/feature-favorite/.../`（FavoriteViewModel / FavoriteScreen / FavoriteUiState） |
| 改搜索功能 | `android/feature-search/.../`（SearchViewModel / SearchScreen / SearchUiState） |
| 改详情功能 | `android/feature-detail/.../`（DetailViewModel / DetailScreen / DetailUiState；收藏联动见 MovieRepository） |
| 加 Koin 绑定 | shared 层 → `SharedKoinModule.kt`；feature 层 → `HomeKoinModule.kt` |
| 改主题/颜色 | `android/app/src/main/res/values/themes.xml` |
| 加新路由 | `android/app/.../MovielyNavHost.kt` |
| 改 API Key 注入 | `android/app/.../MovielyApp.kt`（appOverrideModule） |
| 加 Room 表 | `shared/.../data/db/` + `SharedKoinModule.kt` 注册 DAO |
| 改 Ktor 超时/拦截器 | `shared/.../data/api/core/HttpClientFactory.kt` |
| 改网络引擎（Android/iOS） | 对应 `PlatformEngine.kt` actual 文件 |
| 改 iOS UI | `ios/feature-*/` 下对应 `.swift` 文件（需 Mac + Xcode） |
| 改 iOS Koin 启动 | `shared/src/iosMain/.../data/IosKoin.kt`（startKoinIos + IosApp） |
| 改 iOS Flow 桥接 | `shared/src/iosMain/.../data/FlowWrapper.kt`（subscribe/cancel） |
| 改 iOS API Key | `ios/app/Secrets.swift`（⚠️ 应 gitignore） |

## 7. 后续 feature 接入步骤

新增 feature（如 feature-detail）时：
1. 新建 `android/feature-detail/` 模块（Android Library，Gradle 模块名 `:android:feature-detail`），`implementation(project(":shared"))`
2. feature 内部写 DetailApi / DetailViewModel / DetailScreen + `DetailKoinModule.kt`
3. `android/app/build.gradle.kts` 加 `implementation(project(":android:feature-detail"))`
4. `android/app/.../MovielyApp.kt` 的 startKoin modules 加 `detailKoinModule`
5. `android/app/.../MovielyNavHost.kt` 加 `composable("detail") { DetailScreen() }`
