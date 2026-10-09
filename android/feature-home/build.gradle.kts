// ══════════════════════════════════════════════════════════════════
// feature-home 模块构建脚本（MovielyKmp 版）
// 纯 Android Library，依赖 :shared（KMP），DI 用 Koin
// ══════════════════════════════════════════════════════════════════

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.moviely.kmp.feature.home"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // KMP 共享层（TmdbClient / MovieRepository / 领域模型 / 常量）
    implementation(project(":shared"))

    // DTO 序列化
    implementation(libs.kotlinx.serialization.json)

    // Compose（BOM 统一版本）
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)

    // Lifecycle（ViewModel + Compose 集成 + collectAsStateWithLifecycle）
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Koin（替代 Hilt）
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // 图片加载（Coil）
    implementation(libs.coil.compose)

    // Activity Compose（HomeScreen 需要 setContent 环境，由壳工程提供但编译期需要 API）
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
}
