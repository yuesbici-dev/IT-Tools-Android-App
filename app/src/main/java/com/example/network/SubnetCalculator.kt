package com.example.network

import java.math.BigInteger
import java.net.Inet6Address
import java.net.InetAddress

data class Ipv4SubnetResult(
    val ip: String,
    val cidr: Int,
    val netmask: String,
    val wildcardMask: String,
    val networkAddress: String,
    val broadcastAddress: String,
    val firstUsableHost: String,
    val lastUsableHost: String,
    val totalHosts: Long,
    val usableHosts: Long,
    val ipClass: String,
    val ipType: String,
    val binaryIp: String,
    val binaryNetmask: String
)

data class Ipv6SubnetResult(
    val inputAddress: String,
    val prefixLength: Int,
    val expandedAddress: String,
    val compressedAddress: String,
    val prefixRangeStart: String,
    val prefixRangeEnd: String,
    val totalAddressesNotation: String,
    val scopeType: String
)

object SubnetCalculator {

    fun calculateIpv4(ipStr: String, cidr: Int): Result<Ipv4SubnetResult> {
        return runCatching {
            val parts = ipStr.trim().split(".")
            if (parts.size != 4) throw IllegalArgumentException("Invalid IPv4 format. Expected 4 octets.")
            val octets = parts.map {
                val num = it.toInt()
                if (num !in 0..255) throw IllegalArgumentException("Octet $num out of range (0-255)")
                num
            }

            val prefix = cidr.coerceIn(0, 32)
            val ipLong = (octets[0].toLong() shl 24) or
                    (octets[1].toLong() shl 16) or
                    (octets[2].toLong() shl 8) or
                    octets[3].toLong()

            val maskLong = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
            val wildcardLong = maskLong.inv() and 0xFFFFFFFFL

            val networkLong = ipLong and maskLong
            val broadcastLong = if (prefix >= 31) ipLong else networkLong or wildcardLong

            val totalHosts = if (prefix == 32) 1L else 1L shl (32 - prefix)
            val usableHosts = when {
                prefix >= 31 -> totalHosts
                else -> (totalHosts - 2).coerceAtLeast(0)
            }

            val firstUsableLong = if (prefix >= 31) networkLong else networkLong + 1
            val lastUsableLong = if (prefix >= 31) broadcastLong else broadcastLong - 1

            val firstOctet = octets[0]
            val ipClass = when (firstOctet) {
                in 1..126 -> "Class A"
                127 -> "Loopback (Class A)"
                in 128..191 -> "Class B"
                in 192..223 -> "Class C"
                in 224..239 -> "Class D (Multicast)"
                in 240..255 -> "Class E (Experimental)"
                else -> "Unknown"
            }

            val ipType = when {
                octets[0] == 10 -> "Private (RFC 1918)"
                octets[0] == 172 && octets[1] in 16..31 -> "Private (RFC 1918)"
                octets[0] == 192 && octets[1] == 168 -> "Private (RFC 1918)"
                octets[0] == 127 -> "Loopback"
                octets[0] == 169 && octets[1] == 254 -> "Link-Local (APIPA)"
                octets[0] in 224..239 -> "Multicast"
                else -> "Public Routable"
            }

            val binaryIp = octets.joinToString(".") { it.toString(2).padStart(8, '0') }
            val maskOctets = longToOctets(maskLong)
            val binaryNetmask = maskOctets.joinToString(".") { it.toString(2).padStart(8, '0') }

            Ipv4SubnetResult(
                ip = ipStr.trim(),
                cidr = prefix,
                netmask = formatIp(maskLong),
                wildcardMask = formatIp(wildcardLong),
                networkAddress = formatIp(networkLong),
                broadcastAddress = formatIp(broadcastLong),
                firstUsableHost = formatIp(firstUsableLong),
                lastUsableHost = formatIp(lastUsableLong),
                totalHosts = totalHosts,
                usableHosts = usableHosts,
                ipClass = ipClass,
                ipType = ipType,
                binaryIp = binaryIp,
                binaryNetmask = binaryNetmask
            )
        }
    }

    fun calculateIpv6(ipStr: String, prefixLength: Int): Result<Ipv6SubnetResult> {
        return runCatching {
            val trimmed = ipStr.trim()
            val addr = InetAddress.getByName(trimmed)
            if (addr !is Inet6Address) {
                throw IllegalArgumentException("Provided address is not a valid IPv6 address")
            }
            val bytes = addr.address
            val prefix = prefixLength.coerceIn(0, 128)

            // Expanded 8 groups
            val expandedGroups = (0 until 8).map { i ->
                val b1 = bytes[i * 2].toInt() and 0xFF
                val b2 = bytes[i * 2 + 1].toInt() and 0xFF
                String.format("%02x%02x", b1, b2)
            }
            val expanded = expandedGroups.joinToString(":")
            val compressed = addr.hostAddress ?: trimmed

            // Scope type
            val scopeType = when {
                addr.isLoopbackAddress -> "Loopback (::1)"
                addr.isLinkLocalAddress -> "Link-Local Unicast (fe80::/10)"
                addr.isMulticastAddress -> "Multicast (ff00::/8)"
                expanded.startsWith("fc") || expanded.startsWith("fd") -> "Unique Local Unicast (fc00::/7)"
                expanded.startsWith("2") || expanded.startsWith("3") -> "Global Unicast (2000::/3)"
                else -> "IPv6 Unicast / Reserved"
            }

            val hostBits = 128 - prefix
            val totalAddressesNotation = when {
                hostBits == 0 -> "1"
                hostBits <= 64 -> BigInteger.valueOf(2).pow(hostBits).toString()
                else -> "2^$hostBits (~${BigInteger.valueOf(2).pow(64)} × 2^${hostBits - 64})"
            }

            // Calculate start and end prefix
            val maskBytes = ByteArray(16)
            for (i in 0 until 16) {
                val bitOffset = i * 8
                val bitsInByte = (prefix - bitOffset).coerceIn(0, 8)
                maskBytes[i] = if (bitsInByte == 0) 0 else ((0xFF shl (8 - bitsInByte)) and 0xFF).toByte()
            }

            val startBytes = ByteArray(16)
            val endBytes = ByteArray(16)
            for (i in 0 until 16) {
                startBytes[i] = (bytes[i].toInt() and maskBytes[i].toInt()).toByte()
                endBytes[i] = (bytes[i].toInt() or (maskBytes[i].toInt().inv() and 0xFF)).toByte()
            }

            val startAddr = InetAddress.getByAddress(startBytes).hostAddress ?: "::"
            val endAddr = InetAddress.getByAddress(endBytes).hostAddress ?: "::"

            Ipv6SubnetResult(
                inputAddress = trimmed,
                prefixLength = prefix,
                expandedAddress = expanded,
                compressedAddress = compressed,
                prefixRangeStart = startAddr,
                prefixRangeEnd = endAddr,
                totalAddressesNotation = totalAddressesNotation,
                scopeType = scopeType
            )
        }
    }

    private fun formatIp(ipLong: Long): String {
        return "${(ipLong shr 24) and 0xFF}.${(ipLong shr 16) and 0xFF}.${(ipLong shr 8) and 0xFF}.${ipLong and 0xFF}"
    }

    private fun longToOctets(value: Long): List<Int> {
        return listOf(
            ((value shr 24) and 0xFF).toInt(),
            ((value shr 16) and 0xFF).toInt(),
            ((value shr 8) and 0xFF).toInt(),
            (value and 0xFF).toInt()
        )
    }
}
