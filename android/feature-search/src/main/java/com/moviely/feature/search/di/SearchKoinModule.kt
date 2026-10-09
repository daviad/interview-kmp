package com.moviely.feature.search.di

import com.moviely.feature.search.SearchViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

/**
 * feature-search Koin Module
 *
 * 同 feature-favorite：数据走 shared 的 MovieRepository 接口，
 * 本模块只需注册 ViewModel，Repository 实现由 Koin 自动注入。
 */
val searchKoinModule = module {
    viewModelOf(::SearchViewModel)
}
