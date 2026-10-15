package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.Category
import com.example.data.model.CustomPortService
import com.example.data.model.DnsServer
import com.example.data.model.PingHistoryItem
import com.example.data.model.QuickAccessItem
import com.example.data.model.RssArticle
import com.example.data.model.RssFeed
import com.example.data.model.SavedHost
import com.example.data.model.WolDevice
import com.example.network.RssParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class NetworkRepository(
    private val database: AppDatabase,
    private val rssParser: RssParser = RssParser()
) {
    // Saved Hosts
    val allHosts: Flow<List<SavedHost>> = database.savedHostDao().getAllHosts()
    fun getHostsByCategory(category: String): Flow<List<SavedHost>> =
        database.savedHostDao().getHostsByCategory(category)

    suspend fun saveHost(host: SavedHost): Long = database.savedHostDao().insertHost(host)
    suspend fun updateHost(host: SavedHost) = database.savedHostDao().updateHost(host)
    suspend fun deleteHost(host: SavedHost) = database.savedHostDao().deleteHost(host)
    suspend fun deleteHostById(id: Long) = database.savedHostDao().deleteHostById(id)

    // Categories
    val allCategories: Flow<List<Category>> = database.categoryDao().getAllCategories()
    suspend fun addCategory(category: Category): Long = database.categoryDao().insertCategory(category)
    suspend fun deleteCategory(category: Category) = database.categoryDao().deleteCategory(category)

    // DNS Servers
    val allDnsServers: Flow<List<DnsServer>> = database.dnsServerDao().getAllDnsServers()
    suspend fun addDnsServer(server: DnsServer): Long = database.dnsServerDao().insertDnsServer(server)
    suspend fun deleteDnsServer(server: DnsServer) = database.dnsServerDao().deleteDnsServer(server)

    // WoL Devices
    val allWolDevices: Flow<List<WolDevice>> = database.wolDeviceDao().getAllWolDevices()
    suspend fun saveWolDevice(device: WolDevice): Long = database.wolDeviceDao().insertWolDevice(device)
    suspend fun deleteWolDevice(device: WolDevice) = database.wolDeviceDao().deleteWolDevice(device)

    // RSS Feeds
    val allFeeds: Flow<List<RssFeed>> = database.rssFeedDao().getAllFeeds()
    suspend fun addFeed(feed: RssFeed): Long = database.rssFeedDao().insertFeed(feed)
    suspend fun deleteFeed(feed: RssFeed) = database.rssFeedDao().deleteFeed(feed)

    suspend fun fetchArticlesForFeed(feed: RssFeed): List<RssArticle> {
        return rssParser.fetchFeed(feed.url, feed.title).getOrDefault(emptyList())
    }

    // Ping History
    val pingHistory: Flow<List<PingHistoryItem>> = database.pingHistoryDao().getAllHistory()
    suspend fun recordPingHistory(item: PingHistoryItem) = database.pingHistoryDao().insertHistory(item)
    suspend fun deletePingHistoryById(id: Long) = database.pingHistoryDao().deleteHistoryById(id)
    suspend fun clearPingHistory() = database.pingHistoryDao().clearHistory()

    // Custom Port Services
    val customPortServices: Flow<List<CustomPortService>> = database.customPortServiceDao().getAllCustomPorts()
    suspend fun addCustomPortService(service: CustomPortService) = database.customPortServiceDao().insertCustomPort(service)
    suspend fun deleteCustomPortService(service: CustomPortService) = database.customPortServiceDao().deleteCustomPort(service)

    // Quick Access Grid Items
    val allQuickAccessItems: Flow<List<QuickAccessItem>> = database.quickAccessDao().getAllItems()
    suspend fun saveQuickAccessItem(item: QuickAccessItem): Long = database.quickAccessDao().insertItem(item)
    suspend fun updateQuickAccessItem(item: QuickAccessItem) = database.quickAccessDao().updateItem(item)
    suspend fun deleteQuickAccessItem(item: QuickAccessItem) = database.quickAccessDao().deleteItem(item)

    suspend fun updateQuickAccessOrder(reorderedItems: List<QuickAccessItem>) {
        val updated = reorderedItems.mapIndexed { index, item ->
            item.copy(orderIndex = index + 1)
        }
        database.quickAccessDao().updateAll(updated)
    }

    suspend fun readdDefaultItem(target: String) {
        val defaultTemplate = DEFAULT_QUICK_ACCESS_ITEMS.find { it.target == target } ?: return
        val currentItems = database.quickAccessDao().getAllItems().first()
        if (currentItems.none { it.target == target }) {
            val maxOrder = currentItems.maxOfOrNull { it.orderIndex } ?: 0
            database.quickAccessDao().insertItem(
                defaultTemplate.copy(id = 0, orderIndex = maxOrder + 1)
            )
        }
    }

    suspend fun restoreAllMissingDefaultItems() {
        val currentItems = database.quickAccessDao().getAllItems().first()
        val currentTargets = currentItems.map { it.target }.toSet()
        var maxOrder = currentItems.maxOfOrNull { it.orderIndex } ?: 0
        val missing = DEFAULT_QUICK_ACCESS_ITEMS.filter { it.target !in currentTargets }
        val toInsert = missing.map { item ->
            maxOrder++
            item.copy(id = 0, orderIndex = maxOrder)
        }
        if (toInsert.isNotEmpty()) {
            database.quickAccessDao().insertAll(toInsert)
        }
    }

    // First-run seed
    suspend fun seedDefaultsIfEmpty() {
        if (database.quickAccessDao().getCount() == 0) {
            // Seed Categories
            database.categoryDao().insertCategory(Category(name = "Home Network", colorHex = "#38BDF8"))
            database.categoryDao().insertCategory(Category(name = "Work Servers", colorHex = "#60A5FA"))
            database.categoryDao().insertCategory(Category(name = "IoT Devices", colorHex = "#34D399"))
            database.categoryDao().insertCategory(Category(name = "Cloud VPS", colorHex = "#A78BFA"))

            // Seed Sample Host
            database.savedHostDao().insertHost(
                SavedHost(
                    ip = "192.168.1.1",
                    label = "Primary Gateway / Router",
                    notes = "Main network gateway & DHCP server. Admin port 80/443.",
                    category = "Home Network",
                    macAddress = "00:1A:2B:3C:4D:5E"
                )
            )

            // Seed DNS Servers
            database.dnsServerDao().insertDnsServer(DnsServer(name = "Cloudflare Primary", address = "1.1.1.1", isDefault = true))
            database.dnsServerDao().insertDnsServer(DnsServer(name = "Cloudflare Secondary", address = "1.0.0.1"))
            database.dnsServerDao().insertDnsServer(DnsServer(name = "Google Public DNS", address = "8.8.8.8"))
            database.dnsServerDao().insertDnsServer(DnsServer(name = "Quad9 Security", address = "9.9.9.9"))
            database.dnsServerDao().insertDnsServer(DnsServer(name = "OpenDNS Standard", address = "208.67.222.222"))

            // Seed RSS Feeds (IT Security & Sysadmin)
            database.rssFeedDao().insertFeed(
                RssFeed(title = "BleepingComputer", url = "https://www.bleepingcomputer.com/feed/", category = "Security", isDefault = true)
            )
            database.rssFeedDao().insertFeed(
                RssFeed(title = "Hacker News", url = "https://news.ycombinator.com/rss", category = "Tech News", isDefault = true)
            )
            database.rssFeedDao().insertFeed(
                RssFeed(title = "The Hacker News", url = "https://feeds.feedburner.com/TheHackersNews", category = "Vulnerabilities", isDefault = true)
            )
            database.rssFeedDao().insertFeed(
                RssFeed(title = "CISA Alerts", url = "https://www.cisa.gov/uscert/ncas/alerts.xml", category = "Gov Advisories", isDefault = true)
            )

            // Seed Default Custom Port Examples
            database.customPortServiceDao().insertCustomPort(CustomPortService(port = 8008, label = "Internal API Service"))
            database.customPortServiceDao().insertCustomPort(CustomPortService(port = 25565, label = "Minecraft Game Server"))

            // Seed Default Quick Access Dashboard Items
            database.quickAccessDao().insertAll(DEFAULT_QUICK_ACCESS_ITEMS)
        }
    }

    companion object {
        val DEFAULT_QUICK_ACCESS_ITEMS = listOf(
            QuickAccessItem(
                title = "Ping",
                subtitle = "ICMP & TCP Latency",
                iconKey = "ping",
                actionType = "TOOL",
                target = "ping",
                categoryTag = "Diagnostics",
                orderIndex = 1
            ),
            QuickAccessItem(
                title = "Port Scanner",
                subtitle = "TCP / UDP Range Probe",
                iconKey = "port_scan",
                actionType = "TOOL",
                target = "port_scan",
                categoryTag = "Diagnostics",
                orderIndex = 2
            ),
            QuickAccessItem(
                title = "Network Info",
                subtitle = "Local & Public IP",
                iconKey = "net_info",
                actionType = "TOOL",
                target = "net_info",
                categoryTag = "Discovery",
                orderIndex = 3
            ),
            QuickAccessItem(
                title = "LAN Scanner",
                subtitle = "Subnet Discovery",
                iconKey = "lan_scan",
                actionType = "TOOL",
                target = "lan_scan",
                categoryTag = "Discovery",
                orderIndex = 4
            ),
            QuickAccessItem(
                title = "Wake on LAN",
                subtitle = "Magic Packet Broadcast",
                iconKey = "wol",
                actionType = "TOOL",
                target = "wol",
                categoryTag = "Discovery",
                orderIndex = 5
            ),
            QuickAccessItem(
                title = "Traceroute",
                subtitle = "Hop-by-Hop Route",
                iconKey = "traceroute",
                actionType = "TOOL",
                target = "traceroute",
                categoryTag = "Diagnostics",
                orderIndex = 6
            ),
            QuickAccessItem(
                title = "DNS Lookup",
                subtitle = "NSLookup & Custom DNS",
                iconKey = "dns",
                actionType = "TOOL",
                target = "dns",
                categoryTag = "Diagnostics",
                orderIndex = 7
            ),
            QuickAccessItem(
                title = "WHOIS / RDAP",
                subtitle = "Domain & IP Records",
                iconKey = "whois",
                actionType = "TOOL",
                target = "whois",
                categoryTag = "Diagnostics",
                orderIndex = 8
            ),
            QuickAccessItem(
                title = "IPv4 Calculator",
                subtitle = "CIDR & Subnetting",
                iconKey = "ip_calc4",
                actionType = "TOOL",
                target = "ip_calc4",
                categoryTag = "Calculators",
                orderIndex = 9
            ),
            QuickAccessItem(
                title = "IPv6 Calculator",
                subtitle = "Prefix & Compression",
                iconKey = "ip_calc6",
                actionType = "TOOL",
                target = "ip_calc6",
                categoryTag = "Calculators",
                orderIndex = 10
            ),
            QuickAccessItem(
                title = "IP Address Book",
                subtitle = "Bookmarks & Notes",
                iconKey = "ip_manager",
                actionType = "TOOL",
                target = "ip_manager",
                categoryTag = "Management",
                orderIndex = 11
            ),
            QuickAccessItem(
                title = "RSS Feeds",
                subtitle = "Security & Tech News",
                iconKey = "rss",
                actionType = "TOOL",
                target = "rss",
                categoryTag = "Management",
                orderIndex = 12
            ),
            QuickAccessItem(
                title = "Termux Terminal",
                subtitle = "External Shell",
                iconKey = "terminal",
                actionType = "EXTERNAL_APP",
                target = "com.termux",
                categoryTag = "External",
                isCustom = true,
                orderIndex = 13
            ),
            QuickAccessItem(
                title = "WireGuard VPN",
                subtitle = "Tunnel Client",
                iconKey = "vpn",
                actionType = "EXTERNAL_APP",
                target = "com.wireguard.android",
                categoryTag = "External",
                isCustom = true,
                orderIndex = 14
            )
        )
    }
}
