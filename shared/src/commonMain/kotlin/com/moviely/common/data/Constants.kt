package com.moviely.common.data

/**
 * 🎯 全局常量（commonMain 层，跨平台共享）
 *
 * 为什么 API Key 不写在这里？
 *   硬编码 API Key 进源代码会被反编译拿到（apktool 直接提取 strings.xml/classes.dex）
 *   正确做法（KMP + Koin 版本）：
 *     ① local.properties 存 Key（加入 .gitignore，不进版本库）
 *     ② androidApp/build.gradle.kts 读取 → buildConfigField 注入 BuildConfig
 *     ③ Koin module override single(named("tmdb_api_key")) { BuildConfig.TMDB_API_KEY }
 *     ④ TmdbClient 构造时通过 Koin 注入 apiKey 参数
 *   🍎 UIKit 类比：
 *     - local.properties = .xcconfig 文件（项目配置，不在 git 中）
 *     - BuildConfig = Info.plist + build settings 的编译时常量
 *     - Koin module = ServiceLocator / Dip 依赖注入容器
 */
object Constants {
    /**
     * TMDB API 基础 URL（不含末尾 '/'）
     *
     * 调用方按 "$TMDB_BASE_URL/movie/popular" 拼接路径，避免双斜杠。
     * /3 = TMDB 的 API 版本号（目前最新 v3）
     */
    const val TMDB_BASE_URL = "https://api.themoviedb.org/3"

    /**
     * TMDB 图片基础 URL
     * 完整 URL 拼接规则：{TMDB_IMAGE_BASE_URL}/{尺寸}/{相对路径}
     * 例：https://image.tmdb.org/t/p/w500/abc.jpg
     */
    const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p"

    /**
     * 默认语言：zh-CN = 简体中文（TMDB API 的 language 查询参数）
     * 效果：返回的 movie.title / overview 优先用中文，没有中文就回退英文
     */
    const val DEFAULT_LANGUAGE = "zh-CN"
}
