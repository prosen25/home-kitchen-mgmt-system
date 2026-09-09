package com.kitchentwenty2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kitchentwenty2.data.local.dao.AppErrorLogDao
import com.kitchentwenty2.data.local.dao.CustomerDao
import com.kitchentwenty2.data.local.dao.ExpenseDao
import com.kitchentwenty2.data.local.dao.MenuItemDao
import com.kitchentwenty2.data.local.dao.OrderDao
import com.kitchentwenty2.data.local.dao.SyncMetadataDao
import com.kitchentwenty2.data.local.entity.AppErrorLogEntity
import com.kitchentwenty2.data.local.entity.CustomerEntity
import com.kitchentwenty2.data.local.entity.ExpenseEntity
import com.kitchentwenty2.data.local.entity.MenuItemEntity
import com.kitchentwenty2.data.local.entity.OrderEntity
import com.kitchentwenty2.data.local.entity.OrderItemEntity
import com.kitchentwenty2.data.local.entity.PaymentLogEntity
import com.kitchentwenty2.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CustomerEntity::class,
        MenuItemEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PaymentLogEntity::class,
        ExpenseEntity::class,
        AppErrorLogEntity::class,
        com.kitchentwenty2.data.local.entity.SyncQueueEntity::class,
        com.kitchentwenty2.data.local.entity.SyncMetadataEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class KitchenDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun menuItemDao(): MenuItemDao
    abstract fun orderDao(): OrderDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun appErrorLogDao(): AppErrorLogDao
    abstract fun syncQueueDao(): com.kitchentwenty2.data.local.dao.SyncQueueDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        const val DATABASE_NAME = "kitchen_twenty2_db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema delta needed for this milestone, but we keep an explicit migration
                // in place to satisfy data safety and version-control requirements.
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sync_queue` (`syncId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `operationType` TEXT NOT NULL, `collectionName` TEXT NOT NULL, `documentId` TEXT, `payload` TEXT NOT NULL, `createdBy` TEXT, `createdDateTimeStamp` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, `lastError` TEXT)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sync_metadata` (`key` TEXT NOT NULL, `collectionName` TEXT, `lastSyncedAt` INTEGER, `pendingCount` INTEGER NOT NULL, `lastError` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))"
                )
            }
        }

        fun create(context: Context): KitchenDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                KitchenDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
        }
    }
}
