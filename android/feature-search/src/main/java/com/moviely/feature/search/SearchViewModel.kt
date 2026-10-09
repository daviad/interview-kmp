package com.moviely.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviely.common.domain.Result
import com.moviely.common.domain.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 搜索 ViewModel —— 演示「用户触发型」suspend 请求
 *
 * 与 FavoriteViewModel（Flow 订阅）相反：
 *   搜索是一次性的——用户输入关键词、按搜索，发一次请求、拿一页结果。
 *   所以用 suspend fun searchMovies() + 手动更新 state，和 HomeViewModel 同族。
 *
 * 数据来源差异（对照学习）：
 *   HomeViewModel     → feature 自管的 HomeApiClient（TmdbClient + 自己的 DTO）
 *   FavoriteViewModel → MovieRepository（Room Flow）
 *   SearchViewModel   → MovieRepository（网络 suspend，返回 domain Movie）
 * 三种取数方式最终都汇成 StateFlow<UiState>，UI 层写法完全一致。
 *
 * 🍎 UIKit 类比：searchBarSearchButtonClicked 里调 service.search(query) { result in ... }
 */
class SearchViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    fun sendIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> onQueryChanged(intent.query)
            SearchIntent.Search -> search()
        }
    }

    /** 只更新输入框文字，不发请求（打字时不打网络，避免无意义请求） */
    private fun onQueryChanged(query: String) {
        _state.update { current ->
            when (current) {
                is SearchUiState.Idle -> current.copy(query = query)
                is SearchUiState.Loading -> current.copy(query = query)
                is SearchUiState.Success -> current.copy(query = query)
                is SearchUiState.Error -> current.copy(query = query)
            }
        }
    }

    /** 发起搜索：空白 query 直接回到 Idle；否则 Loading → Success/Error */
    private fun search() {
        val query = _state.value.query.trim()
        if (query.isEmpty()) {
            _state.value = SearchUiState.Idle("")
            return
        }

        viewModelScope.launch {
            _state.value = SearchUiState.Loading(query)
            when (val result = repository.searchMovies(query, page = 1)) {
                is Result.Success -> _state.value = SearchUiState.Success(query, result.data)
                is Result.Error -> {
                    android.util.Log.e("MovielySearch", "搜索失败: query=$query", result.exception)
                    _state.value = SearchUiState.Error(query, result.message ?: "搜索失败，请稍后重试")
                }
                Result.Loading -> Unit // 不会出现：suspend 返回时已终态
            }
        }
    }
}
