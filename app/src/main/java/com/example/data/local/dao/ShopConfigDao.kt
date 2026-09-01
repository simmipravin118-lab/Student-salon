package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ShopConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopConfigDao {
    @Query("SELECT * FROM shop_config WHERE id = 1 LIMIT 1")
    fun getShopConfig(): Flow<ShopConfigEntity?>

    @Query("SELECT * FROM shop_config WHERE id = 1 LIMIT 1")
    suspend fun getShopConfigDirect(): ShopConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: ShopConfigEntity)

    @Query("UPDATE shop_config SET isOpen = :isOpen WHERE id = 1")
    suspend fun updateShopStatus(isOpen: Boolean)

    @Query("UPDATE shop_config SET announcement = :announcement WHERE id = 1")
    suspend fun updateAnnouncement(announcement: String)
}
