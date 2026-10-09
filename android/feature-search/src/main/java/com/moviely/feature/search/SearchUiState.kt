package com.moviely.feature.search

import com.moviely.common.domain.model.Movie

/**
 * MVI 三要素 —— 搜索模块
 *
 * 状态机比首页多一个 Idle（还没搜过）：
 *
 *   Idle（输入框空/未触发）→ Loading（搜索中）→ Success（结果/空结果）
 *                                        ↘ Error（网络失败）
 *
 * 所有状态都带 query：输入框内容是状态的一部分（单向数据流，
 * UI 不自己持有文字，TextField 的值永远来自 state）。
 *
 * 🍎 SwiftUI 类比：enum SearchState { case idle(String), loading(String),
 *                              success(String, [Movie]), error(String, String) }
 */

// ── UI 状态 ──
sealed interface SearchUiState {
    /** 当前输入框文本（每个子状态都持有，保证 TextField 重组不丢字） */
    val query: String

    /** 初始态：还没发起过搜索（展示引导文案） */
    data class Idle(override val query: String = "") : SearchUiState

    /** 搜索中：query 已提交，等网络结果（显示转圈） */
    data class Loading(override val query: String) : SearchUiState

    /** 搜索完成：results 为空时 UI 显示「没有找到」 */
    data class Success(override val query: String, val results: List<Movie>) : SearchUiState

    /** 搜索失败：保留 query 方便用户重试 */
    data class Error(override val query: String, val message: String) : SearchUiState
}

// ── 意图（用户操作） ──
sealed interface SearchIntent {
    /** 输入框文字变化（每敲一个字触发一次，只更新 query，不发请求） */
    data class QueryChanged(val query: String) : SearchIntent

    /** 点击搜索图标 / 软键盘 Search 键：发起搜索 */
    data object Search : SearchIntent
}
