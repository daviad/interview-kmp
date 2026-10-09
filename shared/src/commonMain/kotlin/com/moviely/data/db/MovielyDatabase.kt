package com.moviely.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import kotlin.experimental.ExperimentalNativeApi

/**
 * Room 数据库定义 + expect Builder（KMP commonMain）
 *
 * Room 2.8+ 支持 @Database 在 commonMain 声明，
 * 但 Room.databaseBuilder 需要平台 API（Android 需 Context，iOS 不需要），
 * 所以用 expect/actual 隔离 Builder。
 *
 * 🍎 UIKit 类比：@Database ≈ Core Data 的 NSPersistentContainer 子类，
 *              Builder ≈ NSPersistentContainer(name:).loadPersistentStores
 */
@Database(
    entities = [FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MovielyDatabase : RoomDatabase() {
    /** 获得 FavoriteDao 的实例（Room KSP 编译期生成实现） */
    abstract fun favoriteDao(): FavoriteDao
}

/**
 * expect 声明：平台数据库 Builder
 * - Android：Room.databaseBuilder(context, MovielyDatabase::class.java, "moviely.db")
 * - iOS：Room.databaseBuilder<MovielyDatabase>(name = "moviely.db")
 *
 * 🍎 UIKit 类比：类似 Swift protocol 定义初始化方法，各平台各自实现
 */
@OptIn(ExperimentalNativeApi::class)
expect class MovielyDatabaseBuilder() {
    fun create(): MovielyDatabase
}
