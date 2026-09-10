package com.kitchentwenty2.domain.repository

import com.kitchentwenty2.domain.model.CustomerProfile
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun searchCustomers(query: String): Flow<List<CustomerProfile>>
    fun getAllCustomers(): Flow<List<CustomerProfile>>
    suspend fun getCustomerById(customerId: Long): CustomerProfile?
    suspend fun findCustomerByMobileOrName(mobile: String, name: String): CustomerProfile?
    suspend fun createCustomer(name: String, phone: String?, address: String?, locationUrl: String?): Long
    suspend fun updateCustomer(customerId: Long, name: String, phone: String?, address: String?, locationUrl: String?): Boolean
    suspend fun deleteCustomer(customerId: Long): Boolean
}
