package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_hosts")
data class SavedHost(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ip: String,
    val label: String,
    val notes: String = "",
    val category: String = "General",
    val macAddress: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#38BDF8"
)

@Entity(tableName = "dns_servers")
data class DnsServer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "wol_devices")
data class WolDevice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val macAddress: String,
    val broadcastIp: String = "255.255.255.255",
    val port: Int = 9
)

@Entity(tableName = "rss_feeds")
data class RssFeed(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val category: String = "IT Security",
    val isDefault: Boolean = false
)

data class RssArticle(
    val title: String,
    val link: String,
    val pubDate: String,
    val description: String,
    val sourceTitle: String
)

@Entity(tableName = "quick_access_items")
data class QuickAccessItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subtitle: String,
    val iconKey: String, // "ping", "port_scan", "net_info", "wol", "lan_scan", "dns", "traceroute", "whois", "ip_calc4", "ip_calc6", "ip_manager", "rss", "terminal", "server", "router", "vpn", "link"
    val actionType: String, // "TOOL", "EXTERNAL_APP", "EXTERNAL_URL"
    val target: String, // Route name (e.g. "ping") or package name (e.g. "com.termux") or URL
    val categoryTag: String = "Diagnostics", // "Diagnostics", "Discovery", "Calculators", "Management", "External"
    val isCustom: Boolean = false,
    val isVisible: Boolean = true,
    val orderIndex: Int = 0
)

@Entity(tableName = "ping_history")
data class PingHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,
    val timestamp: Long = System.currentTimeMillis(),
    val packetsTransmitted: Int = 0,
    val packetsReceived: Int = 0,
    val avgRttMs: Double = 0.0
)

@Entity(tableName = "custom_port_services")
data class CustomPortService(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val port: Int,
    val label: String,
    val protocol: String = "TCP" // "TCP", "UDP", "BOTH"
)
