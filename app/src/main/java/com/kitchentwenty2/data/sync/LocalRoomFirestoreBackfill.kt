package com.kitchentwenty2.data.sync

import android.content.Context
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kitchentwenty2.data.local.KitchenDatabase
import com.kitchentwenty2.data.local.dao.CustomerDao
import com.kitchentwenty2.data.local.dao.ExpenseDao
import com.kitchentwenty2.data.local.dao.MenuItemDao
import com.kitchentwenty2.data.local.dao.OrderDao
import com.kitchentwenty2.data.remote.firestore.FirestoreCustomer
import com.kitchentwenty2.data.remote.firestore.FirestoreExpense
import com.kitchentwenty2.data.remote.firestore.FirestoreMenuItem
import com.kitchentwenty2.data.remote.firestore.FirestoreOrder
import com.kitchentwenty2.data.remote.firestore.FirestoreOrderItem
import com.kitchentwenty2.data.remote.firestore.FirestorePaymentLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class BackfillReport(
    val backupPath: String,
    val customers: Int,
    val menuItems: Int,
    val expenses: Int,
    val orders: Int,
    val paymentLogs: Int
)

@Singleton
class LocalRoomFirestoreBackfill @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val database: KitchenDatabase,
    private val customerDao: CustomerDao,
    private val menuItemDao: MenuItemDao,
    private val expenseDao: ExpenseDao,
    private val orderDao: OrderDao
) {
    private val runMutex = Mutex()

    suspend fun run(onProgress: (String) -> Unit = {}): BackfillReport = runMutex.withLock {
        checkNotNull(FirebaseAuth.getInstance().currentUser) {
            "No Firebase user is signed in. Open the app, sign in, then retry."
        }

        onProgress("Creating a local database backup...")
        val backupFile = createDatabaseBackup()
        val publicBackupPath = publishBackupToDownloads(backupFile)

        onProgress("Reading local Room data...")
        val customers = customerDao.getAllCustomersSnapshot()
        val menuItems = menuItemDao.getAllMenuItemsSnapshot()
        val expenses = expenseDao.getAllExpensesSnapshot()
        val orders = orderDao.getAllOrdersWithDetailsSnapshot()

        val writes = buildList<Pair<com.google.firebase.firestore.DocumentReference, Any>> {
            customers.forEach { customer ->
                add(
                    firestore.collection("customers").document(customer.customerId.toString()) to
                        FirestoreCustomer(
                            id = customer.customerId.toString(),
                            name = customer.name,
                            mobileNumber = customer.mobileNumber,
                            address = customer.address,
                            googleLocationUrl = customer.googleLocationUrl,
                            createdBy = customer.createdBy,
                            createdAt = customer.createdDateTimeStamp.toTimestamp(),
                            modifiedAt = customer.modifiedDateTimeStamp.toTimestamp()
                        )
                )
            }

            menuItems.forEach { item ->
                add(
                    firestore.collection("menu_items").document(item.menuItemId.toString()) to
                        FirestoreMenuItem(
                            id = item.menuItemId.toString(),
                            name = item.name,
                            description = item.description,
                            defaultPrice = item.defaultPrice,
                            createdBy = item.createdBy,
                            createdAt = item.createdDateTimeStamp.toTimestamp(),
                            modifiedAt = item.modifiedDateTimeStamp.toTimestamp()
                        )
                )
            }

            expenses.forEach { expense ->
                add(
                    firestore.collection("expenses").document(expense.expenseId.toString()) to
                        FirestoreExpense(
                            id = expense.expenseId.toString(),
                            expenseDate = expense.expenseDate,
                            category = expense.category,
                            amount = expense.amount,
                            notes = expense.notes,
                            createdBy = expense.createdBy,
                            createdAt = expense.createdDateTimeStamp.toTimestamp(),
                            modifiedAt = expense.modifiedDateTimeStamp.toTimestamp()
                        )
                )
            }

            orders.forEach { details ->
                val order = details.order
                add(
                    firestore.collection("orders").document(order.orderId.toString()) to
                        FirestoreOrder(
                            id = order.orderId.toString(),
                            customerId = order.customerId?.toString(),
                            customerSnapshot = FirestoreCustomer(
                                id = order.customerId?.toString().orEmpty(),
                                name = order.customerName,
                                mobileNumber = order.customerPhone,
                                address = order.customerAddress,
                                googleLocationUrl = order.googleLocationUrl,
                                createdBy = details.customer?.createdBy ?: order.createdBy
                            ),
                            orderDate = order.orderDate,
                            items = details.items.mapIndexed { index, item ->
                                FirestoreOrderItem(
                                    itemId = item.orderItemId.takeIf { it > 0 }?.toString()
                                        ?: "${order.orderId}-$index",
                                    menuItemId = item.menuItemId?.toString(),
                                    itemName = item.itemName,
                                    unitPrice = item.unitPrice,
                                    quantity = item.quantity,
                                    subtotal = item.subtotal,
                                    isCustom = item.isCustom
                                )
                            },
                            paymentLogs = details.paymentLogs.map { payment ->
                                FirestorePaymentLog(
                                    id = payment.paymentId.toString(),
                                    orderId = order.orderId.toString(),
                                    paymentDate = payment.paymentDate,
                                    amount = payment.amount,
                                    paymentType = payment.paymentType,
                                    createdBy = payment.createdBy,
                                    createdAt = payment.createdDateTimeStamp.toTimestamp()
                                )
                            },
                            upfrontDiscount = order.upfrontDiscount,
                            settlementDiscount = order.settlementDiscount,
                            advancePaid = order.advancePaid,
                            totalCollected = order.totalCollected,
                            refundedAmount = order.refundedAmount,
                            totalAmount = order.totalAmount,
                            status = order.status,
                            createdBy = order.createdBy,
                            createdAt = order.createdDateTimeStamp.toTimestamp(),
                            modifiedAt = order.modifiedDateTimeStamp.toTimestamp()
                        )
                )
            }
        }

        writes.chunked(MAX_BATCH_WRITES).forEachIndexed { index, chunk ->
            onProgress("Uploading batch ${index + 1} of ${(writes.size + MAX_BATCH_WRITES - 1) / MAX_BATCH_WRITES}...")
            val batch = firestore.batch()
            chunk.forEach { (document, value) -> batch.set(document, value) }
            batch.commit().await()
        }
        firestore.waitForPendingWrites().await()

        BackfillReport(
            backupPath = publicBackupPath,
            customers = customers.size,
            menuItems = menuItems.size,
            expenses = expenses.size,
            orders = orders.size,
            paymentLogs = orders.sumOf { it.paymentLogs.size }
        )
    }

    private fun createDatabaseBackup(): File {
        val backupDirectory = checkNotNull(context.getExternalFilesDir("backups")) {
            "External backup storage is unavailable."
        }
        check(backupDirectory.exists() || backupDirectory.mkdirs()) {
            "Could not create the database backup directory."
        }
        val backupFile = File(backupDirectory, "kitchen_twenty2_before_backfill_${System.currentTimeMillis()}.db")
        val escapedPath = backupFile.absolutePath.replace("'", "''")
        database.openHelper.writableDatabase.execSQL("VACUUM INTO '$escapedPath'")
        check(backupFile.exists() && backupFile.length() > 0L) {
            "The local database backup was not created."
        }
        return backupFile
    }

    private fun publishBackupToDownloads(backupFile: File): String {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, backupFile.name)
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.sqlite3")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = checkNotNull(
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ) { "Could not create the public database backup." }

        try {
            checkNotNull(context.contentResolver.openOutputStream(uri)).use { output ->
                backupFile.inputStream().use { input -> input.copyTo(output) }
            }
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null
            )
        } catch (error: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
        return "/storage/emulated/0/${Environment.DIRECTORY_DOWNLOADS}/${backupFile.name}"
    }

    private fun Long.toTimestamp(): Timestamp = Timestamp(Date(this))

    private companion object {
        const val MAX_BATCH_WRITES = 450
    }
}
