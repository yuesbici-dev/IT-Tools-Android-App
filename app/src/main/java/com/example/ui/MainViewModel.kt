package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.repository.NetworkRepository
import com.example.network.DeviceNetworkInfo
import com.example.network.DnsLookupEngine
import com.example.network.DnsQueryResult
import com.example.network.Ipv4SubnetResult
import com.example.network.Ipv6SubnetResult
import com.example.network.LanDevice
import com.example.network.LanScanEngine
import com.example.network.NetworkInfoProvider
import com.example.network.PingEngine
import com.example.network.PingPacket
import com.example.network.PingStats
import com.example.network.PortResult
import com.example.network.PortScanEngine
import com.example.network.ScanProtocol
import com.example.network.SubnetCalculator
import com.example.network.TraceHop
import com.example.network.TracerouteEngine
import com.example.network.WhoisEngine
import com.example.network.WhoisResult
import com.example.network.WolEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HostSortBy {
    LABEL, IP, DATE, CATEGORY
}

enum class SortOrder {
    ASCENDING, DESCENDING
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NetworkRepository(AppDatabase.getInstance(application))
    private val pingEngine = PingEngine()
    private val portScanEngine = PortScanEngine()
    private val networkInfoProvider = NetworkInfoProvider(application)
    private val wolEngine = WolEngine()
    private val tracerouteEngine = TracerouteEngine()
    private val dnsLookupEngine = DnsLookupEngine()
    private val whoisEngine = WhoisEngine()
    private val lanScanEngine = LanScanEngine()

    // Navigation State
    private val _currentScreen = MutableStateFlow("home")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Shared target IP prefill for tool cross-launch
    private val _sharedTarget = MutableStateFlow("")
    val sharedTarget: StateFlow<String> = _sharedTarget.asStateFlow()

    // Database Flows
    val quickAccessItems: StateFlow<List<QuickAccessItem>> = repository.allQuickAccessItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedHosts: StateFlow<List<SavedHost>> = repository.allHosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dnsServers: StateFlow<List<DnsServer>> = repository.allDnsServers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wolDevices: StateFlow<List<WolDevice>> = repository.allWolDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rssFeeds: StateFlow<List<RssFeed>> = repository.allFeeds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pingHistory: StateFlow<List<PingHistoryItem>> = repository.pingHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customPortServices: StateFlow<List<CustomPortService>> = repository.customPortServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Network Info State
    private val _networkInfo = MutableStateFlow<DeviceNetworkInfo?>(null)
    val networkInfo: StateFlow<DeviceNetworkInfo?> = _networkInfo.asStateFlow()

    private val _isRefreshingNetInfo = MutableStateFlow(false)
    val isRefreshingNetInfo: StateFlow<Boolean> = _isRefreshingNetInfo.asStateFlow()

    // Ping State
    private val _pingPackets = MutableStateFlow<List<PingPacket>>(emptyList())
    val pingPackets: StateFlow<List<PingPacket>> = _pingPackets.asStateFlow()

    private val _pingStats = MutableStateFlow<PingStats?>(null)
    val pingStats: StateFlow<PingStats?> = _pingStats.asStateFlow()

    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()
    private var pingJob: Job? = null

    // Port Scan State
    private val _portResults = MutableStateFlow<List<PortResult>>(emptyList())
    val portResults: StateFlow<List<PortResult>> = _portResults.asStateFlow()

    private val _portScanProgress = MutableStateFlow(0f)
    val portScanProgress: StateFlow<Float> = _portScanProgress.asStateFlow()

    private val _portScanStatusText = MutableStateFlow("Ready to scan")
    val portScanStatusText: StateFlow<String> = _portScanStatusText.asStateFlow()

    private val _isPortScanning = MutableStateFlow(false)
    val isPortScanning: StateFlow<Boolean> = _isPortScanning.asStateFlow()
    private var portScanJob: Job? = null

    // LAN Scanner State
    private val _lanDevices = MutableStateFlow<List<LanDevice>>(emptyList())
    val lanDevices: StateFlow<List<LanDevice>> = _lanDevices.asStateFlow()

    private val _lanScanProgress = MutableStateFlow(0f)
    val lanScanProgress: StateFlow<Float> = _lanScanProgress.asStateFlow()

    private val _isLanScanning = MutableStateFlow(false)
    val isLanScanning: StateFlow<Boolean> = _isLanScanning.asStateFlow()
    private var lanScanJob: Job? = null

    // Traceroute State
    private val _tracerouteHops = MutableStateFlow<List<TraceHop>>(emptyList())
    val tracerouteHops: StateFlow<List<TraceHop>> = _tracerouteHops.asStateFlow()

