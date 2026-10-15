package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress

data class TraceHop(
    val hopNumber: Int,
    val ip: String,
    val hostName: String,
    val rttMs: Double,
    val isDestinationReached: Boolean,
    val isTimeout: Boolean
)

class TracerouteEngine {

    fun executeTraceroute(
        target: String,
        maxHops: Int = 30,
        timeoutSec: Int = 2
    ): Flow<TraceHop> = flow {
        val cleanHost = target.trim().removePrefix("http://").removePrefix("https://").split("/").first()
        val destinationIp = withContext(Dispatchers.IO) {
            try {
                InetAddress.getByName(cleanHost).hostAddress ?: cleanHost
            } catch (e: Exception) {
                cleanHost
            }
        }

        for (ttl in 1..maxHops) {
            val hop = probeHop(cleanHost, destinationIp, ttl, timeoutSec)
            emit(hop)
            if (hop.isDestinationReached) {
                break
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun probeHop(target: String, targetIp: String, ttl: Int, timeoutSec: Int): TraceHop {
        try {
            val startTime = System.currentTimeMillis()
            val process = ProcessBuilder(
                "/system/bin/ping",
                "-c", "1",
                "-t", ttl.toString(),
                "-W", timeoutSec.toString(),
                target
            ).redirectErrorStream(true).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var hopIp = ""
            var rtt = -1.0
            var isDest = false

            while (reader.readLine().also { line = it } != null) {
                val l = line?.lowercase() ?: continue
                if (l.contains("time to live exceeded") || l.contains("ttl exceeded")) {
                    // Example: "From 192.168.1.1: icmp_seq=1 Time to live exceeded"
                    hopIp = l.substringAfter("from ").substringBefore(":").substringBefore(" ").trim()
                    rtt = (System.currentTimeMillis() - startTime).toDouble()
                    break
                } else if (l.contains("bytes from") && (l.contains("time=") || l.contains("time<"))) {
                    // Reached destination!
                    hopIp = l.substringBefore(" bytes from").substringAfterLast(" ").trim().ifEmpty { targetIp }
                    val timePart = l.substringAfter("time=").substringBefore(" ms").substringBefore("ms").trim()
                    rtt = timePart.toDoubleOrNull() ?: (System.currentTimeMillis() - startTime).toDouble()
                    isDest = true
                    break
                }
            }
            process.waitFor()

            if (hopIp.isNotBlank()) {
                val hostName = try {
                    InetAddress.getByName(hopIp).canonicalHostName
                } catch (_: Exception) {
                    hopIp
                }
                if (hopIp == targetIp) {
                    isDest = true
                }
                return TraceHop(
                    hopNumber = ttl,
                    ip = hopIp,
                    hostName = hostName,
                    rttMs = rtt.coerceAtLeast(0.5),
                    isDestinationReached = isDest,
                    isTimeout = false
                )
            }
        } catch (_: Exception) {}

        return TraceHop(
            hopNumber = ttl,
            ip = "*",
            hostName = "Request timed out",
            rttMs = 0.0,
            isDestinationReached = false,
            isTimeout = true
        )
    }
}
