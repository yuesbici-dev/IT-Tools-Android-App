package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Category
import com.example.data.model.CustomPortService
import com.example.data.model.DnsServer
import com.example.data.model.PingHistoryItem
import com.example.data.model.QuickAccessItem
import com.example.data.model.RssFeed
import com.example.data.model.SavedHost
import com.example.data.model.WolDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedHostDao {
    @Query("SELECT * FROM saved_hosts ORDER BY createdAt DESC")
    fun getAllHosts(): Flow<List<SavedHost>>

    @Query("SELECT * FROM saved_hosts WHERE category = :category ORDER BY createdAt DESC")
    fun getHostsByCategory(category: String): Flow<List<SavedHost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHost(host: SavedHost): Long

    @Update
    suspend fun updateHost(host: SavedHost)

    @Delete
    suspend fun deleteHost(host: SavedHost)

    @Query("DELETE FROM saved_hosts WHERE id = :id")
    suspend fun deleteHostById(id: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface DnsServerDao {
    @Query("SELECT * FROM dns_servers ORDER BY id ASC")
    fun getAllDnsServers(): Flow<List<DnsServer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDnsServer(server: DnsServer): Long

    @Delete
    suspend fun deleteDnsServer(server: DnsServer)
}

@Dao
interface WolDeviceDao {
    @Query("SELECT * FROM wol_devices ORDER BY name ASC")
    fun getAllWolDevices(): Flow<List<WolDevice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWolDevice(device: WolDevice): Long

    @Delete
    suspend fun deleteWolDevice(device: WolDevice)
}

@Dao
interface RssFeedDao {
    @Query("SELECT * FROM rss_feeds ORDER BY id ASC")
    fun getAllFeeds(): Flow<List<RssFeed>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: RssFeed): Long

    @Delete
    suspend fun deleteFeed(feed: RssFeed)
}

@Dao
interface QuickAccessDao {
    @Query("SELECT * FROM quick_access_items ORDER BY orderIndex ASC, id ASC")
    fun getAllItems(): Flow<List<QuickAccessItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: QuickAccessItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QuickAccessItem>)

    @Update
    suspend fun updateItem(item: QuickAccessItem)

    @Update
    suspend fun updateAll(items: List<QuickAccessItem>)

    @Delete
    suspend fun deleteItem(item: QuickAccessItem)

    @Query("DELETE FROM quick_access_items WHERE target = :target")
    suspend fun deleteByTarget(target: String)

    @Query("SELECT COUNT(*) FROM quick_access_items")
    suspend fun getCount(): Int

    @Query("DELETE FROM quick_access_items")
    suspend fun deleteAll()
}

@Dao
interface PingHistoryDao {
    @Query("SELECT * FROM ping_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllHistory(): Flow<List<PingHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: PingHistoryItem): Long

    @Query("DELETE FROM ping_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM ping_history")
    suspend fun clearHistory()
}

@Dao
interface CustomPortServiceDao {
    @Query("SELECT * FROM custom_port_services ORDER BY port ASC")
    fun getAllCustomPorts(): Flow<List<CustomPortService>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomPort(service: CustomPortService): Long

    @Delete
    suspend fun deleteCustomPort(service: CustomPortService)
}
