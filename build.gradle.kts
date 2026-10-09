// ══════════════════════════════════════════════════════════════════
// MovielyKmp - 根 build.gradle.kts
// 顶层插件声明（均 apply false，由子模块按需 apply）
// ══════════════════════════════════════════════════════════════════

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

// ══════════════════════════════════════════════════════════════════
// 统一 group：com.moviely.kmp（注意：与原工程 com.moviely 不同）
// 【为什么不同？】MovielyKmp 是独立仓，不参与原工程 Composite Build。
// 如果将来需要 includeBuild 接入原工程，不同 group 避免坐标冲突。
//
// 🍎 UIKit 类比：相当于给新 framework 用不同的 bundle identifier 前缀，
// 避免与原 App 的 bundle ID 冲突。
// ══════════════════════════════════════════════════════════════════
allprojects {
    group = "com.moviely.kmp"
}
