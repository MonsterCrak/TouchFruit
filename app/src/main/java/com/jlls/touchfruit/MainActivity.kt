package com.jlls.touchfruit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jlls.touchfruit.ui.Screen
import com.jlls.touchfruit.ui.screens.emisor.DashboardEmisorScreen
import com.jlls.touchfruit.ui.screens.emisor.NuevaComandaScreen
import com.jlls.touchfruit.ui.screens.emisor.HistorialEmisorScreen
import com.jlls.touchfruit.ui.screens.emisor.ResumenSesionScreen
import com.jlls.touchfruit.ui.screens.login.LoginScreen
import com.jlls.touchfruit.ui.screens.receptor.DashboardReceptorScreen
import com.jlls.touchfruit.ui.screens.receptor.VistaPedidosScreen
import com.jlls.touchfruit.ui.screens.receptor.ReportesScreen
import com.jlls.touchfruit.ui.theme.TouchFruitTheme
import com.jlls.touchfruit.data.repository.TouchFruitRepository
import com.jlls.touchfruit.data.local.DatabaseProvider
import com.jlls.touchfruit.data.sync.FirebaseSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var firebaseSyncService: FirebaseSyncService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize database
        DatabaseProvider.init(this)
        CoroutineScope(Dispatchers.IO).launch {
            TouchFruitRepository.loadInitialData()
        }

        // Initialize Firebase Sync Service
        firebaseSyncService = FirebaseSyncService(TouchFruitRepository)
        firebaseSyncService.bindToLifecycleOwner(this)

        // Connect SyncManager to Repository so Firebase sync works
        TouchFruitRepository.syncManager = firebaseSyncService.getSyncManager()

        enableEdgeToEdge()
        setContent {
            TouchFruitTheme {
                TouchFruitApp(firebaseSyncService)
            }
        }
    }
}

@Composable
fun TouchFruitApp(firebaseSyncService: FirebaseSyncService) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var isEmisor by remember { mutableStateOf(true) }
    var sesionIdParaResumen by remember { mutableStateOf<String?>(null) }

    when (currentScreen) {
        Screen.Login -> {
            LoginScreen(
                firebaseSyncService = firebaseSyncService,
                onLoginExito = { esEmisor ->
                    isEmisor = esEmisor
                    currentScreen = if (esEmisor) Screen.EmisorDashboard else Screen.ReceptorDashboard
                }
            )
        }

        Screen.EmisorDashboard -> {
            DashboardEmisorScreen(
                onNuevaComanda = { currentScreen = Screen.EmisorNuevaComanda },
                onHistorial = { currentScreen = Screen.EmisorHistorial },
                onCerrarSesion = {
                    TouchFruitRepository.logout()
                    currentScreen = Screen.Login
                }
            )
        }

        Screen.EmisorNuevaComanda -> {
            NuevaComandaScreen(
                onBack = { currentScreen = Screen.EmisorDashboard },
                onSuccess = { currentScreen = Screen.EmisorDashboard },
                onCerrarSesion = {
                    TouchFruitRepository.logout()
                    currentScreen = Screen.Login
                }
            )
        }

        Screen.EmisorHistorial -> {
            HistorialEmisorScreen(
                onBack = { currentScreen = Screen.EmisorDashboard },
                onCerrarSesion = {
                    TouchFruitRepository.logout()
                    currentScreen = Screen.Login
                },
                onVerResumenSesion = { sesionId ->
                    sesionIdParaResumen = sesionId
                    currentScreen = Screen.EmisorResumenSesion
                }
            )
        }

        Screen.EmisorResumenSesion -> {
            sesionIdParaResumen?.let { sesionId ->
                ResumenSesionScreen(
                    sesionId = sesionId,
                    onBack = { currentScreen = Screen.EmisorHistorial }
                )
            }
        }

        Screen.ReceptorDashboard -> {
            DashboardReceptorScreen(
                onVerPedidos = { currentScreen = Screen.ReceptorPedidos },
                onVerReportes = { currentScreen = Screen.ReceptorReportes },
                onCerrarSesion = {
                    TouchFruitRepository.logout()
                    currentScreen = Screen.Login
                }
            )
        }

        Screen.ReceptorPedidos -> {
            VistaPedidosScreen(
                onBack = { currentScreen = Screen.ReceptorDashboard },
                onCerrarSesion = {
                    TouchFruitRepository.logout()
                    currentScreen = Screen.Login
                }
            )
        }

        Screen.ReceptorReportes -> {
            ReportesScreen(
                onBack = { currentScreen = Screen.ReceptorDashboard },
                onCerrarSesion = {
                    TouchFruitRepository.logout()
                    currentScreen = Screen.Login
                }
            )
        }
    }
}