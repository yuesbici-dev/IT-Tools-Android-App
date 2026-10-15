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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

@Composable
fun Ipv4CalculatorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ipInput by remember { mutableStateOf("192.168.1.100") }
    var cidrFloat by remember { mutableFloatStateOf(24f) }

    val result by viewModel.ipv4Result.collectAsStateWithLifecycle()

    LaunchedEffect(ipInput, cidrFloat) {
        viewModel.calculateIpv4(ipInput, cidrFloat.toInt())
    }

    val presetCidrs = listOf(8, 16, 24, 27, 28, 29, 30, 32)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        NetPulseTopBar(
            title = "IPv4 Subnet Calculator",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Input Controls Card
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
                            value = ipInput,
                            onValueChange = { ipInput = it },
                            label = { Text("IPv4 Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("ipv4_calc_ip_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Slate100,
                                unfocusedTextColor = Slate100
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CIDR Prefix Length", fontSize = 12.sp, color = Slate400)
                            Text(
                                text = "/${cidrFloat.toInt()}",
                                fontSize = 16.sp,
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
                            modifier = Modifier.testTag("ipv4_calc_cidr_slider")
                        )

                        // Quick Presets
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(presetCidrs) { p ->
                                val isSelected = cidrFloat.toInt() == p
                                Button(
                                    onClick = { cidrFloat = p.toFloat() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) ElectricCyan else Slate800,
                                        contentColor = if (isSelected) Slate950 else Slate100
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("/$p", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            // Subnet Calculation Results
            if (result != null) {
                val res = result!!
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${res.ip}/${res.cidr}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Slate100,
                                    fontFamily = FontFamily.Monospace
                                )
                                MonospaceBadge(text = res.ipClass, color = ElectricCyan, backgroundColor = DarkBlueContainer)
                            }

                            InfoCalcRow("Network Address", res.networkAddress, true)
                            InfoCalcRow("Subnet Mask", res.netmask, true)
                            InfoCalcRow("Wildcard Mask", res.wildcardMask, true)
                            InfoCalcRow("Broadcast Address", res.broadcastAddress, true)
                            InfoCalcRow("Usable Range", "${res.firstUsableHost} - ${res.lastUsableHost}", true)
                            InfoCalcRow("Total Host Addresses", res.totalHosts.toString(), true)
                            InfoCalcRow("Usable Host Count", res.usableHosts.toString(), true)
                            InfoCalcRow("Scope / Type", res.ipType, false)
                        }
                    }
                }

                // Binary Representations Card
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("BINARY REPRESENTATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)

                            Text("IP Address (Binary)", fontSize = 11.sp, color = Slate400)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate950, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(res.binaryIp, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ElectricCyan)
                            }

                            Text("Subnet Mask (Binary)", fontSize = 11.sp, color = Slate400)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate950, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(res.binaryNetmask, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = StatusEmerald)
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

@Composable
fun InfoCalcRow(label: String, value: String, isMono: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = Slate400)
        Text(
            text = value,
            fontSize = 13.sp,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            color = Slate100
        )
    }
}
