package com.jlls.touchfruit.ui.screens.emisor

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
import com.jlls.touchfruit.data.model.EstadoPedido
import com.jlls.touchfruit.data.model.Pedido
import com.jlls.touchfruit.data.model.Sesion
import com.jlls.touchfruit.data.repository.TouchFruitRepository
import com.jlls.touchfruit.ui.components.NavegacionInferior
import com.jlls.touchfruit.ui.theme.*

// ===============================
// HISTORIAL EMISOR
// ===============================

data class SesionConPedidos(
    val sesion: Sesion,
    val pedidos: List<Pedido>,
    val mesaId: Int
) {
    val total: Double get() = pedidos.sumOf { it.total }
    val itemCount: Int get() = pedidos.sumOf { it.itemCount }
}

@Composable
fun HistorialEmisorScreen(
    onBack: () -> Unit,
    onCerrarSesion: () -> Unit,
    onVerResumenSesion: (String) -> Unit = {}
) {
    val sesiones by TouchFruitRepository.todasLasSesionesFlow().collectAsState(initial = emptyList())
    val pedidos by TouchFruitRepository.todosLosPedidosFlow().collectAsState(initial = emptyList())

    val sesionesConPedidos = sesiones.mapNotNull { sesion ->
        val pedidosSesion = pedidos.filter { it.sesionId == sesion.id }
        if (pedidosSesion.isEmpty()) null
        else SesionConPedidos(sesion, pedidosSesion, sesion.mesaId)
    }.sortedByDescending { it.sesion.abiertaEn }

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
                text = "Historial",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(SpacingLg))

        if (sesionesConPedidos.isEmpty()) {
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
                        text = "Aún no hay sesiones",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SpacingSm)
            ) {
                items(sesionesConPedidos) { sesionConPedidos ->
                    TarjetaSesionHistorial(
                        sesionConPedidos = sesionConPedidos,
                        onClick = { onVerResumenSesion(sesionConPedidos.sesion.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SpacingMd))

        NavegacionInferior(
            itemActivo = 1,
            onItemClick = { index ->
                when (index) {
                    0 -> onBack()
                    2 -> onCerrarSesion()
                }
            }
        )
    }
}

@Composable
private fun TarjetaSesionHistorial(
    sesionConPedidos: SesionConPedidos,
    onClick: () -> Unit
) {
    val tiempoRelativo = getTiempoRelativo(sesionConPedidos.sesion.abiertaEn)
    val esActiva = sesionConPedidos.sesion.cerradaEn == null

    Surface(
        onClick = onClick,
        shape = ShapeCard,
        color = BgSurface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mesa ${sesionConPedidos.mesaId}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = ShapePill,
                    color = if (esActiva) CardGreen else TextSecondary.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = if (esActiva) "🟢 Activa" else "⚫ Cerrada",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingSm))

            Text(
                text = "Sesión ${sesionConPedidos.sesion.id.takeLast(4)} · $tiempoRelativo",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(SpacingSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${sesionConPedidos.itemCount} items · ${sesionConPedidos.pedidos.size} pedidos",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = "$${sesionConPedidos.total.toInt()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
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

// ===============================
// RESUMEN SESION (detalle de pedido por sesion)
// ===============================

@Composable
fun ResumenSesionScreen(
    sesionId: String,
    onBack: () -> Unit
) {
    val sesiones by TouchFruitRepository.todasLasSesionesFlow().collectAsState(initial = emptyList())
    val pedidos by TouchFruitRepository.todosLosPedidosFlow().collectAsState(initial = emptyList())

    val sesion = sesiones.find { it.id == sesionId }
    val pedidosSesion = pedidos.filter { it.sesionId == sesionId }
    val mesaId = sesion?.mesaId ?: 0

    // Agregar todos los items de todos los pedidos
    val todosLosItems = pedidosSesion.flatMap { it.items }
    val totalSesion = todosLosItems.sumOf { it.subtotal }

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
                text = "Resumen Sesión",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(SpacingLg))

        // Info de la sesion
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeCard,
            color = BgSurface,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(SpacingMd)) {
                Text(
                    text = "Mesa $mesaId",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sesión ${sesionId.takeLast(6)}",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${pedidosSesion.size} pedidos · ${todosLosItems.sumOf { it.cantidad }} items",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(SpacingLg))

        Text(
            text = "Productos",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(SpacingSm))

        if (todosLosItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay productos en esta sesión",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        } else {
            // Agrupar por producto y sumar cantidades
            val itemsAgrupados = todosLosItems
                .groupBy { it.producto.id }
                .map { (productoId, items) ->
                    items.first().producto to items.sumOf { it.cantidad }
                }
                .sortedByDescending { it.second * it.first.precio }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SpacingSm)
            ) {
                items(itemsAgrupados) { (producto, cantidad) ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ShapeCard,
                        color = BgSurface,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(SpacingMd),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = producto.nombre,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$${producto.precio.toInt()} c/u",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "x$cantidad",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$${(producto.precio * cantidad).toInt()}",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SpacingMd))

        // Total
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeCard,
            color = BgSurface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SpacingMd),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total sesión",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$${totalSesion.toInt()}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ActionBg
                )
            }
        }
    }
}