package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Category
import com.example.data.model.CustomPortService
import com.example.data.model.DnsServer
import com.example.data.model.PingHistoryItem
import com.example.data.model.QuickAccessItem
import com.example.data.model.RssFeed
import com.example.data.model.SavedHost
import com.example.data.model.WolDevice

@Database(
    entities = [
        SavedHost::class,
        Category::class,
        DnsServer::class,
        WolDevice::class,
        RssFeed::class,
        QuickAccessItem::class,
        PingHistoryItem::class,
        CustomPortService::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedHostDao(): SavedHostDao
    abstract fun categoryDao(): CategoryDao
    abstract fun dnsServerDao(): DnsServerDao
    abstract fun wolDeviceDao(): WolDeviceDao
    abstract fun rssFeedDao(): RssFeedDao
    abstract fun quickAccessDao(): QuickAccessDao
    abstract fun pingHistoryDao(): PingHistoryDao
    abstract fun customPortServiceDao(): CustomPortServiceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "netpulse_db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
