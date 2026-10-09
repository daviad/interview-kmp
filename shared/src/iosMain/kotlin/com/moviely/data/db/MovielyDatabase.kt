package com.moviely.data.db

import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * iOS actual 实现：Room iOS native SQLite builder（仅占位，无 Mac 不验证编译）
 *
 * Room 2.8+ 在 iOS 上用 SQLite native binding，
 * Builder 不需要 Context（iOS 没有 Application Context 概念）
 *
 * 🍎 UIKit 类比：NSPersistentContainer(name:) 直接初始化，不需要 Application 参数
 */
actual class MovielyDatabaseBuilder actual constructor() {
    actual fun create(): MovielyDatabase {
        // Room iOS builder 语法（Room 2.8 KMP 官方文档）
        // 实际编译需 Mac 工具链，当前仅占位
        return RoomDatabaseBuilder<MovielyDatabase>(name = "moviely.db")
            .build()
    }
}
