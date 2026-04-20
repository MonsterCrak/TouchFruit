package com.jlls.touchfruit.ui.screens.receptor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jlls.touchfruit.data.model.EstadoPedido
import com.jlls.touchfruit.data.model.Pedido
import com.jlls.touchfruit.ui.components.BotonPrincipal
import com.jlls.touchfruit.ui.components.NavegacionInferior
import com.jlls.touchfruit.ui.theme.*
import com.jlls.touchfruit.viewmodel.ReceptorViewModel

// ===============================
// VISTA PEDIDOS
// ===============================

@Composable
fun VistaPedidosScreen(
    onBack: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: ReceptorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
            TextButton(onClick = onBack) {
                Text("← Volver")
            }
            Text(
                text = "Pedidos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(SpacingLg))

        if (uiState.pedidosActivos.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📋", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(SpacingMd))
                    Text(
                        text = "No hay pedidos nuevos",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SpacingMd)
            ) {
                items(uiState.pedidosActivos) { pedido ->
                    TarjetaPedidoReceptor(
                        pedido = pedido,
                        onIniciar = { viewModel.iniciarPedido(pedido.id) },
                        onMarcarListo = {
                            viewModel.marcarListo(pedido.id)
                            if (pedido.estado == EstadoPedido.EN_PREPARACION) {
                                // Si estaba en preparación, también cerrar la mesa
                                viewModel.cerrarMesa(pedido.mesaId)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SpacingMd))

        NavegacionInferior(
            itemActivo = 0,
            onItemClick = { index ->
                if (index == 2) onCerrarSesion()
            }
        )
    }
}

@Composable
private fun TarjetaPedidoReceptor(
    pedido: Pedido,
    onIniciar: () -> Unit,
    onMarcarListo: () -> Unit
) {
    val tiempoRelativo = getTiempoRelativo(pedido.enviadoEn ?: pedido.creadoEn)

    val estadoColor = when (pedido.estado) {
        EstadoPedido.NUEVO -> CardBlue
        EstadoPedido.EN_PREPARACION -> CardYellow
        EstadoPedido.LISTO -> CardGreen
        EstadoPedido.CANCELADO -> TextSecondary
    }

    Surface(
        shape = ShapeCard,
        color = BgSurface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingLg)
        ) {
            // Header: Mesa y Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mesa ${pedido.mesaId}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = ShapePill,
                    color = estadoColor
                ) {
                    Text(
                        text = when (pedido.estado) {
                            EstadoPedido.NUEVO -> "Nuevo"
                            EstadoPedido.EN_PREPARACION -> "En preparación"
                            EstadoPedido.LISTO -> "Listo"
                            EstadoPedido.CANCELADO -> "Cancelado"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$tiempoRelativo",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(SpacingMd))

            // Items
            pedido.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.producto.nombre} x${item.cantidad}",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "$${item.subtotal.toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingSm))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$${pedido.total.toInt()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(SpacingMd))

            // Botones de acción
            when (pedido.estado) {
                EstadoPedido.NUEVO -> {
                    BotonPrincipal(
                        texto = "INICIAR PREPARACIÓN",
                        onClick = onIniciar
                    )
                }
                EstadoPedido.EN_PREPARACION -> {
                    BotonPrincipal(
                        texto = "MARCAR LISTO",
                        onClick = onMarcarListo
                    )
                }
                else -> { /* No hay acciones */ }
            }
        }
    }
}

private fun getTiempoRelativo(timestamp: Long): String {
    val ahora = System.currentTimeMillis()
    val diff = ahora - timestamp
    val minutos = diff / 60000
    val horas = minutos / 60

    return when {
        minutos < 1 -> "Hace un momento"
        minutos < 60 -> "Hace $minutos min"
        horas < 24 -> "Hace ${horas}h"
        else -> "Hace ${horas / 24}d"
    }
}