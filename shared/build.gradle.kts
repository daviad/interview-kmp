// ══════════════════════════════════════════════════════════════════
// shared 模块 - KMP 共享层 build.gradle.kts
// kotlin("multiplatform") + com.android.library
// 三源集：commonMain（跨平台）+ androidMain + iosMain
// ══════════════════════════════════════════════════════════════════

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// ── Android 配置（KMP 模块的 Android target 需要） ──
android {
    namespace = "com.moviely.kmp.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// ── KMP JVM target 统一 ──
// 【为什么要单独设置？】JDK 25 环境下，Kotlin 2.0.20 不支持 JVM_25，自动回退到 JVM_22。
// 而 compileOptions 是 17，导致 Java(17) vs Kotlin(22) 不一致 → 编译失败。
// 在 androidTarget.compilerOptions 中显式设 jvmTarget=17，确保两者一致。
// 🍎 UIKit 类比：相当于 Xcode 中统一 Swift 和 Obj-C 的 deployment target

// ── KMP targets 声明 ──
// 🍎 UIKit 类比：
//   expect/actual 机制类似于 Swift 的 protocol + extension：commonMain 定义协议，
//   各平台（androidMain/iosMain）提供具体实现。编译器保证每个 expect 都有对应 actual。
kotlin {
    // Android target：产出 AAR，供 :android:feature-home 和 :android:app 依赖
    androidTarget {
        // 不需要 publishLibraryVariants（直接用 project() 依赖，不发布 Maven 产物）

        // 显式设置 JVM target=17，与 compileOptions 一致
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // iOS targets：当前无 Mac 环境，注释掉避免 Gradle 内部 API 报错
    // iosMain 源文件保留在 src/iosMain/ 目录，不删（有 Mac 后取消注释即可）
    // iosArm64()
    // iosSimulatorArm64()
    // iosX64()

    // ── 源集依赖配置 ──
    sourceSets {

        // 🔥 commonMain：跨平台共享代码（75% 的代码在此）
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.room.runtime)
            implementation(libs.room.ktx)
            implementation(libs.koin.core)
        }

        // 🤖 androidMain：Android 平台 actual 实现
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.okhttp)
            implementation(libs.okhttp.logging)
        }

        // 🍎 iosMain：iOS 平台 actual 实现（仅占位，不验证编译）
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

// ── Room KMP 配置 ──
// Room 2.8+ 多平台：KSP 在 commonMain 上跑注解处理，生成跨平台 DAO 实现
// 🍎 UIKit 类比：Room KMP 类似 Core Data 的跨平台 Model 定义
room {
    schemaDirectory("$projectDir/schemas")
}

// KSP：Room 编译器需要处理 commonMain 的 @Entity/@Dao 注解
// Room 2.8+ KMP 方式：在 dependencies 块用 add("kspCommonMainMetadata", ...) 注册
// 注意：如果 KSP 插件未生成此 task，降级用 kspAndroid（Android target 专属）
dependencies {
    add("kspCommonMainMetadata", libs.room.compiler)
    // 降级方案：kspAndroid 也能生成 MovielyDatabase_Impl（仅 Android 变体可用）
    add("kspAndroid", libs.room.compiler)
}
