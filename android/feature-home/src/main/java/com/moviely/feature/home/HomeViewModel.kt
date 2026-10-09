package com.moviely.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviely.common.domain.model.Movie
import com.moviely.feature.home.data.HomeApi
import com.moviely.feature.home.data.MovieDto
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * MVI ViewModel - 首页业务逻辑中枢
 *
 * 与原工程差异：去掉 @HiltViewModel + @Inject constructor，改为普通构造
 * Koin 在 HomeKoinModule 中用 viewModelOf(::HomeViewModel) 注册
 *
 * MVI 数据流：
 *   View → sendIntent(HomeIntent) → ViewModel 处理 → StateFlow<HomeUiState> 更新
 *                                          → Channel<HomeEffect> 发一次性事件
 *
 * 🍎 UIKit 类比：
 *   @HiltViewModel → DI 容器注册的 ObservableObject
 *   @Published var state = .loading → StateFlow<HomeUiState>
 */
class HomeViewModel(
    private val homeApi: HomeApi
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        refresh()
    }

    fun sendIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Refresh -> refresh()
            is HomeIntent.NavigateToDetail -> navigateToDetail(intent.movieId)
        }
    }

    /**
     * 核心业务：并发拉取热门 + 正在上映
     *
     * 首次加载：Loading → Success/Error（全屏切换）
     * 下拉刷新：Success(isRefreshing=true) → Success(isRefreshing=false)
     *
     * 🍎 Swift async 并发：
     *   async let popular = api.getPopular()
     *   async let now = api.getNowPlaying()
     */
    private fun refresh() {
        viewModelScope.launch {
            val previous = _state.value as? HomeUiState.Success
            val hasData = previous?.popularMovies?.isNotEmpty() == true

            if (hasData) {
                _state.value = previous!!.copy(isRefreshing = true, refreshMessage = null)
            } else {
                _state.value = HomeUiState.Loading
            }

            var popularMovies: List<Movie> = emptyList()
            var nowPlayingMovies: List<Movie> = emptyList()
            var errorMsg: String? = null

            try {
                coroutineScope {
                    val popularDeferred = async {
                        homeApi.getPopularMovies().results.map { it.toMovie() }
                    }
                    val nowPlayingDeferred = async {
                        homeApi.getNowPlayingMovies().results.map { it.toMovie() }
                    }
                    popularMovies = popularDeferred.await()
                    nowPlayingMovies = nowPlayingDeferred.await()
                }
            } catch (e: Exception) {
                // 排查网络失败的关键一步：把异常【类型 + 完整堆栈】打进 Logcat。
                // e.message 经常为 null 或过于简略（如 "timeout"），堆栈才能定位到具体失败环节。
                // Logcat 过滤 tag「MovielyHome」即可看到。
                // 常见异常对照：
                //   UnknownHostException        → DNS 解析失败（无网 / DNS 被污染）
                //   ConnectTimeoutException     → 连接超时（TMDB 在大陆被墙，需代理）
                //   SSLHandshakeException       → TLS 证书问题（代理中间人 / 系统时间不对）
                //   SerializationException      → JSON 与 DTO 字段不匹配（配合响应体日志看）
                //   RedirectResponseException 等 3xx/4xx/5xx → HTTP 状态错误（Key 无效常为 401）
                android.util.Log.e("MovielyHome", "首页数据加载失败: ${e.javaClass.simpleName}", e)
                errorMsg = e.message ?: "网络请求失败，请稍后重试"
            }

            _state.value = if (errorMsg != null) {
                if (hasData && previous != null) {
                    previous.copy(isRefreshing = false, refreshMessage = errorMsg)
                } else {
                    HomeUiState.Error(errorMsg)
                }
            } else {
                HomeUiState.Success(
                    popularMovies = popularMovies,
                    nowPlayingMovies = nowPlayingMovies,
                    isRefreshing = false,
                    refreshMessage = if (hasData) "刷新成功" else null
                )
            }
        }
    }

    private fun navigateToDetail(movieId: Int) {
        viewModelScope.launch {
            _effect.send(HomeEffect.NavigateToDetail(movieId))
        }
    }

    /** DTO → Domain 映射 */
    private fun MovieDto.toMovie(): Movie = Movie(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        genreIds = genreIds
    )
}
