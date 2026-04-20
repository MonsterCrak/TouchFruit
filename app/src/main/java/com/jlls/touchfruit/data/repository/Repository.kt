package com.jlls.touchfruit.data.repository

import com.jlls.touchfruit.data.local.DatabaseProvider
import com.jlls.touchfruit.data.local.EntityMappers.toDomain
import com.jlls.touchfruit.data.local.EntityMappers.toEntity
import com.jlls.touchfruit.data.local.entity.PedidoEntity
import com.jlls.touchfruit.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

// ===============================
// TOUCHFRUIT REPOSITORY
// Single source of truth for app state
// backed by Room database
// ===============================

object TouchFruitRepository {

    private val db get() = DatabaseProvider.getDatabase()

    // ===============================
    // FIREBASE SYNC (set from MainActivity)
    // ===============================

    // Sync manager for Firebase - set by FirebaseSyncService
    var syncManager: com.jlls.touchfruit.data.sync.SyncManager? = null

    // ===============================
    // PRODUCTOS (in-memory cache for domain model)
    // ===============================

    private val _productos = MutableStateFlow<List<Producto>>(emptyList())
    val productos: StateFlow<List<Producto>> = _productos.asStateFlow()

    // Sesión actual del usuario (for login)
    private val _usuarioActual = MutableStateFlow<Usuario?>(null)
    val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()

    // In-memory cache for synchronous access
    private val _mesasCache = MutableStateFlow<List<Mesa>>(emptyList())
    private val _sesionesCache = MutableStateFlow<List<Sesion>>(emptyList())
    private val _pedidosCache = MutableStateFlow<List<Pedido>>(emptyList())

    // ===============================
    // PRODUCTOS
    // ===============================

    suspend fun loadProductos() {
        _productos.value = db.productoDao().getAllProductos().first().map { it.toDomain() }
    }

    fun getProductosFlow(): Flow<List<Producto>> = _productos

    fun getProductosPorCategoria(categoria: Categoria): List<Producto> {
        return _productos.value.filter { it.categoria == categoria && it.disponible }
    }

    // ===============================
    // MESAS
    // ===============================

