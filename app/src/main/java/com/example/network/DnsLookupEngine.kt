package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.URLEncoder

data class DnsRecord(
    val name: String,
    val type: String,
    val data: String,
    val ttl: Int
)

data class DnsQueryResult(
    val domain: String,
    val serverUsed: String,
    val status: String,
    val records: List<DnsRecord>,
    val queryTimeMs: Long
)

class DnsLookupEngine {

    suspend fun queryDns(
        domain: String,
        recordType: String = "A",
        dnsServerIp: String = "1.1.1.1"
    ): Result<DnsQueryResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val cleanDomain = domain.trim().removePrefix("http://").removePrefix("https://").split("/").first()

        runCatching {
            // If querying standard A/AAAA via system default DNS
            if (dnsServerIp.isBlank() || dnsServerIp == "System Default") {
                val addrs = InetAddress.getAllByName(cleanDomain)
                val records = addrs.map { addr ->
                    DnsRecord(
                        name = cleanDomain,
                        type = if (addr.hostAddress?.contains(":") == true) "AAAA" else "A",
                        data = addr.hostAddress ?: "",
                        ttl = 300
                    )
                }
                return@runCatching DnsQueryResult(
                    domain = cleanDomain,
                    serverUsed = "Device Default Resolver",
                    status = "NOERROR",
                    records = records,
                    queryTimeMs = System.currentTimeMillis() - startTime
                )
            }

            // Using DoH / DNS endpoint (Cloudflare 1.1.1.1 or Google 8.8.8.8) or custom DNS query
            val dohEndpoint = when (dnsServerIp.trim()) {
                "8.8.8.8", "8.8.4.4" -> "https://dns.google/resolve?name="
                else -> "https://cloudflare-dns.com/dns-query?name="
            }

            val encodedDomain = URLEncoder.encode(cleanDomain, "UTF-8")
            val encodedType = URLEncoder.encode(recordType, "UTF-8")
            val url = URL("$dohEndpoint$encodedDomain&type=$encodedType")

            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/dns-json")
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val body = reader.readText()
                reader.close()

                val json = JSONObject(body)
                val statusInt = json.optInt("Status", 0)
                val statusText = when (statusInt) {
                    0 -> "NOERROR"
                    1 -> "FORMERR"
                    2 -> "SERVFAIL"
                    3 -> "NXDOMAIN"
                    4 -> "NOTIMP"
                    5 -> "REFUSED"
                    else -> "CODE_$statusInt"
                }

                val records = mutableListOf<DnsRecord>()
                val answers = json.optJSONArray("Answer")
                if (answers != null) {
                    for (i in 0 until answers.length()) {
                        val obj = answers.getJSONObject(i)
                        val typeNum = obj.optInt("type")
                        val typeName = recordTypeFromNumber(typeNum)
                        val data = obj.optString("data")
                        val ttl = obj.optInt("TTL", 300)
                        val name = obj.optString("name", cleanDomain)
                        records.add(DnsRecord(name, typeName, data, ttl))
                    }
                }

                DnsQueryResult(
                    domain = cleanDomain,
                    serverUsed = dnsServerIp,
                    status = statusText,
                    records = records,
                    queryTimeMs = System.currentTimeMillis() - startTime
                )
            } else {
                // Fallback to standard inet query
                val addrs = InetAddress.getAllByName(cleanDomain)
                val records = addrs.map { addr ->
                    DnsRecord(
                        name = cleanDomain,
                        type = if (addr.hostAddress?.contains(":") == true) "AAAA" else "A",
                        data = addr.hostAddress ?: "",
                        ttl = 300
                    )
                }
                DnsQueryResult(
                    domain = cleanDomain,
                    serverUsed = dnsServerIp,
                    status = "FALLBACK_RESOLVED",
                    records = records,
                    queryTimeMs = System.currentTimeMillis() - startTime
                )
            }
        }
    }

    private fun recordTypeFromNumber(type: Int): String {
        return when (type) {
            1 -> "A"
            2 -> "NS"
            5 -> "CNAME"
            6 -> "SOA"
            12 -> "PTR"
            15 -> "MX"
            16 -> "TXT"
            28 -> "AAAA"
            else -> "TYPE_$type"
        }
    }
}
