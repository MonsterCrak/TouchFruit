package com.jlls.touchfruit.data.local

import com.jlls.touchfruit.data.local.entity.*
import com.jlls.touchfruit.data.model.*

object EntityMappers {

    // ===============================
    // MESA
    // ===============================

    fun MesaEntity.toDomain(): Mesa = Mesa(
        id = id,
        estado = when (estado) {
            "ABIERTA" -> EstadoMesa.ABIERTA
            else -> EstadoMesa.CERRADA
        },
        sesionId = sesionActivaId
    )

    fun Mesa.toEntity(): MesaEntity = MesaEntity(
        id = id,
        estado = when (estado) {
            EstadoMesa.ABIERTA -> "ABIERTA"
            EstadoMesa.CERRADA -> "CERRADA"
        },
        sesionActivaId = sesionId
    )

    // ===============================
    // SESION
    // ===============================

    fun SesionEntity.toDomain(): Sesion = Sesion(
        id = id,
        mesaId = mesaId,
        emisorId = emisorId,
        abiertaEn = abiertaEn,
        cerradaEn = cerradaEn
    )

    fun Sesion.toEntity(): SesionEntity = SesionEntity(
        id = id,
        mesaId = mesaId,
        emisorId = emisorId,
        abiertaEn = abiertaEn,
        cerradaEn = cerradaEn
    )

    // ===============================
    // PRODUCTO
    // ===============================

    fun ProductoEntity.toDomain(): Producto = Producto(
        id = id,
        nombre = nombre,
        categoria = when (categoria) {
            "BEBIDAS" -> Categoria.BEBIDAS
            "JUGOS" -> Categoria.JUGOS
            "COMIDA" -> Categoria.COMIDA
            "PANES" -> Categoria.PANES
            "COMPLEMENTOS" -> Categoria.COMPLEMENTOS
            else -> Categoria.BEBIDAS
        },
        precio = precio,
        disponible = disponible
    )

    fun Producto.toEntity(): ProductoEntity = ProductoEntity(
        id = id,
        nombre = nombre,
        categoria = categoria.name,
        precio = precio,
        disponible = disponible
    )

    // ===============================
    // ITEM PEDIDO
    // ===============================

    fun ItemPedidoEntity.toDomain(productos: List<Producto>): ItemPedido? {
        val producto = productos.find { it.id == productoId }
        return if (producto != null) {
            ItemPedido(producto, cantidad)
        } else null
    }

    // ===============================
    // PEDIDO
    // ===============================

    fun PedidoEntity.toDomain(items: List<ItemPedidoEntity>, productos: List<Producto>): Pedido {
        val domainItems = items.mapNotNull { it.toDomain(productos) }

        return Pedido(
            id = id,
            sesionId = sesionId,
            mesaId = mesaId,
            items = domainItems,
            estado = when (estado) {
                "NUEVO" -> EstadoPedido.NUEVO
                "EN_PREPARACION" -> EstadoPedido.EN_PREPARACION
                "LISTO" -> EstadoPedido.LISTO
                "CANCELADO" -> EstadoPedido.CANCELADO
                else -> EstadoPedido.NUEVO
            },
            creadoEn = creadoEn,
            enviadoEn = enviadoEn,
            completadoEn = completadoEn
        )
    }

    fun Pedido.toEntity(): PedidoEntity = PedidoEntity(
        id = id,
        sesionId = sesionId,
        mesaId = mesaId,
        estado = estado.name,
        creadoEn = creadoEn,
        enviadoEn = enviadoEn,
        completadoEn = completadoEn
    )

    // ===============================
    // USUARIO
    // ===============================

    fun UsuarioEntity.toDomain(): Usuario = Usuario(
        id = id,
        codigo = codigo,
        nombre = nombre,
        rol = when (rol) {
            "EMISOR" -> RolUsuario.EMISOR
            "RECEPTOR" -> RolUsuario.RECEPTOR
            else -> RolUsuario.EMISOR
        }
    )

    fun Usuario.toEntity(): UsuarioEntity = UsuarioEntity(
        id = id,
        codigo = codigo,
        nombre = nombre,
        rol = rol.name
    )
}
