package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WolDevice
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
fun WakeOnLanScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savedDevices by viewModel.wolDevices.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var deviceName by remember { mutableStateOf("") }
    var macAddress by remember { mutableStateOf("") }
    var broadcastIp by remember { mutableStateOf("255.255.255.255") }
    var portStr by remember { mutableStateOf("9") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        NetPulseTopBar(
            title = "Wake on LAN (WoL)",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Magic Packet Form Card
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
                        Text(
                            text = "SEND MAGIC PACKET",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )

                        OutlinedTextField(
                            value = macAddress,
                            onValueChange = { macAddress = it.uppercase() },
                            label = { Text("Target MAC Address (e.g. 00:11:22:33:44:55)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("wol_mac_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Slate100,
                                unfocusedTextColor = Slate100
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = broadcastIp,
                                onValueChange = { broadcastIp = it },
                                label = { Text("Broadcast IP") },
                                singleLine = true,
                                modifier = Modifier.weight(2f).testTag("wol_broadcast_ip"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCyan,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Slate100,
                                    unfocusedTextColor = Slate100
                                )
                            )

                            OutlinedTextField(
                                value = portStr,
                                onValueChange = { portStr = it.filter { c -> c.isDigit() } },
                                label = { Text("Port (7/9)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("wol_port"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCyan,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Slate100,
                                    unfocusedTextColor = Slate100
                                )
                            )
                        }

                        OutlinedTextField(
                            value = deviceName,
                            onValueChange = { deviceName = it },
                            label = { Text("Device Name (Optional, for saving)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Slate100,
                                unfocusedTextColor = Slate100
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val port = portStr.toIntOrNull() ?: 9
                                    viewModel.sendWolPacket(macAddress, broadcastIp, port) { success, msg ->
                                        isSuccessStatus = success
                                        statusMessage = msg
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = macAddress.isNotBlank(),
                                modifier = Modifier.weight(1f).height(46.dp).testTag("wol_send_btn"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Slate950)
                            ) {
                                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Wake Device", fontWeight = FontWeight.Bold)
                            }

                            if (deviceName.isNotBlank() && macAddress.isNotBlank()) {
                                Button(
                                    onClick = {
                                        val port = portStr.toIntOrNull() ?: 9
                                        viewModel.saveWolDevice(
                                            WolDevice(
                                                name = deviceName.trim(),
                                                macAddress = macAddress.trim(),
                                                broadcastIp = broadcastIp.trim(),
                                                port = port
                                            )
                                        )
                                        deviceName = ""
                                        Toast.makeText(context, "Saved to WoL targets", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.height(46.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = ElectricCyan)
                                ) {
                                    Icon(Icons.Default.Bookmark, contentDescription = "Save Target")
                                }
                            }
                        }

                        if (statusMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSuccessStatus) StatusEmerald.copy(alpha = 0.15f) else StatusCrimson.copy(alpha = 0.15f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = statusMessage ?: "",
                                    color = if (isSuccessStatus) StatusEmerald else StatusCrimson,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Saved WoL Devices
            item {
                Text(
                    text = "SAVED WOL TARGETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (savedDevices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No saved WoL devices yet", color = Slate400, fontSize = 13.sp)
                    }
                }
            } else {
                items(savedDevices, key = { it.id }) { dev ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dev.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Slate100
                                )
                                Text(
                                    text = dev.macAddress,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "Broadcast: ${dev.broadcastIp}:${dev.port}",
                                    fontSize = 11.sp,
                                    color = Slate400
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        viewModel.sendWolPacket(dev.macAddress, dev.broadcastIp, dev.port) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Slate950),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Wake", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = { viewModel.deleteWolDevice(dev) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusCrimson, modifier = Modifier.size(16.dp))
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
}
