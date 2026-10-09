// ══════════════════════════════════════════════════════════════════
// feature-favorite 模块构建脚本
// 纯 Android Library，依赖 :shared（KMP），DI 用 Koin
// 与 feature-home 的差异：本模块不直接碰网络端点，数据全部走 shared 的
// MovieRepository（domain 层接口），因此不需要 serialization / ksp 插件
// ══════════════════════════════════════════════════════════════════

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.moviely.kmp.feature.favorite"
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
    // KMP 共享层（MovieRepository 接口 / Movie 领域模型 / Result）
    implementation(project(":shared"))

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

    // Activity Compose（Screen 由壳工程 setContent 承载，编译期需要 API）
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
}
