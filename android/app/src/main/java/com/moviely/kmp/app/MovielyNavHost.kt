package com.moviely.kmp.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moviely.feature.detail.DetailScreen
import com.moviely.feature.favorite.FavoriteScreen
import com.moviely.feature.home.HomeScreen
import com.moviely.feature.search.SearchScreen

/**
 * MovielyNavHost - 导航容器（壳的唯一职责之一）
 *
 * 路由表：
 *   home              → HomeScreen      （首页，startDestination）
 *   search            → SearchScreen    （搜索页）
 *   favorites         → FavoriteScreen  （收藏列表页）
 *   detail/{movieId}  → DetailScreen    （电影详情页，movieId 为 Int 路径参数）
 *
 * 跳转规则：feature 之间【不直接互相依赖】，统一由壳的 NavHost 接线
 * （feature 只暴露 Screen + 回调，壳决定回调里导航到哪）。
 * 详情页相似电影点击会 push 新的 detail/{id}（导航栈可叠多层详情）。
 *
 * 🍎 UIKit 类比：Coordinator / Router 集中管理 push/pop
 * 🍎 SwiftUI 类比：NavigationStack + navigationDestination 路由表
 */
@Composable
fun MovielyNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onMovieClick = { movieId -> navController.navigate("detail/$movieId") },
                onSearchClick = { navController.navigate("search") },
                onFavoritesClick = { navController.navigate("favorites") }
            )
        }
        composable("search") {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { movieId -> navController.navigate("detail/$movieId") }
            )
        }
        composable("favorites") {
            FavoriteScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { movieId -> navController.navigate("detail/$movieId") }
            )
        }
        composable(
            route = "detail/{movieId}",
            arguments = listOf(
                navArgument("movieId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getInt("movieId")
            if (movieId != null) {
                DetailScreen(
                    movieId = movieId,
                    onBack = { navController.popBackStack() },
                    onMovieClick = { id -> navController.navigate("detail/$id") }
                )
            }
        }
    }
}
