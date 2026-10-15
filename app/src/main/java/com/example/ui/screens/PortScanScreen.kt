package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomPortService
import com.example.data.model.SavedHost
import com.example.network.PortResult
import com.example.network.PortServiceDirectory
import com.example.network.ScanProtocol
import com.example.ui.MainViewModel
import com.example.ui.components.NetPulseTopBar
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald

@Composable
fun PortScanScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sharedTarget by viewModel.sharedTarget.collectAsStateWithLifecycle()
    var targetHost by remember { mutableStateOf(sharedTarget.ifBlank { "192.168.1.1" }) }
    var protocol by remember { mutableStateOf(ScanProtocol.TCP) }
    var startPortStr by remember { mutableStateOf("1") }
    var endPortStr by remember { mutableStateOf("1024") }
    var timeoutMs by remember { mutableFloatStateOf(300f) }
    var resultFilter by remember { mutableStateOf("Open Only") } // "All", "Open Only", "Closed Only"
    var showAddCustomPortDialog by remember { mutableStateOf(false) }

    val isScanning by viewModel.isPortScanning.collectAsStateWithLifecycle()
    val progress by viewModel.portScanProgress.collectAsStateWithLifecycle()
    val statusText by viewModel.portScanStatusText.collectAsStateWithLifecycle()
    val results by viewModel.portResults.collectAsStateWithLifecycle()
    val customPorts by viewModel.customPortServices.collectAsStateWithLifecycle()

    LaunchedEffect(sharedTarget) {
        if (sharedTarget.isNotBlank()) {
            targetHost = sharedTarget
        }
    }

    val filteredResults = remember(results, resultFilter) {
        when (resultFilter) {
            "Open Only" -> results.filter { it.isOpen }
            "Closed Only" -> results.filter { !it.isOpen }
            else -> results
        }
    }

    val openCount = remember(results) { results.count { it.isOpen } }
    val closedCount = remember(results) { results.count { !it.isOpen } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        NetPulseTopBar(
            title = "Port Scanner",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { showAddCustomPortDialog = true },
                    modifier = Modifier.testTag("port_scan_add_custom_type_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Custom Port Type",
                        tint = ElectricCyan
                    )
                }
                if (openCount > 0) {
                    IconButton(
                        onClick = {
                            val openPortList = results.filter { it.isOpen }.joinToString(", ") { "${it.port} (${it.serviceName})" }
                            viewModel.saveHost(
                                SavedHost(
                                    ip = targetHost.trim(),
                                    label = "Host ($targetHost)",
                                    notes = "Open Ports: $openPortList",
                                    category = "Work Servers"
                                )
                            )
                        },
                        modifier = Modifier.testTag("port_scan_save_host_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Bookmark Host",
                            tint = ElectricCyan
                        )
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Configuration Card
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
                        // Target Input
                        OutlinedTextField(
                            value = targetHost,
                            onValueChange = { targetHost = it },
                            label = { Text("Target Host / IP") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("port_scan_target_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Slate100,
                                unfocusedTextColor = Slate100
                            )
                        )

                        // Protocol Selector: TCP vs UDP
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Protocol", fontSize = 13.sp, color = Slate400, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = protocol == ScanProtocol.TCP,
                                    onClick = { protocol = ScanProtocol.TCP },
                                    label = { Text("TCP") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricCyan,
                                        selectedLabelColor = Slate950,
                                        containerColor = Slate800,
                                        labelColor = Slate100
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = protocol == ScanProtocol.TCP,
                                        borderColor = Slate700,
                                        selectedBorderColor = ElectricCyan
                                    )
                                )
                                FilterChip(
                                    selected = protocol == ScanProtocol.UDP,
                                    onClick = { protocol = ScanProtocol.UDP },
                                    label = { Text("UDP") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricCyan,
                                        selectedLabelColor = Slate950,
                                        containerColor = Slate800,
                                        labelColor = Slate100
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = protocol == ScanProtocol.UDP,
                                        borderColor = Slate700,
                                        selectedBorderColor = ElectricCyan
                                    )
                                )
                            }
                        }

                        // Port Range Inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = startPortStr,
                                onValueChange = { startPortStr = it.filter { ch -> ch.isDigit() }.take(5) },
                                label = { Text("Start Port") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("port_scan_start_port"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCyan,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Slate100,
                                    unfocusedTextColor = Slate100
                                )
                            )
                            OutlinedTextField(
                                value = endPortStr,
                                onValueChange = { endPortStr = it.filter { ch -> ch.isDigit() }.take(5) },
                                label = { Text("End Port") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("port_scan_end_port"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCyan,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Slate100,
                                    unfocusedTextColor = Slate100
                                )
                            )
                        }

                        // Presets Row (including Custom Ports)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                OutlinedChip("1-1024") {
                                    startPortStr = "1"
                                    endPortStr = "1024"
                                }
                            }
                            item {
                                OutlinedChip("Web (80, 443, 8080)") {
                                    startPortStr = "80"
                                    endPortStr = "8443"
                                }
                            }
                            item {
                                OutlinedChip("Common (20-100)") {
                                    startPortStr = "20"
                                    endPortStr = "100"
                                }
                            }
                            if (customPorts.isNotEmpty()) {
                                item {
                                    OutlinedChip("Custom Ports (${customPorts.size})") {
                                        startPortStr = customPorts.minOf { it.port }.toString()
                                        endPortStr = customPorts.maxOf { it.port }.toString()
                                    }
                                }
                            }
                            item {
                                Button(
                                    onClick = { showAddCustomPortDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = ElectricCyan),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("+ Custom Port", fontSize = 11.sp)
                                }
                            }
                        }

                        // Custom Ports Chips List
                        if (customPorts.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Configured Custom Ports (tap to load)", fontSize = 11.sp, color = Slate400)
                                Text(
                                    text = "Scan All Custom (${customPorts.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan,
                                    modifier = Modifier.clickable {
                                        startPortStr = customPorts.minOf { it.port }.toString()
                                        endPortStr = customPorts.maxOf { it.port }.toString()
                                    }
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(customPorts, key = { it.id }) { cp ->
                                    Box(
                                        modifier = Modifier
                                            .background(Slate800, RoundedCornerShape(6.dp))
                                            .clickable {
                                                startPortStr = cp.port.toString()
                                                endPortStr = cp.port.toString()
                                                protocol = if (cp.protocol.equals("UDP", ignoreCase = true)) ScanProtocol.UDP else ScanProtocol.TCP
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${cp.port}: ${cp.label} (${cp.protocol})",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = ElectricCyan
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Slate400,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .clickable { viewModel.deleteCustomPortService(cp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Connection Timeout Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Connection Timeout", fontSize = 12.sp, color = Slate400)
                                Text("${timeoutMs.toInt()} ms", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = ElectricCyan)
                            }
                            Slider(
                                value = timeoutMs,
                                onValueChange = { timeoutMs = it },
                                valueRange = 100f..2000f,
                                steps = 18,
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricCyan,
                                    activeTrackColor = ElectricCyan,
                                    inactiveTrackColor = Slate700
                                )
                            )
                        }

                        // Start / Stop Action
                        Button(
                            onClick = {
                                if (isScanning) {
                                    viewModel.stopPortScan()
                                } else {
                                    val start = startPortStr.toIntOrNull()?.coerceIn(1, 65535) ?: 1
                                    val end = endPortStr.toIntOrNull()?.coerceIn(start, 65535) ?: 1024
                                    val portList = (start..end).toList()
                                    viewModel.startPortScan(
                                        target = targetHost,
                                        ports = portList,
                                        protocol = protocol,
                                        timeoutMs = timeoutMs.toInt()
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("port_scan_action_btn"),
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
                                text = if (isScanning) "Stop Scanning" else "Start ${protocol.name} Port Scan",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Progress & Status Indicator
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(statusText, fontSize = 12.sp, color = Slate400)
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

            // Results Filter Segmented Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = resultFilter == "Open Only",
                        onClick = { resultFilter = "Open Only" },
                        label = { Text("Open ($openCount)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusEmerald,
                            selectedLabelColor = Slate950,
                            containerColor = Slate900,
                            labelColor = Slate100
                        )
                    )
                    FilterChip(
                        selected = resultFilter == "Closed Only",
                        onClick = { resultFilter = "Closed Only" },
                        label = { Text("Closed ($closedCount)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate700,
                            selectedLabelColor = Slate100,
                            containerColor = Slate900,
                            labelColor = Slate400
                        )
                    )
                    FilterChip(
                        selected = resultFilter == "All",
                        onClick = { resultFilter = "All" },
                        label = { Text("All (${results.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan,
                            selectedLabelColor = Slate950,
                            containerColor = Slate900,
                            labelColor = Slate100
                        )
                    )
                }
            }

            // Port Results List
            if (filteredResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isScanning) "Probing ports..." else "No ports matched the filter",
                            color = Slate400,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(filteredResults, key = { "${it.protocol}_${it.port}" }) { portRes ->
                    PortResultCard(portRes)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Add Custom Port Dialog
    if (showAddCustomPortDialog) {
        var portNumberStr by remember { mutableStateOf("") }
        var portLabel by remember { mutableStateOf("") }
        var portProtocol by remember { mutableStateOf("TCP") }

        val popularCustomPresets = listOf(
            Triple(3000, "Node/React Dev", "TCP"),
            Triple(5173, "Vite Dev", "TCP"),
            Triple(5432, "PostgreSQL", "TCP"),
            Triple(6379, "Redis", "TCP"),
            Triple(8000, "FastAPI/Django", "TCP"),
            Triple(25565, "Minecraft", "TCP"),
            Triple(51820, "WireGuard", "UDP")
        )

        AlertDialog(
            onDismissRequest = { showAddCustomPortDialog = false },
            containerColor = Slate900,
            title = { Text("Add Custom Port Definition", color = Slate100, style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Popular Custom Presets", fontSize = 11.sp, color = Slate400)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(popularCustomPresets) { (p, l, proto) ->
                            Button(
                                onClick = {
                                    portNumberStr = p.toString()
                                    portLabel = l
                                    portProtocol = proto
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = ElectricCyan),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("$p ($l)", fontSize = 10.sp)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = portNumberStr,
                        onValueChange = { portNumberStr = it.filter { c -> c.isDigit() }.take(5) },
                        label = { Text("Port Number (e.g. 8008, 25565)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("custom_port_number_input")
                    )

                    OutlinedTextField(
                        value = portLabel,
                        onValueChange = { portLabel = it },
                        label = { Text("Label (e.g. Dev API, Game Server)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("custom_port_label_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = portProtocol == "TCP",
                            onClick = { portProtocol = "TCP" },
                            label = { Text("TCP") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = Slate950
                            )
                        )
                        FilterChip(
                            selected = portProtocol == "UDP",
                            onClick = { portProtocol = "UDP" },
                            label = { Text("UDP") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = Slate950
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = portNumberStr.toIntOrNull()
                        if (p != null && p in 1..65535 && portLabel.isNotBlank()) {
                            viewModel.addCustomPortService(p, portLabel.trim(), portProtocol)
                            showAddCustomPortDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Slate950)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomPortDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }
}

@Composable
fun PortResultCard(result: PortResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (result.isOpen) StatusEmerald.copy(alpha = 0.4f) else Slate800
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (result.isOpen) StatusEmerald else Slate700)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PORT ${result.port}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Slate100
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = result.protocol.name,
                            fontSize = 11.sp,
                            color = ElectricCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = result.serviceName,
                        fontSize = 12.sp,
                        color = Slate400
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(
                            if (result.isOpen) StatusEmerald.copy(alpha = 0.15f) else Slate800,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (result.isOpen) "OPEN" else "CLOSED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (result.isOpen) StatusEmerald else Slate400,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (result.isOpen && result.latencyMs > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${result.latencyMs} ms",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Slate400
                    )
                }
            }
        }
    }
}

@Composable
private fun OutlinedChip(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = Slate100),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(text, fontSize = 11.sp)
    }
}
