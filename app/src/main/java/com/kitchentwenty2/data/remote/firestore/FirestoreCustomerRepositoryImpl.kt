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
class FirestoreCustomerRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: KitchenDatabase,
    private val appErrorLogger: AppErrorLogger
) : FirestoreCustomerRepository {

    private val gson = Gson()

    private fun currentActorId(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.uid ?: user?.email ?: "SYSTEM"
    }

    private fun hasAuthenticatedUser(): Boolean = FirebaseAuth.getInstance().currentUser != null

    override fun listenAllCustomers(): Flow<List<FirestoreCustomer>> = callbackFlow {
        val registration = firestore.collection("customers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val customers = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(FirestoreCustomer::class.java)?.copy(id = doc.id)
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(customers)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun getCustomer(id: String): FirestoreCustomer? {
        val document = firestore.collection("customers").document(id).get().await()
        return document.toObject(FirestoreCustomer::class.java)?.copy(id = document.id)
    }

    override suspend fun createOrUpdateCustomer(customer: FirestoreCustomer): String {
        val now = Timestamp.now()
        val customerToWrite = customer.copy(
            modifiedAt = now,
            createdAt = customer.createdAt ?: now,
            createdBy = customer.createdBy ?: currentActorId()
        )
        val documentId = customerToWrite.id.ifBlank { firestore.collection("customers").document().id }

        if (!hasAuthenticatedUser()) {
            Log.w("FirestoreSync", "No authenticated Firebase user; queueing customer $documentId for later sync.")
            queueUpsert(customerToWrite.copy(id = documentId), documentId)
            return documentId
        }

        return try {
            firestore.collection("customers").document(documentId).set(customerToWrite.copy(id = documentId)).await()
            Log.d("FirestoreSync", "Customer $documentId successfully written to Cloud Firestore.")
            documentId
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Error writing customer $documentId to Cloud Firestore.", e)
            appErrorLogger.logException(e, "FirestoreCustomerRepository.createOrUpdateCustomer")
            queueUpsert(customerToWrite.copy(id = documentId), documentId)
            documentId
        }
    }

    override suspend fun deleteCustomer(id: String) {
        if (!hasAuthenticatedUser()) {
            Log.w("FirestoreSync", "No authenticated Firebase user; queueing customer delete for $id.")
            queueDelete(id)
            return
        }

        try {
            firestore.collection("customers").document(id).delete().await()
            Log.d("FirestoreSync", "Customer $id successfully deleted from Cloud Firestore.")
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Error deleting customer $id from Cloud Firestore.", e)
            appErrorLogger.logException(e, "FirestoreCustomerRepository.deleteCustomer")
            queueDelete(id)
        }
    }

    private suspend fun queueUpsert(customer: FirestoreCustomer, documentId: String) {
        database.syncQueueDao().insert(
            SyncQueueEntity(
                operationType = "UPSERT",
                collectionName = "customers",
                documentId = documentId,
                payload = gson.toJson(customer),
                createdBy = customer.createdBy
            )
        )
    }

    private suspend fun queueDelete(documentId: String) {
        database.syncQueueDao().insert(
            SyncQueueEntity(
                operationType = "DELETE",
                collectionName = "customers",
                documentId = documentId,
                payload = "{}",
                createdBy = currentActorId()
            )
        )
    }
}
