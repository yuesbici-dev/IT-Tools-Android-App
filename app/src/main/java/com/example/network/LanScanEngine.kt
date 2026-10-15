package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

data class LanDevice(
    val ip: String,
    val hostName: String,
    val latencyMs: Long,
    val isReachable: Boolean,
    val vendorOrType: String = "Network Device"
)

data class LanScanProgress(
    val scanned: Int,
    val total: Int,
    val activeDevices: List<LanDevice>,
    val currentSubnet: String,
    val isFinished: Boolean
)

class LanScanEngine {

    fun scanSubnet(
        localIp: String,
        cidr: Int = 24,
        timeoutMs: Int = 300,
        concurrency: Int = 35
    ): Flow<LanScanProgress> = flow {
        val cleanIp = localIp.trim().split("/").first()
        val subnetCalc = SubnetCalculator.calculateIpv4(cleanIp, cidr).getOrNull()
        if (subnetCalc == null) {
            emit(LanScanProgress(0, 0, emptyList(), "Invalid IP or CIDR", true))
            return@flow
        }

        val firstLong = ipToLong(subnetCalc.firstUsableHost)
        val lastLong = ipToLong(subnetCalc.lastUsableHost)
        val targetIps = if (firstLong <= lastLong) {
            (firstLong..lastLong).map { longToIp(it) }.take(512)
        } else {
            listOf(cleanIp)
        }

        val totalHosts = targetIps.size
        val semaphore = Semaphore(concurrency)
        val channel = Channel<LanDevice?>(Channel.BUFFERED)
        val discoveredList = mutableListOf<LanDevice>()
        val subnetDescriptor = "${subnetCalc.networkAddress}/$cidr"

        coroutineScope {
            val scanJob = async(Dispatchers.IO) {
                targetIps.forEach { targetIp ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val dev = probeHost(targetIp, timeoutMs)
                            channel.send(dev)
                        }
                    }
                }
                // Wait for all to complete then close channel
                channel.close()
            }

            var scanned = 0
            for (dev in channel) {
                scanned++
                if (dev != null) {
                    discoveredList.add(dev)
                }
                if (scanned % 5 == 0 || scanned == totalHosts) {
                    emit(
                        LanScanProgress(
                            scanned = scanned,
                            total = totalHosts,
                            activeDevices = discoveredList.sortedBy { ipToSortableLong(it.ip) },
                            currentSubnet = subnetDescriptor,
                            isFinished = false
                        )
                    )
                }
            }
            scanJob.await()
        }

        emit(
            LanScanProgress(
                scanned = totalHosts,
                total = totalHosts,
                activeDevices = discoveredList.sortedBy { ipToSortableLong(it.ip) },
                currentSubnet = subnetDescriptor,
                isFinished = true
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun probeHost(ip: String, timeoutMs: Int): LanDevice? {
        val start = System.currentTimeMillis()

        // Probe 1: InetAddress isReachable
        try {
            val addr = InetAddress.getByName(ip)
            if (addr.isReachable(timeoutMs)) {
                val latency = (System.currentTimeMillis() - start).coerceAtLeast(1)
                val hostName = try { addr.canonicalHostName } catch (_: Exception) { ip }
                return LanDevice(ip, if (hostName == ip) "Local Host" else hostName, latency, true)
            }
        } catch (_: Exception) {}

        // Probe 2: Quick socket connect to common local service ports (80, 443, 53, 445, 22)
        val commonPorts = intArrayOf(80, 443, 53, 445, 22)
        for (port in commonPorts) {
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(ip, port), timeoutMs / 2)
                val latency = (System.currentTimeMillis() - start).coerceAtLeast(1)
                val hostName = try { InetAddress.getByName(ip).hostName } catch (_: Exception) { ip }
                return LanDevice(
                    ip = ip,
                    hostName = if (hostName == ip) "Active Node (port $port)" else hostName,
                    latencyMs = latency,
                    isReachable = true
                )
            } catch (_: Exception) {
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }

        return null
    }

    private fun ipToLong(ip: String): Long {
        return try {
            val parts = ip.split(".").map { it.toLong() }
            (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
        } catch (_: Exception) {
            0L
        }
    }

    private fun longToIp(ipLong: Long): String {
        return "${(ipLong shr 24) and 0xFF}.${(ipLong shr 16) and 0xFF}.${(ipLong shr 8) and 0xFF}.${ipLong and 0xFF}"
    }

    private fun ipToSortableLong(ip: String): Long = ipToLong(ip)
}
