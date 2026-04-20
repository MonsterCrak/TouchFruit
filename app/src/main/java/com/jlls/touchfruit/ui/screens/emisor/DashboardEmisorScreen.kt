package com.jlls.touchfruit.ui.screens.emisor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jlls.touchfruit.data.model.EstadoMesa
import com.jlls.touchfruit.data.model.Mesa
import com.jlls.touchfruit.ui.components.*
import com.jlls.touchfruit.ui.theme.*
import com.jlls.touchfruit.viewmodel.EmisorViewModel

// ===============================
// DASHBOARD EMISOR
// ===============================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardEmisorScreen(
    onNuevaComanda: () -> Unit,
    onHistorial: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: EmisorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var mesaAConfirmar by remember { mutableStateOf<Mesa?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .padding(SpacingLg)
    ) {
        // Header
        Text(
            text = "Hola, Emisor",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "¿Qué necesitas hoy?",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(SpacingXl))

        // Título Mesas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mesas",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = ActionBg
                )
            }
        }

        Spacer(modifier = Modifier.height(SpacingMd))

        // Grid de Mesas - 3 columnas con celdas más grandes y pull-to-refresh
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = SpacingMd)
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.onRefresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ActionBg,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(SpacingMd),
                        verticalArrangement = Arrangement.spacedBy(SpacingMd),
                        contentPadding = PaddingValues(bottom = SpacingMd)
                    ) {
                        items(uiState.mesas) { mesa ->
                            CeldaMesaGrande(
                                mesaId = mesa.id,
                                estaAbierta = mesa.estado == EstadoMesa.ABIERTA,
                                onClick = {
                                    if (mesa.estado == EstadoMesa.CERRADA) {
                                        mesaAConfirmar = mesa
                                    } else {
                                        viewModel.iniciarNuevaComanda(mesaId = mesa.id)
                                        onNuevaComanda()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SpacingMd))

        // Leyenda
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "🔴 = Cerrada", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.width(SpacingLg))
            Text(text = "🟢 = Abierta", fontSize = 12.sp, color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(SpacingMd))

        // Navegación inferior
        NavegacionInferior(
            itemActivo = 0,
            onItemClick = { index ->
                when (index) {
                    1 -> onHistorial()
                    2 -> onCerrarSesion()
                }
            }
        )
    }

    // Dialogo confirmar abrir mesa
    mesaAConfirmar?.let { mesa ->
        AlertDialog(
            onDismissRequest = { mesaAConfirmar = null },
            title = { Text("Abrir mesa ${mesa.id}") },
            text = { Text("¿Deseas abrir esta mesa? Un cliente se sentará.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.abrirMesa(mesa.id)
                        mesaAConfirmar = null
                    }
                ) {
                    Text("Abrir")
                }
            },
            dismissButton = {
                TextButton(onClick = { mesaAConfirmar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}