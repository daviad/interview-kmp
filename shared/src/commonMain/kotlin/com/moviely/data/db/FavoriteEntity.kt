package com.moviely.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room 实体：收藏表 favorites（KMP commonMain，跨平台共享）
 *
 * Room 2.8+ 的 @Entity / @PrimaryKey 在 commonMain 可用，
 * KSP 在 commonMain 上跑注解处理，生成的 DAO 实现跨平台共享。
 *
 * 字段设计：
 *   id          电影 ID（主键，去重）
 *   title       电影名（收藏列表显示）
 *   posterPath  海报相对路径
 *   voteAverage 评分
 *   releaseDate 上映日期
 *   overview    简介
 *   addedAt     加入时间戳（默认当前时间，用于排序）
 *
 * 🍎 UIKit 类比：Core Data 的 Entity attributes 定义 / SwiftData 的 @Model class
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val posterPath: String?,
    val voteAverage: Double,
    val releaseDate: String,
    val overview: String,
    val addedAt: Long
)
