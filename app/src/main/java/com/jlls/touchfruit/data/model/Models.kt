package com.jlls.touchfruit.data.model

// ===============================
// MESA
// ===============================

enum class EstadoMesa {
    CERRADA,
    ABIERTA
}

data class Mesa(
    val id: Int,
    val estado: EstadoMesa = EstadoMesa.CERRADA,
    val sesionId: String? = null
)

// ===============================
// SESION
// ===============================

data class Sesion(
    val id: String,
    val mesaId: Int,
    val emisorId: String,
    val abiertaEn: Long = System.currentTimeMillis(),
    val cerradaEn: Long? = null
)

// ===============================
// CATEGORIA
// ===============================

enum class Categoria(val displayName: String, val emoji: String) {
    BEBIDAS("Bebidas", "💧"),
    JUGOS("Jugos", "🍊"),
    COMIDA("Comida", "🍽"),
    PANES("Panes", "🥐"),
    COMPLEMENTOS("Complementos", "🍯")
}

// ===============================
// PRODUCTO
// ===============================

data class Producto(
    val id: String,
    val nombre: String,
    val categoria: Categoria,
    val precio: Double,
    val disponible: Boolean = true
)

// ===============================
// ITEM PEDIDO
// ===============================

data class ItemPedido(
    val producto: Producto,
    var cantidad: Int = 1
) {
    val subtotal: Double get() = producto.precio * cantidad
}

// ===============================
// ESTADO PEDIDO
// ===============================

enum class EstadoPedido {
    NUEVO,
    EN_PREPARACION,
    LISTO,
    CANCELADO
}

// ===============================
// PEDIDO (COMANDA)
// ===============================

data class Pedido(
    val id: String,
    val sesionId: String,
    val mesaId: Int,
    val items: List<ItemPedido> = emptyList(),
    val estado: EstadoPedido = EstadoPedido.NUEVO,
    val creadoEn: Long = System.currentTimeMillis(),
    val enviadoEn: Long? = null,
    val completadoEn: Long? = null
) {
    val total: Double get() = items.sumOf { it.subtotal }
    val itemCount: Int get() = items.sumOf { it.cantidad }
}

// ===============================
// USUARIO
// ===============================

enum class RolUsuario {
    EMISOR,
    RECEPTOR
}

data class Usuario(
    val id: String,
    val codigo: String,
    val nombre: String,
    val rol: RolUsuario
)

// ===============================
// COMANDA TEMPORAL (en construcción)
// ===============================

data class ComandaTemporal(
    val mesaId: Int? = null,
    val sesionId: String? = null,
    val items: List<ItemPedido> = emptyList(),
    var categoriaActual: Categoria? = null,
    var sesionIdCierre: String? = null,
    var pedidoIdEditar: String? = null,
    var hayCambiosSinGuardar: Boolean = false,
    var totalOriginal: Double = 0.0
) {
    val total: Double get() = items.sumOf { it.subtotal }
    val itemCount: Int get() = items.sumOf { it.cantidad }
    val isEmpty: Boolean get() = items.isEmpty()

    fun agregarItem(producto: Producto, cantidad: Int = 1): ComandaTemporal {
        val existente = items.find { it.producto.id == producto.id }
        val nuevaLista = if (existente != null) {
            items.map {
                if (it.producto.id == producto.id) {
                    it.copy(cantidad = it.cantidad + cantidad)
                } else it
            }
        } else {
            items + ItemPedido(producto, cantidad)
        }
        return this.copy(items = nuevaLista)
    }

    fun actualizarCantidad(productoId: String, nuevaCantidad: Int): ComandaTemporal {
        val nuevaLista = if (nuevaCantidad <= 0) {
            items.filter { it.producto.id != productoId }
        } else {
            items.map {
                if (it.producto.id == productoId) {
                    it.copy(cantidad = nuevaCantidad)
                } else it
            }
        }
        return this.copy(items = nuevaLista)
    }

    fun quitarItem(productoId: String): ComandaTemporal {
        return this.copy(items = items.filter { it.producto.id != productoId })
    }

    fun limpiar(): ComandaTemporal {
        return this.copy(items = emptyList(), mesaId = null, categoriaActual = null, sesionIdCierre = null)
    }
}