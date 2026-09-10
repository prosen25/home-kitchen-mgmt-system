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
class FirestoreOrderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: KitchenDatabase,
    private val appErrorLogger: AppErrorLogger
) : FirestoreOrderRepository {

    private val gson = Gson()

    private fun currentActorId(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val email = FirebaseAuth.getInstance().currentUser?.email
        return uid ?: email ?: "SYSTEM"
    }

    private fun hasAuthenticatedUser(): Boolean = FirebaseAuth.getInstance().currentUser != null

    override fun listenOrdersForDate(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<FirestoreOrder>> =
        callbackFlow {
            val query = firestore.collection("orders")
                .whereGreaterThanOrEqualTo("orderDate", startOfDayMillis)
                .whereLessThanOrEqualTo("orderDate", endOfDayMillis)
                .orderBy("orderDate")

            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(FirestoreOrder::class.java)?.copy(id = doc.id)
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(list)
            }

            awaitClose { registration.remove() }
        }

    override fun listenOrder(orderId: String): Flow<FirestoreOrder?> =
        callbackFlow {
            val docRef = firestore.collection("orders").document(orderId)
            val registration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val obj = snapshot?.toObject(FirestoreOrder::class.java)?.copy(id = snapshot.id)
                trySend(obj)
            }
            awaitClose { registration.remove() }
        }

    override suspend fun getOrder(orderId: String): FirestoreOrder? {
        val doc = firestore.collection("orders").document(orderId).get().await()
        return doc.toObject(FirestoreOrder::class.java)?.copy(id = doc.id)
    }

    override suspend fun createOrUpdateOrder(order: FirestoreOrder): String {
        val now = Timestamp.now()
        val orderToWrite = order.copy(
            modifiedAt = now,
            createdAt = order.createdAt ?: now,
            createdBy = order.createdBy ?: currentActorId()
        )
        val documentId = orderToWrite.id.ifBlank { firestore.collection("orders").document().id }

        if (!hasAuthenticatedUser()) {
            Log.w("FirestoreSync", "No authenticated Firebase user; queueing order $documentId for later sync.")
            queueUpsert(orderToWrite.copy(id = documentId), documentId)
            return documentId
        }

        return try {
            firestore.collection("orders").document(documentId).set(orderToWrite.copy(id = documentId)).await()
            Log.d("FirestoreSync", "Order $documentId successfully written to Cloud Firestore.")
            documentId
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Error writing order $documentId to Cloud Firestore.", e)
            appErrorLogger.logException(e, "FirestoreOrderRepository.createOrUpdateOrder")
            queueUpsert(orderToWrite.copy(id = documentId), documentId)
            documentId
        }
    }

    override suspend fun deleteOrder(orderId: String) {
        if (!hasAuthenticatedUser()) {
            Log.w("FirestoreSync", "No authenticated Firebase user; queueing order delete for $orderId.")
            queueDelete(orderId)
            return
        }

        try {
            firestore.collection("orders").document(orderId).delete().await()
            Log.d("FirestoreSync", "Order $orderId successfully deleted from Cloud Firestore.")
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Error deleting order $orderId from Cloud Firestore.", e)
            appErrorLogger.logException(e, "FirestoreOrderRepository.deleteOrder")
            queueDelete(orderId)
        }
    }

    private suspend fun queueUpsert(order: FirestoreOrder, documentId: String) {
        val payload = gson.toJson(order)
        val queue = SyncQueueEntity(
            operationType = "UPSERT",
            collectionName = "orders",
            documentId = documentId,
            payload = payload,
            createdBy = order.createdBy
        )
        database.syncQueueDao().insert(queue)
    }

    private suspend fun queueDelete(orderId: String) {
        val queue = SyncQueueEntity(
            operationType = "DELETE",
            collectionName = "orders",
            documentId = orderId,
            payload = "{}",
            createdBy = currentActorId()
        )
        database.syncQueueDao().insert(queue)
    }
}
