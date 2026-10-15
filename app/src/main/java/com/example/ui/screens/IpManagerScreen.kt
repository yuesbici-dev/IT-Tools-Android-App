package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sort
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
import com.example.data.model.SavedHost
import com.example.ui.HostSortBy
import com.example.ui.MainViewModel
import com.example.ui.SortOrder
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
import com.example.ui.theme.StatusCrimson

@Composable
fun IpManagerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedHosts by viewModel.savedHosts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddHostDialog by remember { mutableStateOf(false) }
    var hostToEdit by remember { mutableStateOf<SavedHost?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    var sortBy by remember { mutableStateOf(HostSortBy.DATE) }
    var sortOrder by remember { mutableStateOf(SortOrder.DESCENDING) }

    val categoryNames = remember(categories) {
        listOf("All") + categories.map { it.name }
    }

    val filteredAndSortedHosts = remember(savedHosts, searchQuery, selectedCategory, sortBy, sortOrder) {
        val filtered = savedHosts.filter { host ->
            val matchesCategory = selectedCategory == "All" || host.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    host.ip.contains(searchQuery, ignoreCase = true) ||
                    host.label.contains(searchQuery, ignoreCase = true) ||
                    host.notes.contains(searchQuery, ignoreCase = true) ||
                    host.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        val sorted = when (sortBy) {
            HostSortBy.LABEL -> filtered.sortedBy { it.label.lowercase() }
            HostSortBy.IP -> filtered.sortedBy { ipToSortKey(it.ip) }
            HostSortBy.DATE -> filtered.sortedBy { it.createdAt }
            HostSortBy.CATEGORY -> filtered.sortedBy { it.category.lowercase() }
        }

        if (sortOrder == SortOrder.DESCENDING) sorted.reversed() else sorted
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        NetPulseTopBar(
            title = "IP Address Book",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = {
                        hostToEdit = null
                        showAddHostDialog = true
                    },
                    modifier = Modifier.testTag("ip_manager_add_host_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add IP Host",
                        tint = ElectricCyan
                    )
                }
            }
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by IP, label, notes, category...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Slate400)
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("ip_manager_search_input"),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Slate900,
                unfocusedContainerColor = Slate900,
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = Slate800,
                focusedTextColor = Slate100,
                unfocusedTextColor = Slate100
            )
        )

        // Order & Sort Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Sort, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                Text("Sort:", fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.SemiBold)

                listOf(
                    Pair(HostSortBy.DATE, "Date"),
                    Pair(HostSortBy.LABEL, "Name"),
                    Pair(HostSortBy.IP, "IP"),
                    Pair(HostSortBy.CATEGORY, "Group")
                ).forEach { (mode, label) ->
                    val isSelected = sortBy == mode
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) ElectricCyan else Slate900, RoundedCornerShape(6.dp))
                            .clickable { sortBy = mode }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Slate950 else Slate400
                        )
                    }
                }
            }

            // Ascending / Descending Toggle
            Button(
                onClick = {
                    sortOrder = if (sortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = ElectricCyan),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(28.dp).testTag("ip_sort_order_toggle")
            ) {
                Icon(
                    imageVector = if (sortOrder == SortOrder.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (sortOrder == SortOrder.ASCENDING) "ASC" else "DESC",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Category Filter Chips Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categoryNames) { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan,
                        selectedLabelColor = Slate950,
                        containerColor = Slate900,
                        labelColor = Slate400
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Slate800,
                        selectedBorderColor = ElectricCyan
                    )
                )
            }
            item {
                Button(
                    onClick = { showAddCategoryDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = ElectricCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Category", fontSize = 11.sp)
                }
            }
        }

        // Host List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (filteredAndSortedHosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No addresses match \"$searchQuery\"" else "No saved IP addresses in this category",
                                color = Slate400,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    hostToEdit = null
                                    showAddHostDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Slate950)
                            ) {
                                Text("+ Add IP Address")
                            }
                        }
                    }
                }
            } else {
                items(filteredAndSortedHosts, key = { it.id }) { host ->
                    SavedHostCard(
                        host = host,
                        onPing = { viewModel.navigateTo("ping", host.ip) },
                        onPortScan = { viewModel.navigateTo("port_scan", host.ip) },
                        onTraceroute = { viewModel.navigateTo("traceroute", host.ip) },
                        onWol = { viewModel.navigateTo("wol", host.ip) },
                        onEdit = {
                            hostToEdit = host
                            showAddHostDialog = true
                        },
                        onDelete = { viewModel.deleteHost(host) }
                    )
                }
            }
        }
    }

    // Add / Edit Host Dialog
    if (showAddHostDialog) {
        AddEditHostDialog(
            hostToEdit = hostToEdit,
            availableCategories = categories.map { it.name },
            onDismiss = { showAddHostDialog = false },
            onSave = { host ->
                if (host.id == 0L) {
                    viewModel.saveHost(host)
                } else {
                    viewModel.updateHost(host)
                }
                showAddHostDialog = false
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = Slate900,
            title = { Text("New Category", color = Slate100) },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Category Name (e.g. Lab Switches)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            viewModel.addCategory(newCatName.trim())
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Slate950)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }
}

