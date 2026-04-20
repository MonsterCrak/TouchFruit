package com.jlls.touchfruit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jlls.touchfruit.data.model.*
import com.jlls.touchfruit.data.repository.TouchFruitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ===============================
// EMISOR VIEWMODEL
// ===============================

data class EmisorUiState(
    val mesas: List<Mesa> = emptyList(),
    val comandaTemporal: ComandaTemporal = ComandaTemporal(),
    val pasoActual: PasoComanda = PasoComanda.CATEGORIA,
    val esNuevo: Boolean = true,
    val isLoading: Boolean = true
)

enum class PasoComanda {
    CATEGORIA,
    PRODUCTOS,
    RESUMEN,
    CONFIRMACION,
    CERRAR_MESA,
    SESSIONES_MESA,
    RESUMEN_CIERRE,
    VER_PEDIDOS_MESA,
    EDITAR_PEDIDO
}

class EmisorViewModel : ViewModel() {

    private val repo = TouchFruitRepository

    private val _uiState = MutableStateFlow(EmisorUiState())
    val uiState: StateFlow<EmisorUiState> = _uiState.asStateFlow()

    private val _pedidosFlow = MutableStateFlow<Pedido?>(null)
    val pedidosFlow: StateFlow<Pedido?> = _pedidosFlow.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        cargarDatos()
        observarMesas()
    }

    private fun cargarDatos() {
        viewModelScope.launch {
            repo.loadInitialData()
            repo.loadProductos()
            refreshMesasFromCache()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun observarMesas() {
        viewModelScope.launch {
            repo.getMesasFlow().collect { mesas ->
                _uiState.value = _uiState.value.copy(
                    mesas = mesas,
                    isLoading = false
                )
            }
        }
    }

    private suspend fun refreshMesasFromCache() {
        repo.refreshMesas()
        _uiState.value = _uiState.value.copy(mesas = repo.getMesasAbiertas() + repo.getMesasCerradas())
    }

    fun onRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repo.refreshPedidosCache()
            repo.refreshMesas()
            _isRefreshing.value = false
        }
    }

    // ===============================
    // ACCIONES DE MESA
    // ===============================

    fun abrirMesa(mesaId: Int) {
        val usuario = repo.usuarioActual.value ?: return
        viewModelScope.launch {
            repo.abrirMesa(mesaId, usuario.id)
            refreshMesasFromCache()
        }
    }

    fun iniciarNuevaComanda(mesaId: Int? = null) {
        val sesionId = if (mesaId != null) repo.getSesionActivaPorMesa(mesaId)?.id else null
        _uiState.value = _uiState.value.copy(
            comandaTemporal = ComandaTemporal(mesaId = mesaId, sesionId = sesionId),
            pasoActual = PasoComanda.CATEGORIA,
            esNuevo = true
        )
    }

    // ===============================
    // FLUJO DE COMANDA
    // ===============================

    fun seleccionarCategoria(categoria: Categoria) {
        _uiState.value = _uiState.value.copy(
            comandaTemporal = _uiState.value.comandaTemporal.copy(categoriaActual = categoria),
            pasoActual = PasoComanda.PRODUCTOS
        )
    }

    fun agregarProducto(producto: Producto, cantidad: Int = 1) {
        val comanda = _uiState.value.comandaTemporal
        val nuevaLista = comanda.items.toMutableList()
        val existente = nuevaLista.find { it.producto.id == producto.id }
        if (existente != null) {
            val index = nuevaLista.indexOf(existente)
            nuevaLista[index] = existente.copy(cantidad = existente.cantidad + cantidad)
        } else {
            nuevaLista.add(ItemPedido(producto, cantidad))
        }
        _uiState.value = _uiState.value.copy(
            comandaTemporal = comanda.copy(items = nuevaLista)
        )
    }

    fun actualizarCantidad(productoId: String, nuevaCantidad: Int) {
        val comanda = _uiState.value.comandaTemporal
        if (nuevaCantidad <= 0) {
            val nuevaLista = comanda.items.filter { it.producto.id != productoId }.toMutableList()
            _uiState.value = _uiState.value.copy(
                comandaTemporal = comanda.copy(items = nuevaLista)
            )
        } else {
            val nuevaLista = comanda.items.map { item ->
                if (item.producto.id == productoId) {
                    item.copy(cantidad = nuevaCantidad)
                } else item
            }.toMutableList()
            _uiState.value = _uiState.value.copy(
                comandaTemporal = comanda.copy(items = nuevaLista)
            )
        }
    }

    fun quitarItem(productoId: String) {
        val comanda = _uiState.value.comandaTemporal
        val nuevaLista = comanda.items.filter { it.producto.id != productoId }.toMutableList()
        _uiState.value = _uiState.value.copy(
            comandaTemporal = comanda.copy(items = nuevaLista)
        )
    }

    fun irAResumen() {
        _uiState.value = _uiState.value.copy(pasoActual = PasoComanda.RESUMEN)
    }

    fun agregarMasProductos() {
        _uiState.value = _uiState.value.copy(pasoActual = PasoComanda.PRODUCTOS)
    }

    fun irASeleccionarMesa() {
        _uiState.value = _uiState.value.copy(pasoActual = PasoComanda.CONFIRMACION)
    }

    fun asignarMesa(mesaId: Int) {
        val comanda = _uiState.value.comandaTemporal
        _uiState.value = _uiState.value.copy(
            comandaTemporal = comanda.copy(mesaId = mesaId),
            pasoActual = PasoComanda.CONFIRMACION
        )
    }

    fun enviarPedido(): Boolean {
        val comanda = _uiState.value.comandaTemporal
        val mesaId = comanda.mesaId ?: return false
        val sesionId = comanda.sesionId ?: repo.getSesionActivaPorMesa(mesaId)?.id ?: return false

        viewModelScope.launch {
            repo.crearPedido(sesionId, mesaId, comanda.items.toList())
        }

        // Resetear comanda temporal
        _uiState.value = _uiState.value.copy(
            comandaTemporal = ComandaTemporal(),
            pasoActual = PasoComanda.CATEGORIA,
            esNuevo = false
        )

        return true
    }

    fun cancelarComanda() {
        _uiState.value = _uiState.value.copy(
            comandaTemporal = ComandaTemporal(),
            pasoActual = PasoComanda.CATEGORIA
        )
    }

    fun volverAtras(): Boolean {
        val pasoActual = _uiState.value.pasoActual
        val nuevoPaso = when (pasoActual) {
            PasoComanda.CATEGORIA -> null // No hay atrás
            PasoComanda.PRODUCTOS -> PasoComanda.CATEGORIA
            PasoComanda.RESUMEN -> PasoComanda.PRODUCTOS
            PasoComanda.CONFIRMACION -> PasoComanda.RESUMEN
            PasoComanda.CERRAR_MESA -> null // No hay atrás
            PasoComanda.SESSIONES_MESA -> PasoComanda.CERRAR_MESA
            PasoComanda.RESUMEN_CIERRE -> PasoComanda.SESSIONES_MESA
            PasoComanda.VER_PEDIDOS_MESA -> PasoComanda.CATEGORIA
            PasoComanda.EDITAR_PEDIDO -> PasoComanda.VER_PEDIDOS_MESA
        }

        if (nuevoPaso == null) return false

        _uiState.value = _uiState.value.copy(pasoActual = nuevoPaso)
        return true
    }

    fun iniciarCierreMesa(mesaId: Int) {
        _uiState.value = _uiState.value.copy(
            comandaTemporal = ComandaTemporal(mesaId = mesaId),
            pasoActual = PasoComanda.CERRAR_MESA
        )
    }

    fun seleccionarSesionCierre(sesionId: String) {
        _uiState.value = _uiState.value.copy(
            comandaTemporal = _uiState.value.comandaTemporal.copy(sesionIdCierre = sesionId),
            pasoActual = PasoComanda.RESUMEN_CIERRE
        )
    }

    fun verPedidosMesa(mesaId: Int) {
        _uiState.value = _uiState.value.copy(
            comandaTemporal = _uiState.value.comandaTemporal.copy(mesaId = mesaId),
            pasoActual = PasoComanda.VER_PEDIDOS_MESA
        )
    }

    fun seleccionarPedidoEditar(pedidoId: String) {
        val pedido = repo.getPedidosActivos().find { it.id == pedidoId }
        _uiState.value = _uiState.value.copy(
            comandaTemporal = _uiState.value.comandaTemporal.copy(
                pedidoIdEditar = pedidoId,
                hayCambiosSinGuardar = false,
                totalOriginal = pedido?.total ?: 0.0
            ),
            pasoActual = PasoComanda.EDITAR_PEDIDO
        )
        refreshPedidoEditable(pedidoId)
    }

    fun marcarCambiosSinGuardar() {
        _uiState.value = _uiState.value.copy(
            comandaTemporal = _uiState.value.comandaTemporal.copy(hayCambiosSinGuardar = true)
        )
    }

    fun marcarCambiosGuardados() {
        val pedidoId = _uiState.value.comandaTemporal.pedidoIdEditar
        val pedido = if (pedidoId != null) repo.getPedidosActivos().find { it.id == pedidoId } else null
        _uiState.value = _uiState.value.copy(
            comandaTemporal = _uiState.value.comandaTemporal.copy(
                hayCambiosSinGuardar = false,
                totalOriginal = pedido?.total ?: 0.0
            )
        )
    }

    fun hayCambiosSinGuardar(): Boolean {
        return _uiState.value.comandaTemporal.hayCambiosSinGuardar
    }

    fun debeMostrarActualizar(): Boolean {
        val pedidoId = _uiState.value.comandaTemporal.pedidoIdEditar ?: return false
        val pedido = repo.getPedidosActivos().find { it.id == pedidoId } ?: return false
        val totalOriginal = _uiState.value.comandaTemporal.totalOriginal
        return pedido.total != totalOriginal
    }

    fun descartarCambiosYVolver() {
        _uiState.value = _uiState.value.copy(
            comandaTemporal = _uiState.value.comandaTemporal.copy(pedidoIdEditar = null, hayCambiosSinGuardar = false),
            pasoActual = PasoComanda.VER_PEDIDOS_MESA
        )
    }

    fun eliminarPedido(pedidoId: String) {
        viewModelScope.launch {
            repo.eliminarPedido(pedidoId)
            _uiState.value = _uiState.value.copy(
                comandaTemporal = _uiState.value.comandaTemporal.copy(pedidoIdEditar = null),
                pasoActual = PasoComanda.VER_PEDIDOS_MESA
            )
        }
    }

    fun cerrarMesaYPedidos(mesaId: Int) {
        viewModelScope.launch {
            repo.cerrarMesaConPedidos(mesaId)
            refreshMesasFromCache()
            _uiState.value = _uiState.value.copy(
                comandaTemporal = ComandaTemporal(),
                pasoActual = PasoComanda.CATEGORIA
            )
        }
    }

    fun getSesionesPorMesa(mesaId: Int): List<Sesion> {
        return repo.getSesionesPorMesa(mesaId)
    }

    fun getPedidosPorSesion(sesionId: String): List<Pedido> {
        return repo.getPedidosPorSesion(sesionId)
    }

    private val _pedidosActivosPorMesaFlow = MutableStateFlow<List<Pedido>>(emptyList())
    val pedidosActivosPorMesaFlow: StateFlow<List<Pedido>> = _pedidosActivosPorMesaFlow.asStateFlow()

    fun cargarPedidosActivosPorMesa(mesaId: Int) {
        viewModelScope.launch {
            val sesionActiva = repo.getSesionActivaPorMesa(mesaId)
            _pedidosActivosPorMesaFlow.value = if (sesionActiva != null) {
                repo.getPedidosPorSesion(sesionActiva.id).filter {
                    it.estado != EstadoPedido.LISTO && it.estado != EstadoPedido.CANCELADO
                }
            } else emptyList()
        }
    }

    fun getPedidoPorId(pedidoId: String): Pedido? {
        return repo.getPedidosActivos().find { it.id == pedidoId }
    }

    fun refreshPedidoEditable(pedidoId: String) {
        _pedidosFlow.value = repo.getPedidosActivos().find { it.id == pedidoId }
    }

    fun actualizarCantidadItemPedido(pedidoId: String, productoId: String, nuevaCantidad: Int) {
        viewModelScope.launch {
            repo.actualizarCantidadItemPedido(pedidoId, productoId, nuevaCantidad)
            refreshPedidoEditable(pedidoId)
            marcarCambiosSinGuardar()
        }
    }

    fun eliminarItemDePedido(pedidoId: String, productoId: String) {
        viewModelScope.launch {
            repo.eliminarItemDePedido(pedidoId, productoId)
            refreshPedidoEditable(pedidoId)
            marcarCambiosSinGuardar()
        }
    }

    // ===============================
    // HELPERS
    // ===============================

    fun getProductosDeCategoria(categoria: Categoria): List<Producto> {
        return repo.getProductosPorCategoria(categoria)
    }

    fun getMesasAbiertas(): List<Mesa> = repo.getMesasAbiertas()

    fun refresh() {
        viewModelScope.launch {
            refreshMesasFromCache()
        }
    }
}