    private val _isTracing = MutableStateFlow(false)
    val isTracing: StateFlow<Boolean> = _isTracing.asStateFlow()
    private var traceJob: Job? = null

    // DNS Lookup State
    private val _dnsResult = MutableStateFlow<DnsQueryResult?>(null)
    val dnsResult: StateFlow<DnsQueryResult?> = _dnsResult.asStateFlow()

    private val _isDnsQuerying = MutableStateFlow(false)
    val isDnsQuerying: StateFlow<Boolean> = _isDnsQuerying.asStateFlow()

    // WHOIS State
    private val _whoisResult = MutableStateFlow<WhoisResult?>(null)
    val whoisResult: StateFlow<WhoisResult?> = _whoisResult.asStateFlow()

    private val _isWhoisLoading = MutableStateFlow(false)
    val isWhoisLoading: StateFlow<Boolean> = _isWhoisLoading.asStateFlow()

    // Subnet Calculators State
    private val _ipv4Result = MutableStateFlow<Ipv4SubnetResult?>(null)
    val ipv4Result: StateFlow<Ipv4SubnetResult?> = _ipv4Result.asStateFlow()

    private val _ipv6Result = MutableStateFlow<Ipv6SubnetResult?>(null)
    val ipv6Result: StateFlow<Ipv6SubnetResult?> = _ipv6Result.asStateFlow()

    // RSS Feed Articles
    private val _rssArticles = MutableStateFlow<List<RssArticle>>(emptyList())
    val rssArticles: StateFlow<List<RssArticle>> = _rssArticles.asStateFlow()

    private val _isRssLoading = MutableStateFlow(false)
    val isRssLoading: StateFlow<Boolean> = _isRssLoading.asStateFlow()

    // IP Address Book Sort & Order
    private val _hostSortBy = MutableStateFlow(HostSortBy.DATE)
    val hostSortBy: StateFlow<HostSortBy> = _hostSortBy.asStateFlow()

