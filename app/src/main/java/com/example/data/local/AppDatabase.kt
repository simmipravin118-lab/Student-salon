package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.QueueTicketDao
import com.example.data.local.dao.ShopConfigDao
import com.example.data.local.entity.QueueTicketEntity
import com.example.data.local.entity.ShopConfigEntity

@Database(
    entities = [QueueTicketEntity::class, ShopConfigEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun queueTicketDao(): QueueTicketDao
    abstract fun shopConfigDao(): ShopConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_salon.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
