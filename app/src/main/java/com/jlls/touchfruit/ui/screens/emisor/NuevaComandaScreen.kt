package com.jlls.touchfruit.ui.screens.emisor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jlls.touchfruit.data.model.Categoria
import com.jlls.touchfruit.data.model.EstadoMesa
import com.jlls.touchfruit.data.model.Producto
import com.jlls.touchfruit.ui.components.*
import com.jlls.touchfruit.ui.theme.*
import com.jlls.touchfruit.viewmodel.EmisorViewModel
import com.jlls.touchfruit.viewmodel.PasoComanda

// ===============================
// NUEVA COMANDA (MANDATO RÁPIDO)
// ===============================

@Composable
fun NuevaComandaScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onCerrarSesion: () -> Unit,
    onCierreCompleto: () -> Unit = onSuccess,
    viewModel: EmisorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
    ) {
        when (uiState.pasoActual) {
            PasoComanda.CATEGORIA -> PasoCategoria(
                categoriaActual = uiState.comandaTemporal.categoriaActual,
                onSeleccionar = { viewModel.seleccionarCategoria(it) },
                onCerrarMesa = { viewModel.iniciarCierreMesa(it) },
                onVerPedidos = { viewModel.verPedidosMesa(it) },
                mesaIdActual = uiState.comandaTemporal.mesaId,
                onBack = onBack
            )

            PasoComanda.PRODUCTOS -> PasoProductos(
                categoria = uiState.comandaTemporal.categoriaActual!!,
                productos = viewModel.getProductosDeCategoria(uiState.comandaTemporal.categoriaActual!!),
                comandaTemporal = uiState.comandaTemporal,
                onAgregar = { p, c -> viewModel.agregarProducto(p, c) },
                onActualizarCantidad = { id, c -> viewModel.actualizarCantidad(id, c) },
                onQuitar = { viewModel.quitarItem(it) },
                onVerResumen = { viewModel.irAResumen() },
                onBack = { viewModel.volverAtras() }
            )

            PasoComanda.RESUMEN -> PasoResumen(
                comandaTemporal = uiState.comandaTemporal,
                onAgregarMas = { viewModel.agregarMasProductos() },
                onEnviar = {
                    val success = viewModel.enviarPedido()
                    if (success) {
                        showSuccessDialog = true
                    }
                },
                onBack = { viewModel.volverAtras() }
            )

            PasoComanda.CONFIRMACION -> PasoConfirmacion(
                comandaTemporal = uiState.comandaTemporal,
                onEnviar = {
                    val success = viewModel.enviarPedido()
                    if (success) {
                        showSuccessDialog = true
                    }
                },
                onBack = { viewModel.volverAtras() }
            )

            PasoComanda.CERRAR_MESA -> PasoSeleccionarSesionCierre(
                sesiones = viewModel.getSesionesPorMesa(uiState.comandaTemporal.mesaId ?: 0),
                mesaId = uiState.comandaTemporal.mesaId ?: 0,
                onSeleccionar = { viewModel.seleccionarSesionCierre(it) },
                onBack = { viewModel.volverAtras() }
            )

            PasoComanda.RESUMEN_CIERRE -> PasoResumenCierre(
                pedidos = viewModel.getPedidosPorSesion(uiState.comandaTemporal.sesionIdCierre ?: ""),
                mesaId = uiState.comandaTemporal.mesaId ?: 0,
                onConfirmarCierre = {
                    viewModel.cerrarMesaYPedidos(uiState.comandaTemporal.mesaId ?: 0)
                    onCierreCompleto()
                },
                onBack = { viewModel.volverAtras() }
            )

            PasoComanda.VER_PEDIDOS_MESA -> {
                var pedidoAEliminar by remember { mutableStateOf<String?>(null) }
                var showDoubleConfirm by remember { mutableStateOf(false) }
                val mesaId = uiState.comandaTemporal.mesaId ?: 0

                LaunchedEffect(mesaId) {
                    viewModel.cargarPedidosActivosPorMesa(mesaId)
                }

                val pedidosActivos by viewModel.pedidosActivosPorMesaFlow.collectAsState(initial = emptyList())

                PasoVerPedidosMesa(
                    pedidos = pedidosActivos,
                    mesaId = mesaId,
                    onEditarPedido = { viewModel.seleccionarPedidoEditar(it) },
                    onEliminarPedido = { pedidoAEliminar = it },
                    onBack = { viewModel.volverAtras() }
                )

                // Primera confirmación
                pedidoAEliminar?.let { pedidoId ->
                    if (!showDoubleConfirm) {
                        AlertDialog(
                            onDismissRequest = { pedidoAEliminar = null },
                            title = { Text("Eliminar pedido") },
                            text = { Text("¿Eliminar este pedido de la mesa ${uiState.comandaTemporal.mesaId}?") },
                            confirmButton = {
                                TextButton(onClick = { showDoubleConfirm = true }) {
                                    Text("Siguiente")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { pedidoAEliminar = null }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    } else {
                        // Doble confirmación
                        AlertDialog(
                            onDismissRequest = {
                                pedidoAEliminar = null
                                showDoubleConfirm = false
                            },
                            title = { Text("¿Confirmar eliminación?") },
                            text = { Text("Esta acción no se puede deshacer. El pedido será eliminado por completo.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.eliminarPedido(pedidoId)
                                        pedidoAEliminar = null
                                        showDoubleConfirm = false
                                    }
                                ) {
                                    Text("Sí, eliminar", color = CardRed)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    pedidoAEliminar = null
                                    showDoubleConfirm = false
                                }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }
            }

            PasoComanda.EDITAR_PEDIDO -> {
                val pedidoId = uiState.comandaTemporal.pedidoIdEditar ?: ""
                val pedido = viewModel.getPedidoPorId(pedidoId)
                if (pedido != null) {
                    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
                    var showDiscardConfirm by remember { mutableStateOf(false) }

                    PasoEditarPedido(
                        pedido = pedido,
                        hayCambiosSinGuardar = viewModel.debeMostrarActualizar(),
                        onActualizarCantidad = { id, c -> viewModel.actualizarCantidadItemPedido(pedidoId, id, c) },
                        onEliminarItem = { id -> viewModel.eliminarItemDePedido(pedidoId, id) },
                        onConfirmDelete = { id -> showDeleteConfirm = id },
                        onGuardar = { viewModel.marcarCambiosGuardados() },
                        onBack = {
                            if (uiState.comandaTemporal.hayCambiosSinGuardar) {
                                showDiscardConfirm = true
                            } else {
                                viewModel.descartarCambiosYVolver()
                            }
                        }
                    )

                    // Confirmación eliminar item
                    showDeleteConfirm?.let { productoId ->
                        val producto = pedido.items.find { it.producto.id == productoId }?.producto
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = null },
                            title = { Text("Eliminar producto") },
                            text = { Text("Se eliminará \"${producto?.nombre}\" de la comanda") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.eliminarItemDePedido(pedidoId, productoId)
                                        showDeleteConfirm = null
                                    }
                                ) {
                                    Text("Eliminar", color = CardRed)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = null }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }

                    // Confirmación descartar cambios
                    if (showDiscardConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDiscardConfirm = false },
                            title = { Text("¿Descartar cambios?") },
                            text = { Text("Tienes cambios sin guardar. ¿Quieres descartarlos?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDiscardConfirm = false
                                        viewModel.descartarCambiosYVolver()
                                    }
                                ) {
                                    Text("Descartar", color = CardRed)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDiscardConfirm = false }) {
                                    Text("Seguir editando")
                                }
                            }
                        )
                    }
                }
            }

            else -> PasoCategoria(
                categoriaActual = uiState.comandaTemporal.categoriaActual,
                onSeleccionar = { viewModel.seleccionarCategoria(it) },
                onCerrarMesa = { viewModel.iniciarCierreMesa(it) },
                onVerPedidos = { viewModel.verPedidosMesa(it) },
                mesaIdActual = uiState.comandaTemporal.mesaId,
                onBack = onBack
            )
        }
    }

    // Dialog de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("✅ Enviado") },
            text = {
                Column {
                    Text("Pedido enviado a cocina")
                    Text("Mesa ${uiState.comandaTemporal.mesaId}", fontWeight = FontWeight.Bold)
                    Text("Items: ${uiState.comandaTemporal.itemCount} · Total: $${uiState.comandaTemporal.total.toInt()}")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    onSuccess()
                }) {
                    Text("Nueva Comanda")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    onBack()
                }) {
                    Text("Volver al dashboard")
                }
            }
        )
    }
}

