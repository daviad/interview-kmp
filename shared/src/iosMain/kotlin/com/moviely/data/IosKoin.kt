package com.moviely.data

import com.moviely.common.domain.repository.MovieRepository
import com.moviely.data.db.FavoriteDao
import com.moviely.data.db.MovielyDatabase
import com.moviely.data.db.MovielyDatabaseBuilder
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

/**
 * iOS Koin 启动器 —— 对标 Android 端 MovielyApp.kt 里的 startKoin { }
 *
 * 差异：
 *   Android：appOverrideModule 提供 TMDB_API_KEY（来自 BuildConfig）+ MovielyDatabase（需 Context）
 *   iOS：    本文件提供 TMDB_API_KEY（从 Swift 传入）+ MovielyDatabase（无需 Context，Room iOS 直接建）
 *
 * Swift 调用方式：
 *   IosKoinKt.startKoinIos(apiKey: "eyJhbGci...")
 *
 * 启动后，Swift 侧通过 IosApp.repository() 拿到 MovieRepository 实例
 */
fun startKoinIos(apiKey: String) {
    startKoin {
        modules(
            sharedModule,
            module {
                // Override：替换 sharedModule 里的 placeholder api key
                single(named("tmdb_api_key")) { apiKey }

                // iOS Room Builder：无需 Context（与 Android 的 actual 实现不同）
                single { MovielyDatabaseBuilder().create() }

                // FavoriteDao：从 DB 获取（Room KSP 生成）
                single<FavoriteDao> { get<MovielyDatabase>().favoriteDao() }
            }
        )
    }
}

/**
 * iOS 依赖访问器 —— Swift 侧通过这个 object 拿 shared 层的实例
 *
 * 用法（Swift）：
 *   let repo = IosApp.shared.repository()
 *   let wrapper = IosApp.shared.wrapFlow(flow: repo.getFavoriteMovies())
 */
object IosApp {
    /** 从 Koin 容器取 MovieRepository（KMP 接口，Swift 侧可直接调用） */
    fun repository(): MovieRepository = KoinPlatform.getKoin().get()
}