    private val _hostSortOrder = MutableStateFlow(SortOrder.DESCENDING)
    val hostSortOrder: StateFlow<SortOrder> = _hostSortOrder.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDefaultsIfEmpty()
            refreshNetworkInfo()
            calculateIpv4("192.168.1.1", 24)
            calculateIpv6("2001:0db8:85a3:0000:0000:8a2e:0370:7334", 64)
            refreshRssFeeds()
        }
    }

    fun navigateTo(screen: String, targetHost: String? = null) {
        if (!targetHost.isNullOrBlank()) {
            _sharedTarget.value = targetHost
        }
        _currentScreen.value = screen
    }

    // Network Info
    fun refreshNetworkInfo() {
        viewModelScope.launch {
            _isRefreshingNetInfo.value = true
            val localInfo = networkInfoProvider.getLocalNetworkInfo()
            _networkInfo.value = localInfo

            val publicIpMap = networkInfoProvider.fetchPublicIpInfo().getOrNull()
            if (publicIpMap != null) {
                _networkInfo.value = localInfo.copy(
                    publicIp = publicIpMap["publicIp"] ?: "",
                    isp = publicIpMap["isp"] ?: "",
                    country = publicIpMap["country"] ?: "",
                    city = publicIpMap["city"] ?: "",
                    asn = publicIpMap["asn"] ?: ""
                )
            }
            _isRefreshingNetInfo.value = false
        }
    }

    // Ping
    fun startPing(host: String, count: Int = 0, intervalMs: Long = 600L) {
        pingJob?.cancel()
        _pingPackets.value = emptyList()
        _pingStats.value = null
        _isPinging.value = true

        pingJob = viewModelScope.launch {
            pingEngine.executePing(host, count, intervalMs).collect { packet ->
                val updated = _pingPackets.value + packet
                _pingPackets.value = updated
                val calculatedStats = PingEngine.calculateStats(updated)
                _pingStats.value = calculatedStats
            }
            _isPinging.value = false

            // Save to ping history
            val finalPackets = _pingPackets.value
            if (finalPackets.isNotEmpty()) {
                val stats = _pingStats.value
                repository.recordPingHistory(
                    PingHistoryItem(
                        host = host.trim(),
                        packetsTransmitted = finalPackets.size,
                        packetsReceived = finalPackets.count { it.isSuccess },
                        avgRttMs = stats?.avgRtt ?: 0.0
                    )
                )
            }
        }
    }

    fun stopPing() {
        val currentPackets = _pingPackets.value
        val host = currentPackets.firstOrNull()?.ip ?: _sharedTarget.value
        pingJob?.cancel()
        _isPinging.value = false

        if (currentPackets.isNotEmpty() && host.isNotBlank()) {
            viewModelScope.launch {
                val stats = _pingStats.value
                repository.recordPingHistory(
                    PingHistoryItem(
                        host = host.trim(),
                        packetsTransmitted = currentPackets.size,
                        packetsReceived = currentPackets.count { it.isSuccess },
                        avgRttMs = stats?.avgRtt ?: 0.0
                    )
                )
            }
        }
    }

    fun clearPingHistory() {
        viewModelScope.launch { repository.clearPingHistory() }
    }

    fun deletePingHistory(id: Long) {
        viewModelScope.launch { repository.deletePingHistoryById(id) }
    }

    // Port Scan
    fun startPortScan(
        target: String,
        ports: List<Int>,
        protocol: ScanProtocol = ScanProtocol.TCP,
        timeoutMs: Int = 300
    ) {
        portScanJob?.cancel()
        _portResults.value = emptyList()
        _portScanProgress.value = 0f
        _isPortScanning.value = true
        _portScanStatusText.value = "Starting scan on ${ports.size} ports..."

        val customMap = customPortServices.value.associate { it.port to it.label }

        portScanJob = viewModelScope.launch {
            val accumulated = mutableListOf<PortResult>()
            portScanEngine.scanPorts(target, ports, protocol, timeoutMs, customServiceMap = customMap).collect { progress ->
                if (progress.totalPorts > 0) {
                    _portScanProgress.value = progress.scannedCount.toFloat() / progress.totalPorts
                }
                progress.latestResult?.let {
                    accumulated.add(it)
                    _portResults.value = accumulated.toList()
                }
                _portScanStatusText.value = "Scanned ${progress.scannedCount}/${progress.totalPorts} ports (${progress.openPortsCount} open)"
                if (progress.isFinished) {
                    _isPortScanning.value = false
                    _portScanStatusText.value = "Scan finished: ${progress.openPortsCount} open of ${progress.totalPorts} ports"
                }
            }
        }
    }

    fun stopPortScan() {
        portScanJob?.cancel()
        _isPortScanning.value = false
        _portScanStatusText.value = "Scan cancelled"
    }

    fun addCustomPortService(port: Int, label: String, protocol: String = "TCP") {
        viewModelScope.launch {
            repository.addCustomPortService(CustomPortService(port = port, label = label, protocol = protocol))
        }
    }

    fun deleteCustomPortService(service: CustomPortService) {
        viewModelScope.launch {
            repository.deleteCustomPortService(service)
        }
    }

    // LAN Scanner
    fun startLanScan(localIp: String? = null, cidr: Int = 24) {
        val targetIp = localIp ?: _networkInfo.value?.localIpv4 ?: "192.168.1.1"
        lanScanJob?.cancel()
        _lanDevices.value = emptyList()
        _lanScanProgress.value = 0f
        _isLanScanning.value = true

        lanScanJob = viewModelScope.launch {
            lanScanEngine.scanSubnet(targetIp, cidr).collect { progress ->
                if (progress.total > 0) {
                    _lanScanProgress.value = progress.scanned.toFloat() / progress.total
                }
                _lanDevices.value = progress.activeDevices
                if (progress.isFinished) {
                    _isLanScanning.value = false
                }
            }
        }
    }

    fun stopLanScan() {
        lanScanJob?.cancel()
        _isLanScanning.value = false
    }

    // Wake-on-LAN
    fun sendWolPacket(mac: String, broadcastIp: String, port: Int, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = wolEngine.sendMagicPacket(mac, broadcastIp, port)
            result.onSuccess { msg ->
                onComplete(true, msg)
            }.onFailure { err ->
                onComplete(false, err.message ?: "Failed to send packet")
            }
        }
    }

    fun saveWolDevice(device: WolDevice) {
        viewModelScope.launch { repository.saveWolDevice(device) }
    }

    fun deleteWolDevice(device: WolDevice) {
        viewModelScope.launch { repository.deleteWolDevice(device) }
    }

    // Traceroute
    fun startTraceroute(target: String) {
        traceJob?.cancel()
        _tracerouteHops.value = emptyList()
        _isTracing.value = true

        traceJob = viewModelScope.launch {
            val list = mutableListOf<TraceHop>()
            tracerouteEngine.executeTraceroute(target).collect { hop ->
                list.add(hop)
                _tracerouteHops.value = list.toList()
                if (hop.isDestinationReached) {
                    _isTracing.value = false
                }
            }
            _isTracing.value = false
        }
    }

    fun stopTraceroute() {
        traceJob?.cancel()
        _isTracing.value = false
    }

    // DNS Query
    fun queryDns(domain: String, type: String, serverIp: String) {
        viewModelScope.launch {
            _isDnsQuerying.value = true
            val res = dnsLookupEngine.queryDns(domain, type, serverIp).getOrNull()
            _dnsResult.value = res
            _isDnsQuerying.value = false
        }
    }

    fun saveDnsServer(name: String, address: String) {
        viewModelScope.launch {
            repository.addDnsServer(DnsServer(name = name, address = address))
        }
    }

    fun deleteDnsServer(server: DnsServer) {
        viewModelScope.launch { repository.deleteDnsServer(server) }
    }

    // WHOIS / RDAP
    fun lookupWhois(query: String) {
        viewModelScope.launch {
            _isWhoisLoading.value = true
            val res = whoisEngine.lookup(query).getOrNull()
            _whoisResult.value = res
            _isWhoisLoading.value = false
        }
    }

    // Calculators
    fun calculateIpv4(ip: String, cidr: Int) {
        _ipv4Result.value = SubnetCalculator.calculateIpv4(ip, cidr).getOrNull()
    }

    fun calculateIpv6(ip: String, prefix: Int) {
        _ipv6Result.value = SubnetCalculator.calculateIpv6(ip, prefix).getOrNull()
    }

    // IP Manager
    fun saveHost(host: SavedHost) {
        viewModelScope.launch { repository.saveHost(host) }
    }

    fun updateHost(host: SavedHost) {
        viewModelScope.launch { repository.updateHost(host) }
    }

    fun deleteHost(host: SavedHost) {
        viewModelScope.launch { repository.deleteHost(host) }
    }

    fun addCategory(name: String, colorHex: String = "#38BDF8") {
        viewModelScope.launch { repository.addCategory(Category(name = name, colorHex = colorHex)) }
    }

    fun setHostSort(sortBy: HostSortBy, order: SortOrder) {
        _hostSortBy.value = sortBy
        _hostSortOrder.value = order
    }

    // RSS Feeds
    fun refreshRssFeeds() {
        viewModelScope.launch {
            _isRssLoading.value = true
            val feeds = repository.allFeeds.first()
            val allArticles = mutableListOf<RssArticle>()
            for (feed in feeds) {
                val articles = repository.fetchArticlesForFeed(feed)
                allArticles.addAll(articles)
            }
            _rssArticles.value = allArticles.sortedByDescending { it.pubDate }
            _isRssLoading.value = false
        }
    }

    fun addRssFeed(title: String, url: String, category: String = "Custom") {
        viewModelScope.launch {
            repository.addFeed(RssFeed(title = title, url = url, category = category))
            refreshRssFeeds()
        }
    }

    fun deleteRssFeed(feed: RssFeed) {
        viewModelScope.launch {
            repository.deleteFeed(feed)
            refreshRssFeeds()
        }
    }

    // Quick Access Grid Management & Reordering
    fun saveQuickAccessItem(item: QuickAccessItem) {
        viewModelScope.launch { repository.saveQuickAccessItem(item) }
    }

    fun updateQuickAccessItem(item: QuickAccessItem) {
        viewModelScope.launch { repository.updateQuickAccessItem(item) }
    }

    fun deleteQuickAccessItem(item: QuickAccessItem) {
        viewModelScope.launch { repository.deleteQuickAccessItem(item) }
    }

    fun reorderQuickAccessItems(reordered: List<QuickAccessItem>) {
        viewModelScope.launch { repository.updateQuickAccessOrder(reordered) }
    }

    fun readdDefaultItem(target: String) {
        viewModelScope.launch {
            repository.readdDefaultItem(target)
            Toast.makeText(getApplication(), "Restored tool to dashboard", Toast.LENGTH_SHORT).show()
        }
    }

    fun restoreAllDefaultItems() {
        viewModelScope.launch {
            repository.restoreAllMissingDefaultItems()
            Toast.makeText(getApplication(), "All default tools restored", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchQuickAccessItem(item: QuickAccessItem) {
        when (item.actionType) {
            "TOOL" -> {
                navigateTo(item.target)
            }
            "EXTERNAL_APP" -> {
                launchExternalApp(item.target, item.title)
            }
            "EXTERNAL_URL" -> {
                launchExternalUrl(item.target)
            }
        }
    }

    private fun launchExternalApp(packageName: String, appName: String) {
        val context = getApplication<Application>()
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "$appName is not installed on this device.", Toast.LENGTH_SHORT).show()
                val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(storeIntent)
                } catch (_: Exception) {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch $appName: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchExternalUrl(urlStr: String) {
        val context = getApplication<Application>()
        try {
            val formatted = if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                "https://$urlStr"
            } else {
                urlStr
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatted)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open URL: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
