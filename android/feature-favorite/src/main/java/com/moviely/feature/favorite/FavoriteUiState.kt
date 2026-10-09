package com.moviely.feature.favorite

import com.moviely.common.domain.model.Movie

/**
 * MVI 三要素 —— 收藏模块
 *
 * 与 feature-home 的差异：收藏数据来自 Room（本地 DB），
 * 通过 MovieRepository.getFavoriteMovies(): Flow 订阅，
 * DB 一变 Flow 自动推新值，所以没有「刷新」意图——状态永远是最新的。
 *
 * 🍎 SwiftUI 类比：@FetchRequest / @Query（SwiftData）自动驱动列表刷新
 */

// ── UI 状态（持久快照） ──
sealed interface FavoriteUiState {
    /** 首次订阅 Flow、还没收到第一帧数据 */
    data object Loading : FavoriteUiState

    /**
     * @param movies 收藏列表（Room Flow 最新值；空列表 = 空收藏夹）
     */
    data class Success(val movies: List<Movie>) : FavoriteUiState

    /** Room 查询异常（极少发生，兜底显示） */
    data class Error(val message: String) : FavoriteUiState
}

// ── 意图（用户操作） ──
sealed interface FavoriteIntent {
    /** 点击删除按钮：取消收藏（Room 删除后 Flow 自动推送新列表，UI 自动少一行） */
    data class RemoveFavorite(val movieId: Int) : FavoriteIntent
}
