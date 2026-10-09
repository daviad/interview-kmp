package com.moviely.common.domain.model

/**
 * 🎯 Domain 层的核心实体：电影（列表用）
 *
 * 为什么用 data class？
 *   data class 自动生成：toString() / equals() / hashCode() / copy() / componentN()
 *   这些方法在 UI diff（LazyColumn 对比两个 Movie 是否相同）、调试打印时非常有用
 *   🍎 UIKit 类比：相当于一个实现了 isEqual:、description、copyWithZone: 的 NSObject 模型
 *              或者 Swift 的 struct（Swift 里 struct 默认 value semantics，和 data class 类似）
 *   🍎 SwiftUI 类比：Swift 的 struct Model: Identifiable（Movie.id 对应 Identifiable 的 id）
 *
 * 所有字段都是 val（不可变）：
 *   Domain 实体必须是不可变的，线程安全，也符合 MVI 的单向数据流思想
 *   如果要改变，应该用 copy() 生成新对象（类似 Swift struct 的 mutating 或 SwiftData 的新实例）
 *
 * TMDB 字段对应关系：
 *   id           电影 ID（主键）
 *   title        电影名
 *   overview     简介 / 剧情梗概
 *   posterPath   海报相对路径（如 "/abc.jpg"），注意 TMDB API 只给相对路径，
 *                全 URL 需要拼接 https://image.tmdb.org/t/p/{宽度}{相对路径}
 *   backdropPath 背景图相对路径
 *   voteAverage  评分（0-10 分）
 *   releaseDate  上映日期（格式 "YYYY-MM-DD"）
 *   genreIds     类型 ID 列表（如 [28, 12] = 动作 + 冒险），类型字典由 TMDB 提供
 */
data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,      // ? = 可空，TMDB 有些电影没有海报
    val backdropPath: String?,    // 可空
    val voteAverage: Double,
    val releaseDate: String,
    val genreIds: List<Int>       // 不可变 List，类似 NSArray / Swift [Int]
) {
    /**
     * 计算属性：完整海报 URL
     * - posterPath 为 null 时用 let 安全调用，整个表达式返回 null，然后 Elvis ?: "" 给空字符串
     * - w500 = 宽度 500px，TMDB 支持 w92/w154/w185/w342/w500/w780/original 等尺寸
     * 🍎 UIKit 类比：@property (nonatomic, copy, readonly) NSURL *posterURL; 重写 getter
     */
    val posterUrl: String
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""

    /** 计算属性：完整背景图 URL（w780，比海报更宽） */
    val backdropUrl: String
        get() = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" } ?: ""

    /**
     * 格式化评分：Double 8.456 → String "8.5"（保留 1 位小数）
     * %.1f 格式符 = 浮点数，1 位小数
     * 🍎 UIKit 类比：[NSString stringWithFormat:@"%.1f", 8.456]
     */
    val formattedVote: String
        get() = "%.1f".format(voteAverage)
}