// ===============================
// PASO 1: SELECCIONAR CATEGORIA
// ===============================

@Composable
private fun PasoCategoria(
    categoriaActual: Categoria?,
    onSeleccionar: (Categoria) -> Unit,
    onCerrarMesa: (Int) -> Unit,
    onVerPedidos: (Int) -> Unit,
    mesaIdActual: Int?,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                text = "Nueva Comanda",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(SpacingXl))

        Text(
            text = "¿Qué categoría?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(SpacingLg))

        // Grid de categorías
        Column(
            verticalArrangement = Arrangement.spacedBy(SpacingMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingMd)
            ) {
                TarjetaCategoria(
                    categoria = Categoria.BEBIDAS,
                    seleccionado = categoriaActual == Categoria.BEBIDAS,
                    modifier = Modifier.weight(1f),
                    onClick = { onSeleccionar(Categoria.BEBIDAS) }
                )
                TarjetaCategoria(
                    categoria = Categoria.JUGOS,
                    seleccionado = categoriaActual == Categoria.JUGOS,
                    modifier = Modifier.weight(1f),
                    onClick = { onSeleccionar(Categoria.JUGOS) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingMd)
            ) {
                TarjetaCategoria(
                    categoria = Categoria.COMIDA,
                    seleccionado = categoriaActual == Categoria.COMIDA,
                    modifier = Modifier.weight(1f),
                    onClick = { onSeleccionar(Categoria.COMIDA) }
                )
                TarjetaCategoria(
                    categoria = Categoria.PANES,
                    seleccionado = categoriaActual == Categoria.PANES,
                    modifier = Modifier.weight(1f),
                    onClick = { onSeleccionar(Categoria.PANES) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SpacingMd)
            ) {
                TarjetaCategoria(
                    categoria = Categoria.COMPLEMENTOS,
                    seleccionado = categoriaActual == Categoria.COMPLEMENTOS,
                    modifier = Modifier.weight(1f),
                    onClick = { onSeleccionar(Categoria.COMPLEMENTOS) }
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón cerrar mesa
        mesaIdActual?.let { mesaId ->
            Spacer(modifier = Modifier.height(SpacingMd))
            BotonSecundario(
                texto = "CERRAR MESA $mesaId",
                onClick = { onCerrarMesa(mesaId) }
            )
            Spacer(modifier = Modifier.height(SpacingSm))
            TextButton(
                onClick = { onVerPedidos(mesaId) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("VER PEDIDOS DE ESTA MESA")
            }
        }
    }
}

@Composable
private fun TarjetaCategoria(
    categoria: Categoria,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = when (categoria) {
        Categoria.BEBIDAS -> CardBlue
        Categoria.JUGOS -> CardGreen
        Categoria.COMIDA -> CardYellow
        Categoria.PANES -> CardPurple
        Categoria.COMPLEMENTOS -> CardBlue
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = ShapeCard,
        color = if (seleccionado) ActionBg else backgroundColor,
        border = if (!seleccionado) null else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SpacingMd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = categoria.emoji,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(SpacingSm))
            Text(
                text = categoria.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (seleccionado) ActionFg else TextPrimary
            )
        }
    }
}

// ===============================
// PASO 2: AGREGAR PRODUCTOS
// ===============================

@Composable
private fun PasoProductos(
    categoria: Categoria,
    productos: List<Producto>,
    comandaTemporal: com.jlls.touchfruit.data.model.ComandaTemporal,
    onAgregar: (Producto, Int) -> Unit,
    onActualizarCantidad: (String, Int) -> Unit,
    onQuitar: (String) -> Unit,
    onVerResumen: () -> Unit,
    onBack: () -> Unit
) {
    val productosConCantidad = productos.associate { producto ->
        val cantidad = comandaTemporal.items.find { it.producto.id == producto.id }?.cantidad ?: 0
        producto.id to cantidad
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SpacingLg)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Atras")
            }
            TextButton(onClick = onBack) {
                Text(categoria.displayName, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(SpacingMd))

        Text(
            text = categoria.displayName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(SpacingLg))

        // Lista de productos (filas horizontales)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SpacingSm)
        ) {
            productos.forEach { producto ->
                TarjetaProductoRow(
                    nombre = producto.nombre,
                    precio = producto.precio,
                    cantidad = productosConCantidad[producto.id] ?: 0,
                    onMas = { onAgregar(producto, 1) },
                    onMenos = {
                        val actual = productosConCantidad[producto.id] ?: 0
                        if (actual > 0) {
                            onActualizarCantidad(producto.id, actual - 1)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(SpacingMd))

        // Resumen parcial y botones
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgSurface, ShapeCard)
                    .padding(SpacingMd),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comanda actual: ${comandaTemporal.itemCount} items",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = "$${comandaTemporal.total.toInt()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(SpacingMd))

            if (comandaTemporal.itemCount > 0) {
                BotonPrincipal(
                    texto = "VER RESUMEN (${comandaTemporal.itemCount})",
                    onClick = onVerResumen
                )
            }

            Spacer(modifier = Modifier.height(SpacingSm))

            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Cambiar categoría")
            }
        }
    }
}

// ===============================
// PASO 3: REVISAR RESUMEN
// ===============================

@Composable
private fun PasoResumen(
    comandaTemporal: com.jlls.touchfruit.data.model.ComandaTemporal,
    onAgregarMas: () -> Unit,
    onEnviar: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SpacingLg)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Atras")
            }
            Text(
                text = "Resumen",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(SpacingXl))

        Text(
            text = "Revisa tu comanda",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(SpacingLg))

        // Lista de items
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SpacingSm)
        ) {
            comandaTemporal.items.forEach { item ->
                ItemResumenComanda(
                    nombre = item.producto.nombre,
                    cantidad = item.cantidad,
                    subtotal = item.subtotal
                )
            }
        }

        // Total
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurface, ShapeCard)
                .padding(SpacingMd),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Items: ${comandaTemporal.itemCount}",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Text(
                text = "Total: $${comandaTemporal.total.toInt()}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(SpacingMd))

        // Botones
        BotonSecundario(
            texto = "AGREGAR MÁS",
            onClick = onAgregarMas
        )

        Spacer(modifier = Modifier.height(SpacingSm))

        BotonPrincipal(
            texto = "ENVIAR A COCINA",
            onClick = onEnviar
        )
    }
}

