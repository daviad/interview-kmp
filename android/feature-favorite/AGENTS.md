# feature-favorite 模块 - AI 导航文档

> **模块定位**：收藏列表页（纯 Android UI）。数据全部来自 `:shared` 的
> `MovieRepository`（domain 层接口），本模块**不写任何网络/DB 代码**。
> Gradle 模块名：`:android:feature-favorite`

---

## 1. 模块信息

- **类型**：Android Library
- **namespace**：`com.moviely.kmp.feature.favorite`（源码包：`com.moviely.feature.favorite`）
- **依赖**：`project(":shared")`（只用 MovieRepository / Movie，无需 serialization / ksp 插件）

## 2. 源文件索引

| 文件 | 职责 |
|---|---|
| `src/main/.../FavoriteUiState.kt` | MVI 三要素：FavoriteUiState（Loading/Success/Error）+ FavoriteIntent.RemoveFavorite |
| `src/main/.../FavoriteViewModel.kt` | init 订阅 `repository.getFavoriteMovies(): Flow`，Room 变化自动驱动 state；删除走 repository.removeFavorite |
| `src/main/.../FavoriteScreen.kt` | Scaffold + TopAppBar（返回键）+ LazyColumn 收藏列表 + 空收藏夹引导 + 删除按钮 |
| `src/main/.../di/FavoriteKoinModule.kt` | `favoriteKoinModule`：仅 `viewModelOf(::FavoriteViewModel)`（Repository 由 sharedModule 提供） |

## 3. 数据流（Flow 订阅型，对照 feature-home 的 suspend 请求型）

```
Room favorites 表变化
  → FavoriteDao.getFavoriteMovies(): Flow<List<FavoriteEntity>>
  → MovieRepositoryImpl.map { it.toMovie() }（shared 层）
  → FavoriteViewModel.state: StateFlow<FavoriteUiState.Success>
  → Compose 自动 recompose（无需手动刷新）
```

删除收藏：`FavoriteIntent.RemoveFavorite(id)` → `repository.removeFavorite(id)` →
Room 删除 → Flow 自动 emit 新列表 → 该行自动消失。

## 4. 硬约束

| # | 约束 |
|---|---|
| 1 | 禁用 Hilt 注解，DI 用 Koin DSL |
| 2 | 不直接碰 FavoriteDao/TmdbClient，数据一律走 MovieRepository 接口 |
| 3 | 不依赖壳 `:android:app`，也不依赖其他 feature |
| 4 | 导航不自行跳转：Screen 只暴露 onBack / onMovieClick 回调，由壳 NavHost 接线 |
