package com.moviely.common.domain.repository

import com.moviely.common.domain.Result
import com.moviely.common.domain.model.Movie
import com.moviely.common.domain.model.MovieDetail
import kotlinx.coroutines.flow.Flow

/**
 * 🎯 Clean Architecture：Repository 接口（定义在 Domain 层）
 *
 * 【核心原则】依赖反转（DIP）：
 *   Domain 层只定义「我需要什么」（接口），不关心「怎么实现」
 *   Data 层负责实现这个接口（MovieRepositoryImpl）
 *   依赖方向：data → domain（Domain 不依赖 Data，Data 依赖 Domain）
 *   🍎 UIKit 类比：Protocol-Oriented Programming 的 Protocol，放 Service/UseCase 层
 *              DataSource/Fetcher 类 conforms to 这个 Protocol
 *
 * 【两种异步 API 的选择标准】
 *   ① suspend fun：「一次性请求」→ 发请求、等结果、返回一次
 *                   对应 UIKit 的 sendAsynchronousRequest completionHandler
 *                   例：getPopularMovies（请求一次，返回这一页的数据就结束）
 *
 *   ② fun xxx(): Flow<T>：「持续订阅」→ 数据源变化时自动推送新值
 *                         对应 UIKit 的 Combine Publisher / RxSwift Observable / KVO
 *                         例：getFavoriteMovies（收藏夹随时可能变化，UI 要实时刷新）
 *
 * 为什么收藏用 Flow 而不是 suspend？
 *   「详情页点收藏按钮 → 收藏列表页自动多出一部电影」
 *   用 Flow 可以让 Room DB 变化自动驱动 UI，不需要手动发通知 / 手动刷新
 *   🍎 UIKit 类比：NSFetchedResultsController + delegate 自动监听 Core Data 变化
 *   🍎 SwiftUI 类比：@FetchRequest（SwiftData / CoreData 的自动监听）
 */
interface MovieRepository {
    // ────────────────── 一次性请求（suspend，请求完就结束） ──────────────────
    /**
     * 热门电影列表（分页）
     * @param page 页码，从 1 开始（TMDB 规则），用于 Paging 3 分页加载
     * @return Result<List<Movie>> 成功=电影列表 / 失败=异常 / 加载中状态由调用方管理
     */
    suspend fun getPopularMovies(page: Int): Result<List<Movie>>

    /** 正在上映电影列表（分页） */
    suspend fun getNowPlayingMovies(page: Int): Result<List<Movie>>

    /**
     * 电影详情
     * @param id 电影 ID，返回的 MovieDetail 里已经组装好了 cast/similarMovies/isFavorite
     */
    suspend fun getMovieDetail(id: Int): Result<MovieDetail>

    /** 关键词搜索（分页） */
    suspend fun searchMovies(query: String, page: Int): Result<List<Movie>>

    // ────────────────── 流式订阅（Flow，持续监听数据变化） ──────────────────
    /**
     * 订阅收藏列表（冷流）
     * 只要 Room 里 FavoriteEntity 有增删，Flow 就会 emit 新的 List<Movie>
     * 🍎 UIKit 类比：NSFetchedResultsController.controllerDidChangeContent
     */
    fun getFavoriteMovies(): Flow<List<Movie>>

    /** 订阅某部电影是否已收藏（详情页❤按钮状态的实时联动） */
    fun isFavorite(id: Int): Flow<Boolean>

    // ────────────────── 写操作（suspend） ──────────────────
    /** 加入收藏：写入 Room DB（因为 Flow 监听，UI 会自动刷新） */
    suspend fun addFavorite(movie: Movie): Result<Unit>

    /** 取消收藏：从 Room DB 删除 */
    suspend fun removeFavorite(id: Int): Result<Unit>
}