// ===============================
// PASO 4: SELECCIONAR MESA
// ===============================

@Composable
private fun PasoSeleccionarMesa(
    mesasAbiertas: List<com.jlls.touchfruit.data.model.Mesa>,
    onSeleccionar: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SpacingLg)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Atras")
            }
            Text(
                text = "Asignar a Mesa",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(SpacingXl))

        Text(
            text = "Selecciona la mesa",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(SpacingLg))

        // Grid de mesas abiertas
        val mesasOrdenadas = mesasAbiertas.sortedBy { it.id }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SpacingSm)
        ) {
            mesasOrdenadas.chunked(5).forEach { fila ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SpacingSm)
                ) {
                    fila.forEach { mesa ->
                        CeldaMesa(
                            mesaId = mesa.id,
                            estaAbierta = true,
                            modifier = Modifier.weight(1f),
                            onClick = { onSeleccionar(mesa.id) }
                        )
                    }
                    // Completar espacios vacíos si la fila no está completa
                    repeat(5 - fila.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SpacingMd))

        // Info
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeCard,
            color = CardBlue
        ) {
            Text(
                text = "Solo mesas abiertas (con sesión activa)",
                fontSize = 14.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(SpacingMd)
            )
        }
    }
}

// ===============================
// PASO 5: CONFIRMAR Y ENVIAR
// ===============================

