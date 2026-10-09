package com.moviely.feature.home.data

/**
 * Home 模块的业务端点接口（feature 自管，不归 shared）
 *
 * 基础层 shared 只提供通用 TmdbClient（怎么发请求），
 * 「请求什么端点」由各 feature 自己定义。
 *
 * 🍎 UIKit 类比：protocol HomeServiceProtocol { func popular() async throws -> MovieResponseDto }
 */
interface HomeApi {
    /** GET /movie/popular?page={page} */
    suspend fun getPopularMovies(page: Int = 1): MovieResponseDto

    /** GET /movie/now_playing?page={page} */
    suspend fun getNowPlayingMovies(page: Int = 1): MovieResponseDto
}
