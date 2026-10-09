# app 模块（android/app/，Gradle 模块名 :android:app）- AI 导航文档

> **模块定位**：薄壳 App。仅负责 Koin 装配 + NavHost 导航 + 启动。
> **不能写任何业务代码**——业务逻辑必须进 `:shared` 或 `:android:feature-home`。

---

## 1. 模块信息

- **类型**：Android Application
- **applicationId**：`com.moviely.kmp.app`
- **依赖**：`project(":shared")` + 4 个 feature 模块（home / favorite / search / detail）

## 2. 源文件索引

| 文件 | 职责 |
|---|---|
| `src/main/java/com/moviely/kmp/app/MovielyApp.kt` | Application 类：`startKoin { modules(sharedModule, homeKoinModule, appOverrideModule) }` |
| `src/main/java/com/moviely/kmp/app/MainActivity.kt` | 唯一 Activity：`setContent { MovielyTheme { MovielyNavHost() } }` |
| `src/main/java/com/moviely/kmp/app/MovielyNavHost.kt` | NavHost 路由表：home / search / favorites / detail/{movieId}，feature 跳转在此接线 |
| `src/main/res/values/themes.xml` | Material3 主题（MovielyTheme） |
| `src/main/res/values/strings.xml` | app_name 等字符串资源 |
| src/main/AndroidManifest.xml | 声明 Application + MainActivity + INTERNET 权限 |

> ⚠️ 本模块无 di/AppKoinModules.kt（历史文档残留）；壳层 Koin override 全部在 MovielyApp.kt 的 appOverrideModule 中。

## 3. Koin 装配链路

```
MovielyApp.kt
  └→ startKoin {
        androidContext(this@MovielyApp)
        modules(
          sharedModule,       // from :shared — TmdbClient / HttpClient / Repository
          homeKoinModule,     // from :android:feature-home — HomeApiClient / HomeViewModel
          appOverrideModule   // from :android:app — TMDB_API_KEY override + DB Builder (needs Context)
                              // 定义于 MovielyApp.kt 文件底部（private val）
        )
      }
```

## 4. TMDB API Key 数据链路

```
local.properties
  → android/app/build.gradle.kts: val tmdbApiKey = properties["TMDB_API_KEY"] ?: ""
  → buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
  → BuildConfig.TMDB_API_KEY
  → MovielyApp.kt (appOverrideModule): single(named("tmdb_api_key")) { BuildConfig.TMDB_API_KEY }
  → Koin override 替换 shared 中的占位值
  → TmdbClient 构造参数
```

## 5. 路由注册机制

当前简化版（单路由）：
```
NavHost(navController, startDestination = "home") {
    composable("home") { HomeScreen(onMovieClick = { /* TODO */ }) }
}
```

后续扩展方式：
```kotlin
// 加 feature-detail 后
composable("detail/{movieId}") { backStackEntry ->
    val movieId = backStackEntry.arguments?.getString("movieId") ?: return@composable
    DetailScreen(movieId)
}
```

## 6. 常见任务速查

| 任务 | 文件 |
|---|---|
| 改主题/颜色 | `res/values/themes.xml` |
| 加壳级权限 | `AndroidManifest.xml` |
| 加新路由 | `MovielyNavHost.kt` |
| 改 API Key 注入 | `MovielyApp.kt`（appOverrideModule） |
| 改 applicationId | `build.gradle.kts` → defaultConfig.applicationId |
| 加 Koin override | `MovielyApp.kt`（appOverrideModule） |
| 加新 feature 依赖 | `build.gradle.kts` + `MovielyApp.kt` 的 modules |

## 7. 硬约束

| # | 约束 |
|---|---|
| 1 | 壳永不写业务代码（业务逻辑必须进 shared 或 feature 模块） |
| 2 | API Key 不入源码，走 local.properties → BuildConfig → Koin |
| 3 | 不直接依赖 feature-home 的内部类（只通过 Koin 注入 + Compose 调用 HomeScreen） |
| 4 | 不使用 @AndroidEntryPoint / @HiltAndroidApp（Koin 不需要 Activity 注解） |
