package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetPulseTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = Slate100
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_bar_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ElectricCyan
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Slate950,
            titleContentColor = Slate100
        )
    )
}

@Composable
fun MonospaceBadge(
    text: String,
    color: Color = ElectricCyan,
    backgroundColor: Color = Slate800,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MetricStatCard(
    label: String,
    value: String,
    unit: String = "",
    color: Color = ElectricCyan,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Slate900,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                color = Slate400,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (unit.isNotBlank()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = unit,
                        fontSize = 11.sp,
                        color = Slate400,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun SparklineChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    strokeColor: Color = ElectricCyan
) {
    if (values.size < 2) return

    val maxVal = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val minVal = (values.minOrNull() ?: 0.0).coerceAtLeast(0.0)
    val range = (maxVal - minVal).coerceAtLeast(1.0)

    Box(
        modifier = modifier
            .background(Slate900, RoundedCornerShape(8.dp))
            .border(1.dp, Slate700.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            val stepX = size.width / (values.size - 1)
            val path = Path()

            values.forEachIndexed { i, v ->
                val x = i * stepX
                val normalizedY = ((v - minVal) / range).toFloat()
                val y = size.height - (normalizedY * (size.height * 0.8f)) - (size.height * 0.1f)

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw current point at the end
            val lastY = size.height - (((values.last() - minVal) / range).toFloat() * (size.height * 0.8f)) - (size.height * 0.1f)
            drawCircle(
                color = strokeColor,
                radius = 3.5.dp.toPx(),
                center = Offset(size.width, lastY)
            )
        }
    }
}

fun getIconForKey(key: String): ImageVector {
    return when (key) {
        "ping" -> Icons.Default.Radar
        "port_scan" -> Icons.Default.Security
        "net_info" -> Icons.Default.Info
        "lan_scan" -> Icons.Default.Hub
        "wol" -> Icons.Default.PowerSettingsNew
        "traceroute" -> Icons.Default.Route
        "dns" -> Icons.Default.Dns
        "whois" -> Icons.Default.Public
        "ip_calc4" -> Icons.Default.Calculate
        "ip_calc6" -> Icons.Default.SettingsEthernet
        "ip_manager" -> Icons.Default.Bookmark
        "rss" -> Icons.Default.RssFeed
        "terminal" -> Icons.Default.Terminal
        "vpn" -> Icons.Default.VpnKey
        "router" -> Icons.Default.Cable
        "server" -> Icons.Default.Computer
        else -> Icons.Default.Link
    }
}
