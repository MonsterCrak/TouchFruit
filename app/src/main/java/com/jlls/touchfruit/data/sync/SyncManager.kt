package com.jlls.touchfruit.data.sync

import android.util.Log
import com.jlls.touchfruit.data.model.*
import com.jlls.touchfruit.data.repository.TouchFruitRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// ===============================
// SYNC MANAGER
// Orchestrates Room <-> Firestore synchronization
// ===============================

class SyncManager(
    private val repository: TouchFruitRepository,
    private val scope: CoroutineScope
) {
    private val TAG = "SyncManager"

    // Listener jobs that need to be cancelled
    private val listenerJobs = mutableListOf<Job>()

    // Flag to prevent sync loops
    private var isSyncingFromRemote = false

    // ===============================
    // EMISOR: Upload local changes to Firebase
    // ===============================

    /**
     * Syncs a newly created pedido to Firebase.
     * Called after Room write succeeds.
     */
    fun syncPedidoToFirebase(pedido: Pedido) {
        scope.launch {
            try {
                val emisorId = FirebaseService.getCurrentUserUid() ?: return@launch
                val pedidoDoc = pedido.toDocument(emisorId)
                FirebaseService.uploadPedido(pedidoDoc)
                Log.d(TAG, "Pedido synced to Firebase: ${pedido.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync pedido to Firebase", e)
            }
        }
    }

    /**
     * Syncs estado change to Firebase.
     * Called when receptor marks pedido as LISTO.
     */
    fun syncEstadoToFirebase(pedidoId: String, estado: EstadoPedido) {
        scope.launch {
            try {
                val completadoEn = if (estado == EstadoPedido.LISTO) System.currentTimeMillis() else null
                FirebaseService.updatePedidoEstado(pedidoId, estado.name, completadoEn)
                Log.d(TAG, "Pedido estado synced: $pedidoId -> ${estado.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync pedido estado", e)
            }
        }
    }

    /**
     * Syncs mesa state to Firebase.
     */
    fun syncMesaToFirebase(mesa: Mesa) {
        scope.launch {
            try {
                FirebaseService.uploadMesa(mesa.toDocument())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync mesa to Firebase", e)
            }
        }
    }

    /**
     * Syncs new sesion to Firebase.
     */
    fun syncSesionToFirebase(sesion: Sesion) {
        scope.launch {
            try {
                FirebaseService.uploadSesion(sesion.toDocument())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync sesion to Firebase", e)
            }
        }
    }

    /**
     * Syncs sesion closure to Firebase.
     */
    fun syncSesionCerrada(sesionId: String) {
        scope.launch {
            try {
                FirebaseService.cerrarSesion(sesionId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync sesion closure", e)
            }
        }
    }

    // ===============================
    // RECEPTOR: Listen to Firebase and update Room
    // ===============================

    /**
     * Starts listening to Firebase for pedido changes.
     * Writes remote changes to Room.
     */
    fun startPedidoListening() {
        val job = scope.launch {
            FirebaseService.observePedidosActivos().collect { remotePedidos ->
                if (isSyncingFromRemote) return@collect

                isSyncingFromRemote = true
                try {
                    // Sync each remote pedido to Room
                    remotePedidos.forEach { pedidoDoc ->
                        syncRemotePedidoToRoom(pedidoDoc)
                    }
                } finally {
                    isSyncingFromRemote = false
                }
            }
        }
        listenerJobs.add(job)
    }

    /**
     * Syncs a single remote pedido to Room.
     */
    private suspend fun syncRemotePedidoToRoom(pedidoDoc: PedidoDocument) {
        val productos = repository.productos.value
        val pedido = pedidoDoc.toDomain(productos)

        // Check if pedido already exists locally
        val localPedido = repository.getPedidosActivos().find { it.id == pedido.id }

        if (localPedido == null) {
            // New pedido from remote - add to Room
            // We don't have direct Room access here, so we use repository
            // For now, the ReceptorViewModel already observes Firebase via Flow
            // This is more complex - we'd need to add a method to repository
            Log.d(TAG, "Remote pedido detected: ${pedido.id}")
        } else if (pedido.estado != localPedido.estado) {
            // Estado changed remotely - update locally
            repository.actualizarEstadoPedido(pedido.id, pedido.estado)
        }
    }

    /**
     * Stops all Firebase listeners.
     */
    fun stopListening() {
        listenerJobs.forEach { it.cancel() }
        listenerJobs.clear()
        Log.d(TAG, "All Firebase listeners stopped")
    }

    // ===============================
    // INITIALIZATION
    // ===============================

    /**
     * Starts listening based on user role.
     */
    fun startListening(rol: RolUsuario) {
        when (rol) {
            RolUsuario.RECEPTOR -> startPedidoListening()
            RolUsuario.EMISOR -> {
                // Emisor doesn't need to listen - just uploads
                // But could listen for estado changes on their pedidos
            }
        }
    }

    /**
     * Binds Firebase Auth and starts listening if user is logged in.
     * Uses email/password authentication for proper Emisor/Receptor linking.
     * If Firebase fails, this is logged but doesn't break the app.
     */
    fun bindFirebaseAuth(usuario: Usuario) {
        scope.launch {
            try {
                // Determine email based on role
                val email = when (usuario.rol) {
                    RolUsuario.EMISOR -> "emisor@touchfruit.app"
                    RolUsuario.RECEPTOR -> "receptor@touchfruit.app"
                }
                val password = "touchfruit123"

                // Try to sign in, if fails try creating the account
                var uid: String
                try {
                    uid = FirebaseService.signInWithEmail(email, password)
                } catch (e: Exception) {
                    // Account doesn't exist, create it
                    Log.d(TAG, "Account not found, creating: $email")
                    uid = FirebaseService.createUserWithEmail(email, password)
                }

                // Create or update user document
                FirebaseService.createUserDocument(
                    uid = uid,
                    codigo = usuario.codigo,
                    nombre = usuario.nombre,
                    rol = usuario.rol.name
                )

                // Start listening based on role
                startListening(usuario.rol)

                Log.d(TAG, "Firebase Auth bound for ${usuario.rol}")
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Auth failed - continuing in offline mode: ${e.message}")
            }
        }
    }

    /**
     * Creates test accounts in Firebase (called once during setup).
     */
    fun createTestAccounts() {
        scope.launch {
            try {
                FirebaseService.createUserWithEmail("emisor@touchfruit.app", "touchfruit123")
                Log.d(TAG, "Emisor test account created")
            } catch (e: Exception) {
                Log.d(TAG, "Emisor account may already exist: ${e.message}")
            }
            try {
                FirebaseService.createUserWithEmail("receptor@touchfruit.app", "touchfruit123")
                Log.d(TAG, "Receptor test account created")
            } catch (e: Exception) {
                Log.d(TAG, "Receptor account may already exist: ${e.message}")
            }
        }
    }
}

// ===============================
// EXTENSIONS: Domain to Document
// ===============================

fun Pedido.toDocument(emisorId: String): PedidoDocument {
    return PedidoDocument(
        id = id,
        sesionId = sesionId,
        mesaId = mesaId,
        emisorId = emisorId,
        estado = estado.name,
        creadoEn = creadoEn,
        enviadoEn = enviadoEn ?: 0,
        completadoEn = completadoEn,
        items = items.map { it.toDocument() },
        total = total,
        itemCount = itemCount
    )
}

fun ItemPedido.toDocument(): ItemPedidoDocument {
    return ItemPedidoDocument(
        productoId = producto.id,
        cantidad = cantidad,
        nombre = producto.nombre,
        precioUnitario = producto.precio
    )
}

fun Mesa.toDocument(): MesaDocument {
    return MesaDocument(
        id = id,
        estado = estado.name,
        sesionActivaId = sesionId
    )
}

fun Sesion.toDocument(): SesionDocument {
    return SesionDocument(
        id = id,
        mesaId = mesaId,
        emisorId = emisorId,
        abiertaEn = abiertaEn,
        cerradaEn = cerradaEn
    )
}

// ===============================
// EXTENSIONS: Document to Domain
// ===============================

fun PedidoDocument.toDomain(productos: List<Producto>): Pedido {
    val itemsDomain = items.mapNotNull { itemDoc ->
        val producto = productos.find { it.id == itemDoc.productoId }
        producto?.let {
            ItemPedido(
                producto = it,
                cantidad = itemDoc.cantidad
            )
        }
    }

    return Pedido(
        id = id,
        sesionId = sesionId,
        mesaId = mesaId,
        estado = EstadoPedido.valueOf(estado),
        creadoEn = creadoEn,
        enviadoEn = enviadoEn,
        completadoEn = completadoEn,
        items = itemsDomain
    )
}
