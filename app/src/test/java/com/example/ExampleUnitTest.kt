package com.example

import com.example.network.PortServiceDirectory
import com.example.network.SubnetCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testIpv4SubnetCalculation() {
        val result = SubnetCalculator.calculateIpv4("192.168.1.100", 24).getOrNull()
        assertNotNull(result)
        assertEquals("192.168.1.0", result?.networkAddress)
        assertEquals("255.255.255.0", result?.netmask)
        assertEquals("0.0.0.255", result?.wildcardMask)
        assertEquals("192.168.1.255", result?.broadcastAddress)
        assertEquals("192.168.1.1", result?.firstUsableHost)
        assertEquals("192.168.1.254", result?.lastUsableHost)
        assertEquals(256L, result?.totalHosts)
        assertEquals(254L, result?.usableHosts)
        assertEquals("Class C", result?.ipClass)
        assertTrue(result?.ipType?.contains("Private") == true)
    }

    @Test
    fun testIpv6Calculation() {
        val result = SubnetCalculator.calculateIpv6("2001:db8::1", 64).getOrNull()
        assertNotNull(result)
        assertEquals("2001:0db8:0000:0000:0000:0000:0000:0001", result?.expandedAddress)
        assertTrue(result?.totalAddressesNotation?.contains("18446744073709551616") == true)
    }

    @Test
    fun testPortServiceDirectory() {
        assertEquals("SSH (Secure Shell)", PortServiceDirectory.getService(22))
        assertEquals("HTTP (Web Server)", PortServiceDirectory.getService(80))
        assertEquals("HTTPS (TLS/SSL)", PortServiceDirectory.getService(443))
        assertEquals("MySQL / MariaDB", PortServiceDirectory.getService(3306))
    }
}
