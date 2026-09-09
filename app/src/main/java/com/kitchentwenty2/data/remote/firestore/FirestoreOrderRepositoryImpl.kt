package com.kitchentwenty2.data.remote.firestore

import com.google.firebase.Timestamp
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
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
        return uid ?: email ?: "SYSTEM"
    }

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
        // ensure audit fields
        val now = Timestamp.now()
        val orderToWrite = order.copy(
            modifiedAt = now,
            createdAt = order.createdAt ?: now,
            createdBy = order.createdBy ?: currentActorId()
        )

        return try {
            if (orderToWrite.id.isBlank()) {
                val ref = firestore.collection("orders").add(orderToWrite).await()
                ref.id
            } else {
                firestore.collection("orders").document(orderToWrite.id).set(orderToWrite).await()
                orderToWrite.id
            }
        } catch (e: Exception) {
            // log and enqueue for later sync
            appErrorLogger.logException(e, "FirestoreOrderRepository.createOrUpdateOrder")
            val payload = gson.toJson(orderToWrite)
            val queue = SyncQueueEntity(
                operationType = "UPSERT",
                collectionName = "orders",
                documentId = if (orderToWrite.id.isBlank()) null else orderToWrite.id,
                payload = payload,
                createdBy = orderToWrite.createdBy
            )
            database.syncQueueDao().insert(queue)
            // return id if available, else empty string
            orderToWrite.id.ifBlank { "" }
        }
    }

    override suspend fun deleteOrder(orderId: String) {
        try {
            firestore.collection("orders").document(orderId).delete().await()
        } catch (e: Exception) {
            appErrorLogger.logException(e, "FirestoreOrderRepository.deleteOrder")
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
}
