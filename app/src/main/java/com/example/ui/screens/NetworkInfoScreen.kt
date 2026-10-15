package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.network.InterfaceInfo
import com.example.ui.MainViewModel
import com.example.ui.components.MonospaceBadge
import com.example.ui.components.NetPulseTopBar
import com.example.ui.theme.DarkBlueContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.StatusEmerald

@Composable
fun NetworkInfoScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val networkInfo by viewModel.networkInfo.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshingNetInfo.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val copyToClipboard = { text: String, label: String ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        NetPulseTopBar(
            title = "Network Information",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { viewModel.refreshNetworkInfo() },
                    modifier = Modifier.testTag("net_info_refresh_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = if (isRefreshing) ElectricCyan else Slate100
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Public IP Card
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
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkBlueContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "PUBLIC IP & ISP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate400
                                )
                            }

                            if (!networkInfo?.publicIp.isNullOrBlank()) {
                                IconButton(
                                    onClick = { copyToClipboard(networkInfo?.publicIp ?: "", "Public IP") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = Slate400,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = networkInfo?.publicIp?.ifBlank { "Detecting public IP..." } ?: "N/A",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )

                        if (!networkInfo?.isp.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ISP: ${networkInfo?.isp} ${networkInfo?.asn?.let { "($it)" } ?: ""}",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                        }

                        if (!networkInfo?.country.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Location: ${listOfNotNull(networkInfo?.city, networkInfo?.country).filter { it.isNotBlank() }.joinToString(", ")}",
                                fontSize = 12.sp,
                                color = ElectricCyan
                            )
                        }
                    }
                }
            }

            // Local Device Network Parameters
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkBlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "INTERNAL NETWORK PARAMETERS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400
                            )
                        }

                        InfoRow("Connection Type", networkInfo?.connectionType ?: "Offline", onCopy = { copyToClipboard(it, "Connection Type") })
                        if (networkInfo?.wifiSsid != "N/A") {
                            InfoRow("Wi-Fi SSID", networkInfo?.wifiSsid ?: "", onCopy = { copyToClipboard(it, "Wi-Fi SSID") })
                        }
                        InfoRow("Internal IPv4", networkInfo?.localIpv4 ?: "127.0.0.1", isMono = true, onCopy = { copyToClipboard(it, "Internal IPv4") })
                        InfoRow("Internal IPv6", networkInfo?.localIpv6 ?: "::1", isMono = true, onCopy = { copyToClipboard(it, "Internal IPv6") })
                        InfoRow("Default Gateway", networkInfo?.gateway ?: "N/A", isMono = true, onCopy = { copyToClipboard(it, "Gateway") })
                        InfoRow("Subnet Mask", networkInfo?.netmask ?: "255.255.255.0", isMono = true, onCopy = { copyToClipboard(it, "Subnet Mask") })
                    }
                }
            }

            // Configured DNS Nameservers
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
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkBlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "CONFIGURED NAME SERVERS (DNS)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val dnsList = networkInfo?.dnsServers ?: emptyList()
                        if (dnsList.isEmpty()) {
                            Text("No DNS servers reported", fontSize = 12.sp, color = Slate400)
                        } else {
                            dnsList.forEachIndexed { idx, dns ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DNS Server ${idx + 1}",
                                        fontSize = 13.sp,
                                        color = Slate400
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = dns,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            color = ElectricCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = { copyToClipboard(dns, "DNS Server") },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = Slate400,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Network Interfaces Breakdown
            item {
                Text(
                    text = "NETWORK INTERFACES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            val interfaces = networkInfo?.interfaces ?: emptyList()
            items(interfaces, key = { it.name }) { iface ->
                InterfaceCard(iface = iface, onCopy = copyToClipboard)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    isMono: Boolean = false,
    onCopy: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = Slate400)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
                fontWeight = if (isMono) FontWeight.SemiBold else FontWeight.Normal,
                color = Slate100
            )
            IconButton(
                onClick = { onCopy(value) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = Slate400,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
fun InterfaceCard(iface: InterfaceInfo, onCopy: (String, String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (iface.isUp) StatusEmerald else Slate700)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = iface.name,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate100
                    )
                }
                MonospaceBadge(
                    text = if (iface.isLoopback) "LOOPBACK" else if (iface.isUp) "UP" else "DOWN",
                    color = if (iface.isUp) StatusEmerald else Slate400,
                    backgroundColor = Slate800
                )
            }

            iface.ipv4.forEach { ip ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("IPv4: $ip", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = ElectricCyan)
                    IconButton(onClick = { onCopy(ip, "IPv4") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Slate400, modifier = Modifier.size(12.dp))
                    }
                }
            }

            iface.ipv6.forEach { ip ->
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("IPv6: $ip", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate400)
                    IconButton(onClick = { onCopy(ip, "IPv6") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Slate400, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}
