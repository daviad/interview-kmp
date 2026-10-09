package com.moviely.feature.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviely.common.domain.Result
import com.moviely.common.domain.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * 收藏 ViewModel —— 演示「Flow 订阅型」数据的 MVI 写法
 *
 * 与 HomeViewModel（suspend 一次性请求）的本质区别：
 *   Home：用户下拉刷新 → 主动调 suspend → 手动把结果塞进 state
 *   Favorite：init 时订阅 Room Flow，之后【什么都不用做】——
 *             任何地方（详情页❤、本页删除）改动收藏表，Flow 自动 emit 新列表，
 *             state 自动更新，UI 自动 recompose。
 *
 * 🍎 UIKit 类比：NSFetchedResultsController 成为 ViewModel 的数据源，
 *              controllerDidChangeContent 里更新 @Published state
 * 🍎 SwiftUI 类比：@Query var favorites: [FavoriteMovie] 自动驱动 View
 */
class FavoriteViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _state = MutableStateFlow<FavoriteUiState>(FavoriteUiState.Loading)
    val state: StateFlow<FavoriteUiState> = _state.asStateFlow()

    init {
        observeFavorites()
    }

    fun sendIntent(intent: FavoriteIntent) {
        when (intent) {
            is FavoriteIntent.RemoveFavorite -> removeFavorite(intent.movieId)
        }
    }

    /**
     * 订阅收藏表：onStart 显示 Loading → collect 持续更新 → catch 兜底异常
     *
     * 注意 collect 是挂起不返回的（Flow 是冷流但 Room 的 Flow 永不结束），
     * 所以整个订阅跑在 viewModelScope 里，ViewModel 销毁时自动取消。
     */
    private fun observeFavorites() {
        viewModelScope.launch {
            repository.getFavoriteMovies()
                .onStart { _state.value = FavoriteUiState.Loading }
                .catch { e ->
                    android.util.Log.e("MovielyFavorite", "收藏列表订阅失败", e)
                    _state.value = FavoriteUiState.Error(e.message ?: "加载收藏失败")
                }
                .collect { movies ->
                    _state.value = FavoriteUiState.Success(movies)
                }
        }
    }

    /** 取消收藏：写 DB 后 Flow 会自动推新列表，无需手动刷新 state */
    private fun removeFavorite(movieId: Int) {
        viewModelScope.launch {
            val result = repository.removeFavorite(movieId)
            if (result is Result.Error) {
                android.util.Log.e("MovielyFavorite", "取消收藏失败: movieId=$movieId", result.exception)
            }
            // 成功路径什么都不做：Room Flow 会自动驱动 UI
        }
    }
}
