package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SavedHost
import com.example.network.LanDevice
import com.example.network.SubnetCalculator
import com.example.ui.MainViewModel
import com.example.ui.components.NetPulseTopBar
import com.example.ui.theme.DarkBlueContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald

@Composable
fun LanScannerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val networkInfo by viewModel.networkInfo.collectAsStateWithLifecycle()
    val isScanning by viewModel.isLanScanning.collectAsStateWithLifecycle()
    val progress by viewModel.lanScanProgress.collectAsStateWithLifecycle()
    val activeDevices by viewModel.lanDevices.collectAsStateWithLifecycle()

    var customSubnetIp by remember(networkInfo) {
        mutableStateOf(networkInfo?.localIpv4 ?: "192.168.1.1")
    }
    var cidrFloat by remember { mutableFloatStateOf(24f) }

    val currentCidr = cidrFloat.toInt()
    val subnetPreview = remember(customSubnetIp, currentCidr) {
        SubnetCalculator.calculateIpv4(customSubnetIp, currentCidr).getOrNull()
    }

    val presetCidrs = listOf(8, 16, 24, 26, 27, 28, 29, 30, 32)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        NetPulseTopBar(
            title = "Subnet LAN Scanner",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Scanner Control Card with Subnet Slider (identical to IPv4 Subnet Calculator)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customSubnetIp,
                            onValueChange = { customSubnetIp = it },
                            label = { Text("Base Local IP (e.g. 192.168.1.1)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("lan_scan_base_ip"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Slate100,
                                unfocusedTextColor = Slate100
                            )
                        )

                        // Subnet CIDR Slider (the exact same subnet slider as in IPv4 Calculator)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Subnet Prefix Length", fontSize = 12.sp, color = Slate400)
                            Text(
                                text = "/$currentCidr (${subnetPreview?.usableHosts ?: 254} hosts)",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                        }

                        Slider(
                            value = cidrFloat,
                            onValueChange = { cidrFloat = it },
                            valueRange = 0f..32f,
                            steps = 31,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan,
                                inactiveTrackColor = Slate700
                            ),
                            modifier = Modifier.testTag("lan_scan_cidr_slider")
                        )

                        // Preset CIDR Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(presetCidrs) { p ->
                                val isSelected = currentCidr == p
                                Button(
                                    onClick = { cidrFloat = p.toFloat() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) ElectricCyan else Slate800,
                                        contentColor = if (isSelected) Slate950 else Slate100
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("/$p", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        if (subnetPreview != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate950, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Range: ${subnetPreview.firstUsableHost} — ${subnetPreview.lastUsableHost}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Slate100
                                    )
                                    Text(
                                        text = "Mask: ${subnetPreview.netmask} | Broadcast: ${subnetPreview.broadcastAddress}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Slate400
                                    )
                                    if (currentCidr < 24) {
                                        Text(
                                            text = "Note: Discovers first 512 hosts in large subnet for fast response.",
                                            fontSize = 10.sp,
                                            color = ElectricCyan
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (isScanning) {
                                    viewModel.stopLanScan()
                                } else {
                                    viewModel.startLanScan(customSubnetIp, currentCidr)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("lan_scan_action_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanning) StatusCrimson else ElectricCyan,
                                contentColor = Slate950
                            )
                        ) {
                            Icon(
                                imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isScanning) "Stop Discovery" else "Scan Subnet (/$currentCidr)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Progress Bar
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isScanning) "Probing subnet nodes..." else "Discovered ${activeDevices.size} active devices",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ElectricCyan,
                        trackColor = Slate800,
                    )
                }
            }

            // Discovered Devices List
            if (activeDevices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isScanning) "Discovering active nodes on LAN..." else "Tap 'Scan Subnet' to find devices",
                            color = Slate400,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(activeDevices, key = { it.ip }) { device ->
                    LanDeviceCard(
                        device = device,
                        onPing = { viewModel.navigateTo("ping", device.ip) },
                        onPortScan = { viewModel.navigateTo("port_scan", device.ip) },
                        onBookmark = {
                            viewModel.saveHost(
                                SavedHost(
                                    ip = device.ip,
                                    label = device.hostName,
                                    notes = "Discovered via LAN Scanner (Latency: ${device.latencyMs} ms)",
                                    category = "Home Network"
                                )
                            )
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun LanDeviceCard(
    device: LanDevice,
    onPing: () -> Unit,
    onPortScan: () -> Unit,
    onBookmark: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusEmerald)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = device.ip,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Slate100
                    )
                    Text(
                        text = device.hostName,
                        fontSize = 12.sp,
                        color = Slate400,
                        maxLines = 1
                    )
                    Text(
                        text = "${device.latencyMs} ms response",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ElectricCyan
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onPing, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Radar, contentDescription = "Ping", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onPortScan, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Security, contentDescription = "Port Scan", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onBookmark, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = "Bookmark", tint = Slate400, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