    fun getMesasFlow(): Flow<List<Mesa>> {
        return db.mesaDao().getAllMesas().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getMesasAbiertas(): List<Mesa> {
        return _mesasCache.value.filter { it.estado == EstadoMesa.ABIERTA }
    }

    fun getMesasCerradas(): List<Mesa> {
        return _mesasCache.value.filter { it.estado == EstadoMesa.CERRADA }
    }

    suspend fun loadInitialData() {
        _productos.value = db.productoDao().getAllProductos().first().map { it.toDomain() }
        _mesasCache.value = db.mesaDao().getAllMesas().first().map { it.toDomain() }
        // Load sessions and pedidos from database
        refreshSesionesCache()
        refreshPedidosCache()
    }

    suspend fun refreshMesas() {
        _mesasCache.value = db.mesaDao().getAllMesas().first().map { it.toDomain() }
    }

    private suspend fun refreshSesionesCache() {
        // Load all sessions for all mesas - in a real app we'd filter by recent
        val todasLasSesiones = mutableListOf<Sesion>()
        db.mesaDao().getAllMesas().first().forEach { mesa ->
            db.sesionDao().getSesionesPorMesa(mesa.id).first().forEach { sesionEntity ->
                todasLasSesiones.add(sesionEntity.toDomain())
            }
        }
        _sesionesCache.value = todasLasSesiones
    }

    suspend fun refreshPedidosCache() {
        val todosLosPedidos = mutableListOf<Pedido>()
        val productos = _productos.value
        db.pedidoDao().getPedidosActivos().first().forEach { pedidoEntity ->
            val items = db.itemPedidoDao().getItemsPorPedido(pedidoEntity.id).first()
            todosLosPedidos.add(pedidoEntity.toDomain(items, productos))
        }
        // Also add LISTO and CANCELADO for historial
        _pedidosCache.value = todosLosPedidos
    }

    // ===============================
    // SESIONES
    // ===============================

    fun getSesionActivaPorMesa(mesaId: Int): Sesion? {
        return _sesionesCache.value.find { it.mesaId == mesaId && it.cerradaEn == null }
    }

    fun getSesionesPorMesa(mesaId: Int): List<Sesion> {
        return _sesionesCache.value.filter { it.mesaId == mesaId }.sortedByDescending { it.abiertaEn }
    }

    suspend fun abrirMesa(mesaId: Int, emisorId: String): Boolean {
        val mesa = db.mesaDao().getMesaById(mesaId) ?: return false
        if (mesa.estado == EstadoMesa.ABIERTA.name) return false

        val nuevaSesion = Sesion(
            id = UUID.randomUUID().toString(),
            mesaId = mesaId,
            emisorId = emisorId
        )

        db.sesionDao().insert(nuevaSesion.toEntity())
        db.mesaDao().update(mesa.copy(estado = EstadoMesa.ABIERTA.name, sesionActivaId = nuevaSesion.id))

        _sesionesCache.value = _sesionesCache.value + nuevaSesion
        _mesasCache.value = _mesasCache.value.map {
            if (it.id == mesaId) it.copy(estado = EstadoMesa.ABIERTA, sesionId = nuevaSesion.id) else it
        }

        // Sync to Firebase
        syncManager?.syncSesionToFirebase(nuevaSesion)

        return true
    }

    suspend fun cerrarMesa(mesaId: Int) {
        val sesionActiva = getSesionActivaPorMesa(mesaId) ?: return
        val now = System.currentTimeMillis()

        // Mark all active pedidos as LISTO before closing session
        val pedidosSesion = _pedidosCache.value.filter { it.sesionId == sesionActiva.id }
        pedidosSesion.forEach { pedido ->
            if (pedido.estado != EstadoPedido.LISTO && pedido.estado != EstadoPedido.CANCELADO) {
                db.pedidoDao().actualizarEstado(pedido.id, EstadoPedido.LISTO.name, now)
            }
        }

        db.sesionDao().cerrarSesion(sesionActiva.id, now)
        db.mesaDao().update(Mesa(id = mesaId, estado = EstadoMesa.CERRADA, sesionId = null).toEntity())

        // Update pedidos cache to reflect LISTO status
        _pedidosCache.value = _pedidosCache.value.map { pedido ->
            if (pedido.sesionId == sesionActiva.id &&
                pedido.estado != EstadoPedido.LISTO &&
                pedido.estado != EstadoPedido.CANCELADO) {
                pedido.copy(estado = EstadoPedido.LISTO, completadoEn = now)
            } else pedido
        }

        _sesionesCache.value = _sesionesCache.value.map {
            if (it.id == sesionActiva.id) it.copy(cerradaEn = now) else it
        }
        _mesasCache.value = _mesasCache.value.map {
            if (it.id == mesaId) it.copy(estado = EstadoMesa.CERRADA, sesionId = null) else it
        }

        // Sync to Firebase
        syncManager?.syncSesionCerrada(sesionActiva.id)
    }

    // ===============================
    // PEDIDOS
    // ===============================

    fun getPedidosPorSesion(sesionId: String): List<Pedido> {
        return _pedidosCache.value.filter { it.sesionId == sesionId }.sortedBy { it.enviadoEn }
    }

    fun getPedidosActivos(): List<Pedido> {
        return _pedidosCache.value.filter {
            it.estado != EstadoPedido.LISTO && it.estado != EstadoPedido.CANCELADO
        }
    }

    fun getPedidosActivosPorMesa(mesaId: Int): List<Pedido> {
        val sesionActiva = getSesionActivaPorMesa(mesaId) ?: return emptyList()
        return getPedidosPorSesion(sesionActiva.id)
    }

    fun getPedidosActivosFlow(): Flow<List<Pedido>> {
        return db.pedidoDao().getPedidosActivos().map { entities ->
            entities.map { entity ->
                val productos = _productos.value
                val items = db.itemPedidoDao().getItemsPorPedido(entity.id).first()
                entity.toDomain(items, productos)
            }
        }
    }

    suspend fun crearPedido(sesionId: String, mesaId: Int, items: List<ItemPedido>): Pedido {
        val productos = _productos.value

        // Buscar si ya existe un pedido NUEVO para esta sesión
        val pedidoExistente = db.pedidoDao().getPedidoNuevoPorSesion(sesionId)

        if (pedidoExistente != null) {
            // Agregar items al pedido existente
            items.forEach { item ->
                val existente = db.itemPedidoDao().getItemsPorPedido(pedidoExistente.id).first()
                    .find { it.productoId == item.producto.id }

                if (existente != null) {
                    db.itemPedidoDao().actualizarCantidad(
                        pedidoExistente.id,
                        item.producto.id,
                        existente.cantidad + item.cantidad
                    )
                } else {
                    db.itemPedidoDao().insert(
                        com.jlls.touchfruit.data.local.entity.ItemPedidoEntity(
                            pedidoId = pedidoExistente.id,
                            productoId = item.producto.id,
                            cantidad = item.cantidad
                        )
                    )
                }
            }

            // Refrescar cache
            val pedidoActualizado = db.pedidoDao().getPedidoById(pedidoExistente.id)!!
            val pedidoItems = db.itemPedidoDao().getItemsPorPedido(pedidoExistente.id).first()
            val domainPedido = pedidoActualizado.toDomain(pedidoItems, productos)

            _pedidosCache.value = _pedidosCache.value.map {
                if (it.id == pedidoExistente.id) domainPedido else it
            }

            return domainPedido
        }

        // Crear nuevo pedido
        val pedidoId = "PED-${System.currentTimeMillis()}"
        val nuevoPedido = PedidoEntity(
            id = pedidoId,
            sesionId = sesionId,
            mesaId = mesaId,
            estado = "NUEVO",
            creadoEn = System.currentTimeMillis(),
            enviadoEn = System.currentTimeMillis()
        )

        db.pedidoDao().insert(nuevoPedido)

        // Insertar items
        val itemEntities = items.map {
            com.jlls.touchfruit.data.local.entity.ItemPedidoEntity(
                pedidoId = pedidoId,
                productoId = it.producto.id,
                cantidad = it.cantidad
            )
        }
        db.itemPedidoDao().insertAll(itemEntities)

        val domainPedido = nuevoPedido.toDomain(itemEntities, productos)
        _pedidosCache.value = _pedidosCache.value + domainPedido

        // Sync to Firebase
        syncManager?.syncPedidoToFirebase(domainPedido)

        return domainPedido
    }

    suspend fun actualizarEstadoPedido(pedidoId: String, nuevoEstado: EstadoPedido) {
        val completadoEn = if (nuevoEstado == EstadoPedido.LISTO) System.currentTimeMillis() else null
        db.pedidoDao().actualizarEstado(pedidoId, nuevoEstado.name, completadoEn)

        _pedidosCache.value = _pedidosCache.value.map {
            if (it.id == pedidoId) {
                it.copy(
                    estado = nuevoEstado,
                    completadoEn = completadoEn
                )
            } else it
        }

        // Sync to Firebase
        syncManager?.syncEstadoToFirebase(pedidoId, nuevoEstado)
    }

    suspend fun eliminarPedido(pedidoId: String) {
        db.itemPedidoDao().deleteAllByPedido(pedidoId)
        db.pedidoDao().deleteById(pedidoId)

        _pedidosCache.value = _pedidosCache.value.filter { it.id != pedidoId }
    }

    suspend fun actualizarCantidadItemPedido(pedidoId: String, productoId: String, nuevaCantidad: Int) {
        if (nuevaCantidad <= 0) {
            db.itemPedidoDao().deleteItem(pedidoId, productoId)
        } else {
            db.itemPedidoDao().actualizarCantidad(pedidoId, productoId, nuevaCantidad)
        }

        // Refrescar cache
        val pedidoEntity = db.pedidoDao().getPedidoById(pedidoId)
        val productos = _productos.value
        if (pedidoEntity != null) {
            val items = db.itemPedidoDao().getItemsPorPedido(pedidoId).first()
            val domainPedido = pedidoEntity.toDomain(items, productos)
            _pedidosCache.value = _pedidosCache.value.map {
                if (it.id == pedidoId) domainPedido else it
            }
        }
    }

    suspend fun eliminarItemDePedido(pedidoId: String, productoId: String) {
        db.itemPedidoDao().deleteItem(pedidoId, productoId)

        // Refrescar cache
        val pedidoEntity = db.pedidoDao().getPedidoById(pedidoId)
        val productos = _productos.value
        if (pedidoEntity != null) {
            val items = db.itemPedidoDao().getItemsPorPedido(pedidoId).first()
            val domainPedido = pedidoEntity.toDomain(items, productos)
            _pedidosCache.value = _pedidosCache.value.map {
                if (it.id == pedidoId) domainPedido else it
            }
        }
    }

    fun getPedidosEnviados(): List<Pedido> {
        return _pedidosCache.value.filter { it.enviadoEn != null }.sortedByDescending { it.enviadoEn }
    }

    fun getTodasLasSesiones(): List<Sesion> {
        return _sesionesCache.value
    }

    fun todasLasSesionesFlow(): Flow<List<Sesion>> = _sesionesCache

    fun getTodosLosPedidos(): List<Pedido> {
        return _pedidosCache.value
    }

    fun todosLosPedidosFlow(): Flow<List<Pedido>> = _pedidosCache

    suspend fun cerrarMesaConPedidos(mesaId: Int) {
        val sesionesActivas = _sesionesCache.value.filter { it.mesaId == mesaId && it.cerradaEn == null }
        val now = System.currentTimeMillis()

        sesionesActivas.forEach { sesion ->
            val pedidos = db.pedidoDao().getPedidosPorSesion(sesion.id).first()
            pedidos.forEach { pedido ->
                if (pedido.estado != EstadoPedido.LISTO.name && pedido.estado != EstadoPedido.CANCELADO.name) {
                    db.pedidoDao().actualizarEstado(pedido.id, EstadoPedido.LISTO.name, now)
                }
            }
            db.sesionDao().cerrarSesion(sesion.id, now)
        }

        db.mesaDao().update(Mesa(id = mesaId, estado = EstadoMesa.CERRADA, sesionId = null).toEntity())

        // Update caches - mark pedidos as LISTO in _pedidosCache
        _pedidosCache.value = _pedidosCache.value.map { pedido ->
            val sesionActiva = sesionesActivas.find { it.id == pedido.sesionId }
            if (sesionActiva != null && pedido.estado != EstadoPedido.LISTO && pedido.estado != EstadoPedido.CANCELADO) {
                pedido.copy(estado = EstadoPedido.LISTO, completadoEn = now)
            } else pedido
        }

        _sesionesCache.value = _sesionesCache.value.map {
            if (it.mesaId == mesaId && it.cerradaEn == null) {
                it.copy(cerradaEn = now)
            } else it
        }
        _mesasCache.value = _mesasCache.value.map {
            if (it.id == mesaId) it.copy(estado = EstadoMesa.CERRADA, sesionId = null) else it
        }

        // Sync to Firebase - close each sesion
        sesionesActivas.forEach { sesion ->
            syncManager?.syncSesionCerrada(sesion.id)
        }
    }

    // ===============================
    // LOGIN
    // ===============================

    fun login(codigo: String): Boolean {
        val usuario = when {
            codigo.startsWith("E") -> Usuario(
                id = UUID.randomUUID().toString(),
                codigo = codigo,
                nombre = "Emisor",
                rol = RolUsuario.EMISOR
            )
            codigo.startsWith("R") -> Usuario(
                id = UUID.randomUUID().toString(),
                codigo = codigo,
                nombre = "Receptor",
                rol = RolUsuario.RECEPTOR
            )
            else -> null
        }

        if (usuario != null) {
            _usuarioActual.value = usuario
            return true
        }
        return false
    }

    fun logout() {
        _usuarioActual.value = null
    }
}
