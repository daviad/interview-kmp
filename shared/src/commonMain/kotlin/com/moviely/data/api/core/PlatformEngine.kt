package com.moviely.data.api.core

import io.ktor.client.engine.HttpClientEngine

/**
 * 🎯 expect 声明：平台 HTTP 引擎工厂
 *
 * KMP 的 expect/actual 机制：
 *   commonMain 中用 expect 声明「我需要什么」，各平台源集用 actual 提供「怎么实现」。
 *   编译器保证每个 expect 都有对应的 actual，类型签名必须完全一致。
 *
 * 🍎 UIKit 类比：
 *   类似 Swift 的 protocol + extension：
 *   - protocol（expect）= 定义一个协议「我需要一个 HttpClientEngine」
 *   - 各平台 extension（actual）= Android 用 OkHttp 实现，iOS 用 Darwin 实现
 *
 * 为什么不直接在 commonMain 里写 HttpClient(OkHttp)？
 *   因为 OkHttp 和 Darwin 都是平台专属 API，commonMain 不可直接 import。
 *   用 expect/actual 隔离平台差异，让 TmdbClient / HttpClientFactory 不关心引擎是什么。
 */
expect class PlatformEngineFactory() {
    /**
     * 创建平台对应的 Ktor HttpClientEngine
     * - Android：返回 OkHttp 引擎（支持系统代理，与原工程行为一致）
     * - iOS：返回 Darwin 引擎（底层 NSURLSession，自动支持系统代理）
     */
    fun create(): HttpClientEngine
}
