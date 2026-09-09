package com.kitchentwenty2.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.kitchentwenty2.data.local.KitchenDatabase
import com.kitchentwenty2.data.local.entity.SyncQueueEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: KitchenDatabase
) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var connectivityManager: ConnectivityManager? = null

    init {
        // Enable Firestore local persistence and reasonable cache size
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestore.firestoreSettings = settings
    }

    fun start() {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch {
                    processQueue()
                }
            }
        })

        // Try initial sync attempt in case app starts online
        scope.launch { processQueue() }
    }

    suspend fun processQueue() {
        val dao = database.syncQueueDao()
        val pending = dao.getAllPending()
        for (item in pending) {
            try {
                when (item.operationType) {
                    "UPSERT" -> performUpsert(item)
                    "DELETE" -> performDelete(item)
                    else -> {
                        // unsupported operation: delete to avoid infinite retries
                        dao.deleteById(item.syncId)
                    }
                }
            } catch (e: Exception) {
                val updated = item.copy(attempts = item.attempts + 1, lastError = e.message)
                dao.update(updated)
            }
        }
    }

    private suspend fun performUpsert(item: SyncQueueEntity) = withContext(Dispatchers.IO) {
        val collection = firestore.collection(item.collectionName)
        if (!item.documentId.isNullOrBlank()) {
            val docRef = collection.document(item.documentId)
            // Write raw map from JSON payload
            val map = com.google.gson.Gson().fromJson(item.payload, Map::class.java) as Map<String, Any?>
            docRef.set(map).await()
            database.syncQueueDao().deleteById(item.syncId)
        } else {
            val map = com.google.gson.JsonParser.parseString(item.payload).asJsonObject
            val ref = collection.add(map.asMap()).await()
            database.syncQueueDao().deleteById(item.syncId)
        }
    }

    private suspend fun performDelete(item: SyncQueueEntity) = withContext(Dispatchers.IO) {
        if (item.documentId.isNullOrBlank()) {
            database.syncQueueDao().deleteById(item.syncId)
            return@withContext
        }
        firestore.collection(item.collectionName).document(item.documentId).delete().await()
        database.syncQueueDao().deleteById(item.syncId)
    }

    fun stop() {
        try {
            connectivityManager?.unregisterNetworkCallback(ConnectivityManager.NetworkCallback())
        } catch (_: Exception) {}
        scope.cancel()
    }
}
