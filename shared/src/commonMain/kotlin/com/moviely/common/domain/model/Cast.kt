package com.moviely.common.domain.model

/**
 * 🎯 Domain 层实体：演员/演职员
 *
 * 字段说明（来自 TMDB /movie/{id}/credits API 的 cast 数组）：
 *   id          演员 ID（TMDB 分配）
 *   name        演员姓名
 *   character   扮演的角色名（如 "Iron Man / Tony Stark"）
 *   profilePath 头像相对路径（可空，有些演员没有照片）
 *   order       排序序号（越小越靠前，0 = 主演，1 = 第二主演…）
 *                 UI 渲染时用 list.sortedBy { it.order } 按顺序显示演员表
 */
data class Cast(
    val id: Int,
    val name: String,
    val character: String,
    val profilePath: String?,
    val order: Int
) {
    /**
     * 演员头像完整 URL：w185 = 185px 宽，TMDB 官方推荐的演员表头像尺寸
     * 🍎 UIKit 类比：重写 readonly property 的 getter 拼接 URL
     */
    val profileUrl: String
        get() = profilePath?.let { "https://image.tmdb.org/t/p/w185$it" } ?: ""
}
