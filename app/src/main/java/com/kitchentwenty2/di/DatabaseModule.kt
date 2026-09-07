package com.kitchentwenty2.di

import android.content.Context
import com.kitchentwenty2.data.local.KitchenDatabase
import com.kitchentwenty2.data.local.dao.AppErrorLogDao
import com.kitchentwenty2.data.local.dao.CustomerDao
import com.kitchentwenty2.data.local.dao.ExpenseDao
import com.kitchentwenty2.data.local.dao.MenuItemDao
import com.kitchentwenty2.data.local.dao.OrderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideKitchenDatabase(
        @ApplicationContext context: Context
    ): KitchenDatabase {
        return KitchenDatabase.create(context)
    }

    @Provides
    fun provideCustomerDao(database: KitchenDatabase): CustomerDao {
        return database.customerDao()
    }

    @Provides
    fun provideMenuItemDao(database: KitchenDatabase): MenuItemDao {
        return database.menuItemDao()
    }

    @Provides
    fun provideOrderDao(database: KitchenDatabase): OrderDao {
        return database.orderDao()
    }

    @Provides
    fun provideExpenseDao(database: KitchenDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    fun provideAppErrorLogDao(database: KitchenDatabase): AppErrorLogDao {
        return database.appErrorLogDao()
    }
}
