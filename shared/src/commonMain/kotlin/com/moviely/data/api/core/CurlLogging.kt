package com.moviely.data.api.core

import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder

/**
 * 🎯 自定义 curl 日志插件：每个请求发送前打印一条【可直接复制到终端执行】的 curl 命令
 *
 * ══════════════════════════════════════════════════════════════════════════════
 * 【为什么需要这个插件？】
 *   Ktor 官方 Logging 插件只有 Default / OkHttp 两种输出格式，【没有 curl 格式】。
 *   排查网络失败时，curl 命令的价值：
 *     ① 一行就能看到完整 URL + api_key + 所有 query 参数（最直观）
 *     ② 复制到 PC 终端执行 = 绕开 App 的运行环境，快速定位是「网络问题」还是「代码问题」：
 *        - 终端也失败 → 网络不通（大陆直连 TMDB 常见）/ Key 无效
 *        - 终端成功   → App 运行环境问题（模拟器代理、DNS 等）
 *
 * 【实现机制：Ktor 3.0.x 的自定义 ClientPlugin API】
 *   createClientPlugin(name) { ... } 创建一个轻量插件。
 *   钩子选型（3.0.x 只有 SetupRequest / SendingRequest / Send 三个）：
 *     - SetupRequest：请求处理最前段，此时 Ktor 各插件的默认头（如 User-Agent）还没合并
 *     - SendingRequest：HttpSendPipeline.State，紧挨着引擎发送之前，
 *       URL / query 参数 / 所有头都已最终定型 —— 打印最准确，选它 ✅
 *     （注：Ktor 3.1+ 新增了 AfterRender / AfterEncoding 等更细的钩子）
 *
 *   🍎 UIKit 类比：发送前拿到完整 URLRequest 打日志
 *                ≈ URLSessionTaskDelegate 里读 task.currentRequest 的时机
 *
 * 【输出位置】
 *   commonMain 只能用 println（不能 import android.*）。
 *   Android Logcat 里 println 的 tag 是 System.out，用文本过滤「curl」即可全部命中。
 *
 * ⚠️ 注意：curl 命令里包含真实 api_key（v3 认证就拼在 query 里），日志仅限本地调试，勿截图外传。
 *
 * 【已知边界】
 *   当前只打印 GET 类无请求体的请求（本项目所有 TMDB 端点都是 GET）。
 *   若将来有 POST + body，需要在 content 参数里取序列化后的请求体拼 -d '...'（TODO）。
 * ══════════════════════════════════════════════════════════════════════════════
 */
val CurlLogging: ClientPlugin<Unit> = createClientPlugin(name = "CurlLogging") {
    on(SendingRequest) { request, _ ->
        println(buildCurlCommand(request))
    }
}

/**
 * 把 HttpRequestBuilder 还原成一条 curl 命令
 *
 * curl 语法对应关系：
 *   -X {method}          → 请求方法（GET 时 curl 可省略，但显式写出更易读）
 *   -H 'Key: Value'      → 每个请求头一个 -H
 *   '{url}'              → 完整 URL（含 ?api_key=xxx&language=zh-CN&page=1）
 *
 * 单引号包裹：macOS/Linux 终端 + Windows PowerShell 都兼容；
 * Windows cmd.exe 不认单引号，复制到 Git Bash 或 PowerShell 执行。
 */
private fun buildCurlCommand(request: HttpRequestBuilder): String = buildString {
    append("curl -X ${request.method.value}")
    // 逐个头转 -H（names() + getAll() 是 Headers 接口的稳定成员，避免 forEach 扩展的版本差异）
    request.headers.names().forEach { name ->
        request.headers.getAll(name)?.forEach { value ->
            append(" -H '$name: $value'")
        }
    }
    // URL 最后放（buildString() 输出含 scheme/host/path/query 的完整地址）
    append(" '${request.url.buildString()}'")
}
