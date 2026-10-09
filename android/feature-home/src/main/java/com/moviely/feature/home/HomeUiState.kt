package com.moviely.feature.home

import com.moviely.common.domain.model.Movie

/**
 * MVI 架构 - UiState / Intent / Effect 三要素
 *
 * sealed interface = 受限继承体系，编译器知道所有子类型，
 * when 表达式可穷举所有分支，不需要 else，遗漏任一分支会编译报错。
 *
 * 🍎 Swift 类比：enum with associated values 几乎一一对应
 */

// ── UI 状态（持久快照，持续渲染） ──
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val popularMovies: List<Movie> = emptyList(),
        val nowPlayingMovies: List<Movie> = emptyList(),
        val isRefreshing: Boolean = false,
        val refreshMessage: String? = null
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

// ── 意图（用户操作的统一封装） ──
sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data class NavigateToDetail(val movieId: Int) : HomeIntent
}

// ── 副作用（一次性事件，消费一次即失效） ──
sealed interface HomeEffect {
    data class NavigateToDetail(val movieId: Int) : HomeEffect
    data class ShowToast(val message: String) : HomeEffect
}
