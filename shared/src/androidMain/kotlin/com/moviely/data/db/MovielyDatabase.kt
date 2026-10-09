package com.moviely.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Android actual 实现：Room.databaseBuilder 需要 Context
 *
 * 🍎 UIKit 类比：NSPersistentContainer(name:) 需要 UIApplication.shared，
 *              Android 端的 Context ≈ UIApplication
 *
 * 注意：Builder 不持有 Context，Context 由 Koin module 注入
 * （androidApp 的 AppKoinModules.kt 提供 get() = androidContext）
 */
actual class MovielyDatabaseBuilder actual constructor() {
    fun create(context: Context): MovielyDatabase {
        return Room.databaseBuilder(
            context,
            MovielyDatabase::class.java,
            "moviely.db"
        ).build()
    }

    // commonMain 的 expect 签名是不带 Context 的 create()，
    // 但 Android 需要 Context，所以这里用重载解决
    // Koin module 会调用 create(get()) 传入 androidContext
    actual fun create(): MovielyDatabase {
        // 这个方法不会实际调用到（Android 需要额外 Context 参数）
        // 但为了让编译器通过，需要实现
        throw NotImplementedError("Use create(context) on Android")
    }
}
