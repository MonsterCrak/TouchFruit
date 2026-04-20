package com.jlls.touchfruit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jlls.touchfruit.data.model.*
import com.jlls.touchfruit.data.repository.TouchFruitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// ===============================
// RECEPTOR VIEWMODEL
// ===============================

data class ReceptorUiState(
    val pedidosActivos: List<Pedido> = emptyList(),
    val pedidoCount: Int = 0,
    val esEmpty: Boolean = true,
    val isLoading: Boolean = true
)

class ReceptorViewModel : ViewModel() {

    private val repo = TouchFruitRepository

    private val _uiState = MutableStateFlow(ReceptorUiState())
    val uiState: StateFlow<ReceptorUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        observarPedidosActivos()
    }

    private fun observarPedidosActivos() {
        viewModelScope.launch {
            // Primero cargar datos iniciales antes de observar
            repo.refreshPedidosCache()

            // Observar ambos flows y combinarlos
            combine(
                repo.getPedidosActivosFlow(),
                repo.getMesasFlow()
            ) { pedidos, _ ->
                ReceptorUiState(
                    pedidosActivos = pedidos,
                    pedidoCount = pedidos.size,
                    esEmpty = pedidos.isEmpty(),
                    isLoading = false
                )
            }.collect { nuevoState ->
                _uiState.value = nuevoState
                _isRefreshing.value = false
            }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repo.refreshPedidosCache()
        }
    }

    fun iniciarPedido(pedidoId: String) {
        viewModelScope.launch {
            repo.actualizarEstadoPedido(pedidoId, EstadoPedido.EN_PREPARACION)
        }
    }

    fun marcarListo(pedidoId: String) {
        viewModelScope.launch {
            repo.actualizarEstadoPedido(pedidoId, EstadoPedido.LISTO)
        }
    }

    fun cerrarMesa(mesaId: Int) {
        viewModelScope.launch {
            repo.cerrarMesa(mesaId)
        }
    }

    // Reportes
    fun getPedidosDelDia(): List<Pedido> {
        val hoy = System.currentTimeMillis()
        val inicioDia = hoy - (hoy % 86400000) // Medianoche de hoy
        return repo.getPedidosEnviados().filter { it.enviadoEn != null && it.enviadoEn >= inicioDia }
    }

    fun getPedidosDeLaSemana(): List<Pedido> {
        val ahora = System.currentTimeMillis()
        val inicioSemana = ahora - (7 * 86400000)
        return repo.getPedidosEnviados().filter { it.enviadoEn != null && it.enviadoEn >= inicioSemana }
    }

    fun getPedidosDelMes(): List<Pedido> {
        val ahora = System.currentTimeMillis()
        val inicioMes = ahora - (30 * 86400000)
        return repo.getPedidosEnviados().filter { it.enviadoEn != null && it.enviadoEn >= inicioMes }
    }

    fun getMesasAbiertas(): List<Mesa> = repo.getMesasAbiertas()
}