@Composable
private fun PasoConfirmacion(
    comandaTemporal: com.jlls.touchfruit.data.model.ComandaTemporal,
    onEnviar: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SpacingLg)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Atras")
            }
            Text(
                text = "Mesa ${comandaTemporal.mesaId}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(SpacingXl))

        Text(
            text = "Listo para enviar",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(SpacingLg))

        // Lista de items
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SpacingSm)
        ) {
            comandaTemporal.items.forEach { item ->
                ItemResumenComanda(
                    nombre = item.producto.nombre,
                    cantidad = item.cantidad,
                    subtotal = item.subtotal
                )
            }
        }

        // Total y mesa
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurface, ShapeCard)
                .padding(SpacingMd),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Mesa: ${comandaTemporal.mesaId}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Total: $${comandaTemporal.total.toInt()}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(SpacingLg))

        BotonPrincipal(
            texto = "ENVIAR PEDIDO",
            onClick = onEnviar
        )
    }
}

// ===============================
// PASO CERRAR MESA: SELECCIONAR SESION
// ===============================

@Composable
private fun PasoSeleccionarSesionCierre(
    sesiones: List<com.jlls.touchfruit.data.model.Sesion>,
    mesaId: Int,
    onSeleccionar: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SpacingLg)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Atras")
            }
            Text(
                text = "Cerrar Mesa",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(SpacingXl))

        Text(
            text = "Selecciona la sesión",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(SpacingLg))

        // Lista de sesiones
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SpacingSm)
        ) {
            sesiones.forEachIndexed { index, sesion ->
                Surface(
                    onClick = { onSeleccionar(sesion.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeCard,
                    color = BgSurface,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SpacingMd),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Sesión ${index + 1} - Mesa $mesaId",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Abierta: ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(sesion.abiertaEn))}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Text(text = "→", fontSize = 20.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SpacingMd))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeCard,
            color = CardYellow
        ) {
            Text(
                text = "Selecciona la sesión activa para cerrar y ver el resumen de pedidos",
                fontSize = 14.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(SpacingMd)
            )
        }
    }
}

// ===============================
// PASO RESUMEN CIERRE: VER PEDIDOS Y TOTAL
// ===============================

@Composable
private fun PasoResumenCierre(
    pedidos: List<com.jlls.touchfruit.data.model.Pedido>,
    mesaId: Int,
    onConfirmarCierre: () -> Unit,
    onBack: () -> Unit
) {
    val totalSesion = pedidos.sumOf { it.total }
    val itemCountSesion = pedidos.sumOf { it.itemCount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SpacingLg)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Atras")
            }
            Text(
                text = "Mesa $mesaId",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(SpacingXl))

        Text(
            text = "Resumen de cierre",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(SpacingLg))

        // Lista de pedidos por sesión
        Column(
            modifier = Modifier.weight(1f)
        ) {
            pedidos.forEachIndexed { index, pedido ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeCard,
                    color = BgSurface,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SpacingMd)
                    ) {
                        Text(
                            text = "Pedido ${index + 1}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(SpacingSm))
                        pedido.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.cantidad}x ${item.producto.nombre}",
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "$${item.subtotal.toInt()}",
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(SpacingSm))
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Subtotal pedido ${index + 1}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$${pedido.total.toInt()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(SpacingSm))
            }
        }

        // Total general
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgSurface, ShapeCard)
                .padding(SpacingMd),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Total Mesa $mesaId",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$itemCountSesion items · ${pedidos.size} pedidos",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = "$${totalSesion.toInt()}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = CardGreen
            )
        }

        Spacer(modifier = Modifier.height(SpacingLg))

        BotonPrincipal(
            texto = "CONFIRMAR PAGO Y CERRAR",
            onClick = onConfirmarCierre
        )
    }
}

