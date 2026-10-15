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

@Composable
fun Ipv6CalculatorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ipv6Input by remember { mutableStateOf("2001:0db8:85a3:0000:0000:8a2e:0370:7334") }
    var prefixFloat by remember { mutableFloatStateOf(64f) }

    val result by viewModel.ipv6Result.collectAsStateWithLifecycle()

    LaunchedEffect(ipv6Input, prefixFloat) {
        viewModel.calculateIpv6(ipv6Input, prefixFloat.toInt())
    }

    val presetPrefixes = listOf(32, 48, 56, 64, 96, 128)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        NetPulseTopBar(
            title = "IPv6 Calculator",
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
                            value = ipv6Input,
                            onValueChange = { ipv6Input = it },
                            label = { Text("IPv6 Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("ipv6_calc_input"),
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
                            Text("Prefix Length", fontSize = 12.sp, color = Slate400)
                            Text(
                                text = "/${prefixFloat.toInt()}",
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                        }

                        Slider(
                            value = prefixFloat,
                            onValueChange = { prefixFloat = it },
                            valueRange = 0f..128f,
                            steps = 127,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan,
                                inactiveTrackColor = Slate700
                            ),
                            modifier = Modifier.testTag("ipv6_calc_slider")
                        )

                        // Quick Presets
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(presetPrefixes) { p ->
                                val isSelected = prefixFloat.toInt() == p
                                Button(
                                    onClick = { prefixFloat = p.toFloat() },
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

            // Calculation Results Card
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
                                    text = "IPv6 Prefix Breakdown",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Slate100
                                )
                                MonospaceBadge(text = res.scopeType, color = ElectricCyan, backgroundColor = DarkBlueContainer)
                            }

                            Text("Expanded (Full 8-Group)", fontSize = 11.sp, color = Slate400)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate950, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(res.expandedAddress, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = ElectricCyan)
                            }

                            Text("Compressed Representation", fontSize = 11.sp, color = Slate400)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate950, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(res.compressedAddress, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Slate100)
                            }

                            InfoCalcRow("Prefix Range Start", res.prefixRangeStart, true)
                            InfoCalcRow("Prefix Range End", res.prefixRangeEnd, true)
                            InfoCalcRow("Available Addresses", res.totalAddressesNotation, true)
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
