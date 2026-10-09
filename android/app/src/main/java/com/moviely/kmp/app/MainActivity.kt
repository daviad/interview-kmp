package com.moviely.kmp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

/**
 * MainActivity - 唯一 Activity（薄壳）
 *
 * Koin 不需要 @AndroidEntryPoint（Koin 通过 koinViewModel() 在 Compose 中直接获取）
 *
 * 🍎 UIKit 类比：UIApplicationDelegate / SceneDelegate → 显示根 VC
 *   setContent { } ≈ window.rootViewController = UIHostingController(rootView: ...)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MovielyNavHost()
                }
            }
        }
    }
}
