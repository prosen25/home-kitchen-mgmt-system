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
class FirestoreExpenseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: KitchenDatabase,
    private val appErrorLogger: AppErrorLogger
) : FirestoreExpenseRepository {

    private val gson = Gson()

    private fun currentActorId(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.uid ?: user?.email ?: "SYSTEM"
    }

    private fun hasAuthenticatedUser(): Boolean = FirebaseAuth.getInstance().currentUser != null

    override fun listenExpensesForDate(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<FirestoreExpense>> =
        callbackFlow {
            val query = firestore.collection("expenses")
                .whereGreaterThanOrEqualTo("expenseDate", startOfDayMillis)
                .whereLessThanOrEqualTo("expenseDate", endOfDayMillis)
                .orderBy("expenseDate")

            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreSync", "Expense listener failed", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(FirestoreExpense::class.java)?.copy(id = doc.id)
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(expenses)
            }

            awaitClose { registration.remove() }
        }

    override suspend fun createOrUpdateExpense(expense: FirestoreExpense): String {
        val now = Timestamp.now()
        val expenseToWrite = expense.copy(
            modifiedAt = now,
            createdAt = expense.createdAt ?: now,
            createdBy = expense.createdBy ?: currentActorId()
        )
        val documentId = expenseToWrite.id.ifBlank { firestore.collection("expenses").document().id }

        if (!hasAuthenticatedUser()) {
            Log.w("FirestoreSync", "No authenticated Firebase user; queueing expense $documentId for later sync.")
            queueUpsert("expenses", documentId, expenseToWrite.copy(id = documentId))
            return documentId
        }

        return try {
            firestore.collection("expenses").document(documentId).set(expenseToWrite.copy(id = documentId)).await()
            Log.d("FirestoreSync", "Expense $documentId successfully written to Cloud Firestore.")
            documentId
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Error writing expense $documentId to Cloud Firestore.", e)
            appErrorLogger.logException(e, "FirestoreExpenseRepository.createOrUpdateExpense")
            queueUpsert("expenses", documentId, expenseToWrite.copy(id = documentId))
            documentId
        }
    }

    override suspend fun deleteExpense(expenseId: String) {
        if (!hasAuthenticatedUser()) {
            Log.w("FirestoreSync", "No authenticated Firebase user; queueing expense delete for $expenseId.")
            queueDelete("expenses", expenseId)
            return
        }

        try {
            firestore.collection("expenses").document(expenseId).delete().await()
            Log.d("FirestoreSync", "Expense $expenseId successfully deleted from Cloud Firestore.")
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Error deleting expense $expenseId from Cloud Firestore.", e)
            appErrorLogger.logException(e, "FirestoreExpenseRepository.deleteExpense")
            queueDelete("expenses", expenseId)
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
