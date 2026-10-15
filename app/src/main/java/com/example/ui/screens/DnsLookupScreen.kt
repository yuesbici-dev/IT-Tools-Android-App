package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DnsServer
import com.example.network.DnsRecord
import com.example.ui.MainViewModel
import com.example.ui.components.MonospaceBadge
import com.example.ui.components.NetPulseTopBar
import com.example.ui.theme.DarkBlueContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.StatusEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsLookupScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedDnsServers by viewModel.dnsServers.collectAsStateWithLifecycle()
    val isQuerying by viewModel.isDnsQuerying.collectAsStateWithLifecycle()
    val queryResult by viewModel.dnsResult.collectAsStateWithLifecycle()

    var domain by remember { mutableStateOf("cloudflare.com") }
    var selectedType by remember { mutableStateOf("A") }
    var selectedServer by remember { mutableStateOf("1.1.1.1") }
    var expandedServerDropdown by remember { mutableStateOf(false) }
    var showAddServerDialog by remember { mutableStateOf(false) }

    val recordTypes = listOf("A", "AAAA", "CNAME", "MX", "TXT", "NS", "SOA")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        NetPulseTopBar(
            title = "NSLookup (DNS Resolver)",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { showAddServerDialog = true },
                    modifier = Modifier.testTag("dns_add_server_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Save Custom DNS Server",
                        tint = ElectricCyan
                    )
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
                        OutlinedTextField(
                            value = domain,
                            onValueChange = { domain = it },
                            label = { Text("Domain Name (e.g. cloudflare.com)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("dns_domain_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Slate100,
                                unfocusedTextColor = Slate100
                            )
                        )

                        // DNS Resolver Selector Dropdown
                        ExposedDropdownMenuBox(
                            expanded = expandedServerDropdown,
                            onExpandedChange = { expandedServerDropdown = !expandedServerDropdown }
                        ) {
                            OutlinedTextField(
                                value = selectedServer,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("DNS Resolver Server") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedServerDropdown) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCyan,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Slate100,
                                    unfocusedTextColor = Slate100
                                ),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedServerDropdown,
                                onDismissRequest = { expandedServerDropdown = false },
                                modifier = Modifier.background(Slate900)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("System Default Resolver", color = Slate100) },
                                    onClick = {
                                        selectedServer = "System Default"
                                        expandedServerDropdown = false
                                    }
                                )
                                savedDnsServers.forEach { srv ->
                                    DropdownMenuItem(
                                        text = { Text("${srv.name} (${srv.address})", color = Slate100) },
                                        onClick = {
                                            selectedServer = srv.address
                                            expandedServerDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Record Type Selector Chips
                        Column {
                            Text("Record Type", fontSize = 12.sp, color = Slate400)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(recordTypes) { type ->
                                    val isSelected = selectedType == type
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedType = type },
                                        label = { Text(type, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricCyan,
                                            selectedLabelColor = Slate950,
                                            containerColor = Slate800,
                                            labelColor = Slate100
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = Slate700,
                                            selectedBorderColor = ElectricCyan
                                        )
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.queryDns(domain, selectedType, selectedServer)
                            },
                            enabled = domain.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("dns_resolve_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Slate950)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isQuerying) "Querying..." else "Resolve DNS Records", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Results Card
            if (queryResult != null) {
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
                                Column {
                                    Text(
                                        text = queryResult?.domain ?: "",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Slate100
                                    )
                                    Text(
                                        text = "Server: ${queryResult?.serverUsed} (${queryResult?.queryTimeMs} ms)",
                                        fontSize = 11.sp,
                                        color = Slate400,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                MonospaceBadge(
                                    text = queryResult?.status ?: "OK",
                                    color = if (queryResult?.status == "NOERROR") StatusEmerald else ElectricCyan,
                                    backgroundColor = DarkBlueContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val records = queryResult?.records ?: emptyList()
                            if (records.isEmpty()) {
                                Text("No records found for $selectedType", color = Slate400, fontSize = 12.sp)
                            } else {
                                records.forEach { rec ->
                                    DnsRecordRow(rec)
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Saved DNS Resolvers Reference
            item {
                Text(
                    text = "SAVED DNS RESOLVERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(savedDnsServers, key = { it.id }) { srv ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = srv.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate100)
                            Text(text = srv.address, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = ElectricCyan)
                        }
                        Row {
                            Button(
                                onClick = {
                                    selectedServer = srv.address
                                    viewModel.queryDns(domain, selectedType, srv.address)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = ElectricCyan),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Use", fontSize = 11.sp)
                            }
                            if (!srv.isDefault) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.deleteDnsServer(srv) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Slate400, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAddServerDialog) {
        var serverName by remember { mutableStateOf("") }
        var serverIp by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddServerDialog = false },
            containerColor = Slate900,
            title = { Text("Save Custom DNS Resolver", color = Slate100) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text("Name (e.g. Pi-hole / Local DNS)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100
                        )
                    )
                    OutlinedTextField(
                        value = serverIp,
                        onValueChange = { serverIp = it },
                        label = { Text("Server IP Address (e.g. 192.168.1.5)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (serverName.isNotBlank() && serverIp.isNotBlank()) {
                            viewModel.saveDnsServer(serverName.trim(), serverIp.trim())
                            showAddServerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Slate950)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddServerDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }
}

@Composable
fun DnsRecordRow(record: DnsRecord) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate950, RoundedCornerShape(6.dp))
            .border(1.dp, Slate800, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = record.type,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ElectricCyan,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "TTL ${record.ttl}s",
                    fontSize = 11.sp,
                    color = Slate400,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = record.data,
                fontSize = 13.sp,
                color = Slate100,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
