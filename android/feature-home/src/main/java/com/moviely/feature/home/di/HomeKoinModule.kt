package com.moviely.feature.home.di

import com.moviely.feature.home.HomeViewModel
import com.moviely.feature.home.data.HomeApi
import com.moviely.feature.home.data.HomeApiClient
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

/**
 * feature-home Koin Module：绑定 HomeApi + HomeViewModel
 *
 * 替代原工程 Hilt 的 @Module + @Binds + @HiltViewModel
 *
 * 🍎 UIKit 类比：DI 容器注册块
 *   container.register(HomeApi.self) { r in HomeApiClient(client: r.resolve(TmdbClient.self)!) }
 *   container.register(HomeViewModel.self) { r in HomeViewModel(api: r.resolve(HomeApi.self)!) }
 */
val homeKoinModule = module {
    // HomeApiClient 注册为 HomeApi 接口的实现
    // factory<接口类型> { 实现类构造函数(get(依赖)) }
    factory<HomeApi> { HomeApiClient(get()) }

    // HomeViewModel（viewModelOf = Koin 管理 ViewModel 生命周期）
    viewModelOf(::HomeViewModel)
}
