package com.kitchentwenty2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = false
)
abstract class KitchenDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun menuItemDao(): MenuItemDao
    abstract fun orderDao(): OrderDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun appErrorLogDao(): AppErrorLogDao

    companion object {
        const val DATABASE_NAME = "kitchen_twenty2_db"

        fun create(context: Context): KitchenDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                KitchenDatabase::class.java,
                DATABASE_NAME
            )
            .addCallback(DatabasePrepopulationCallback(context))
            .fallbackToDestructiveMigration()
            .build()
        }
    }

    private class DatabasePrepopulationCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = create(context)
                seedDefaultData(database)
            }
        }

        private suspend fun seedDefaultData(database: KitchenDatabase) {
            val now = System.currentTimeMillis()
            val todayStart = DateTimeUtils.getStartOfDay(now)

            // 1. Seed Master Menu Catalog
            val menuItems = listOf(
                MenuItemEntity(name = "Chicken Dum Biryani", description = "Fragrant basmati rice slow-cooked with spiced chicken", defaultPrice = 260.0),
                MenuItemEntity(name = "Mutton Dum Biryani", description = "Tender mutton with saffron flavored long-grain rice", defaultPrice = 340.0),
                MenuItemEntity(name = "Veg Fried Rice", description = "Wok-tossed basmati rice with crunchy vegetables", defaultPrice = 180.0),
                MenuItemEntity(name = "Paneer Butter Masala", description = "Fresh cottage cheese in creamy tomato-cashew gravy", defaultPrice = 220.0),
                MenuItemEntity(name = "Chicken Tikka Masala", description = "Charcoal-grilled chicken chunks in aromatic spicy sauce", defaultPrice = 280.0),
                MenuItemEntity(name = "Special Raita", description = "Chilled yogurt mixed with roasted cumin and cucumber", defaultPrice = 60.0),
                MenuItemEntity(name = "Gulab Jamun (4 pcs)", description = "Melt-in-mouth milk solids soaked in cardamom syrup", defaultPrice = 90.0)
            )
            database.menuItemDao().insertAllMenuItems(menuItems)

            // 2. Seed Sample Customers
            val custId1 = database.customerDao().insertCustomer(
                CustomerEntity(
                    name = "Jane Doe",
                    mobileNumber = "+91 98765 43210",
                    address = "Flat 402, Green Valley Apts, Indiranagar, Bengaluru",
                    googleLocationUrl = "https://maps.google.com/?q=12.9716,77.5946"
                )
            )

            val custId2 = database.customerDao().insertCustomer(
                CustomerEntity(
                    name = "Rahul Sharma",
                    mobileNumber = "+91 91234 56780",
                    address = "House #12, 4th Cross, Koramangala, Bengaluru",
                    googleLocationUrl = "https://maps.google.com/?q=12.9352,77.6245"
                )
            )

            // 3. Seed Sample Orders for Today
            // Order 1: Partially Paid
            val orderId1 = database.orderDao().insertOrder(
                OrderEntity(
                    customerId = custId1,
                    customerName = "Jane Doe",
                    customerPhone = "+91 98765 43210",
                    customerAddress = "Flat 402, Green Valley Apts, Indiranagar, Bengaluru",
                    googleLocationUrl = "https://maps.google.com/?q=12.9716,77.5946",
                    orderDate = todayStart,
                    upfrontDiscount = 0.0,
                    settlementDiscount = 0.0,
                    advancePaid = 200.0,
                    totalCollected = 200.0,
                    refundedAmount = 0.0,
                    totalAmount = 580.0,
                    status = "PARTIALLY_PAID"
                )
            )

            database.orderDao().insertOrderItems(
                listOf(
                    OrderItemEntity(orderId = orderId1, itemName = "Chicken Dum Biryani", unitPrice = 260.0, quantity = 2, subtotal = 520.0),
                    OrderItemEntity(orderId = orderId1, itemName = "Special Raita", unitPrice = 60.0, quantity = 1, subtotal = 60.0)
                )
            )

            database.orderDao().insertPaymentLog(
                PaymentLogEntity(orderId = orderId1, paymentDate = now - 3600000, amount = 200.0, paymentType = "ADVANCE")
            )

            // Order 2: Fully Paid
            val orderId2 = database.orderDao().insertOrder(
                OrderEntity(
                    customerId = custId2,
                    customerName = "Rahul Sharma",
                    customerPhone = "+91 91234 56780",
                    customerAddress = "House #12, 4th Cross, Koramangala, Bengaluru",
                    googleLocationUrl = "https://maps.google.com/?q=12.9352,77.6245",
                    orderDate = todayStart,
                    upfrontDiscount = 0.0,
                    settlementDiscount = 0.0,
                    advancePaid = 980.0,
                    totalCollected = 980.0,
                    refundedAmount = 0.0,
                    totalAmount = 980.0,
                    status = "FULLY_PAID"
                )
            )

            database.orderDao().insertOrderItems(
                listOf(
                    OrderItemEntity(orderId = orderId2, itemName = "Veg Fried Rice", unitPrice = 180.0, quantity = 3, subtotal = 540.0),
                    OrderItemEntity(orderId = orderId2, itemName = "Paneer Butter Masala", unitPrice = 220.0, quantity = 2, subtotal = 440.0)
                )
            )

            database.orderDao().insertPaymentLog(
                PaymentLogEntity(orderId = orderId2, paymentDate = now - 7200000, amount = 980.0, paymentType = "SETTLEMENT")
            )

            // 4. Seed Sample Expenses
            database.expenseDao().insertExpense(
                ExpenseEntity(
                    expenseDate = todayStart,
                    category = "Groceries",
                    amount = 450.0,
                    notes = "Fresh vegetables, herbs, and dairy"
                )
            )
            database.expenseDao().insertExpense(
                ExpenseEntity(
                    expenseDate = todayStart,
                    category = "Packaging",
                    amount = 180.0,
                    notes = "Takeaway containers & carry bags"
                )
            )
        }
    }
}
