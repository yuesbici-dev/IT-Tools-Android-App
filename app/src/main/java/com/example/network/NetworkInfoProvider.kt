package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.net.URL

data class InterfaceInfo(
    val name: String,
    val displayName: String,
    val ipv4: List<String>,
    val ipv6: List<String>,
    val isUp: Boolean,
    val isLoopback: Boolean
)

data class DeviceNetworkInfo(
    val connectionType: String,
    val isConnected: Boolean,
    val wifiSsid: String,
    val localIpv4: String,
    val localIpv6: String,
    val gateway: String,
    val netmask: String,
    val dnsServers: List<String>,
    val interfaces: List<InterfaceInfo>,
    val publicIp: String = "",
    val isp: String = "",
    val country: String = "",
    val city: String = "",
    val asn: String = ""
)

class NetworkInfoProvider(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun getLocalNetworkInfo(): DeviceNetworkInfo {
        var connectionType = "Disconnected"
        var isConnected = false
        var wifiSsid = "N/A"
        var gateway = "N/A"
        var netmask = "255.255.255.0"
        val dnsServers = mutableListOf<String>()

        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        val linkProperties = activeNetwork?.let { connectivityManager.getLinkProperties(it) }

        if (capabilities != null) {
            isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            connectionType = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "Active Network"
            }
        }

        // Get SSID if Wi-Fi
        if (connectionType == "Wi-Fi") {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val info = wifiManager?.connectionInfo
                val ssid = info?.ssid?.removeSurrounding("\"")
                if (!ssid.isNullOrBlank() && ssid != "<unknown ssid>") {
                    wifiSsid = ssid
                }
            } catch (_: Exception) {}
        }

        // LinkProperties: DNS & Routes (Gateway)
        if (linkProperties != null) {
            linkProperties.dnsServers.forEach { inetAddr ->
                inetAddr.hostAddress?.let { dnsServers.add(it) }
            }

            linkProperties.routes.forEach { route ->
                if (route.isDefaultRoute && route.gateway != null) {
                    val gw = route.gateway?.hostAddress
                    if (!gw.isNullOrBlank()) {
                        gateway = gw
                    }
                }
            }

            // Estimate netmask from link address
            linkProperties.linkAddresses.firstOrNull { it.address is Inet4Address }?.let { linkAddr ->
                val prefix = linkAddr.prefixLength
                netmask = prefixToNetmask(prefix)
            }
        }

        // Scan Network Interfaces
        val interfaces = mutableListOf<InterfaceInfo>()
        var primaryIpv4 = ""
        var primaryIpv6 = ""

        try {
            val netInterfaces = NetworkInterface.getNetworkInterfaces()
            while (netInterfaces.hasMoreElements()) {
                val intf = netInterfaces.nextElement()
                val ipv4List = mutableListOf<String>()
                val ipv6List = mutableListOf<String>()

                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address) {
                        val ip = addr.hostAddress ?: ""
                        if (ip.isNotBlank()) {
                            ipv4List.add(ip)
                            if (!addr.isLoopbackAddress && primaryIpv4.isBlank()) {
                                primaryIpv4 = ip
                            }
                        }
                    } else if (addr is Inet6Address) {
                        val ip = addr.hostAddress?.substringBefore("%") ?: ""
                        if (ip.isNotBlank()) {
                            ipv6List.add(ip)
                            if (!addr.isLoopbackAddress && primaryIpv6.isBlank() && !addr.isLinkLocalAddress) {
                                primaryIpv6 = ip
                            }
                        }
                    }
                }

                if (ipv4List.isNotEmpty() || ipv6List.isNotEmpty()) {
                    interfaces.add(
                        InterfaceInfo(
                            name = intf.name,
                            displayName = intf.displayName ?: intf.name,
                            ipv4 = ipv4List,
                            ipv6 = ipv6List,
                            isUp = intf.isUp,
                            isLoopback = intf.isLoopback
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        if (primaryIpv4.isBlank()) primaryIpv4 = "127.0.0.1"
        if (primaryIpv6.isBlank()) primaryIpv6 = "::1"
        if (dnsServers.isEmpty()) {
            dnsServers.addAll(listOf("8.8.8.8", "1.1.1.1"))
        }

        return DeviceNetworkInfo(
            connectionType = connectionType,
            isConnected = isConnected,
            wifiSsid = wifiSsid,
            localIpv4 = primaryIpv4,
            localIpv6 = primaryIpv6,
            gateway = gateway,
            netmask = netmask,
            dnsServers = dnsServers,
            interfaces = interfaces
        )
    }

    suspend fun fetchPublicIpInfo(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            // First attempt: ip-api.com
            try {
                val url = URL("http://ip-api.com/json/?fields=status,message,country,city,isp,query,as")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    requestMethod = "GET"
                }

                if (connection.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val body = reader.readText()
                    reader.close()
                    val json = JSONObject(body)
                    if (json.optString("status") == "success") {
                        return@runCatching mapOf(
                            "publicIp" to json.optString("query"),
                            "isp" to json.optString("isp"),
                            "country" to json.optString("country"),
                            "city" to json.optString("city"),
                            "asn" to json.optString("as")
                        )
                    }
                }
            } catch (_: Exception) {}

            // Fallback: ipify.org
            val fallbackUrl = URL("https://api.ipify.org?format=json")
            val conn = (fallbackUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
            }
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val text = reader.readText()
            reader.close()
            val json = JSONObject(text)
            val ip = json.optString("ip")
            mapOf(
                "publicIp" to ip,
                "isp" to "Internet Service Provider",
                "country" to "Global",
                "city" to "",
                "asn" to ""
            )
        }
    }

    private fun prefixToNetmask(prefix: Int): String {
        val mask = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        return "${(mask shr 24) and 0xFF}.${(mask shr 16) and 0xFF}.${(mask shr 8) and 0xFF}.${mask and 0xFF}"
    }
}
