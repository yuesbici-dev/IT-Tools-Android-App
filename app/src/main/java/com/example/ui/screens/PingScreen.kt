package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.PingHistoryItem
import com.example.data.model.SavedHost
import com.example.network.PingPacket
import com.example.ui.MainViewModel
import com.example.ui.components.MetricStatCard
import com.example.ui.components.NetPulseTopBar
import com.example.ui.components.SparklineChart
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PingScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sharedTarget by viewModel.sharedTarget.collectAsStateWithLifecycle()
    var targetHost by remember { mutableStateOf(sharedTarget.ifBlank { "8.8.8.8" }) }
    var packetCount by remember { mutableIntStateOf(0) } // Default: Continuous (0) as requested!
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Live Ping, 1: History

    val isPinging by viewModel.isPinging.collectAsStateWithLifecycle()
    val packets by viewModel.pingPackets.collectAsStateWithLifecycle()
    val stats by viewModel.pingStats.collectAsStateWithLifecycle()
    val historyList by viewModel.pingHistory.collectAsStateWithLifecycle()

    LaunchedEffect(sharedTarget) {
        if (sharedTarget.isNotBlank()) {
            targetHost = sharedTarget
        }
    }

    val rttValues = remember(packets) {
        packets.filter { it.isSuccess }.map { it.rttMs }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        NetPulseTopBar(
            title = "Ping Diagnostic",
            onBack = onBack,
            actions = {
                if (selectedTab == 0) {
                    IconButton(
                        onClick = {
                            viewModel.saveHost(
                                SavedHost(
                                    ip = targetHost.trim(),
                                    label = "Host ($targetHost)",
                                    notes = "Ping tested. Avg RTT: ${String.format("%.1f", stats?.avgRtt ?: 0.0)} ms",
                                    category = "General"
                                )
                            )
                        },
                        modifier = Modifier.testTag("ping_save_bookmark_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Save Target",
                            tint = ElectricCyan
                        )
                    }
                } else if (historyList.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearPingHistory() },
                        modifier = Modifier.testTag("ping_clear_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History",
                            tint = StatusCrimson
                        )
                    }
                }
            }
        )

        // Tab Row: Live Ping vs History
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Slate900,
            contentColor = ElectricCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ElectricCyan,
                    height = 2.5.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Live Ping", fontWeight = FontWeight.SemiBold)
                    }
                },
                selectedContentColor = ElectricCyan,
                unselectedContentColor = Slate400
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("History (${historyList.size})", fontWeight = FontWeight.SemiBold)
                    }
                },
                selectedContentColor = ElectricCyan,
                unselectedContentColor = Slate400
            )
        }

        if (selectedTab == 0) {
            // Live Ping Tab Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Target Input and Controls
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
                                value = targetHost,
                                onValueChange = { targetHost = it },
                                label = { Text("Host / IP Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("ping_target_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCyan,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Slate100,
                                    unfocusedTextColor = Slate100
                                )
                            )

                            // Quick recent targets carousel
                            if (historyList.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Recent: ", fontSize = 11.sp, color = Slate400)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(historyList.take(6)) { hist ->
                                            Box(
                                                modifier = Modifier
                                                    .background(Slate800, RoundedCornerShape(4.dp))
                                                    .clickable { targetHost = hist.host }
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = hist.host,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = ElectricCyan
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Packets", fontSize = 12.sp, color = Slate400)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(0, 4, 10, 30).forEach { count ->
                                        val label = if (count == 0) "Continuous" else "$count"
                                        FilterChip(
                                            selected = packetCount == count,
                                            onClick = { packetCount = count },
                                            label = { Text(label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ElectricCyan,
                                                selectedLabelColor = Slate950,
                                                containerColor = Slate800,
                                                labelColor = Slate100
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = packetCount == count,
                                                borderColor = Slate700,
                                                selectedBorderColor = ElectricCyan
                                            )
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (isPinging) {
                                        viewModel.stopPing()
                                    } else {
                                        viewModel.startPing(targetHost, packetCount)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("ping_action_btn"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPinging) StatusCrimson else ElectricCyan,
                                    contentColor = Slate950
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPinging) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPinging) "Stop Ping" else if (packetCount == 0) "Start Continuous Ping" else "Send $packetCount Packets",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Real-time Metrics Cards
                if (stats != null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricStatCard(
                                label = "Loss",
                                value = "${stats?.lossPercent ?: 0}%",
                                color = if ((stats?.lossPercent ?: 0) > 0) StatusCrimson else StatusEmerald,
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                label = "Min",
                                value = String.format("%.1f", stats?.minRtt ?: 0.0),
                                unit = "ms",
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                label = "Avg",
                                value = String.format("%.1f", stats?.avgRtt ?: 0.0),
                                unit = "ms",
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                label = "Max",
                                value = String.format("%.1f", stats?.maxRtt ?: 0.0),
                                unit = "ms",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // RTT Sparkline
                if (rttValues.size >= 2) {
                    item {
                        Column {
                            Text(
                                text = "LATENCY TIMELINE (RTT ms)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            SparklineChart(values = rttValues)
                        }
                    }
                }

                // Packet Log Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PACKET STREAM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )
                        Text(
                            text = "${packets.size} packets",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ElectricCyan
                        )
                    }
                }

                // Packet List
                if (packets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ready to ping target host", color = Slate400, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(packets.reversed(), key = { "${it.seq}_${it.message.hashCode()}" }) { packet ->
                        PingPacketRow(packet)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        } else {
            // History Tab Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (historyList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.History, contentDescription = null, tint = Slate700, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No ping history yet", color = Slate400, fontSize = 14.sp)
                                Text("Tested hosts will appear here for fast re-pinging", color = Slate700, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(historyList, key = { it.id }) { item ->
                        PingHistoryCard(
                            history = item,
                            onPingAgain = {
                                targetHost = item.host
                                selectedTab = 0
                                viewModel.startPing(item.host, 0)
                            },
                            onDelete = { viewModel.deletePingHistory(item.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun PingHistoryCard(
    history: PingHistoryItem,
    onPingAgain: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(history.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        sdf.format(Date(history.timestamp))
    }

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = history.host,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Slate100
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$dateStr • ${history.packetsReceived}/${history.packetsTransmitted} pkts",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                    if (history.avgRttMs > 0.0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${String.format("%.1f", history.avgRttMs)} ms avg",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ElectricCyan
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onPingAgain,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Slate950),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("Ping", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Slate400,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PingPacketRow(packet: PingPacket) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (packet.isSuccess) Slate800 else StatusCrimson.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (packet.isSuccess) StatusEmerald else StatusCrimson)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "#${packet.seq}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = packet.message,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = if (packet.isSuccess) Slate100 else StatusCrimson
                )
            }

            if (packet.isSuccess) {
                Text(
                    text = "${String.format("%.1f", packet.rttMs)} ms",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
