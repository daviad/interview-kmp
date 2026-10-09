package com.moviely.data

import com.moviely.common.data.Constants
import com.moviely.common.domain.repository.MovieRepository
import com.moviely.data.api.core.HttpClientFactory
import com.moviely.data.api.core.PlatformEngineFactory
import com.moviely.data.api.tmdb.TmdbClient
import com.moviely.data.repository.MovieRepositoryImpl
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * shared 层 Koin Module：提供跨平台共享的 DI 绑定
 *
 * 装配链路：
 *   ① PlatformEngineFactory → create() → HttpClientEngine
 *   ② HttpClientFactory.create(json, engine) → HttpClient（含 ContentNegotiation + HttpTimeout + Logging）
 *   ③ TmdbClient(httpClient, baseUrl, apiKey, defaultLanguage)
 *   ④ MovieRepositoryImpl(tmdbClient, favoriteDao) bind MovieRepository
 *
 * ⚠️ tmdb_api_key 在 shared 中为占位值 "placeholder-need-replace"，
 *    由 androidApp 的 AppKoinModules.kt 用 Koin override 替换为 BuildConfig.TMDB_API_KEY
 *
 * 🍎 UIKit 类比：Koin module ≈ ServiceLocator / DI Container 的注册块，
 *              在 App 启动时一次性注册所有依赖
 */
val sharedModule = module {

    // ── JSON 配置单例 ──
    single { HttpClientFactory.createJson() }

    // ── HTTP 引擎工厂 ──
    single { PlatformEngineFactory() }

    // ── HttpClient 单例（引擎由 PlatformEngineFactory 按平台提供） ──
    single { HttpClientFactory.create(get(), get<PlatformEngineFactory>().create()) }

    // ── TMDB 配置常量（用 Koin qualifier 区分） ──
    single(named("tmdb_base_url")) { Constants.TMDB_BASE_URL }
    single(named("tmdb_api_key")) { "placeholder-need-replace" }  // ← androidApp override
    single(named("tmdb_language")) { Constants.DEFAULT_LANGUAGE }

    // ── TmdbClient 单例 ──
    single {
        TmdbClient(
            httpClient = get(),
            baseUrl = get(named("tmdb_base_url")),
            apiKey = get(named("tmdb_api_key")),
            defaultLanguage = get(named("tmdb_language"))
        )
    }

    // ── Repository 绑定 ──
    // singleOf(::MovieRepositoryImpl) bind MovieRepository::class
    // 注意：FavoriteDao 需要平台 DB Builder（Android 需 Context），由 androidApp 层 override 提供
    single<MovieRepository> {
        MovieRepositoryImpl(
            tmdbClient = get(),
            favoriteDao = get()  // ← 由 androidApp 层提供 FavoriteDao（需要 Context 创建 DB）
        )
    }
}
