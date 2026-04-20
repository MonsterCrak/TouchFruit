package com.jlls.touchfruit.ui.screens.receptor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jlls.touchfruit.data.model.Pedido
import com.jlls.touchfruit.ui.components.NavegacionInferior
import com.jlls.touchfruit.ui.theme.*
import com.jlls.touchfruit.viewmodel.ReceptorViewModel

// ===============================
// REPORTES
// ===============================

enum class PeriodoReporte {
    DIARIO, SEMANAL, MENSUAL
}

@Composable
fun ReportesScreen(
    onBack: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: ReceptorViewModel = viewModel()
) {
    var periodoSeleccionado by remember { mutableStateOf(PeriodoReporte.DIARIO) }

    val pedidos = when (periodoSeleccionado) {
        PeriodoReporte.DIARIO -> viewModel.getPedidosDelDia()
        PeriodoReporte.SEMANAL -> viewModel.getPedidosDeLaSemana()
        PeriodoReporte.MENSUAL -> viewModel.getPedidosDelMes()
    }

    val metricas = calcularMetricas(pedidos)

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
                text = "Reportes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(SpacingLg))

        // Tabs de período
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingSm)
        ) {
            PeriodoTab(
                texto = "Diario",
                seleccionado = periodoSeleccionado == PeriodoReporte.DIARIO,
                modifier = Modifier.weight(1f),
                onClick = { periodoSeleccionado = PeriodoReporte.DIARIO }
            )
            PeriodoTab(
                texto = "Semanal",
                seleccionado = periodoSeleccionado == PeriodoReporte.SEMANAL,
                modifier = Modifier.weight(1f),
                onClick = { periodoSeleccionado = PeriodoReporte.SEMANAL }
            )
            PeriodoTab(
                texto = "Mensual",
                seleccionado = periodoSeleccionado == PeriodoReporte.MENSUAL,
                modifier = Modifier.weight(1f),
                onClick = { periodoSeleccionado = PeriodoReporte.MENSUAL }
            )
        }

        Spacer(modifier = Modifier.height(SpacingLg))

        if (pedidos.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📊", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(SpacingMd))
                    Text(
                        text = "No hay datos para este período",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SpacingMd)
            ) {
                // Total Pedidos
                TarjetaMetrica(
                    titulo = "📦 Total Pedidos",
                    valor = metricas.totalPedidos.toString(),
                    comparativa = metricas.comparativaPedidos,
                    color = CardBlue
                )

                // Producto más vendido
                TarjetaMetrica(
                    titulo = "🍊 Producto Más Vendido",
                    valor = metricas.productoMasVendido,
                    subvalor = "${metricas.unidadesProductoMasVendido} unidades",
                    color = CardGreen
                )

                // Ticket promedio
                TarjetaMetrica(
                    titulo = "💰 Ticket Promedio",
                    valor = "$${metricas.ticketPromedio.toInt()}",
                    comparativa = metricas.comparativaTicket,
                    color = CardYellow
                )

                // Resumen
                TarjetaResumen(
                    mesaMayorVenta = metricas.mesaMayorVenta,
                    horarioPico = metricas.horarioPico
                )
            }
        }

        Spacer(modifier = Modifier.height(SpacingMd))

        // Navegación inferior
        NavegacionInferior(
            itemActivo = 1,
            segundoItemLabel = "Reportes",
            onItemClick = { index ->
                when (index) {
                    0 -> onBack()  // Volver al Dashboard
                    2 -> onCerrarSesion()
                }
            }
        )
    }
}

@Composable
private fun PeriodoTab(
    texto: String,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ShapePill,
        color = if (seleccionado) ActionBg else BgSearch
    ) {
        Text(
            text = texto,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (seleccionado) ActionFg else TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun TarjetaMetrica(
    titulo: String,
    valor: String,
    subvalor: String? = null,
    comparativa: String? = null,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = ShapeCard,
        color = color
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingLg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titulo,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(SpacingSm))
            Text(
                text = valor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            if (subvalor != null) {
                Text(
                    text = subvalor,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
            if (comparativa != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comparativa,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TarjetaResumen(
    mesaMayorVenta: Int?,
    horarioPico: String
) {
    Surface(
        shape = ShapeCard,
        color = CardPurple
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingLg)
        ) {
            Text(
                text = "📊 Resumen",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(SpacingMd))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mayor venta:",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = if (mesaMayorVenta != null) "Mesa $mesaMayorVenta" else "-",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Horario pico:",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = horarioPico,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ===============================
// HELPERS
// ===============================

data class Metricas(
    val totalPedidos: Int,
    val comparativaPedidos: String?,
    val productoMasVendido: String,
    val unidadesProductoMasVendido: Int,
    val ticketPromedio: Double,
    val comparativaTicket: String?,
    val mesaMayorVenta: Int?,
    val horarioPico: String
)

private fun calcularMetricas(pedidos: List<Pedido>): Metricas {
    if (pedidos.isEmpty()) {
        return Metricas(
            totalPedidos = 0,
            comparativaPedidos = null,
            productoMasVendido = "-",
            unidadesProductoMasVendido = 0,
            ticketPromedio = 0.0,
            comparativaTicket = null,
            mesaMayorVenta = null,
            horarioPico = "-"
        )
    }

    // Total pedidos
    val totalPedidos = pedidos.size

    // Producto más vendido
    val productosAgrupados = pedidos
        .flatMap { it.items }
        .groupBy { it.producto.nombre }
        .mapValues { (_, items) -> items.sumOf { it.cantidad } }

    val productoMasVendido = productosAgrupados.maxByOrNull { it.value }?.key ?: "-"
    val unidadesProductoMasVendido = productosAgrupados.values.maxOrNull() ?: 0

    // Ticket promedio
    val totalVentas = pedidos.sumOf { it.total }
    val ticketPromedio = totalVentas / pedidos.size

    // Mesa con mayor venta
    val ventasPorMesa = pedidos.groupBy { it.mesaId }
        .mapValues { (_, pedidosMesa) -> pedidosMesa.sumOf { it.total } }
    val mesaMayorVenta = ventasPorMesa.maxByOrNull { it.value }?.key

    // Horario pico (hora más común)
    val horas = pedidos
        .mapNotNull { it.enviadoEn }
        .map { java.util.Calendar.getInstance().apply { timeInMillis = it }.get(java.util.Calendar.HOUR_OF_DAY) }
        .groupingBy { it }.eachCount()
    val horarioPico = horas.maxByOrNull { it.value }?.key?.let { "${it}:00" } ?: "-"

    return Metricas(
        totalPedidos = totalPedidos,
        comparativaPedidos = null, // Simplificado por ahora
        productoMasVendido = productoMasVendido,
        unidadesProductoMasVendido = unidadesProductoMasVendido,
        ticketPromedio = ticketPromedio,
        comparativaTicket = null,
        mesaMayorVenta = mesaMayorVenta,
        horarioPico = horarioPico
    )
}