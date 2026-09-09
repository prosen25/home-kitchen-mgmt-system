package com.kitchentwenty2.di

import com.google.firebase.firestore.FirebaseFirestore
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
    fun provideOrderRepository(impl: FirestoreOrderRepositoryImpl): FirestoreOrderRepository = impl
}
