package com.jlls.touchfruit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mesas")
data class MesaEntity(
    @PrimaryKey val id: Int,
    val estado: String, // "CERRADA" o "ABIERTA"
    val sesionActivaId: String? = null
)

@Entity(tableName = "sesiones")
data class SesionEntity(
    @PrimaryKey val id: String,
    val mesaId: Int,
    val emisorId: String,
    val abiertaEn: Long,
    val cerradaEn: Long? = null
)

@Entity(tableName = "productos")
data class ProductoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val categoria: String,
    val precio: Double,
    val disponible: Boolean = true
)

@Entity(tableName = "pedidos")
data class PedidoEntity(
    @PrimaryKey val id: String,
    val sesionId: String,
    val mesaId: Int,
    val estado: String, // "NUEVO", "EN_PREPARACION", "LISTO", "CANCELADO"
    val creadoEn: Long,
    val enviadoEn: Long? = null,
    val completadoEn: Long? = null
)

@Entity(tableName = "itemspedido")
data class ItemPedidoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pedidoId: String,
    val productoId: String,
    val cantidad: Int
)

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val id: String,
    val codigo: String,
    val nombre: String,
    val rol: String // "EMISOR" o "RECEPTOR"
)
