package com.jlls.touchfruit.ui.screens.receptor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jlls.touchfruit.ui.components.NavegacionInferior
import com.jlls.touchfruit.ui.components.TarjetaPastel
import com.jlls.touchfruit.ui.theme.*
import com.jlls.touchfruit.viewmodel.ReceptorViewModel

// ===============================
// DASHBOARD RECEPTOR
// ===============================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardReceptorScreen(
    onVerPedidos: () -> Unit,
    onVerReportes: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: ReceptorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .padding(SpacingLg)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hola, Receptor",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            // Badge de notificaciones
            if (uiState.pedidoCount > 0) {
                Surface(
                    shape = ShapePill,
                    color = NotificationDot
                ) {
                    Text(
                        text = "${uiState.pedidoCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ActionFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SpacingSm))

        Text(
            text = if (uiState.pedidoCount > 0) {
                "Tienes ${uiState.pedidoCount} pedidos activos"
            } else {
                "Sin pedidos pendientes"
            },
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(SpacingMd))

        // Contenido
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = ActionBg,
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // Card Pedidos Activos
                TarjetaPastel(
                    titulo = "PEDIDOS ACTIVOS",
                    icono = "📋",
                    colorFondo = CardGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    onClick = onVerPedidos
                )

                Spacer(modifier = Modifier.height(SpacingMd))

                // Card Reportes
                TarjetaPastel(
                    titulo = "REPORTES",
                    icono = "📊",
                    colorFondo = CardPurple,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    onClick = onVerReportes
                )
            }
        }

        // Pull-to-refresh indicator
        if (isRefreshing) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = ActionBg,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(SpacingSm))

        // Navegación inferior
        NavegacionInferior(
            itemActivo = 0,
            segundoItemLabel = "Reportes",
            onItemClick = { index ->
                when (index) {
                    1 -> onVerReportes()
                    2 -> onCerrarSesion()
                }
            }
        )
    }
}