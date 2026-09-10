package com.kitchentwenty2.data.remote.firestore

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.kitchentwenty2.data.local.KitchenDatabase
import com.kitchentwenty2.data.local.entity.SyncQueueEntity
import com.kitchentwenty2.util.AppErrorLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreMenuRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: KitchenDatabase,
    private val appErrorLogger: AppErrorLogger
) : FirestoreMenuRepository {

    private val gson = Gson()

    private fun currentActorId(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.uid ?: user?.email ?: "SYSTEM"
    }

    private fun hasAuthenticatedUser(): Boolean = FirebaseAuth.getInstance().currentUser != null

    override fun listenAllMenuItems(): Flow<List<FirestoreMenuItem>> = callbackFlow {
        val registration = firestore.collection("menu_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreSync", "Menu listener failed", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(FirestoreMenuItem::class.java)?.copy(id = doc.id)
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(items)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun getMenuItem(id: String): FirestoreMenuItem? {
        val document = firestore.collection("menu_items").document(id).get().await()
        return document.toObject(FirestoreMenuItem::class.java)?.copy(id = document.id)
    }

    override suspend fun createOrUpdateMenuItem(menuItem: FirestoreMenuItem): String {
        val now = Timestamp.now()
        val itemToWrite = menuItem.copy(
            modifiedAt = now,
            createdAt = menuItem.createdAt ?: now,
            createdBy = menuItem.createdBy ?: currentActorId()
        )
        val documentId = itemToWrite.id.ifBlank { firestore.collection("menu_items").document().id }

        if (!hasAuthenticatedUser()) {
            Log.w("FirestoreSync", "No authenticated Firebase user; queueing menu item $documentId for later sync.")
            queueUpsert("menu_items", documentId, itemToWrite.copy(id = documentId))
            return documentId
        }

        return try {
            firestore.collection("menu_items").document(documentId).set(itemToWrite.copy(id = documentId)).await()
            Log.d("FirestoreSync", "Menu item $documentId successfully written to Cloud Firestore.")
            documentId
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Error writing menu item $documentId to Cloud Firestore.", e)
            appErrorLogger.logException(e, "FirestoreMenuRepository.createOrUpdateMenuItem")
            queueUpsert("menu_items", documentId, itemToWrite.copy(id = documentId))
            documentId
        }
    }

    override suspend fun deleteMenuItem(id: String) {
        if (!hasAuthenticatedUser()) {
            Log.w("FirestoreSync", "No authenticated Firebase user; queueing menu item delete for $id.")
            queueDelete("menu_items", id)
            return
        }

        try {
            firestore.collection("menu_items").document(id).delete().await()
            Log.d("FirestoreSync", "Menu item $id successfully deleted from Cloud Firestore.")
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Error deleting menu item $id from Cloud Firestore.", e)
            appErrorLogger.logException(e, "FirestoreMenuRepository.deleteMenuItem")
            queueDelete("menu_items", id)
        }
    }

    private suspend fun queueUpsert(collectionName: String, documentId: String, payload: Any) {
        database.syncQueueDao().insert(
            SyncQueueEntity(
                operationType = "UPSERT",
                collectionName = collectionName,
                documentId = documentId,
                payload = gson.toJson(payload),
                createdBy = currentActorId()
            )
        )
    }

    private suspend fun queueDelete(collectionName: String, documentId: String) {
        database.syncQueueDao().insert(
            SyncQueueEntity(
                operationType = "DELETE",
                collectionName = collectionName,
                documentId = documentId,
                payload = "{}",
                createdBy = currentActorId()
            )
        )
    }
}
