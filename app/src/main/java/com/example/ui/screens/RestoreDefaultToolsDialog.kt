package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuickAccessItem
import com.example.data.repository.NetworkRepository
import com.example.ui.components.getIconForKey
import com.example.ui.theme.DarkBlueContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.StatusEmerald

@Composable
fun RestoreDefaultToolsDialog(
    currentItems: List<QuickAccessItem>,
    onDismiss: () -> Unit,
    onReaddTool: (String) -> Unit,
    onRestoreAll: () -> Unit
) {
    var locallyReadded by remember { mutableStateOf(setOf<String>()) }
    val activeTargets = remember(currentItems, locallyReadded) {
        currentItems.map { it.target }.toSet() + locallyReadded
    }
    val allDefaults = NetworkRepository.DEFAULT_QUICK_ACCESS_ITEMS

    val missingCount = allDefaults.count { it.target !in activeTargets }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        titleContentColor = Slate100,
        textContentColor = Slate400,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Default Tools Catalog",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (missingCount > 0) {
                    Button(
                        onClick = {
                            locallyReadded = locallyReadded + allDefaults.map { it.target }
                            onRestoreAll()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Slate900),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("restore_all_defaults_btn")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore All ($missingCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                Text(
                    text = if (missingCount == 0) "All 12 default tools are active on your dashboard."
                    else "$missingCount default tool${if (missingCount > 1) "s have" else " has"} been removed. You can re-add individually or restore all.",
                    fontSize = 12.sp,
                    color = Slate400
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allDefaults, key = { it.target }) { defaultItem ->
                        val isPresent = defaultItem.target in activeTargets
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Slate800),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(DarkBlueContainer, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getIconForKey(defaultItem.iconKey),
                                            contentDescription = null,
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = defaultItem.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Slate100
                                        )
                                        Text(
                                            text = defaultItem.subtitle,
                                            fontSize = 11.sp,
                                            color = Slate400
                                        )
                                    }
                                }

                                if (isPresent) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Added",
                                            tint = StatusEmerald,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Active", fontSize = 11.sp, color = StatusEmerald, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            locallyReadded = locallyReadded + defaultItem.target
                                            onReaddTool(defaultItem.target)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Slate900),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.testTag("readd_tool_${defaultItem.target}")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Re-add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = ElectricCyan)
            ) {
                Text("Close")
            }
        }
    )
}
