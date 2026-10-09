package com.moviely.data.repository

import com.moviely.common.domain.Result
import com.moviely.common.domain.model.Cast
import com.moviely.common.domain.model.Movie
import com.moviely.common.domain.model.MovieDetail
import com.moviely.common.domain.repository.MovieRepository
import com.moviely.data.api.tmdb.CastDto
import com.moviely.data.api.tmdb.MovieDetailDto
import com.moviely.data.api.tmdb.MovieDto
import com.moviely.data.api.tmdb.MovieResponseDto
import com.moviely.data.api.tmdb.TmdbClient
import com.moviely.data.db.FavoriteDao
import com.moviely.data.db.FavoriteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * MovieRepository 的具体实现（KMP commonMain，跨平台共享）
 *
 * 与原 component-moviely 的差异：
 *   ① 去掉 @Singleton @Inject constructor，改为普通构造（Koin 负责注入和单例）
 *   ② 去掉 TmdbApi（原工程过渡期接口），直接注入 TmdbClient
 *   ③ 去掉 javax.inject / java.io.IOException（KMP commonMain 不可用），
 *      用 kotlin.coroutines.cancellation.CancellationException 替代
 *   ④ Dispatchers.IO 在 KMP commonMain 中可用（kotlinx.coroutines 1.8+ 多平台）
 *
 * Repository 核心职责：
 *   - 聚合 TmdbClient（网络）+ FavoriteDao（本地 Room）
 *   - 实现 domain 层 MovieRepository 接口
 *   - 跨来源数据拼装（如详情 = 网络 + 本地收藏状态）
 *   - DTO ↔ Domain 映射
 *
 * 🍎 UIKit 类比：Service 层的 Fetcher class conforms to Protocol，
 *              DI 容器（Koin）注册时把实现绑到协议上
 */
class MovieRepositoryImpl(
    private val tmdbClient: TmdbClient,
    private val favoriteDao: FavoriteDao
) : MovieRepository {

    // ────────────────── 一次性网络请求 ──────────────────

    /** 热门电影列表（分页） */
    override suspend fun getPopularMovies(page: Int): Result<List<Movie>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response: MovieResponseDto = tmdbClient.get("/movie/popular", mapOf("page" to page.toString()))
            response.results.map { it.toMovie() }
        }
    }

    /** 正在上映列表（分页） */
    override suspend fun getNowPlayingMovies(page: Int): Result<List<Movie>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response: MovieResponseDto = tmdbClient.get("/movie/now_playing", mapOf("page" to page.toString()))
            response.results.map { it.toMovie() }
        }
    }

    /**
     * 跨来源数据拼装：电影详情
     *   ① 先查 Room DB 拿收藏状态
     *   ② 再查 TmdbClient 拿详情
     *   ③ Mapper 把 isFavorite 组装到 MovieDetail
     */
    override suspend fun getMovieDetail(id: Int): Result<MovieDetail> = withContext(Dispatchers.IO) {
        safeApiCall {
            val isFav = favoriteDao.isFavorite(id).first()
            val detail: MovieDetailDto = tmdbClient.get("/movie/$id", mapOf("append_to_response" to "credits,similar"))
            detail.toMovieDetail(isFav)
        }
    }

    /** 关键词搜索（分页） */
    override suspend fun searchMovies(query: String, page: Int): Result<List<Movie>> = withContext(Dispatchers.IO) {
        safeApiCall {
            val response: MovieResponseDto = tmdbClient.get("/search/movie", mapOf("query" to query, "page" to page.toString()))
            response.results.map { it.toMovie() }
        }
    }

    // ────────────────── 本地收藏 Flow ──────────────────

    override fun getFavoriteMovies(): Flow<List<Movie>> {
        return favoriteDao.getFavoriteMovies().map { entities ->
            entities.map { it.toMovie() }
        }
    }

    override fun isFavorite(id: Int): Flow<Boolean> = favoriteDao.isFavorite(id)

    // ────────────────── 写操作 ──────────────────

    override suspend fun addFavorite(movie: Movie): Result<Unit> = try {
        favoriteDao.insertFavorite(movie.toFavoriteEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e, "收藏失败")
    }

    override suspend fun removeFavorite(id: Int): Result<Unit> = try {
        favoriteDao.deleteFavorite(id)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e, "取消收藏失败")
    }

    // ────────────────── 异常统一捕获 ──────────────────

    /**
     * 统一异常捕获：不依赖 Ktor 类型，通过异常类名前缀识别错误类型
     *
     * commonMain 不能 import java.io.IOException，
     * 用 CancellationException 单独处理（协程取消不应当作错误），
     * 其他所有 Exception 兜底。
     */
    private suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> {
        return try {
            Result.Success(block())
        } catch (e: CancellationException) {
            throw e  // 协程取消不应当作错误，继续抛出
        } catch (e: Exception) {
            val className = e::class.simpleName ?: ""
            val msg = when {
                className.contains("Connect", ignoreCase = true) ||
                className.contains("Timeout", ignoreCase = true) ||
                className.contains("Unresolved", ignoreCase = true) -> "网络连接失败，请检查网络或代理设置"
                className.contains("Serialization", ignoreCase = true) ||
                className.contains("Json", ignoreCase = true) -> "数据解析失败：${e.message ?: "API 返回格式异常"}"
                else -> "未知错误：${e.message ?: className}"
            }
            Result.Error(e, msg)
        }
    }

    // ────────────────── Mapper（DTO → Domain、Entity ↔ Domain） ──────────────────

    /** MovieDto → Movie */
    private fun MovieDto.toMovie(): Movie = Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        genreIds = genreIds
    )

    /** MovieDetailDto → MovieDetail */
    private fun MovieDetailDto.toMovieDetail(isFavorite: Boolean): MovieDetail = MovieDetail(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        runtime = runtime,
        genres = genres.map { it.name },
        cast = credits?.cast?.map { it.toCast() } ?: emptyList(),
        similarMovies = similar?.results?.map { it.toMovie() } ?: emptyList(),
        isFavorite = isFavorite
    )

    /** CastDto → Cast */
    private fun CastDto.toCast(): Cast = Cast(
        id = id,
        name = name,
        character = character,
        profilePath = profilePath,
        order = order
    )

    /** FavoriteEntity → Movie（收藏列表显示用） */
    private fun FavoriteEntity.toMovie(): Movie = Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = null,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        genreIds = emptyList()
    )

    /** Movie → FavoriteEntity（写入收藏表） */
    private fun Movie.toFavoriteEntity(): FavoriteEntity = FavoriteEntity(
        id = id,
        title = title,
        posterPath = posterPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        overview = overview,
        addedAt = 0L  // addedAt 由 DB 写入时设置（Android 端用 System.currentTimeMillis()，iOS 用 NSDate）
    )
}
