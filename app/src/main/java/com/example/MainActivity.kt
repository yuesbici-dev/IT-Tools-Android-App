package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.DnsLookupScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IpManagerScreen
import com.example.ui.screens.Ipv4CalculatorScreen
import com.example.ui.screens.Ipv6CalculatorScreen
import com.example.ui.screens.LanScannerScreen
import com.example.ui.screens.NetworkInfoScreen
import com.example.ui.screens.PingScreen
import com.example.ui.screens.PortScanScreen
import com.example.ui.screens.RssReaderScreen
import com.example.ui.screens.TracerouteScreen
import com.example.ui.screens.WakeOnLanScreen
import com.example.ui.screens.WhoisScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate950

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val mainViewModel: MainViewModel = viewModel()
                NetPulseApp(viewModel = mainViewModel)
            }
        }
    }
}

@Composable
fun NetPulseApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    BackHandler(enabled = currentScreen != "home") {
        viewModel.navigateTo("home")
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate950)
        ) {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    "home" -> HomeScreen(viewModel = viewModel)
                    "ping" -> PingScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("home") })
                    "port_scan" -> PortScanScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("home") })
                    "net_info" -> NetworkInfoScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("home") })
                    "lan_scan" -> LanScannerScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("home") })
                    "wol" -> WakeOnLanScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("home") })
                    "traceroute" -> TracerouteScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("home") })
                    "dns" -> DnsLookupScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("home") })
                    "whois" -> WhoisScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("home") })
                    "ip_calc4" -> Ipv4CalculatorScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("home") })
                    "ip_calc6" -> Ipv6CalculatorScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("home") })
                    "ip_manager" -> IpManagerScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("home") })
                    "rss" -> RssReaderScreen(viewModel = viewModel, onBack = { viewModel.navigateTo("home") })
                    else -> HomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}
