// ══════════════════════════════════════════════════════════════════
// MovielyKmp - settings.gradle.kts
// 目录约定（路线 A：数据层 KMP 共享，UI 平台原生）：
//   shared/            KMP 跨平台共享层（domain + data）
//   android/           所有 Android 专属 Gradle 模块（壳 + UI feature）
//   ios/               预留：未来 iOS 侧（Xcode 工程 + SwiftUI feature）
// 不 includeBuild 原工程 component-moviely，代码完全搬过来独立运行
// ══════════════════════════════════════════════════════════════════

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MovielyKmp"

// KMP 共享层（domain + data 全部在此模块的 commonMain/androidMain/iosMain）
include(":shared")

// Android 专属模块（android/ 目录下；模块名带 :android: 前缀）
include(":android:feature-home")       // 首页 UI feature（纯 Android Library）
include(":android:feature-favorite")   // 收藏列表 UI feature（Room Flow 订阅）
include(":android:feature-search")     // 搜索 UI feature（suspend 网络请求）
include(":android:feature-detail")     // 电影详情 UI feature（suspend 详情 + Room Flow 收藏联动）
include(":android:app")                // 薄壳 App（Android Application，装配 Koin + 导航 + 启动）
