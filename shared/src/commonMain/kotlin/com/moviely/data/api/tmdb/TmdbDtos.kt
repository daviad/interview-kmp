package com.moviely.data.api.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * TMDB 接口的 DTO（Data Transfer Object）集合：网络层返回结构的直接映射
 *
 * DTO vs Domain Model:
 *   DTO  = 贴着 TMDB JSON 蛇形字段（poster_path），只给 data 层用
 *   Domain Model = 经 Mapper 净化后的纯业务实体（驼峰 posterPath），全 App 通用
 *
 * kotlinx.serialization 注解:
 *   @Serializable       = 标记可序列化 (= Swift Codable)
 *   @SerialName("xxx")  = JSON 蛇形字段映射到 Kotlin 驼峰属性 (= Swift CodingKeys)
 *
 * 所有字段加默认值 + HttpClientFactory 的 coerceInputValues=true:
 *   JSON 缺字段 / null → 用默认值兜底，避免 MissingFieldException
 */

/** 单部电影信息 DTO（对应 results 数组的每个元素） */
@Serializable
data class MovieDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("title") val title: String = "",
    @SerialName("overview") val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("release_date") val releaseDate: String = "",
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList()
)

/** 分页响应 DTO（热门 / 正在上映 接口的返回结构） */
@Serializable
data class MovieResponseDto(
    @SerialName("page") val page: Int = 1,
    @SerialName("results") val results: List<MovieDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0
)

/** 搜索响应 DTO */
@Serializable
data class SearchResponseDto(
    @SerialName("page") val page: Int = 1,
    @SerialName("results") val results: List<MovieDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0
)

/** 电影详情 DTO + 嵌套子结构（来自 append_to_response=credits,similar） */
@Serializable
data class MovieDetailDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("title") val title: String = "",
    @SerialName("overview") val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("release_date") val releaseDate: String = "",
    @SerialName("runtime") val runtime: Int? = null,
    @SerialName("genres") val genres: List<GenreDto> = emptyList(),
    @SerialName("credits") val credits: CreditsDto? = null,
    @SerialName("similar") val similar: MovieResponseDto? = null
)

/** 类型 DTO（{ id: 28, name: "动作" }） */
@Serializable
data class GenreDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = ""
)

/** 演职员 DTO（只解析演员 cast） */
@Serializable
data class CreditsDto(
    @SerialName("cast") val cast: List<CastDto> = emptyList()
)

/** 单个演员 DTO（order 越小越靠前） */
@Serializable
data class CastDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("character") val character: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("order") val order: Int = 0
)
