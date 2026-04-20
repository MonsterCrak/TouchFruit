package com.jlls.touchfruit.data.local.dao

import androidx.room.*
import com.jlls.touchfruit.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MesaDao {
    @Query("SELECT * FROM mesas ORDER BY id ASC")
    fun getAllMesas(): Flow<List<MesaEntity>>

    @Query("SELECT * FROM mesas WHERE id = :id")
    suspend fun getMesaById(id: Int): MesaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mesas: List<MesaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mesa: MesaEntity)

    @Update
    suspend fun update(mesa: MesaEntity)
}

@Dao
interface SesionDao {
    @Query("SELECT * FROM sesiones WHERE mesaId = :mesaId ORDER BY abiertaEn DESC")
    fun getSesionesPorMesa(mesaId: Int): Flow<List<SesionEntity>>

    @Query("SELECT * FROM sesiones WHERE mesaId = :mesaId AND cerradaEn IS NULL LIMIT 1")
    suspend fun getSesionActivaPorMesa(mesaId: Int): SesionEntity?

    @Query("SELECT * FROM sesiones WHERE id = :id")
    suspend fun getSesionById(id: String): SesionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sesion: SesionEntity)

    @Update
    suspend fun update(sesion: SesionEntity)

    @Query("UPDATE sesiones SET cerradaEn = :cerradaEn WHERE id = :id")
    suspend fun cerrarSesion(id: String, cerradaEn: Long)
}

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos ORDER BY id ASC")
    fun getAllProductos(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE categoria = :categoria AND disponible = 1")
    fun getProductosPorCategoria(categoria: String): Flow<List<ProductoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(productos: List<ProductoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(producto: ProductoEntity)
}

@Dao
interface PedidoDao {
    @Query("SELECT * FROM pedidos WHERE sesionId = :sesionId ORDER BY creadoEn ASC")
    fun getPedidosPorSesion(sesionId: String): Flow<List<PedidoEntity>>

    @Query("SELECT * FROM pedidos WHERE sesionId = :sesionId AND estado = 'NUEVO' LIMIT 1")
    suspend fun getPedidoNuevoPorSesion(sesionId: String): PedidoEntity?

    @Query("SELECT * FROM pedidos WHERE id = :id")
    suspend fun getPedidoById(id: String): PedidoEntity?

    @Query("SELECT * FROM pedidos WHERE estado NOT IN ('LISTO', 'CANCELADO')")
    fun getPedidosActivos(): Flow<List<PedidoEntity>>

    @Query("SELECT * FROM pedidos WHERE enviadoEn IS NOT NULL ORDER BY enviadoEn DESC")
    fun getPedidosEnviados(): Flow<List<PedidoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pedido: PedidoEntity): Long

    @Update
    suspend fun update(pedido: PedidoEntity)

    @Query("UPDATE pedidos SET estado = :estado, completadoEn = :completadoEn WHERE id = :id")
    suspend fun actualizarEstado(id: String, estado: String, completadoEn: Long?)

    @Delete
    suspend fun delete(pedido: PedidoEntity)

    @Query("DELETE FROM pedidos WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ItemPedidoDao {
    @Query("SELECT * FROM itemspedido WHERE pedidoId = :pedidoId")
    fun getItemsPorPedido(pedidoId: String): Flow<List<ItemPedidoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemPedidoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemPedidoEntity>)

    @Query("UPDATE itemspedido SET cantidad = :cantidad WHERE pedidoId = :pedidoId AND productoId = :productoId")
    suspend fun actualizarCantidad(pedidoId: String, productoId: String, cantidad: Int)

    @Query("DELETE FROM itemspedido WHERE pedidoId = :pedidoId AND productoId = :productoId")
    suspend fun deleteItem(pedidoId: String, productoId: String)

    @Query("DELETE FROM itemspedido WHERE pedidoId = :pedidoId")
    suspend fun deleteAllByPedido(pedidoId: String)
}

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuarios LIMIT 1")
    suspend fun getUsuarioActual(): UsuarioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usuario: UsuarioEntity)

    @Query("DELETE FROM usuarios")
    suspend fun deleteAll()
}