private fun ipToSortKey(ip: String): Long {
    return try {
        val parts = ip.split(".").map { it.toLong() }
        (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
    } catch (_: Exception) {
        0L
    }
}

@Composable
fun SavedHostCard(
    host: SavedHost,
    onPing: () -> Unit,
    onPortScan: () -> Unit,
    onTraceroute: () -> Unit,
    onWol: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = host.label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Slate100
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = host.ip,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = ElectricCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                MonospaceBadge(
                    text = host.category,
                    color = Slate100,
                    backgroundColor = DarkBlueContainer
                )
            }

            if (host.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = host.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    maxLines = 2
                )
            }

            if (host.macAddress.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "MAC: ${host.macAddress}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    HostActionButton(icon = Icons.Default.Radar, label = "Ping", onClick = onPing)
                    HostActionButton(icon = Icons.Default.Security, label = "Port Scan", onClick = onPortScan)
                    HostActionButton(icon = Icons.Default.Route, label = "Trace", onClick = onTraceroute)
                    if (host.macAddress.isNotBlank()) {
                        HostActionButton(icon = Icons.Default.PowerSettingsNew, label = "WoL", onClick = onWol)
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate400, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusCrimson, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HostActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = ElectricCyan),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.height(28.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHostDialog(
    hostToEdit: SavedHost?,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (SavedHost) -> Unit
) {
    var ip by remember { mutableStateOf(hostToEdit?.ip ?: "") }
    var label by remember { mutableStateOf(hostToEdit?.label ?: "") }
    var notes by remember { mutableStateOf(hostToEdit?.notes ?: "") }
    var macAddress by remember { mutableStateOf(hostToEdit?.macAddress ?: "") }
    var category by remember { mutableStateOf(hostToEdit?.category ?: availableCategories.firstOrNull() ?: "General") }
    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Text(
                text = if (hostToEdit == null) "Save IP Address" else "Edit IP Address",
                color = Slate100,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address / Hostname") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_host_ip_input")
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Custom Label (e.g. Core Switch)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_host_label_input")
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.background(Slate900)
                    ) {
                        availableCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = Slate100) },
                                onClick = {
                                    category = cat
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = macAddress,
                    onValueChange = { macAddress = it },
                    label = { Text("MAC Address (optional for WoL)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_host_mac_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. Rack 3, VLAN 20)") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_host_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ip.isNotBlank() && label.isNotBlank()) {
                        val newHost = SavedHost(
                            id = hostToEdit?.id ?: 0,
                            ip = ip.trim(),
                            label = label.trim(),
                            notes = notes.trim(),
                            category = category,
                            macAddress = macAddress.trim()
                        )
                        onSave(newHost)
                    }
                },
                enabled = ip.isNotBlank() && label.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Slate950)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        }
    )
}
