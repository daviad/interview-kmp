package com.moviely.kmp.app

import android.app.Application
import com.moviely.data.sharedModule
import com.moviely.feature.detail.di.detailKoinModule
import com.moviely.feature.favorite.di.favoriteKoinModule
import com.moviely.feature.home.di.homeKoinModule
import com.moviely.feature.search.di.searchKoinModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * MovielyApp - 薄壳 App 入口（Koin 装配）
 *
 * 替代原工程 Hilt 的 @HiltAndroidApp + Application()。
 * Koin 用 startKoin { } 在 App 启动时一次性注册所有依赖。
 *
 * 装配链路：
 *   startKoin {
 *     modules(
 *       sharedModule,      ← shared 层（TmdbClient / HttpClient / Repository）
 *       homeKoinModule,    ← feature-home（HomeApiClient / HomeViewModel）
 *       favoriteKoinModule,  ← feature-favorite（FavoriteViewModel）
 *       searchKoinModule,    ← feature-search（SearchViewModel）
 *       detailKoinModule,    ← feature-detail（DetailViewModel，movieId 走 Koin parametersOf）
 *       appOverrideModule    ← app 壳 override（TMDB_API_KEY / FavoriteDao）
 *     )
 *   }
 *
 * 🍎 UIKit 类比：
 *   func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: ...)
 *     → ServiceLocator.shared.registerAll()
 */

/**
  *
 * Koin 默认不带运行时反射自发现。当前 onCreate() 中 modules(...) 是手动列出所有
 * feature 的 Koin module——新增 feature 时必须回来加一行（加 feature 改动点之一）。
 *
 * 三种"自发现"路线对比：
 *
 * ① KSP + koin-annotations（官方推荐，编译期生成）
 *    - 给依赖打 @Single / @KoinViewModel，给模块打 @Module
 *    - KSP 编译期扫描所有 @Module，生成聚合索引 defaultModule()
 *    - 壳层改成 modules(defaultModule()) 即可，新增 feature 不用再改本行
 *    - 启动性能最优（无运行时反射），但需要重写所有 module，学习曲线高
 *    - 当前未引入 koin-annotations 依赖（libs.versions.toml 只有 koin-core/android/compose）
 *
 * ② Classpath 反射扫描（运行时遍历 dex 找 Module 子类）
 *    - 启动慢，不推荐
 *
 * ③ 手动列出（当前模式）
 *    - 启动最快，modules 一眼看完，调试可见性最佳
 *    - 代价：加 feature 改 2 处（feature 自身 + 本文件 modules(...) 这行）
 *
 * 🍎 UIKit 类比：
 *    - 手动模式 = AppDelegate.didFinishLaunching 里手写多行 [ServiceLocator register:]
 *    - KSP 模式 = 给每个类打 @Register，编译期生成注册表
 *      （类似 SwiftUI @main：编译器自动收集所有 @main 入口，不用手动列）
 *
 * 切换时机建议：
 *    - feature 数量 > 8、或多人协作频繁忘记改 modules(...) 时，再切换 KSP 模式
 *    - 现状（4 个 feature）手动维护成本极低，保持手动更利于学习理解和调试
 */
class MovielyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MovielyApp)
            modules(sharedModule, homeKoinModule, favoriteKoinModule, searchKoinModule, detailKoinModule, appOverrideModule)
        }
    }
}

/**
 * androidApp 层 Koin override module
 *
 * ① 覆盖 sharedModule 中的占位 tmdb_api_key 为 BuildConfig.TMDB_API_KEY
 * ② 提供 FavoriteDao（Android 端需要 Context 创建 Room DB）
 */
private val appOverrideModule = module {
    // override: 用真实 API Key 替换 shared 中的占位值
    single(named("tmdb_api_key")) { BuildConfig.TMDB_API_KEY }

    // Android 端 FavoriteDao：需要 Context 创建 Room DB
    single {
        com.moviely.data.db.MovielyDatabaseBuilder()
            .create(androidContext())
            .favoriteDao()
    }
}
