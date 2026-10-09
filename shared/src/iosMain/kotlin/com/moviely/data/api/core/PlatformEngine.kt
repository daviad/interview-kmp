package com.moviely.data.api.core

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/**
 * 🍎 iOS actual 实现：Darwin 引擎（仅占位，无 Mac 环境不验证编译）
 *
 * Darwin 引擎底层使用 iOS 的 NSURLSession：
 *   - 自动支持系统代理（WiFi 代理 / VPN）
 *   - 自动复用连接池（NSURLSession 默认行为）
 *   - 与 OkHttp 一样支持 HTTP/2
 *
 * 🍎 UIKit 类比：Darwin 引擎 ≈ 直接用 NSURLSession.shared 或自定义 session
 */
actual class PlatformEngineFactory actual constructor() {
    actual fun create(): HttpClientEngine = Darwin.create()
}
