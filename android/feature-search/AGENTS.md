# feature-search 模块 - AI 导航文档

> **模块定位**：电影搜索页（纯 Android UI）。数据来自 `:shared` 的
> `MovieRepository.searchMovies(query, page)` suspend 接口，
> 本模块**不写端点/DTO**（TMDB `search/movie` 已由 shared 封装为 domain 结果）。
> Gradle 模块名：`:android:feature-search`

---

## 1. 模块信息

- **类型**：Android Library
- **namespace**：`com.moviely.kmp.feature.search`（源码包：`com.moviely.feature.search`）
- **依赖**：`project(":shared")`（MovieRepository / Movie / Result）

## 2. 源文件索引

| 文件 | 职责 |
|---|---|
| `src/main/.../SearchUiState.kt` | MVI 三要素：SearchUiState（Idle/Loading/Success/Error，均带 query）+ SearchIntent（QueryChanged / Search） |
| `src/main/.../SearchViewModel.kt` | QueryChanged 只更新输入框；Search 调 `repository.searchMovies()`（page=1），Result.Success/Error 更新 state |
| `src/main/.../SearchScreen.kt` | TopAppBar（返回）+ OutlinedTextField（IME Search 键触发）+ 结果 LazyColumn + Idle/空结果/Error 四种态 |
| `src/main/.../di/SearchKoinModule.kt` | `searchKoinModule`：仅 `viewModelOf(::SearchViewModel)` |

## 3. 数据流（用户触发型 suspend，对照 feature-favorite 的 Flow 订阅型）

```
用户输入（QueryChanged，仅更新 state.query，不发请求）
  → 点软键盘 Search 键 / 搜索图标（SearchIntent.Search）
  → ViewModel: repository.searchMovies(query, 1)   // suspend
  → Result.Success(list) → SearchUiState.Success（空 list 显示「没有找到」）
  → Result.Error(e, msg) → SearchUiState.Error（保留 query 便于重试）
```

## 4. 硬约束

| # | 约束 |
|---|---|
| 1 | 禁用 Hilt 注解，DI 用 Koin DSL |
| 2 | 不在 feature 内重复定义 search 端点/DTO（shared MovieRepository 已提供） |
| 3 | 不依赖壳 `:android:app`，也不依赖其他 feature |
| 4 | 导航只暴露 onBack / onMovieClick 回调，由壳 NavHost 接线 |
| 5 | 打字过程不发请求（仅 Search 意图触发），避免无意义网络调用 |
