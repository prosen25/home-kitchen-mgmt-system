package com.kitchentwenty2.data.repository

import com.kitchentwenty2.data.local.dao.CustomerDao
import com.kitchentwenty2.data.local.entity.CustomerEntity
import com.kitchentwenty2.domain.model.CustomerProfile
import com.kitchentwenty2.domain.repository.CustomerRepository
import com.kitchentwenty2.util.AppErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val errorLogger: AppErrorLogger
) : CustomerRepository {

    override fun searchCustomers(query: String): Flow<List<CustomerProfile>> {
        return customerDao.searchCustomers(query)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getAllCustomers(): Flow<List<CustomerProfile>> {
        return customerDao.getAllCustomers()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getCustomerById(customerId: Long): CustomerProfile? {
        return withContext(Dispatchers.IO) {
            try {
                customerDao.getCustomerById(customerId)?.toDomain()
            } catch (e: Exception) {
                errorLogger.logException(e, "CustomerRepository.getCustomerById")
                null
            }
        }
    }

    override suspend fun saveOrUpdateCustomer(
        name: String,
        phone: String?,
        address: String?,
        locationUrl: String?
    ): Long {
        return withContext(Dispatchers.IO) {
            try {
                val existing = customerDao.getCustomerByName(name.trim())
                if (existing != null) {
                    val updated = existing.copy(
                        mobileNumber = phone ?: existing.mobileNumber,
                        address = address ?: existing.address,
                        googleLocationUrl = locationUrl ?: existing.googleLocationUrl,
                        modifiedDateTimeStamp = System.currentTimeMillis()
                    )
                    customerDao.updateCustomer(updated)
                    existing.customerId
                } else {
                    val entity = CustomerEntity(
                        name = name.trim(),
                        mobileNumber = phone,
                        address = address,
                        googleLocationUrl = locationUrl,
                        createdDateTimeStamp = System.currentTimeMillis(),
                        modifiedDateTimeStamp = System.currentTimeMillis()
                    )
                    customerDao.insertCustomer(entity)
                }
            } catch (e: Exception) {
                errorLogger.logException(e, "CustomerRepository.saveOrUpdateCustomer")
                -1L
            }
        }
    }

    private fun CustomerEntity.toDomain() = CustomerProfile(
        customerId = customerId,
        name = name,
        mobileNumber = mobileNumber ?: "",
        address = address ?: "",
        googleLocationUrl = googleLocationUrl
    )
}
