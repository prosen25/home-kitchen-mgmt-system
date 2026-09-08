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
        AppErrorLogEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class KitchenDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun menuItemDao(): MenuItemDao
    abstract fun orderDao(): OrderDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun appErrorLogDao(): AppErrorLogDao

    companion object {
        const val DATABASE_NAME = "kitchen_twenty2_db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No schema delta needed for this milestone, but we keep an explicit migration
                // in place to satisfy data safety and version-control requirements.
            }
        }

        fun create(context: Context): KitchenDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                KitchenDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
