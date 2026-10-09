package com.moviely.data.api.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * 🎯 HTTP 通用封装：用 Ktor 构建一个【与业务无关】的 HttpClient（KMP 跨平台版）
 *
 * 与原 component-moviely 的差异：
 *   ① 原版直接用 `HttpClient(OkHttp) { ... }` → KMP 版需要从 PlatformEngineFactory 获取引擎
 *   ② 原版 Logging 用 `android.util.Log` → KMP commonMain 不能 import android.*，
 *      改用 Logger.DEFAULT（println，在 Android 上输出到 Logcat，iOS 上输出到 console）
 *
 * 【四大插件】（都在 HttpClient level 配置，全局生效）
 *   ① HttpTimeout  — 超时（硬约束 15s/30s/60s，在 HttpClient level 配置）
 *   ② ContentNegotiation — JSON 自动编解码（DTO ↔ String 互转）
 *   ③ Logging       — 请求/响应日志（Debug 用；Ktor 无内置 curl 格式，见 CurlLogging.kt）
 *   ④ CurlLogging   — 发送前打印一条可复制的 curl 命令（排查网络失败用）
 *
 * 【超时三段式含义】
 *   connectTimeoutMillis = 15_000  → 建立 TCP 连接最多 15s
 *   socketTimeoutMillis  = 30_000  → 两次数据传输之间最长 30s（读超时）
 *   requestTimeoutMillis = 60_000  → 整个请求生命周期上限 60s
 *   🍎 UIKit 类比：URLSessionConfiguration 的 timeoutIntervalForRequest / timeoutIntervalForResource
 */
object HttpClientFactory {

    /**
     * 构建一个配置好的 HttpClient（跨平台）
     *
     * @param json   kotlinx.serialization 的 Json 配置（由 Koin 提供单例）
     * @param engine 平台 HTTP 引擎（Android=OkHttp / iOS=Darwin，由 PlatformEngineFactory 创建）
     */
    fun create(json: Json, engine: HttpClientEngine): HttpClient = HttpClient(engine) {
        // ── ① HttpTimeout：硬约束 15s/30s/60s，在 HttpClient level 配置 ──
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
            requestTimeoutMillis = 60_000
        }

        // ── ② ContentNegotiation：JSON ↔ DTO 自动转换 ──
        install(ContentNegotiation) {
            json(json)
        }

        // ── ③ Logging：请求/响应日志（Debug 用）──
        // KMP 版用 println（Android 上输出到 Logcat，tag 为 System.out）
        // Ktor 3.x 移除了 Logger.DEFAULT，改用 SAM 转换 lambda
        // 级别 ALL：诊断期把响应体也打出来（如 401 的错误详情、序列化失败时的原始 JSON）；
        //          平时性能敏感可降回 HEADERS
        // TODO: 后续可改为 expect/actual 平台专属 Logger（Android → android.util.Log.d）
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) { println(message) }
            }
        }

        // ── ④ CurlLogging：每个请求发送前打印一条可复制的 curl 命令 ──
        // Ktor 不内置 curl 格式，自定义实现见 CurlLogging.kt
        // Logcat 里用文本过滤「curl」即可命中所有请求
        install(CurlLogging)
    }

    /**
     * 提供 kotlinx.serialization 的 JSON 解析配置（全局单例配置）
     * 🍎 UIKit 类比：JSONDecoder 的 keyDecodingStrategy / dateDecodingStrategy 配置
     */
    fun createJson(): Json = Json {
        ignoreUnknownKeys = true   // JSON 里有 Kotlin 类没定义的字段？忽略（TMDB 经常加字段）
        isLenient = true           // 宽松模式：允许非标准 JSON
        coerceInputValues = true   // 类型不匹配时用默认值兜底（null→0 等）
        explicitNulls = false      // JSON 的 null 和缺字段都当缺字段（用默认值）
    }
}
