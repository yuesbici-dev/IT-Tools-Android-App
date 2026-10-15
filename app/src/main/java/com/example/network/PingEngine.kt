package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class PingPacket(
    val seq: Int,
    val ip: String,
    val bytes: Int = 64,
    val rttMs: Double,
    val ttl: Int = 64,
    val isSuccess: Boolean,
    val message: String
)

data class PingStats(
    val transmitted: Int,
    val received: Int,
    val lossPercent: Int,
    val minRtt: Double,
    val avgRtt: Double,
    val maxRtt: Double,
    val jitter: Double
)

class PingEngine {

    fun executePing(
        target: String,
        count: Int,
        intervalMs: Long = 800L,
        timeoutSec: Int = 2
    ): Flow<PingPacket> = flow {
        val cleanHost = target.trim().removePrefix("http://").removePrefix("https://").split("/").first()
        val resolvedIp = withContext(Dispatchers.IO) {
            try {
                InetAddress.getByName(cleanHost).hostAddress ?: cleanHost
            } catch (e: Exception) {
                cleanHost
            }
        }

        val totalToRun = if (count <= 0) 1000 else count
        for (i in 1..totalToRun) {
            val packet = pingSingle(cleanHost, resolvedIp, i, timeoutSec)
            emit(packet)
            if (i < totalToRun) {
                delay(intervalMs)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun pingSingle(host: String, resolvedIp: String, seq: Int, timeoutSec: Int): PingPacket {
        // Try OS ping binary first
        try {
            val process = ProcessBuilder(
                "/system/bin/ping",
                "-c", "1",
                "-W", timeoutSec.toString(),
                host
            ).redirectErrorStream(true).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var rtt = -1.0
            var ttl = 64
            var bytes = 64

            while (reader.readLine().also { line = it } != null) {
                val l = line?.lowercase() ?: continue
                if (l.contains("time=") || l.contains("time<")) {
                    val timePart = l.substringAfter("time=").substringBefore(" ms").substringBefore("ms").trim()
                    rtt = timePart.toDoubleOrNull() ?: 1.0
                    if (l.contains("ttl=")) {
                        ttl = l.substringAfter("ttl=").substringBefore(" ").trim().toIntOrNull() ?: 64
                    }
                    if (l.contains("bytes from")) {
                        bytes = l.substringBefore(" bytes from").substringAfterLast(" ").trim().toIntOrNull() ?: 64
                    }
                    break
                }
            }
            process.waitFor()

            if (rtt >= 0.0) {
                return PingPacket(
                    seq = seq,
                    ip = resolvedIp,
                    bytes = bytes,
                    rttMs = rtt,
                    ttl = ttl,
                    isSuccess = true,
                    message = "$bytes bytes from $resolvedIp: icmp_seq=$seq ttl=$ttl time=${String.format(Locale.US, "%.2f", rtt)} ms"
                )
            }
        } catch (_: Exception) {
            // Fallback to socket latency probe
        }

        // Fallback: TCP Socket connect latency probe on common port or isReachable
        val startTime = System.nanoTime()
        return try {
            val socket = Socket()
            val socketAddress = InetSocketAddress(host, 80)
            socket.connect(socketAddress, timeoutSec * 1000)
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0
            socket.close()
            PingPacket(
                seq = seq,
                ip = resolvedIp,
                bytes = 32,
                rttMs = elapsedMs,
                ttl = 64,
                isSuccess = true,
                message = "TCP probe to $resolvedIp: seq=$seq time=${String.format(Locale.US, "%.2f", elapsedMs)} ms"
            )
        } catch (_: Exception) {
            // Try isReachable fallback
            try {
                val start = System.nanoTime()
                val reachable = InetAddress.getByName(host).isReachable(timeoutSec * 1000)
                val elapsed = (System.nanoTime() - start) / 1_000_000.0
                if (reachable) {
                    PingPacket(
                        seq = seq,
                        ip = resolvedIp,
                        bytes = 32,
                        rttMs = elapsed,
                        ttl = 64,
                        isSuccess = true,
                        message = "Reachable: $resolvedIp: seq=$seq time=${String.format(Locale.US, "%.2f", elapsed)} ms"
                    )
                } else {
                    PingPacket(
                        seq = seq,
                        ip = resolvedIp,
                        bytes = 0,
                        rttMs = 0.0,
                        ttl = 0,
                        isSuccess = false,
                        message = "Request timeout for icmp_seq $seq"
                    )
                }
            } catch (ex: Exception) {
                PingPacket(
                    seq = seq,
                    ip = resolvedIp,
                    bytes = 0,
                    rttMs = 0.0,
                    ttl = 0,
                    isSuccess = false,
                    message = "Destination unreachable: ${ex.message ?: "Host unreachable"}"
                )
            }
        }
    }

    companion object {
        fun calculateStats(packets: List<PingPacket>): PingStats {
            if (packets.isEmpty()) {
                return PingStats(0, 0, 0, 0.0, 0.0, 0.0, 0.0)
            }
            val transmitted = packets.size
            val successful = packets.filter { it.isSuccess }
            val received = successful.size
            val lossPercent = (((transmitted - received).toDouble() / transmitted) * 100).roundToInt()

            if (successful.isEmpty()) {
                return PingStats(transmitted, 0, 100, 0.0, 0.0, 0.0, 0.0)
            }

            val rtts = successful.map { it.rttMs }
            val minRtt = rtts.minOrNull() ?: 0.0
            val maxRtt = rtts.maxOrNull() ?: 0.0
            val avgRtt = rtts.average()

            var jitterSum = 0.0
            for (i in 1 until rtts.size) {
                jitterSum += abs(rtts[i] - rtts[i - 1])
            }
            val jitter = if (rtts.size > 1) jitterSum / (rtts.size - 1) else 0.0

            return PingStats(
                transmitted = transmitted,
                received = received,
                lossPercent = lossPercent,
                minRtt = minRtt,
                avgRtt = avgRtt,
                maxRtt = maxRtt,
                jitter = jitter
            )
        }
    }
}