// ===============================
// VER PEDIDOS DE UNA MESA
// ===============================

@Composable
private fun PasoVerPedidosMesa(
    pedidos: List<com.jlls.touchfruit.data.model.Pedido>,
    mesaId: Int,
    onEditarPedido: (String) -> Unit,
    onEliminarPedido: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SpacingLg)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Atras")
            }
            Text(
                text = "Mesa $mesaId",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(SpacingXl))

        Text(
            text = "Pedidos activos",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(SpacingLg))

        if (pedidos.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = BgSurface
            ) {
                Text(
                    text = "No hay pedidos activos en esta mesa",
                    fontSize = 16.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(SpacingLg)
                )
            }
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SpacingSm)
            ) {
                pedidos.forEachIndexed { index, pedido ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Pedido ${index + 1}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = pedido.estado.name,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = "$${pedido.total.toInt()}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(SpacingSm))
                            pedido.items.forEach { item ->
                                Text(
                                    text = "${item.cantidad}x ${item.producto.nombre}",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(SpacingMd))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(SpacingSm)
                            ) {
                                BotonSecundario(
                                    texto = "ELIMINAR",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onEliminarPedido(pedido.id) }
                                )
                                BotonPrincipal(
                                    texto = "EDITAR",
                                    modifier = Modifier.weight(1f),
                                    enabled = true,
                                    onClick = { onEditarPedido(pedido.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===============================
// EDITAR PEDIDO
// ===============================

@Composable
private fun PasoEditarPedido(
    pedido: com.jlls.touchfruit.data.model.Pedido?,
    hayCambiosSinGuardar: Boolean,
    onActualizarCantidad: (String, Int) -> Unit,
    onEliminarItem: (String) -> Unit,
    onConfirmDelete: (String) -> Unit,
    onGuardar: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SpacingLg)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Atras")
            }
            Text(
                text = "Editar Pedido",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(SpacingXl))

        Text(
            text = "Modifica o elimina items",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(SpacingLg))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SpacingSm)
        ) {
            pedido?.items?.forEach { item ->
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.producto.nombre,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$${item.subtotal.toInt()}",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                        StepperCantidad(
                            cantidad = item.cantidad,
                            onMas = { onActualizarCantidad(item.producto.id, item.cantidad + 1) },
                            onMenos = {
                                if (item.cantidad > 1) {
                                    onActualizarCantidad(item.producto.id, item.cantidad - 1)
                                } else {
                                    onConfirmDelete(item.producto.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Total y Actualizar
        Column {
            if (hayCambiosSinGuardar) {
                Spacer(modifier = Modifier.height(SpacingSm))
                BotonRelleno(
                    texto = "ACTUALIZAR COMANDA",
                    color = CardOrange,
                    textColor = Color.White,
                    onClick = onGuardar
                )
                Spacer(modifier = Modifier.height(SpacingSm))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgSurface, ShapeCard)
                    .padding(SpacingMd),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total pedido",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$${pedido?.total?.toInt() ?: 0}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}