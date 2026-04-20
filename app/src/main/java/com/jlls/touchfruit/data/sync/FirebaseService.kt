package com.jlls.touchfruit.data.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// ===============================
// FIREBASE SERVICE
// Wraps all Firestore operations
// ===============================

object FirebaseService {
    private const val TAG = "FirebaseService"

    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Collections
    private const val COLLECTION_PEDIDOS = "pedidos"
    private const val COLLECTION_MESAS = "mesas"
    private const val COLLECTION_SESIONES = "sesiones"
    private const val COLLECTION_PRODUCTOS = "productos"
    private const val COLLECTION_USUARIOS = "usuarios"

    // ===============================
    // AUTHENTICATION
    // ===============================

    /**
     * Signs in anonymously and returns the Firebase UID.
     * Creates a new anonymous account if one doesn't exist.
     */
    suspend fun signInAnonymously(): String {
        return try {
            val result = auth.signInAnonymously().await()
            result.user?.uid ?: throw IllegalStateException("Firebase user is null after sign-in")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sign in anonymously", e)
            throw e
        }
    }

    /**
     * Returns the current Firebase user, if any.
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Returns the current Firebase UID, if any.
     */
    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    /**
     * Observes authentication state changes.
     * Emits null when user is signed out.
     */
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Signs in with email/password.
     * Returns the Firebase UID on success.
     */
    suspend fun signInWithEmail(email: String, password: String): String {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.uid ?: throw IllegalStateException("Firebase user is null after sign-in")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sign in with email", e)
            throw e
        }
    }

    /**
     * Creates a new account with email/password.
     * Returns the Firebase UID on success.
     */
    suspend fun createUserWithEmail(email: String, password: String): String {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.uid ?: throw IllegalStateException("Firebase user is null after sign-in")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create user with email", e)
            throw e
        }
    }

    /**
     * Signs out the current user.
     */
    fun signOut() {
        auth.signOut()
    }

    // ===============================
    // USER DOCUMENTS
    // ===============================

    /**
     * Creates a user document in Firestore.
     * Called after anonymous sign-in to store user metadata.
     */
    suspend fun createUserDocument(uid: String, codigo: String, nombre: String, rol: String) {
        try {
            val usuario = UsuarioDocument(
                codigo = codigo,
                nombre = nombre,
                rol = rol,
                creadoEn = System.currentTimeMillis()
            )
            db.collection(COLLECTION_USUARIOS)
                .document(uid)
                .set(usuario)
                .await()
            Log.d(TAG, "User document created for uid: $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create user document", e)
            throw e
        }
    }

    /**
     * Gets a user document by UID.
     */
    suspend fun getUserDocument(uid: String): UsuarioDocument? {
        return try {
            val doc = db.collection(COLLECTION_USUARIOS)
                .document(uid)
                .get()
                .await()
            doc.toObject(UsuarioDocument::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user document", e)
            null
        }
    }

    // ===============================
    // PEDIDOS
    // ===============================

    /**
     * Uploads a pedido to Firestore.
     * Creates or replaces the document.
     */
    suspend fun uploadPedido(pedido: PedidoDocument) {
        try {
            db.collection(COLLECTION_PEDIDOS)
                .document(pedido.id)
                .set(pedido)
                .await()
            Log.d(TAG, "Pedido uploaded: ${pedido.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload pedido ${pedido.id}", e)
            throw e
        }
    }

    /**
     * Updates just the estado field of a pedido.
     * More efficient than uploading the entire document.
     */
    suspend fun updatePedidoEstado(pedidoId: String, estado: String, completadoEn: Long? = null) {
        try {
            val updates = mutableMapOf<String, Any>("estado" to estado)
            completadoEn?.let { updates["completadoEn"] = it }

            db.collection(COLLECTION_PEDIDOS)
                .document(pedidoId)
                .update(updates)
                .await()
            Log.d(TAG, "Pedido estado updated: $pedidoId -> $estado")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update pedido estado $pedidoId", e)
            throw e
        }
    }

    /**
     * Deletes a pedido from Firestore.
     */
    suspend fun deletePedido(pedidoId: String) {
        try {
            db.collection(COLLECTION_PEDIDOS)
                .document(pedidoId)
                .delete()
                .await()
            Log.d(TAG, "Pedido deleted: $pedidoId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete pedido $pedidoId", e)
            throw e
        }
    }

    /**
     * Observes all pedidos in real-time.
     * Returns a Flow that emits whenever pedidos change.
     */
    fun observePedidos(): Flow<List<PedidoDocument>> = callbackFlow {
        val listener = db.collection(COLLECTION_PEDIDOS)
            .orderBy("enviadoEn", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing pedidos", error)
                    return@addSnapshotListener
                }
                val pedidos = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PedidoDocument::class.java)
                } ?: emptyList()
                trySend(pedidos)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Observes pedidos for a specific mesa in real-time.
     */
    fun observePedidosPorMesa(mesaId: Int): Flow<List<PedidoDocument>> = callbackFlow {
        val listener = db.collection(COLLECTION_PEDIDOS)
            .whereEqualTo("mesaId", mesaId)
            .orderBy("enviadoEn", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing pedidos for mesa $mesaId", error)
                    return@addSnapshotListener
                }
                val pedidos = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PedidoDocument::class.java)
                } ?: emptyList()
                trySend(pedidos)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Observes only active (non-completed) pedidos in real-time.
     * Uses whereIn since Firestore doesn't allow multiple != filters.
     */
    fun observePedidosActivos(): Flow<List<PedidoDocument>> = callbackFlow {
        val listener = db.collection(COLLECTION_PEDIDOS)
            .whereNotIn("estado", listOf("LISTO", "CANCELADO"))
            .orderBy("enviadoEn", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing active pedidos", error)
                    return@addSnapshotListener
                }
                val pedidos = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PedidoDocument::class.java)
                } ?: emptyList()
                trySend(pedidos)
            }
        awaitClose { listener.remove() }
    }

    // ===============================
    // MESAS
    // ===============================

    /**
     * Uploads a mesa to Firestore.
     */
    suspend fun uploadMesa(mesa: MesaDocument) {
        try {
            db.collection(COLLECTION_MESAS)
                .document(mesa.id.toString())
                .set(mesa)
                .await()
            Log.d(TAG, "Mesa uploaded: ${mesa.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload mesa ${mesa.id}", e)
            throw e
        }
    }

    /**
     * Observes all mesas in real-time.
     */
    fun observeMesas(): Flow<List<MesaDocument>> = callbackFlow {
        val listener = db.collection(COLLECTION_MESAS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing mesas", error)
                    return@addSnapshotListener
                }
                val mesas = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(MesaDocument::class.java)
                }?.sortedBy { it.id } ?: emptyList()
                trySend(mesas)
            }
        awaitClose { listener.remove() }
    }

    // ===============================
    // SESIONES
    // ===============================

    /**
     * Uploads a sesion to Firestore.
     */
    suspend fun uploadSesion(sesion: SesionDocument) {
        try {
            db.collection(COLLECTION_SESIONES)
                .document(sesion.id)
                .set(sesion)
                .await()
            Log.d(TAG, "Sesion uploaded: ${sesion.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload sesion ${sesion.id}", e)
            throw e
        }
    }

    /**
     * Closes a sesion by updating its cerradaEn timestamp.
     */
    suspend fun cerrarSesion(sesionId: String) {
        try {
            db.collection(COLLECTION_SESIONES)
                .document(sesionId)
                .update("cerradaEn", System.currentTimeMillis())
                .await()
            Log.d(TAG, "Sesion closed: $sesionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close sesion $sesionId", e)
            throw e
        }
    }

    /**
     * Observes sesiones for a specific mesa.
     */
    fun observeSesionesPorMesa(mesaId: Int): Flow<List<SesionDocument>> = callbackFlow {
        val listener = db.collection(COLLECTION_SESIONES)
            .whereEqualTo("mesaId", mesaId)
            .orderBy("abiertaEn", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing sesiones for mesa $mesaId", error)
                    return@addSnapshotListener
                }
                val sesiones = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(SesionDocument::class.java)
                } ?: emptyList()
                trySend(sesiones)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Observes the active sesion for a mesa (cerradaEn == null).
     */
    fun observeSesionActiva(mesaId: Int): Flow<SesionDocument?> = callbackFlow {
        val listener = db.collection(COLLECTION_SESIONES)
            .whereEqualTo("mesaId", mesaId)
            .whereEqualTo("cerradaEn", null)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing active sesion for mesa $mesaId", error)
                    return@addSnapshotListener
                }
                val sesion = snapshot?.documents?.firstOrNull()?.toObject(SesionDocument::class.java)
                trySend(sesion)
            }
        awaitClose { listener.remove() }
    }

    // ===============================
    // PRODUCTOS
    // ===============================

    /**
     * Syncs all products to Firestore.
     * Replaces all existing products.
     */
    suspend fun syncProductos(productos: List<ProductoDocument>) {
        try {
            val batch = db.batch()
            productos.forEach { producto ->
                val ref = db.collection(COLLECTION_PRODUCTOS)
                    .document(producto.id)
                batch.set(ref, producto)
            }
            batch.commit().await()
            Log.d(TAG, "Synced ${productos.size} productos to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync productos", e)
            throw e
        }
    }

    /**
     * Observes all products in real-time.
     */
    fun observeProductos(): Flow<List<ProductoDocument>> = callbackFlow {
        val listener = db.collection(COLLECTION_PRODUCTOS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing productos", error)
                    return@addSnapshotListener
                }
                val productos = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ProductoDocument::class.java)
                } ?: emptyList()
                trySend(productos)
            }
        awaitClose { listener.remove() }
    }
}
