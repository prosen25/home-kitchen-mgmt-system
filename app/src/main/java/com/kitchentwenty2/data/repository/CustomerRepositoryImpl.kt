package com.kitchentwenty2.data.repository

import com.kitchentwenty2.data.local.dao.CustomerDao
import com.kitchentwenty2.data.local.entity.CustomerEntity
import com.kitchentwenty2.data.remote.firestore.FirestoreCustomer
import com.kitchentwenty2.data.remote.firestore.FirestoreCustomerRepository
import com.kitchentwenty2.domain.model.CustomerProfile
import com.kitchentwenty2.domain.repository.CustomerRepository
import com.kitchentwenty2.util.AppErrorLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val firestoreCustomerRepository: FirestoreCustomerRepository,
    private val errorLogger: AppErrorLogger
) : CustomerRepository {

    override fun searchCustomers(query: String): Flow<List<CustomerProfile>> {
        return withRemoteCustomers(customerDao.searchCustomers(query))
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getAllCustomers(): Flow<List<CustomerProfile>> {
        return withRemoteCustomers(customerDao.getAllCustomers())
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

    override suspend fun findCustomerByMobileOrName(mobile: String, name: String): CustomerProfile? {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedMobile = normalizeMobile(mobile)
                val normalizedName = normalizeName(name)
                val customers = customerDao.getAllCustomersSnapshot()

                val byMobile = normalizedMobile.takeIf { it.isNotBlank() }?.let { targetMobile ->
                    customers.firstOrNull { normalizeMobile(it.mobileNumber) == targetMobile }
                }

                val match = byMobile ?: customers.firstOrNull { normalizeName(it.name) == normalizedName }
                match?.toDomain()
            } catch (e: Exception) {
                errorLogger.logException(e, "CustomerRepository.findCustomerByMobileOrName")
                null
            }
        }
    }

    override suspend fun createCustomer(
        name: String,
        phone: String?,
        address: String?,
        locationUrl: String?
    ): Long {
        return withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val entity = CustomerEntity(
                    name = name.trim(),
                    mobileNumber = phone?.trim().takeUnless { it.isNullOrBlank() },
                    address = address?.trim().takeUnless { it.isNullOrBlank() },
                    googleLocationUrl = locationUrl?.trim().takeUnless { it.isNullOrBlank() },
                    createdDateTimeStamp = now,
                    modifiedDateTimeStamp = now
                )
                val customerId = customerDao.insertCustomer(entity)
                syncCustomerUpsert(entity.copy(customerId = customerId))
                customerId
            } catch (e: Exception) {
                errorLogger.logException(e, "CustomerRepository.createCustomer")
                -1L
            }
        }
    }

    override suspend fun updateCustomer(
        customerId: Long,
        name: String,
        phone: String?,
        address: String?,
        locationUrl: String?
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val existing = customerDao.getCustomerById(customerId) ?: return@withContext false
                val updated = existing.copy(
                    name = name.trim(),
                    mobileNumber = phone?.trim().takeUnless { it.isNullOrBlank() },
                    address = address?.trim().takeUnless { it.isNullOrBlank() },
                    googleLocationUrl = locationUrl?.trim().takeUnless { it.isNullOrBlank() },
                    modifiedDateTimeStamp = System.currentTimeMillis()
                )
                customerDao.updateCustomer(updated)
                syncCustomerUpsert(updated)
                true
            } catch (e: Exception) {
                errorLogger.logException(e, "CustomerRepository.updateCustomer")
                false
            }
        }
    }

    override suspend fun deleteCustomer(customerId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val deleted = customerDao.deleteCustomerById(customerId) > 0
                if (deleted) {
                    firestoreCustomerRepository.deleteCustomer(customerId.toString())
                }
                deleted
            } catch (e: Exception) {
                errorLogger.logException(e, "CustomerRepository.deleteCustomer")
                false
            }
        }
    }

    private suspend fun syncCustomerUpsert(customer: CustomerEntity) {
        firestoreCustomerRepository.createOrUpdateCustomer(
            FirestoreCustomer(
                id = customer.customerId.toString(),
                name = customer.name,
                mobileNumber = customer.mobileNumber,
                address = customer.address,
                googleLocationUrl = customer.googleLocationUrl,
                createdBy = customer.createdBy
            )
        )
    }

    private fun withRemoteCustomers(local: Flow<List<CustomerEntity>>): Flow<List<CustomerEntity>> = channelFlow {
        launch {
            firestoreCustomerRepository.listenAllCustomers()
                .catch { errorLogger.logException(it, "CustomerRepository.listenAllCustomers") }
                .collect { customers ->
                    customers.forEach { customer ->
                        val customerId = customer.id.toLongOrNull()?.takeIf { it > 0 } ?: return@forEach
                        val now = System.currentTimeMillis()
                        customerDao.upsertSyncedCustomer(
                            CustomerEntity(
                                customerId = customerId,
                                name = customer.name,
                                mobileNumber = customer.mobileNumber,
                                address = customer.address,
                                googleLocationUrl = customer.googleLocationUrl,
                                createdBy = customer.createdBy ?: "SYSTEM",
                                createdDateTimeStamp = customer.createdAt?.toDate()?.time ?: now,
                                modifiedDateTimeStamp = customer.modifiedAt?.toDate()?.time ?: now
                            )
                        )
                    }
                }
        }
        local.collect { send(it) }
    }

    private fun normalizeMobile(mobile: String?): String {
        return mobile.orEmpty().filter(Char::isDigit)
    }

    private fun normalizeName(name: String): String {
        return name.trim().lowercase()
    }

    private fun CustomerEntity.toDomain() = CustomerProfile(
        customerId = customerId,
        name = name,
        mobileNumber = mobileNumber ?: "",
        address = address ?: "",
        googleLocationUrl = googleLocationUrl
    )
}
