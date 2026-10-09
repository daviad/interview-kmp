package com.moviely.data.api.tmdb

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders

/**
 * 🎯 通用 TMDB 客户端（端点无关）—— data-api 对 feature 暴露的唯一网络门面
 *
 * ══════════════════════════════════════════════════════════════════════════════
 * 【架构定位：基础层与业务层的分工】
 *
 *   data-api（基础层）只负责「怎么发请求」：
 *     - HttpClient 单例（OkHttp/Darwin engine + 超时 + JSON + 日志）
 *     - baseUrl / api_key 鉴权 / 默认语言 —— 全 App 统一的 TMDB 配置
 *     - 泛型 get<T>(path, params) —— 不认识任何业务端点
 *
 *   各 feature（业务层）负责「请求什么」：
 *     - feature-home 定义自己的 HomeApi（/movie/popular、/movie/now_playing）
 *     - 将来 feature-detail / feature-search 各自定义自己的接口
 *
 *   🍎 UIKit 类比：AFHTTPSessionManager / URLSession 是通用封装（data-api），
 *                 各业务自己写 APIService 协议 + 端点常量（feature 仓）
 * ══════════════════════════════════════════════════════════════════════════════
 *
 * 【inline + reified —— 泛型反序列化的实现机制】
 *   Kotlin 的泛型运行时会被擦除（Type Erasure），普通函数拿不到 T 的真实类型。
 *   reified + inline 让类型在编译期「内联进调用点」，从而能在函数体内使用 T::class
 *   —— Ktor 的 body<T>() 就是这么实现的，要求调用方函数也是 inline。
 *
 *   🍎 Swift 类比：Swift 泛型没有擦除，func get<T: Decodable>(...) 直接可用
 *
 * 【@PublishedApi internal —— inline 函数访问私有成员的官方解法】
 *   public inline 函数会被内联进【调用方的模块】（feature 仓），
 *   因此不能访问 private 成员。@PublishedApi internal = 「对外仍是 internal，
 *   但允许被 public inline 函数内联访问」。
 *
 * @param httpClient      通用 HttpClient 单例（Koin 提供）
 * @param baseUrl         TMDB v3 基础地址（不带末尾 '/'，来自 Constants）
 * @param apiKey          TMDB v3 api_key（Koin named("tmdb_api_key") 提供）
 * @param defaultLanguage 默认语言（Constants.DEFAULT_LANGUAGE，zh-CN）
 */
class TmdbClient(
    @PublishedApi internal val httpClient: HttpClient,
    @PublishedApi internal val baseUrl: String,
    @PublishedApi internal val apiKey: String,
    @PublishedApi internal val defaultLanguage: String
) {

    /**
     * 泛型 GET：拼接 baseUrl + path，自动附带 api_key 鉴权与默认 language。
     *
     * @param path   端点路径，以 '/' 开头（如 "/movie/popular"），由【业务方】传入
     * @param params 额外查询参数（page、query 等）；键 "language" 可覆盖默认语言
     * @return T    JSON 响应反序列化结果（T 由调用方的 DTO 决定）
     *
     * 🍎 UIKit 类比：
     *   func get<T: Decodable>(path: String, params: [String: String]) async throws -> T
     */
    suspend inline fun <reified T> get(
        path: String,
        params: Map<String, String> = emptyMap()
    ): T {
        // 合并参数：默认 language 先放，调用方 params 后放（同 key 覆盖）
        val allParams = buildMap {
            put("language", defaultLanguage)
            putAll(params)
        }
        return httpClient.get("$baseUrl$path") {
            // ══ 认证方式自动识别（本次网络失败 401 的根因修复）══
            // TMDB 有两种凭证，认证方式【必须匹配】否则一律 401：
            //   ① v3 API Key        —— 32 位十六进制串 → 只能放 query 参数 ?api_key=xxx
            //   ② v4 Read Access Token —— JWT（固定以 "eyJ" 开头，base64 的 {" ）→ 只能放 Bearer 请求头
            // ❌ 曾经的 bug：把 v4 token 塞进 ?api_key= → TMDB 直接 401（query 方式不认 JWT）
            // ✅ 现在：按前缀自动选择认证方式，两种 Key 都能工作
            if (apiKey.startsWith("eyJ")) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            } else {
                parameter("api_key", apiKey)
            }
            allParams.forEach { (key, value) ->
                parameter(key, value)             // 逐个拼 ?key=value
            }
        }.body()                                  // ContentNegotiation 自动 JSON → T
    }
}
