# feature-home 模块（android/feature-home/，Gradle 模块名 :android:feature-home）- AI 导航文档

> **模块定位**：首页 MVI 功能模块。纯 Android Library，依赖 `:shared` 的 KMP 产物。
> 从原 `c:\Users\dxw\Documents\dev\Learn\feature-home\` 迁移而来，DI 从 Hilt 切换为 Koin。

---

## 1. 模块信息

- **类型**：Android Library（暂仅 library 模式，TODO：后续加 standalone 双模式开关）
- **namespace**：`com.moviely.kmp.feature.home`
- **依赖**：`project(":shared")`

## 2. 源文件索引

| 文件 | 职责 |
|---|---|
| `src/main/.../HomeViewModel.kt` | MVI ViewModel：StateFlow<HomeUiState> + Channel<HomeEffect>，去掉 @HiltViewModel，改为普通 constructor |
| `src/main/.../HomeUiState.kt` | UI 状态 sealed class（Loading / Success / Error），纯 Kotlin |
| `src/main/.../HomeScreen.kt` | Compose 首页屏幕：用 `koinViewModel()` 替代 `hiltViewModel()` |
| `src/main/.../data/HomeApi.kt` | 首页业务端点接口（getPopularMovies / getNowPlaying 等） |
| `src/main/.../data/HomeDtos.kt` | 首页专属 DTO（@Serializable，不归 shared） |
| `src/main/.../data/HomeApiClient.kt` | HomeApi 实现，构造注入 TmdbClient |
| `src/main/.../di/HomeKoinModule.kt` | Koin module：`factoryOf(::HomeApiClient) bind HomeApi::class` + `viewModelOf(::HomeViewModel)` |

## 3. 对外暴露服务（Koin）

`HomeKoinModule.kt` 中的 `homeKoinModule`：

| 服务 | 绑定 | Scope |
|---|---|---|
| `HomeApiClient` | bind `HomeApi::class` | factory |
| `HomeViewModel` | — | viewModel（Koin 管理 ViewModel 生命周期） |

## 4. 依赖关系

- **依赖 :shared 的服务**：`TmdbClient`（通过 HomeApiClient 构造注入）
- **对外暴露**：`homeKoinModule`（由 android/app 壳在 startKoin 中加载）
- **不依赖**：`:android:app`（壳不可被 feature 依赖）

## 5. MVI 数据流

```
HomeScreen
  │ koinViewModel<HomeViewModel>()
  ▼
HomeViewModel
  │ state: StateFlow<HomeUiState>     ← UI 订阅
  │ effect: Channel<HomeEffect>       ← 一次性事件
  │ sendIntent(HomeIntent)
  ▼
HomeApiClient (implements HomeApi)
  │ tmdbClient.get<HomeResponse>("movie/popular")
  ▼
TmdbClient (from shared)
  │ HttpClient GET → JSON → kotlinx.serialization
  ▼
TMDB API
```

## 6. 常见任务速查

| 任务 | 文件 |
|---|---|
| 加首页端点（如 getTopRated） | `data/HomeApi.kt` 加接口 → `HomeApiClient.kt` 实现 |
| 加首页 DTO 字段 | `data/HomeDtos.kt` |
| 改首页 UI 布局 | `HomeScreen.kt` |
| 改首页 MVI 状态/事件 | `HomeUiState.kt` + `HomeViewModel.kt` |
| 加 Koin 绑定 | `di/HomeKoinModule.kt` |

## 7. 硬约束

| # | 约束 |
|---|---|
| 1 | 禁用所有 Hilt 注解（@HiltViewModel / @Inject 等），DI 用 Koin DSL |
| 2 | HomeApi / HomeDtos 归 feature 自管，不放入 shared |
| 3 | 不依赖 `:android:app`（壳不可被 feature 依赖） |
| 4 | HomeViewModel 的 MVI 逻辑保持与原工程同构（StateFlow + Channel） |
