package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuickAccessItem
import com.example.ui.components.getIconForKey
import com.example.ui.theme.DarkBlueContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusCrimson

@Composable
fun QuickAccessEditDialog(
    itemToEdit: QuickAccessItem? = null,
    onDismiss: () -> Unit,
    onSave: (QuickAccessItem) -> Unit,
    onDelete: ((QuickAccessItem) -> Unit)? = null,
    onOpenRestoreCatalog: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(itemToEdit?.title ?: "") }
    var subtitle by remember { mutableStateOf(itemToEdit?.subtitle ?: "") }
    var actionType by remember { mutableStateOf(itemToEdit?.actionType ?: "EXTERNAL_APP") }
    var target by remember { mutableStateOf(itemToEdit?.target ?: "") }
    var iconKey by remember { mutableStateOf(itemToEdit?.iconKey ?: "terminal") }
    var categoryTag by remember { mutableStateOf(itemToEdit?.categoryTag ?: "External") }

    val isDefaultTool = itemToEdit != null && itemToEdit.actionType == "TOOL"

    val iconOptions = listOf(
        "terminal", "vpn", "router", "server", "ping", "port_scan", "net_info", "lan_scan", "dns", "whois", "link"
    )

    val popularAppPresets = listOf(
        Triple("Termux", "com.termux", "terminal"),
        Triple("WireGuard", "com.wireguard.android", "vpn"),
        Triple("ConnectBot", "org.connectbot", "terminal"),
        Triple("JuiceSSH", "com.sonelli.juicessh", "server"),
        Triple("OpenVPN", "net.openvpn.openvpn", "vpn")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        titleContentColor = Slate100,
        textContentColor = Slate400,
        title = {
            Text(
                text = if (itemToEdit == null) "Add Quick Access Shortcut" else if (isDefaultTool) "Edit Tool / Card" else "Edit Grid Object",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // If adding new, offer fast button to re-add removed default tools
                if (itemToEdit == null && onOpenRestoreCatalog != null) {
                    Button(
                        onClick = onOpenRestoreCatalog,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlueContainer, contentColor = ElectricCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_open_restore_catalog_btn")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Looking for a deleted default tool? Tap to re-add", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (isDefaultTool) {
                    Text(
                        text = "This is a built-in NetPulse tool (${itemToEdit?.target}). You can customize its title, subtitle, or remove it from your dashboard.",
                        fontSize = 12.sp,
                        color = Slate400
                    )
                } else {
                    Text("Select Shortcut Type", fontSize = 12.sp, color = Slate400)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = actionType == "EXTERNAL_APP",
                            onClick = {
                                actionType = "EXTERNAL_APP"
                                categoryTag = "External"
                            },
                            label = { Text("App Package", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = Slate900
                            )
                        )
                        FilterChip(
                            selected = actionType == "EXTERNAL_URL",
                            onClick = {
                                actionType = "EXTERNAL_URL"
                                categoryTag = "External"
                            },
                            label = { Text("Web Console", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = Slate900
                            )
                        )
                    }

                    if (actionType == "EXTERNAL_APP") {
                        Text("Popular App Presets", fontSize = 12.sp, color = Slate400)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(popularAppPresets) { (name, pkg, icon) ->
                                OutlinedButton(
                                    onClick = {
                                        title = name
                                        subtitle = "Companion Tool"
                                        target = pkg
                                        iconKey = icon
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(name, fontSize = 11.sp, color = ElectricCyan)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("grid_edit_title_input")
                )

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle / Description") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("grid_edit_subtitle_input")
                )

                if (!isDefaultTool) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = {
                            Text(if (actionType == "EXTERNAL_APP") "Package Name (e.g. com.termux)" else "URL (e.g. 192.168.1.1:8080)")
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("grid_edit_target_input")
                    )
                }

                Text("Icon", fontSize = 12.sp, color = Slate400)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(iconOptions) { k ->
                        val isSelected = iconKey == k
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) ElectricCyan else Slate800)
                                .border(1.dp, if (isSelected) ElectricCyan else Slate700, CircleShape)
                                .clickable { iconKey = k },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconForKey(k),
                                contentDescription = k,
                                tint = if (isSelected) Slate900 else Slate100,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && target.isNotBlank()) {
                        val newItem = QuickAccessItem(
                            id = itemToEdit?.id ?: 0,
                            title = title.trim(),
                            subtitle = subtitle.trim().ifEmpty { if (isDefaultTool) "Built-in Tool" else "External Tool" },
                            iconKey = iconKey,
                            actionType = actionType,
                            target = target.trim(),
                            categoryTag = categoryTag,
                            isCustom = if (isDefaultTool) false else true,
                            isVisible = true,
                            orderIndex = itemToEdit?.orderIndex ?: 99
                        )
                        onSave(newItem)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && target.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Slate900),
                modifier = Modifier.testTag("grid_edit_save_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (itemToEdit != null && onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete(itemToEdit)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = StatusCrimson),
                        modifier = Modifier.testTag("grid_edit_delete_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isDefaultTool) "Remove" else "Delete")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = Slate400)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}
