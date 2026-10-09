package com.moviely.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.moviely.common.domain.model.Cast
import com.moviely.common.domain.model.Movie
import com.moviely.common.domain.model.MovieDetail
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 电影详情页
 *
 * @param movieId 路由参数（壳 NavHost 从 "detail/{movieId}" 解析）
 * @param onBack 返回上一页
 * @param onMovieClick 点击相似电影 → 壳导航到新的 detail/{id}（push 新详情页）
 *
 * movieId 通过 Koin parametersOf 注入 DetailViewModel 构造函数
 * （每个路由条目有独立 ViewModel，相似电影跳转会创建新的 VM 实例）
 *
 * 🍎 SwiftUI 类比：NavigationStack navigationDestination(for: MovieID.self) { DetailView(id: $0) }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    movieId: Int,
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit = {},
    viewModel: DetailViewModel = koinViewModel(parameters = { parametersOf(movieId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { (state as? DetailUiState.Success)?.movie?.title?.let { Text(it) } },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 顶栏❤：与正文收藏按钮同一 state，Flow 驱动自动切换
                    (state as? DetailUiState.Success)?.let { s ->
                        IconButton(onClick = { viewModel.sendIntent(DetailIntent.ToggleFavorite) }) {
                            Icon(
                                imageVector = if (s.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (s.isFavorite) "取消收藏" else "收藏",
                                tint = if (s.isFavorite) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                DetailUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                is DetailUiState.Error -> Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )

                is DetailUiState.Success -> DetailContent(
                    movie = s.movie,
                    isFavorite = s.isFavorite,
                    onToggleFavorite = { viewModel.sendIntent(DetailIntent.ToggleFavorite) },
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    movie: MovieDetail,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onMovieClick: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 背景大图
        item {
            AsyncImage(
                model = movie.backdropUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentScale = ContentScale.Crop
            )
        }

        // 海报 + 基础信息
        item {
            Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .width(100.dp)
                        .height(150.dp),
                    contentScale = ContentScale.Crop
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${movie.year} · ${movie.formattedRuntime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "评分: ${movie.formattedVote}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (movie.genresText.isNotEmpty()) {
                        Text(
                            text = movie.genresText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 收藏按钮（Flow 驱动：点击后文案/图标自动切换）
        item {
            Button(
                onClick = onToggleFavorite,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(if (isFavorite) "已收藏（点击取消）" else "收藏")
            }
        }

        // 剧情简介
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("剧情简介", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = movie.overview.ifEmpty { "暂无简介" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // 演员表（TMDB 返回已按 order 排序，这里再保险排一次）
        item {
            Text(
                "主要演员",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp)
            )
            val cast: List<Cast> = movie.cast.sortedBy { it.order }.take(12)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = cast, key = { it.id }) { person ->
                    CastCard(person)
                }
            }
        }

        // 相似电影（点击 push 新详情页）
        if (movie.similarMovies.isNotEmpty()) {
            item {
                Text(
                    "你可能还喜欢",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = movie.similarMovies, key = { it.id }) { similar ->
                        SimilarMovieCard(movie = similar, onClick = { onMovieClick(similar.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CastCard(cast: Cast) {
    Card(modifier = Modifier.width(96.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = cast.profileUrl,
                contentDescription = cast.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = cast.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = cast.character,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SimilarMovieCard(movie: Movie, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Column(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier
                    .width(110.dp)
                    .height(165.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = movie.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp).width(110.dp)
            )
        }
    }
}
