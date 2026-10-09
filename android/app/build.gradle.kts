// ══════════════════════════════════════════════════════════════════
// androidApp 模块 - 薄壳 App（Koin 装配 + NavHost + 启动）
// ══════════════════════════════════════════════════════════════════

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.moviely.kmp.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.moviely.kmp.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // TMDB API Key: 从 local.properties 读取 → BuildConfig 注入
        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localProps.load(localPropsFile.bufferedReader())
        }
        val tmdbApiKey = localProps.getProperty("TMDB_API_KEY", "")

        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
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
        buildConfig = true
    }
}

dependencies {
    // KMP 共享层 + 各 UI feature
    implementation(project(":shared"))
    implementation(project(":android:feature-home"))
    implementation(project(":android:feature-favorite"))
    implementation(project(":android:feature-search"))
    implementation(project(":android:feature-detail"))

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)

    // Lifecycle + Activity
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)

    // Navigation Compose
    implementation(libs.compose.navigation)

    // Room（androidApp 需要访问 MovielyDatabase 的 RoomDatabase 父类型）
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
}
