package com.moviely.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO：favorites 表的 CRUD 接口（KMP commonMain）
 *
 * Room KSP 在 commonMain 上生成 DAO 实现，各平台共用同一份 SQL 逻辑。
 *
 * @Dao                        = 告诉 Room：这是一个 DAO
 * @Query("SELECT ...")        = 自定义 SQL
 * @Insert(onConflict=REPLACE) = 插入，主键冲突时替换
 * fun xxx(): Flow<T>          = 表数据变化时自动 emit 新值
 *
 * 🍎 UIKit 类比：NSFetchedResultsController + delegate
 */
@Dao
interface FavoriteDao {

    /** 查询全部收藏（按加入时间倒序），Flow 自动监听表变化 */
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getFavoriteMovies(): Flow<List<FavoriteEntity>>

    /** 判断某部电影是否已收藏（Flow<Boolean> 自动推送状态变化） */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    fun isFavorite(id: Int): Flow<Boolean>

    /** 插入收藏（主键冲突时替换） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    /** 按 id 删除收藏 */
    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavorite(id: Int)

    /** 按 id 查询单个 */
    @Query("SELECT * FROM favorites WHERE id = :id")
    suspend fun getFavoriteById(id: Int): FavoriteEntity?
}
