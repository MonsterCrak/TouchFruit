package com.jlls.touchfruit.data.sync

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jlls.touchfruit.data.model.Usuario
import com.jlls.touchfruit.data.repository.TouchFruitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// ===============================
// FIREBASE SYNC SERVICE
// Lifecycle-aware service for starting/stopping sync
// ===============================

class FirebaseSyncService(
    private val repository: TouchFruitRepository
) {
    private val TAG = "FirebaseSyncService"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncManager = SyncManager(repository, scope)

    // Track if we've bound auth this session
    private var authBound = false

    /**
     * Binds Firebase to the lifecycle owner.
     * Call this from onCreate().
     */
    fun bindToLifecycleOwner(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                Log.d(TAG, "Lifecycle: onStart")

                // Check if user is logged in and auth not yet bound
                if (!authBound) {
                    repository.usuarioActual.value?.let { usuario ->
                        syncManager.bindFirebaseAuth(usuario)
                        authBound = true
                    }
                } else {
                    // Just restart listening
                    repository.usuarioActual.value?.let { usuario ->
                        syncManager.startListening(usuario.rol)
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                Log.d(TAG, "Lifecycle: onStop")
                // Don't stop listening - keep sync alive in background
                // If we want strict lifecycle, uncomment:
                // syncManager.stopListening()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                Log.d(TAG, "Lifecycle: onDestroy")
                syncManager.stopListening()
            }
        })
    }

    /**
     * Manually bind auth when user logs in.
     * Call this after successful login.
     * If Firebase fails, the app continues in offline-only mode.
     */
    fun onUserLoggedIn(usuario: Usuario) {
        Log.d(TAG, "Binding Firebase auth for user: ${usuario.codigo}")
        try {
            syncManager.bindFirebaseAuth(usuario)
            authBound = true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase binding failed, continuing in offline mode", e)
        }
    }

    /**
     * Gets the SyncManager instance for direct operations.
     */
    fun getSyncManager(): SyncManager = syncManager
}
