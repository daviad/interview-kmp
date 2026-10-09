# OkHttp 引擎执行流程（expect/actual + Koin 懒加载）

> **适用范围**：`shared/src/androidMain/kotlin/com/moviely/data/api/core/PlatformEngine.kt#L21`
> **结论先行**：`OkHttp.create()` **不是 App 一启动就执行的**。Koin 启动时只"登记配方"，直到首页 Compose 第一次通过 `koinViewModel()` 索取 `HomeViewModel` 时，Koin 沿依赖链逐级解析，才最终调到这行代码。而"为什么走到 androidMain 而不是 iosMain"是**编译期**由 expect/actual 绑定决定的，运行时没有任何平台判断。

---

## 1. 全景时序

```
【编译期】编译 Android target
  commonMain 的 expect class PlatformEngineFactory
        └── 绑定 ──→ androidMain actual（OkHttp.create()）   ← iosMain 的 Darwin 版本不参与本次编译

【运行时】App 进程启动
 ① MovielyApp.onCreate()
    └─ startKoin { modules(sharedModule, ...) }
       ⚠️ 只登记 single{...} 的 lambda，一行都不执行 → OkHttp 还没创建

 ② MainActivity.setContent → MovielyNavHost(startDestination="home") → HomeScreen
    └─ HomeScreen 第 62 行: val viewModel = koinViewModel<HomeViewModel>()   ← 触发点！

 ③ Koin 递归解析构造参数（依赖图自顶向下）：
    HomeViewModel(homeApi)                                    ← 需要 HomeApi
      └─ factory<HomeApi> { HomeApiClient(get()) }            ← 需要 TmdbClient
           └─ single { TmdbClient(httpClient = get(), ...) }  ← 需要 HttpClient
                └─ single {
                     HttpClientFactory.create(
                       get(),
                       get<PlatformEngineFactory>().create()  ← 🔵 执行到这里
                     )
                   }
                   │
                   ▼
                   actual fun create() = OkHttp.create()      ★ PlatformEngine.kt 第 21 行

 ④ 引擎就位后逐级返回：HttpClient(engine){安装插件} → TmdbClient → HomeApiClient → HomeViewModel
 ⑤ HomeViewModel 构造完成 → init { refresh() } → 首页发第一个 GET 请求时，才真正"使用"这个引擎
```

---

## 2. 分阶段详解

### 阶段 0｜编译期：expect/actual 怎么选中这行代码

`shared/src/commonMain/.../api/core/PlatformEngine.kt` 只有一句声明：

```kotlin
expect class PlatformEngineFactory() {
    fun create(): HttpClientEngine
}
```

它是"需求声明"，不含实现。

- 编译 **Android target** 时，Kotlin 编译器把 common 代码里所有对 `PlatformEngineFactory.create()` 的调用，直接解析到 `androidMain` 源集里的 actual 实现（`OkHttp.create()`）；
- 编译 **iOS framework** 时，则解析到 `iosMain` 的 Darwin 版本（底层 NSURLSession）。

所以打出来的 APK 里根本不存在 Darwin 那份代码，运行时就是一次**普通的方法调用**，没有反射、没有 `if (isAndroid)`。

> 🍎 iOS 类比：这比 Swift protocol 更"硬"——更接近 `#if os(iOS)` / `#if os(android)` 的**编译期条件编译**：每个平台产物里只保留自己那份实现。UIKit 里可类比为不同 target 分别编译 `URLSessionEngine.swift` 和 `OkHttpEngine.kt`，通用代码只面向协议（expect 声明）编程。

**为什么不直接在 commonMain 里写 `HttpClient(OkHttp)`？**
因为 OkHttp / Darwin 都是平台专属 API，commonMain 不可直接 import。用 expect/actual 隔离平台差异，让 `TmdbClient` / `HttpClientFactory` 不关心引擎是什么。

### 阶段 1｜App 启动：只注册，不创建

系统创建 Application，回调 `android/app/.../MovielyApp.kt`：

```kotlin
class MovielyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MovielyApp)
            modules(sharedModule, homeKoinModule, favoriteKoinModule,
                    searchKoinModule, detailKoinModule, appOverrideModule)
        }
    }
}
```

此时 `sharedModule`（`shared/src/commonMain/.../data/SharedKoinModule.kt`）里的 `single { ... }` **只是把 lambda 存进 Koin 的定义表**（BeanDefinition），lambda 体不执行。

> 🍎 类比（UIKit）：`AppDelegate.application(_:didFinishLaunchingWithOptions:)` 里向 DI 容器注册了一堆 provider closure，但此时没有一个 closure 被调用。SwiftUI 则类似在 `App.init()` 里配置容器。

### 阶段 2｜首页取 ViewModel：依赖链被"点燃"

