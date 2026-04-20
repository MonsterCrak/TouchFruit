package com.jlls.touchfruit.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jlls.touchfruit.data.local.dao.*
import com.jlls.touchfruit.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MesaEntity::class,
        SesionEntity::class,
        ProductoEntity::class,
        PedidoEntity::class,
        ItemPedidoEntity::class,
        UsuarioEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TouchFruitDatabase : RoomDatabase() {

    abstract fun mesaDao(): MesaDao
    abstract fun sesionDao(): SesionDao
    abstract fun productoDao(): ProductoDao
    abstract fun pedidoDao(): PedidoDao
    abstract fun itemPedidoDao(): ItemPedidoDao
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        @Volatile
        private var INSTANCE: TouchFruitDatabase? = null

        fun getDatabase(context: Context): TouchFruitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TouchFruitDatabase::class.java,
                    "touchfruit_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(database: TouchFruitDatabase) {
            // Crear 20 mesas iniciales
            val mesas = (1..20).map { MesaEntity(id = it, estado = "CERRADA") }
            database.mesaDao().insertAll(mesas)

            // Productos default
            val productos = listOf(
                // Bebidas
                ProductoEntity("b1", "Agua", "BEBIDAS", 20.0),
                ProductoEntity("b2", "Refresco", "BEBIDAS", 25.0),
                ProductoEntity("b3", "Café", "BEBIDAS", 30.0),
                ProductoEntity("b4", "Té", "BEBIDAS", 25.0),
                ProductoEntity("b5", "Jugo de naranja", "BEBIDAS", 35.0),
                ProductoEntity("b6", "Latte", "BEBIDAS", 45.0),
                // Jugos
                ProductoEntity("j1", "Jugo de Naranja", "JUGOS", 35.0),
                ProductoEntity("j2", "Jugo de Uva", "JUGOS", 35.0),
                ProductoEntity("j3", "Jugo de Mango", "JUGOS", 40.0),
                ProductoEntity("j4", "Jugo de Fresa", "JUGOS", 35.0),
                ProductoEntity("j5", "Jugo de Leche", "JUGOS", 30.0),
                ProductoEntity("j6", "Jugo de Banano", "JUGOS", 35.0),
                // Comida
                ProductoEntity("c1", "Ensalada", "COMIDA", 85.0),
                ProductoEntity("c2", "Sándwich", "COMIDA", 65.0),
                ProductoEntity("c3", "Pasta", "COMIDA", 95.0),
                ProductoEntity("c4", "Arroz con pollo", "COMIDA", 75.0),
                ProductoEntity("c5", "Hamburguesa", "COMIDA", 80.0),
                ProductoEntity("c6", "Tacos", "COMIDA", 60.0),
                // Panes
                ProductoEntity("p1", "Croissant", "PANES", 25.0),
                ProductoEntity("p2", "Baguette", "PANES", 30.0),
                ProductoEntity("p3", "Pan integral", "PANES", 20.0),
                ProductoEntity("p4", "Molletes", "PANES", 35.0),
                ProductoEntity("p5", "Conchas", "PANES", 15.0),
                ProductoEntity("p6", "Bolillo", "PANES", 12.0),
                // Complementos
                ProductoEntity("co1", "Aderezo extra", "COMPLEMENTOS", 10.0),
                ProductoEntity("co2", "Salsa", "COMPLEMENTOS", 5.0),
                ProductoEntity("co3", "Queso extra", "COMPLEMENTOS", 15.0),
                ProductoEntity("co4", "Fruta", "COMPLEMENTOS", 20.0),
                ProductoEntity("co5", "Miel", "COMPLEMENTOS", 10.0),
                ProductoEntity("co6", "Mantequilla", "COMPLEMENTOS", 8.0)
            )
            database.productoDao().insertAll(productos)
        }
    }
}
