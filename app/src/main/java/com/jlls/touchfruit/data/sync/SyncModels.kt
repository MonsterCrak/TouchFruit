package com.jlls.touchfruit.data.sync

// ===============================
// FIRESTORE DTOs (Data Transfer Objects)
// Separate from domain models for Firestore serialization
// ===============================

/**
 * Represents an item in a pedido for Firestore storage.
 * Items are embedded within the PedidoDocument.
 */
data class ItemPedidoDocument(
    val productoId: String = "",
    val cantidad: Int = 0,
    val nombre: String = "",  // Denormalized for display
    val precioUnitario: Double = 0.0  // Denormalized for display
)

/**
 * Represents a pedido (order) document in Firestore.
 * Maps to the domain Pedido model.
 */
data class PedidoDocument(
    val id: String = "",
    val sesionId: String = "",
    val mesaId: Int = 0,
    val emisorId: String = "",  // Firebase UID
    val estado: String = "NUEVO",  // NUEVO, EN_PREPARACION, LISTO, CANCELADO
    val creadoEn: Long = 0,
    val enviadoEn: Long = 0,
    val completadoEn: Long? = null,
    val items: List<ItemPedidoDocument> = emptyList(),
    val total: Double = 0.0,  // Denormalized for queries
    val itemCount: Int = 0   // Denormalized for display
)

/**
 * Represents a mesa (table) document in Firestore.
 * Maps to the domain Mesa model.
 */
data class MesaDocument(
    val id: Int = 0,
    val estado: String = "CERRADA",  // ABIERTA, CERRADA
    val sesionActivaId: String? = null
)

/**
 * Represents a sesion (session) document in Firestore.
 * Maps to the domain Sesion model.
 */
data class SesionDocument(
    val id: String = "",
    val mesaId: Int = 0,
    val emisorId: String = "",  // Firebase UID
    val abiertaEn: Long = 0,
    val cerradaEn: Long? = null
)

/**
 * Represents a producto (product) document in Firestore.
 * Maps to the domain Producto model.
 */
data class ProductoDocument(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",  // BEBIDAS, JUGOS, COMIDA, PANES, COMPLEMENTOS
    val precio: Double = 0.0,
    val disponible: Boolean = true
)

/**
 * Represents a user document in Firestore.
 * Bound to Firebase Auth UID.
 */
data class UsuarioDocument(
    val codigo: String = "",  // E001, R002
    val nombre: String = "",
    val rol: String = "",  // EMISOR, RECEPTOR
    val creadoEn: Long = 0
)
