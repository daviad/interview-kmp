package com.moviely.feature.home.data

import com.moviely.data.api.tmdb.TmdbClient

/**
 * HomeApi 的实现：基于 shared 的通用 TmdbClient 拼接首页端点
 *
 * 本类只做「端点 + 业务参数」的描述（popular / now_playing + page），
 * 鉴权 / 语言 / URL 拼接 / JSON 反序列化全部由 TmdbClient 统一处理。
 *
 * 与原工程差异：去掉 @Inject constructor，改为普通构造（Koin 负责注入）
 *
 * 🍎 UIKit 类比：DI 容器注册 impl：
 *   container.register(HomeApi.self) { r in HomeApiClient(client: r.resolve(TmdbClient.self)!) }
 */
class HomeApiClient(
    private val tmdbClient: TmdbClient
) : HomeApi {

    override suspend fun getPopularMovies(page: Int): MovieResponseDto =
        tmdbClient.get("/movie/popular", mapOf("page" to page.toString()))

    override suspend fun getNowPlayingMovies(page: Int): MovieResponseDto =
        tmdbClient.get("/movie/now_playing", mapOf("page" to page.toString()))
}