`MainActivity` 的 `setContent` 渲染 `MovielyNavHost`，起始路由 `home` 组合出 `HomeScreen`，在 `HomeScreen.kt` 第 62 行：

```kotlin
viewModel: HomeViewModel = koinViewModel()
```

**这是整个链条的第一个触发动作。** Koin 发现 `HomeViewModel` 构造函数需要 `HomeApi`，于是自顶向下递归 resolve：

1. `HomeKoinModule`：`factory<HomeApi> { HomeApiClient(get()) }` → 需要 `TmdbClient`
2. `sharedModule`：`single { TmdbClient(httpClient = get(), ...) }` → 需要 `HttpClient`
3. `sharedModule` 中 HttpClient 的 single：

```kotlin
single { PlatformEngineFactory() }                                        // 引擎工厂
single { HttpClientFactory.create(get(), get<PlatformEngineFactory>().create()) }
```

   - `get<PlatformEngineFactory>()` 先实例化工厂类——在 Android 设备上，这个类的实际字节码就是 androidMain 的 `actual class`；
   - 紧接着 `.create()` → **执行 `OkHttp.create()`**，产出 Ktor 的 OkHttp 引擎（底层 new 出一个 `okhttp3.OkHttpClient`，自动读取系统代理，调试时可走 Charles/NanoProxy）。

### 阶段 3｜引擎被包装、缓存、再被使用

- 引擎传入 `HttpClientFactory.create(json, engine)`，包成装好四大插件的 `HttpClient`：
  - `HttpTimeout`（15s 连接 / 30s 读 / 60s 整请求）
  - `ContentNegotiation`（JSON ↔ DTO）
  - `Logging`（请求/响应日志）
  - `CurlLogging`（发送前打印可复制的 curl 命令）
- 然后沿链条返回，完成 `TmdbClient → HomeApiClient → HomeViewModel` 的构造；
- 注意时序：`HomeViewModel` 的 `init { refresh() }` 在构造完成**之后**才执行，协程里首个 `homeApi.getPopularMovies()` 发出 GET 请求时，才真正通过这个 OkHttp 引擎建立 TCP/TLS 连接。

---

## 3. 三个容易混淆的机制

| 机制 | 说明 |
|---|---|
| **懒加载** | `single{}` / `factory{}` 的 lambda 在首次 `get()` 时才执行。若 App 启动后从不进首页，`OkHttp.create()` 永远不会跑 |
| **只执行一次** | `single` 实例会被 Koin 缓存，`OkHttp.create()` 整个 App 生命周期**仅执行一次**；而 `factory<HomeApi>` 每次 resolve 都新建，但其中 `get()` 到的 `TmdbClient` / `HttpClient` 仍是同一个单例 |
| **创建 ≠ 发请求** | `OkHttp.create()` 只是"造引擎"（类似 `URLSession(configuration:)`）；真正发网络请求是在 ViewModel `init` 触发的首个挂起 GET 中 |

---

## 4. 关键文件索引

| 文件 | 角色 |
|---|---|
| `shared/src/commonMain/.../data/api/core/PlatformEngine.kt` | `expect` 声明：平台 HTTP 引擎工厂 |
| `shared/src/androidMain/.../data/api/core/PlatformEngine.kt` | `actual`：`OkHttp.create()`（本文主角） |
| `shared/src/iosMain/.../data/api/core/PlatformEngine.kt` | `actual`：Darwin 引擎（仅占位，无 Mac 不编译） |
| `shared/src/commonMain/.../data/api/core/HttpClientFactory.kt` | 用引擎装配 HttpClient + 四大插件 |
| `shared/src/commonMain/.../data/SharedKoinModule.kt` | Koin 注册：工厂 → HttpClient → TmdbClient → Repository |
| `android/app/.../MovielyApp.kt` | `startKoin { modules(...) }`（只注册不实例化） |
| `android/app/.../MovielyNavHost.kt` | startDestination = "home"，决定首页先被组合 |
| `android/feature-home/.../HomeScreen.kt` | `koinViewModel<HomeViewModel>()`（依赖链触发点） |
| `android/feature-home/.../di/HomeKoinModule.kt` | HomeApi / HomeViewModel 的 Koin 绑定 |
| `android/feature-home/.../HomeViewModel.kt` | `init { refresh() }` 发出首个网络请求 |

---

## 5. iOS 心智模型总结

- `startKoin { modules(...) }` = 在 AppDelegate 里注册一批 **lazy** 的 provider closure；
- `koinViewModel()` = 第一次 `resolver.resolve(HomeViewModel.self)`，容器顺着构造器逐层 resolve；
- 最终调用的 `OkHttpEngine()` 是 **Android target 编译期选定**的 actual 实现（等价于条件编译），拿到后缓存为全局单例，供后续所有 TMDB 请求复用。
