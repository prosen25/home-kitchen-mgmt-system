package com.kitchentwenty2.di

import com.google.firebase.firestore.FirebaseFirestore
import com.kitchentwenty2.data.remote.firestore.FirestoreCustomerRepository
import com.kitchentwenty2.data.remote.firestore.FirestoreCustomerRepositoryImpl
import com.kitchentwenty2.data.remote.firestore.FirestoreExpenseRepository
import com.kitchentwenty2.data.remote.firestore.FirestoreExpenseRepositoryImpl
import com.kitchentwenty2.data.remote.firestore.FirestoreMenuRepository
import com.kitchentwenty2.data.remote.firestore.FirestoreMenuRepositoryImpl
import com.kitchentwenty2.data.remote.firestore.FirestoreOrderRepository
import com.kitchentwenty2.data.remote.firestore.FirestoreOrderRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirestoreModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideCustomerRepository(impl: FirestoreCustomerRepositoryImpl): FirestoreCustomerRepository = impl

    @Provides
    @Singleton
    fun provideMenuRepository(impl: FirestoreMenuRepositoryImpl): FirestoreMenuRepository = impl

    @Provides
    @Singleton
    fun provideExpenseRepository(impl: FirestoreExpenseRepositoryImpl): FirestoreExpenseRepository = impl

    @Provides
    @Singleton
    fun provideOrderRepository(impl: FirestoreOrderRepositoryImpl): FirestoreOrderRepository = impl
}
