package com.moviely.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviely.common.domain.Result
import com.moviely.common.domain.model.Movie
import com.moviely.common.domain.model.MovieDetail
import com.moviely.common.domain.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * 详情 ViewModel —— 一个页面里同时演示两种取数模式
 *
 *   ① suspend 快照：getMovieDetail(id) 拉一次详情（简介/演职表/相似电影）
 *   ② Flow 订阅：isFavorite(id) 持续监听收藏表，❤ 状态实时联动
 *
 * movieId 从哪来？
 *   壳 NavHost 解析路由 "detail/{movieId}" 后，通过 Koin parametersOf(movieId)
 *   注入构造函数（见 DetailScreen 的 koinViewModel(parameters = ...) 与
 *   DetailKoinModule 的 viewModel { params -> DetailViewModel(get(), params.get()) }）。
 *   🍎 UIKit 类比：Coordinator push DetailViewController(movieId: id) 的构造传参
 *
 * 收藏联动链路（重点）：
 *   点❤ → ToggleFavorite → addFavorite/removeFavorite 写 Room
 *        → isFavorite Flow 自动 emit 新值 → state.isFavorite 更新 → UI 图标切换
 *   全程不需要手动改 state，也不需要重新请求详情。
 */
class DetailViewModel(
    private val repository: MovieRepository,
    private val movieId: Int
) : ViewModel() {

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        loadDetail()
        observeFavorite()
    }

    fun sendIntent(intent: DetailIntent) {
        when (intent) {
            DetailIntent.ToggleFavorite -> toggleFavorite()
        }
    }

    /** ① 一次性拉详情 */
    private fun loadDetail() {
        viewModelScope.launch {
            _state.value = DetailUiState.Loading
            when (val result = repository.getMovieDetail(movieId)) {
                is Result.Success -> _state.value = DetailUiState.Success(
                    movie = result.data,
                    isFavorite = result.data.isFavorite   // 初始值，随后由 Flow 接管
                )
                is Result.Error -> {
                    android.util.Log.e("MovielyDetail", "详情加载失败: id=$movieId", result.exception)
                    _state.value = DetailUiState.Error(result.message ?: "详情加载失败")
                }
                Result.Loading -> Unit
            }
        }
    }

    /**
     * ② 订阅收藏状态：任何地方改动收藏表（本页❤、未来其他入口），
     *    这里都会收到最新值并更新 Success.isFavorite
     */
    private fun observeFavorite() {
        viewModelScope.launch {
            repository.isFavorite(movieId)
                .catch { e -> android.util.Log.e("MovielyDetail", "收藏状态订阅失败", e) }
                .collect { isFav ->
                    val current = _state.value
                    if (current is DetailUiState.Success) {
                        _state.value = current.copy(isFavorite = isFav)
                    }
                }
        }
    }

    /** 收藏/取消收藏：写库后 Flow 自动回推，这里不手动改 state */
    private fun toggleFavorite() {
        val current = _state.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            val result = if (current.isFavorite) {
                repository.removeFavorite(movieId)
            } else {
                repository.addFavorite(current.movie.toFavoriteMovie())
            }
            if (result is Result.Error) {
                android.util.Log.e("MovielyDetail", "收藏操作失败: id=$movieId", result.exception)
            }
        }
    }

    /**
     * MovieDetail → Movie：收藏表写入需要 Movie 类型（FavoriteEntity 只存列表级字段）
     * genreIds 详情接口不返回，收藏表也用不到，给空列表即可
     */
    private fun MovieDetail.toFavoriteMovie(): Movie = Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        genreIds = emptyList()
    )
}
