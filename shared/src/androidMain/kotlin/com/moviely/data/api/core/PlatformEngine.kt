package com.moviely.data.api.core

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

/**
 * 🤖 Android actual 实现：OkHttp 引擎
 *
 * 为什么用 OkHttp 而不是 CIO？
 *   CIO（Coroutine I/O）引擎不读 Android 系统代理设置，调试时无法走 Charles/NanoProxy。
 *   OkHttp 引擎自动读取系统代理（systemProperty），与原工程行为完全一致。
 *
 * OkHttp 版本锁定 4.12.0（见 libs.versions.toml）：
 *   4.12.0 以上版本的 bytecode metadata 与 Kotlin 2.0 编译器有 mismatch，
 *   导致编译警告或运行时 ClassCastException。
 *
 * 🍎 UIKit 类比：OkHttp 引擎 ≈ 基于 NSURLSessionConfiguration 的自定义 URLSession，
 *              可以配置代理、拦截器、连接池等
 */
actual class PlatformEngineFactory actual constructor() {
    actual fun create(): HttpClientEngine = OkHttp.create()
}
