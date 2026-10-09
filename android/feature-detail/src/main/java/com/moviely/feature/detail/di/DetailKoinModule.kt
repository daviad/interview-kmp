package com.moviely.feature.detail.di

import com.moviely.feature.detail.DetailViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * feature-detail Koin Module
 *
 * 与其他 feature 的差异：DetailViewModel 需要【运行时参数 movieId】
 * （路由 "detail/{movieId}" 解析得到），不能用无参的 viewModelOf。
 *
 * 写法：viewModel { params -> DetailViewModel(repository = get(), movieId = params.get()) }
 *   调用侧（DetailScreen）：
 *     koinViewModel(parameters = { parametersOf(movieId) })
 *
 * 🍎 UIKit 类比：register { (movieId: Int) in DetailViewController(id: movieId, ...) }
 *      push 时由 Coordinator 把 id 传进去，而不是容器里预先造好
 */
val detailKoinModule = module {
    viewModel { params ->
        DetailViewModel(
            repository = get(),
            movieId = params.get()
        )
    }
}
