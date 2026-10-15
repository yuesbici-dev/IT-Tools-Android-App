package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WolEngine {

    suspend fun sendMagicPacket(
        macAddress: String,
        broadcastIp: String = "255.255.255.255",
        port: Int = 9
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanMac = macAddress.trim()
                .replace(":", "")
                .replace("-", "")
                .replace(".", "")
                .uppercase()

            if (cleanMac.length != 12) {
                throw IllegalArgumentException("Invalid MAC address. Must be 12 hex characters (e.g. AA:BB:CC:DD:EE:FF).")
            }

            val macBytes = ByteArray(6)
            for (i in 0 until 6) {
                macBytes[i] = cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }

            // Construct 102-byte magic packet: 6 bytes 0xFF followed by 16 repetitions of 6 MAC bytes
            val magicPacket = ByteArray(102)
            for (i in 0 until 6) {
                magicPacket[i] = 0xFF.toByte()
            }
            var offset = 6
            for (i in 0 until 16) {
                System.arraycopy(macBytes, 0, magicPacket, offset, 6)
                offset += 6
            }

            val broadcastTarget = InetAddress.getByName(broadcastIp.ifBlank { "255.255.255.255" })
            val datagramSocket = DatagramSocket()
            datagramSocket.broadcast = true

            val packet = DatagramPacket(magicPacket, magicPacket.size, broadcastTarget, port)
            datagramSocket.send(packet)
            datagramSocket.close()

            val formattedMac = cleanMac.chunked(2).joinToString(":")
            "Magic Packet successfully broadcast to $formattedMac on $broadcastIp:$port"
        }
    }
}
