package com.kitchentwenty2.ui.screens.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitchentwenty2.domain.model.CustomerProfile
import com.kitchentwenty2.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomerListViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val customers: StateFlow<List<CustomerProfile>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) customerRepository.getAllCustomers()
            else customerRepository.searchCustomers(query.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun addCustomer(name: String, phone: String?, address: String?, locationUrl: String?) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            customerRepository.createCustomer(name, phone, address, locationUrl)
        }
    }

    fun updateCustomer(customerId: Long, name: String, phone: String?, address: String?, locationUrl: String?) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            customerRepository.updateCustomer(customerId, name, phone, address, locationUrl)
        }
    }

    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch {
            customerRepository.deleteCustomer(customerId)
        }
    }
}
