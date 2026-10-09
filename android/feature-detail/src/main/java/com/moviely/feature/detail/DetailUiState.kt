package com.moviely.feature.detail

import com.moviely.common.domain.model.MovieDetail

/**
 * MVI 三要素 —— 电影详情模块
 *
 * 状态结构与其他 feature 的差异：
 *   Success 同时持有【详情快照】和【实时收藏状态】两个字段。
 *   详情（演职表、简介等）是 suspend 一次性请求的快照；
 *   收藏状态是 Room Flow 持续推送的——点❤后无需重新请求详情，
 *   isFavorite 由 Flow 单独驱动更新。
 */

// ── UI 状态 ──
sealed interface DetailUiState {
    data object Loading : DetailUiState

    /**
     * @param movie 详情数据（suspend 请求快照）
     * @param isFavorite 收藏状态（Room Flow 实时值，初始取 movie.isFavorite）
     */
    data class Success(
        val movie: MovieDetail,
        val isFavorite: Boolean
    ) : DetailUiState

    data class Error(val message: String) : DetailUiState
}

// ── 意图（用户操作） ──
sealed interface DetailIntent {
    /** 点击收藏按钮：已收藏→取消收藏；未收藏→加入收藏（Room Flow 会自动回推新状态） */
    data object ToggleFavorite : DetailIntent
}
