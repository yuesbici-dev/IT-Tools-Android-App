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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.PortUnreachableException
import java.net.Socket
import java.net.SocketTimeoutException

enum class ScanProtocol {
    TCP, UDP
}

data class PortResult(
    val port: Int,
    val isOpen: Boolean,
    val protocol: ScanProtocol,
    val serviceName: String,
    val latencyMs: Long,
    val detail: String = ""
)

data class PortScanProgress(
    val scannedCount: Int,
    val totalPorts: Int,
    val openPortsCount: Int,
    val currentPort: Int,
    val latestResult: PortResult? = null,
    val isFinished: Boolean = false
)

object PortServiceDirectory {
    private val wellKnownServices = mapOf(
        20 to "FTP-Data (File Transfer)",
        21 to "FTP (Control)",
        22 to "SSH (Secure Shell)",
        23 to "Telnet",
        25 to "SMTP (Email Routing)",
        53 to "DNS (Domain Name System)",
        67 to "DHCP (Server)",
        68 to "DHCP (Client)",
        69 to "TFTP (Trivial FTP)",
        80 to "HTTP (Web Server)",
        88 to "Kerberos",
        110 to "POP3 (Post Office)",
        119 to "NNTP (Usenet)",
        123 to "NTP (Network Time)",
        135 to "MS RPC (EPMAP)",
        137 to "NetBIOS Name Service",
        138 to "NetBIOS Datagram",
        139 to "NetBIOS Session",
        143 to "IMAP (Mail Access)",
        161 to "SNMP (Network Management)",
        162 to "SNMP Trap",
        389 to "LDAP (Directory)",
        443 to "HTTPS (TLS/SSL)",
        445 to "SMB (Direct Host)",
        465 to "SMTPS (Secure Mail)",
        500 to "ISAKMP / IKE (IPsec)",
        514 to "Syslog",
        587 to "SMTP (Submission)",
        636 to "LDAPS (Secure LDAP)",
        873 to "Rsync",
        993 to "IMAPS (Secure IMAP)",
        995 to "POP3S (Secure POP3)",
        1080 to "SOCKS Proxy",
        1194 to "OpenVPN",
        1433 to "MS SQL Server",
        1521 to "Oracle Database",
        1723 to "PPTP VPN",
        1883 to "MQTT (IoT)",
        2049 to "NFS (Network File System)",
        2375 to "Docker REST API (Plain)",
        2376 to "Docker REST API (TLS)",
        3306 to "MySQL / MariaDB",
        3389 to "RDP (Remote Desktop)",
        5060 to "SIP (VoIP)",
        51820 to "WireGuard VPN",
        5353 to "mDNS (Multicast DNS)",
        5432 to "PostgreSQL",
        5900 to "VNC Remote Desktop",
        6379 to "Redis Cache",
        8000 to "HTTP Alternate / Dev",
        8080 to "HTTP Proxy / Web Alt",
        8443 to "HTTPS Alternate",
        8888 to "HTTP Alternate",
        9000 to "SonarQube / Portainer",
        9090 to "Prometheus / Cockpit",
        9200 to "Elasticsearch",
        27017 to "MongoDB"
    )

    fun getService(port: Int, customMap: Map<Int, String> = emptyMap()): String {
        return customMap[port] ?: wellKnownServices[port] ?: "Service on port $port"
    }

    val COMMON_PRESET = listOf(
        21, 22, 23, 25, 53, 80, 110, 123, 143, 443, 445, 587,
        993, 995, 1433, 1521, 3306, 3389, 5432, 5900, 6379, 8080, 8443, 27017
    )

    val WEB_PRESET = listOf(80, 443, 8000, 8080, 8443, 8888, 9000, 9090)
    val REMOTE_PRESET = listOf(22, 23, 3389, 5900, 1723, 1194, 51820)
    val DB_PRESET = listOf(1433, 1521, 3306, 5432, 6379, 9200, 27017)
}

class PortScanEngine {

