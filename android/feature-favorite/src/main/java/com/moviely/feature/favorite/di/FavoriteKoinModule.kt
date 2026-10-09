package com.moviely.feature.favorite.di

import com.moviely.feature.favorite.FavoriteViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

/**
 * feature-favorite Koin Module
 *
 * 与 homeKoinModule 的差异：本模块没有自己的 Api/Dao ——
 * 数据直接走 shared 的 MovieRepository 接口（domain 层），
 * Koin 解析 FavoriteViewModel 构造参数时自动注入 sharedModule 里
 * 绑定的 MovieRepositoryImpl（其 FavoriteDao 依赖由 appOverrideModule 提供）。
 *
 * 🍎 UIKit 类比：container.register(FavoriteViewModel.self) {
 *                  FavoriteViewModel(repository: r.resolve(MovieRepository.self)!) }
 */
val favoriteKoinModule = module {
    viewModelOf(::FavoriteViewModel)
}
