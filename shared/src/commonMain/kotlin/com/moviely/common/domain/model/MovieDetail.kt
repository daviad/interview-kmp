package com.moviely.common.domain.model

/**
 * 🎯 Domain 层实体：电影详情（详情页用）
 *
 * 为什么不直接复用 Movie 列表实体？
 *   列表页和详情页的数据粒度不同：
 *   - Movie 只存「列表需要的最小字段」，避免请求过大内存浪费
 *   - MovieDetail 额外包含：时长、类型字符串、演员表、相似电影、收藏状态
 *   🍎 UIKit 类比：不同场景使用不同的 Model，而不是一个 50 个字段的 MegaModel
 *
 * 新增字段说明：
 *   runtime         时长，单位「分钟」，可空（有些 TMDB 数据没有录时长）
 *   genres          类型字符串列表（如 ["科幻", "冒险"]），不是 ID 了，Mapper 已转好
 *   cast            演员列表（List<Cast>），详情页要显示演员头像和姓名
 *   similarMovies   相似推荐电影列表（又是 Movie 类型，递归复用），「你可能还喜欢」区块
 *   isFavorite      是否已被收藏（true/false），由 Repository 层在组装时查 Room DB 注入
 */
data class MovieDetail(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val releaseDate: String,
    val runtime: Int?,
    val genres: List<String>,
    val cast: List<Cast>,
    val similarMovies: List<Movie>,
    val isFavorite: Boolean
) {
    /** 海报 URL（与 Movie 里的同名计算属性一致） */
    val posterUrl: String
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""

    /** 背景图 URL */
    val backdropUrl: String
        get() = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" } ?: ""

    /** 格式化评分 8.45 → "8.5" */
    val formattedVote: String
        get() = "%.1f".format(voteAverage)

    /**
     * 格式化时长：分钟 → "Xh Ym"
     * 例：148 分钟 → "2h 28m"（it/60=2 小时，it%60=28 分钟）
     * 🍎 UIKit 类比：NSDateComponentsFormatter 也能做，不过这里手写更轻量
     */
    val formattedRuntime: String
        get() = runtime?.let { "${it / 60}h ${it % 60}m" } ?: ""

    /**
     * 上映年份：日期字符串 "2024-07-15" → "2024"（取前 4 个字符）
     * take(4) = Swift 的 .prefix(4)，安全，短于 4 个字符时不会越界
     */
    val year: String
        get() = releaseDate.take(4)

    /**
     * 类型列表拼接成显示用字符串：["科幻", "冒险", "剧情"] → "科幻 · 冒险 · 剧情"
     * joinToString(" · ") = Swift 的 .joined(separator: " · ")
     */
    val genresText: String
        get() = genres.joinToString(" · ")
}