    fun scanPorts(
        target: String,
        ports: List<Int>,
        protocol: ScanProtocol = ScanProtocol.TCP,
        timeoutMs: Int = 300,
        maxConcurrency: Int = 40,
        customServiceMap: Map<Int, String> = emptyMap()
    ): Flow<PortScanProgress> = flow {
        val cleanHost = target.trim().removePrefix("http://").removePrefix("https://").split("/").first()
        val inetAddress = try {
            InetAddress.getByName(cleanHost)
        } catch (e: Exception) {
            emit(
                PortScanProgress(
                    scannedCount = 0,
                    totalPorts = ports.size,
                    openPortsCount = 0,
                    currentPort = 0,
                    latestResult = PortResult(
                        port = 0,
                        isOpen = false,
                        protocol = protocol,
                        serviceName = "Error",
                        latencyMs = 0,
                        detail = "Cannot resolve target host: ${e.message}"
                    ),
                    isFinished = true
                )
            )
            return@flow
        }

        val total = ports.size
        var scanned = 0
        var openCount = 0
        val semaphore = Semaphore(maxConcurrency.coerceIn(5, 60))

        val channel = Channel<PortResult>(Channel.BUFFERED)

        coroutineScope {
            val scanJob = async(Dispatchers.IO) {
                ports.map { port ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            val result = when (protocol) {
                                ScanProtocol.TCP -> probeTcp(inetAddress, port, timeoutMs, customServiceMap)
                                ScanProtocol.UDP -> probeUdp(inetAddress, port, timeoutMs, customServiceMap)
                            }
                            channel.send(result)
                        }
                    }
                }.awaitAll()
                channel.close()
            }

            for (result in channel) {
                scanned++
                if (result.isOpen) openCount++
                emit(
                    PortScanProgress(
                        scannedCount = scanned,
                        totalPorts = total,
                        openPortsCount = openCount,
                        currentPort = result.port,
                        latestResult = result,
                        isFinished = false
                    )
                )
            }

            scanJob.await()
        }

        emit(
            PortScanProgress(
                scannedCount = total,
                totalPorts = total,
                openPortsCount = openCount,
                currentPort = ports.lastOrNull() ?: 0,
                latestResult = null,
                isFinished = true
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun probeTcp(
        target: InetAddress,
        port: Int,
        timeoutMs: Int,
        customMap: Map<Int, String>
    ): PortResult {
        val service = PortServiceDirectory.getService(port, customMap)
        val startTime = System.currentTimeMillis()
        var socket: Socket? = null
        return try {
            socket = Socket()
            socket.connect(InetSocketAddress(target, port), timeoutMs)
            val latency = System.currentTimeMillis() - startTime
            PortResult(
                port = port,
                isOpen = true,
                protocol = ScanProtocol.TCP,
                serviceName = service,
                latencyMs = latency,
                detail = "TCP Handshake Successful"
            )
        } catch (_: SocketTimeoutException) {
            val latency = System.currentTimeMillis() - startTime
            PortResult(
                port = port,
                isOpen = false,
                protocol = ScanProtocol.TCP,
                serviceName = service,
                latencyMs = latency,
                detail = "Timed Out / Filtered"
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            PortResult(
                port = port,
                isOpen = false,
                protocol = ScanProtocol.TCP,
                serviceName = service,
                latencyMs = latency,
                detail = e.message ?: "Connection Refused"
            )
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    private fun probeUdp(
        target: InetAddress,
        port: Int,
        timeoutMs: Int,
        customMap: Map<Int, String>
    ): PortResult {
        val service = PortServiceDirectory.getService(port, customMap)
        val startTime = System.currentTimeMillis()
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket()
            socket.soTimeout = timeoutMs

            val payload = if (port == 53) {
                byteArrayOf(0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
            } else {
                ByteArray(8)
            }

            val packet = DatagramPacket(payload, payload.size, target, port)
            socket.send(packet)

            val receiveBuf = ByteArray(512)
            val receivePacket = DatagramPacket(receiveBuf, receiveBuf.size)
            socket.receive(receivePacket)

            val latency = System.currentTimeMillis() - startTime
            PortResult(
                port = port,
                isOpen = true,
                protocol = ScanProtocol.UDP,
                serviceName = service,
                latencyMs = latency,
                detail = "UDP Response Received (${receivePacket.length} bytes)"
            )
        } catch (_: SocketTimeoutException) {
            val latency = System.currentTimeMillis() - startTime
            PortResult(
                port = port,
                isOpen = false,
                protocol = ScanProtocol.UDP,
                serviceName = service,
                latencyMs = latency,
                detail = "No response (Closed / Filtered)"
            )
        } catch (_: PortUnreachableException) {
            val latency = System.currentTimeMillis() - startTime
            PortResult(
                port = port,
                isOpen = false,
                protocol = ScanProtocol.UDP,
                serviceName = service,
                latencyMs = latency,
                detail = "ICMP Port Unreachable (Closed)"
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            PortResult(
                port = port,
                isOpen = false,
                protocol = ScanProtocol.UDP,
                serviceName = service,
                latencyMs = latency,
                detail = e.message ?: "Closed"
            )
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }
}
