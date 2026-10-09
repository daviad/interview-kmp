import Foundation

// ══════════════════════════════════════════════════════════════════
// iOS TMDB API Key 配置
// 对标 Android 的 local.properties → BuildConfig.TMDB_API_KEY
//
// 正式项目用 .xcconfig 文件管理（gitignore），这里简化为单文件
// ⚠️ 此文件应该加入 .gitignore，不要提交真实 key
// ══════════════════════════════════════════════════════════════════

enum Secrets {
    /// TMDB v4 Read Access Token（以 eyJ 开头）
    /// TmdbClient 按前缀自动识别 → Bearer 头
    static let tmdbApiKey = "YOUR_TMDB_TOKEN_HERE"
}
