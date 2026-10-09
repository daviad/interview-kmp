import SwiftUI
import shared

// ══════════════════════════════════════════════════════════════════
// MovielyApp - iOS 应用入口
// 对标 Android 的 android/app/.../MovielyApp.kt (Application + startKoin)
// ══════════════════════════════════════════════════════════════════

@main
struct MovielyApp: App {
    init() {
        // 启动 Koin（对标 Android MovielyApp.onCreate 里的 startKoin）
        // shared 模块的 sharedModule + iOS override（API Key + DB Builder）
        IosKoinKt.startKoinIos(apiKey: Secrets.tmdbApiKey)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// 全局依赖访问器
// 对标 Android 的 Koin get<MovieRepository>()
//
// Swift 不用 Koin DSL，而是通过 Kotlin 侧的 IosApp.repository() 拿到实例
// 🍎 UIKit 类比：相当于 ServiceLocator.shared.resolve()
// ══════════════════════════════════════════════════════════════════

enum AppDependencies {
    /// 取 shared 层的 MovieRepository（KMP 接口，Swift 直接调用）
    static func repository() -> MovieRepository {
        IosApp.shared.repository()
    }
}
