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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.network.TraceHop
import com.example.ui.MainViewModel
import com.example.ui.components.NetPulseTopBar
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCrimson
import com.example.ui.theme.StatusEmerald

@Composable
fun TracerouteScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sharedTarget by viewModel.sharedTarget.collectAsStateWithLifecycle()
    var targetHost by remember { mutableStateOf(sharedTarget.ifBlank { "google.com" }) }

    val isTracing by viewModel.isTracing.collectAsStateWithLifecycle()
    val hops by viewModel.tracerouteHops.collectAsStateWithLifecycle()

    LaunchedEffect(sharedTarget) {
        if (sharedTarget.isNotBlank()) {
            targetHost = sharedTarget
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        NetPulseTopBar(
            title = "Traceroute Probe",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Control Card
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
                            label = { Text("Target Host / Domain / IP") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("trace_target_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Slate100,
                                unfocusedTextColor = Slate100
                            )
                        )

                        Button(
                            onClick = {
                                if (isTracing) {
                                    viewModel.stopTraceroute()
                                } else {
                                    viewModel.startTraceroute(targetHost)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("trace_action_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTracing) StatusCrimson else ElectricCyan,
                                contentColor = Slate950
                            )
                        ) {
                            Icon(
                                imageVector = if (isTracing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isTracing) "Stop Traceroute" else "Start Traceroute", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (isTracing) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = ElectricCyan,
                        trackColor = Slate800
                    )
                }
            }

            // Hops Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ROUTE HOPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                    Text("${hops.size} hops resolved", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ElectricCyan)
                }
            }

            if (hops.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isTracing) "Tracing network route to destination..." else "Tap 'Start Traceroute' to trace path",
                            color = Slate400,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(hops, key = { "${it.hopNumber}_${it.ip}" }) { hop ->
                    HopCard(hop)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun HopCard(hop: TraceHop) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (hop.isDestinationReached) StatusEmerald.copy(alpha = 0.4f) else Slate800
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                hop.isDestinationReached -> StatusEmerald
                                hop.isTimeout -> StatusAmber
                                else -> Slate800
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${hop.hopNumber}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hop.isDestinationReached) Slate950 else Slate100,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = hop.ip,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (hop.isTimeout) Slate400 else Slate100
                    )
                    if (hop.hostName != hop.ip && !hop.isTimeout) {
                        Text(
                            text = hop.hostName,
                            fontSize = 11.sp,
                            color = Slate400,
                            maxLines = 1
                        )
                    }
                }
            }

            if (!hop.isTimeout) {
                Text(
                    text = "${String.format("%.1f", hop.rttMs)} ms",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "* * *",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = StatusAmber,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
