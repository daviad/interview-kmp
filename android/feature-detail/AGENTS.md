# feature-detail 模块 - AI 导航文档

> **模块定位**：电影详情页（纯 Android UI）。数据全部来自 `:shared` 的
> `MovieRepository`（`getMovieDetail` suspend + `isFavorite` Flow + `add/removeFavorite`）。
> Gradle 模块名：`:android:feature-detail`

---

## 1. 模块信息

- **类型**：Android Library
- **namespace**：`com.moviely.kmp.feature.detail`（源码包：`com.moviely.feature.detail`）
- **依赖**：`project(":shared")`（MovieRepository / MovieDetail / Cast / Movie）
- **路由**：`detail/{movieId}`（movieId 为 Int 路径参数，由壳 NavHost 解析）

## 2. 源文件索引

| 文件 | 职责 |
|---|---|
| `src/main/.../DetailUiState.kt` | DetailUiState（Loading/Success(movie,isFavorite)/Error）+ DetailIntent.ToggleFavorite |
| `src/main/.../DetailViewModel.kt` | ① suspend 拉详情快照；② 订阅 `isFavorite(id): Flow` 驱动❤状态；③ ToggleFavorite 写 Room（Flow 自动回推）。movieId 由 Koin 构造参数注入 |
| `src/main/.../DetailScreen.kt` | 背景大图 + 海报信息 + 收藏按钮 + 简介 + 演员 LazyRow + 相似电影 LazyRow（点击 push 新详情页） |
| `src/main/.../di/DetailKoinModule.kt` | `detailKoinModule`：`viewModel { params -> DetailViewModel(get(), params.get()) }`（运行时参数 movieId） |

## 3. 关键机制：ViewModel 运行时参数（本模块特有）

```
壳 NavHost: composable("detail/{movieId}") { backStackEntry ->
    val movieId = backStackEntry.arguments?.getInt("movieId")
    DetailScreen(movieId, ...)
}
  → DetailScreen: koinViewModel(parameters = { parametersOf(movieId) })
  → DetailKoinModule: viewModel { params -> DetailViewModel(get(), params.get()) }
  → DetailViewModel(repository, movieId)
```

对比其他 feature：home/favorite/search 的 ViewModel 无参用 `viewModelOf(::XxxViewModel)`；
detail 需要路由参数，改用 `viewModel { params -> ... }` + `parametersOf(...)`。
相似电影点击 push 新 `detail/{id}` 时，导航栈新建条目 → 新的 ViewModel 实例。

## 4. 收藏联动链路（suspend + Flow 混合示范）

```
点❤ → DetailIntent.ToggleFavorite
  → 当前未收藏：repository.addFavorite(MovieDetail→Movie 转换)
    当前已收藏：repository.removeFavorite(id)
  → Room favorites 表变化
  → repository.isFavorite(id) Flow emit 新值
  → DetailViewModel state Success.copy(isFavorite = ...)
  → 顶栏❤图标 + 正文按钮文案自动切换（无需手动改 state / 无需重新拉详情）
  → 收藏列表页（feature-favorite）同时自动增减
```

## 5. 硬约束

| # | 约束 |
|---|---|
| 1 | 禁用 Hilt 注解，DI 用 Koin DSL |
| 2 | 不直接碰 FavoriteDao/TmdbClient，数据一律走 MovieRepository 接口 |
| 3 | 不依赖壳 `:android:app`，也不依赖其他 feature（相似电影跳转变为 onMovieClick 回调交壳接线） |
| 4 | 收藏写操作后不手动改 state，统一由 isFavorite Flow 回推（单一数据源） |
| 5 | movieId 只从 Koin parametersOf 注入，Screen 不自行解析路由 Bundle |
