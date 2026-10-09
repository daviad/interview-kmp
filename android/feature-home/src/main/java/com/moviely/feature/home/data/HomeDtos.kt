package com.moviely.feature.home.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Home 模块自带的网络 DTO（feature 专属，不归 shared）
 *
 * DTO = 贴着 TMDB JSON 蛇形字段，只给网络层用
 * Domain Model = common-domain 的 Movie（驼峰），全 App 通用
 *
 * 所有字段加默认值 + HttpClientFactory 的 coerceInputValues=true:
 * JSON 缺字段 / null → 用默认值兜底
 */

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

@Serializable
data class MovieResponseDto(
    @SerialName("page") val page: Int = 1,
    @SerialName("results") val results: List<MovieDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
    @SerialName("total_results") val totalResults: Int = 0
)
