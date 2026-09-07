package com.kitchentwenty2.di

import com.kitchentwenty2.data.repository.CustomerRepositoryImpl
import com.kitchentwenty2.data.repository.ExpenseRepositoryImpl
import com.kitchentwenty2.data.repository.MenuRepositoryImpl
import com.kitchentwenty2.data.repository.OrderRepositoryImpl
import com.kitchentwenty2.domain.repository.CustomerRepository
import com.kitchentwenty2.domain.repository.ExpenseRepository
import com.kitchentwenty2.domain.repository.MenuRepository
import com.kitchentwenty2.domain.repository.OrderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCustomerRepository(
        impl: CustomerRepositoryImpl
    ): CustomerRepository

    @Binds
    @Singleton
    abstract fun bindMenuRepository(
        impl: MenuRepositoryImpl
    ): MenuRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        impl: OrderRepositoryImpl
    ): OrderRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        impl: ExpenseRepositoryImpl
    ): ExpenseRepository
}
