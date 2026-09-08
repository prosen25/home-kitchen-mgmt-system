package com.kitchentwenty2.ui.screens.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitchentwenty2.domain.model.CustomerProfile
import com.kitchentwenty2.domain.model.MenuItemModel
import com.kitchentwenty2.domain.model.OrderFormState
import com.kitchentwenty2.domain.model.OrderItemForm
import com.kitchentwenty2.domain.repository.CustomerRepository
import com.kitchentwenty2.domain.repository.MenuRepository
import com.kitchentwenty2.domain.repository.OrderRepository
import com.kitchentwenty2.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OrderCreateEditViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val menuRepository: MenuRepository,
    private val customerRepository: CustomerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val orderIdToEdit: Long? = savedStateHandle.get<Long>("orderId")?.takeIf { it > 0 }

    private val _formState = MutableStateFlow(
        OrderFormState(
            isEditMode = orderIdToEdit != null,
            orderId = orderIdToEdit ?: 0L,
            orderDate = DateTimeUtils.formatDate(System.currentTimeMillis())
        )
    )
    val formState: StateFlow<OrderFormState> = _formState.asStateFlow()

    val menuItems: StateFlow<List<MenuItemModel>> = menuRepository.getAllMenuItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSuggestions: StateFlow<List<CustomerProfile>> = _customerSearchQuery
        .flatMapLatest { query ->
            if (query.length >= 2) customerRepository.searchCustomers(query)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (orderIdToEdit != null) {
            loadOrderForEdit(orderIdToEdit)
        }
    }

    private fun loadOrderForEdit(orderId: Long) {
        viewModelScope.launch {
            orderRepository.getOrderDetails(orderId).collect { details ->
                details?.let { d ->
                    _formState.update {
                        it.copy(
                            isEditMode = true,
                            orderId = d.orderId,
                            orderDate = d.orderDate,
                            customerName = d.customerName,
                            mobileNumber = d.customerPhone,
                            address = d.customerAddress,
                            googleLocationUrl = d.googleLocationUrl,
                            items = d.items,
                            upfrontDiscount = d.upfrontDiscount,
                            advancePayment = d.totalCollected
                        )
                    }
                }
            }
        }
    }

    fun onDateChanged(newDate: String) {
        _formState.update { it.copy(orderDate = newDate) }
    }

    fun updateFormState(newState: OrderFormState) {
        _formState.value = newState
    }

    fun onCustomerNameChanged(name: String) {
        _formState.update { it.copy(customerName = name) }
        _customerSearchQuery.value = name
    }

    fun selectCustomer(profile: CustomerProfile) {
        _formState.update {
            it.copy(
                customerName = profile.name,
                mobileNumber = profile.mobileNumber,
                address = profile.address,
                googleLocationUrl = profile.googleLocationUrl ?: ""
            )
        }
        _customerSearchQuery.value = ""
    }

    fun onMobileChanged(mobile: String) {
        _formState.update { it.copy(mobileNumber = mobile) }
    }

    fun onAddressChanged(address: String) {
        _formState.update { it.copy(address = address) }
    }

    fun onLocationUrlChanged(url: String) {
        _formState.update { it.copy(googleLocationUrl = url) }
    }

    fun addItem(name: String, price: Double) {
        _formState.update { current ->
            val existingIndex = current.items.indexOfFirst { it.itemName.equals(name, ignoreCase = true) }
            val newItems = current.items.toMutableList()
            if (existingIndex >= 0) {
                val existing = newItems[existingIndex]
                newItems[existingIndex] = existing.copy(quantity = existing.quantity + 1)
            } else {
                newItems.add(OrderItemForm(itemName = name, unitPrice = price, quantity = 1))
            }
            current.copy(items = newItems)
        }
    }

    fun increaseQuantity(index: Int) {
        _formState.update { current ->
            val newItems = current.items.toMutableList()
            if (index in newItems.indices) {
                val item = newItems[index]
                newItems[index] = item.copy(quantity = item.quantity + 1)
            }
            current.copy(items = newItems)
        }
    }

    fun decreaseQuantity(index: Int) {
        _formState.update { current ->
            val newItems = current.items.toMutableList()
            if (index in newItems.indices) {
                val item = newItems[index]
                if (item.quantity > 1) {
                    newItems[index] = item.copy(quantity = item.quantity - 1)
                } else {
                    newItems.removeAt(index)
                }
            }
            current.copy(items = newItems)
        }
    }

    fun removeItem(index: Int) {
        _formState.update { current ->
            val newItems = current.items.toMutableList()
            if (index in newItems.indices) {
                newItems.removeAt(index)
            }
            current.copy(items = newItems)
        }
    }

    fun onUpfrontDiscountChanged(discount: Double) {
        _formState.update { it.copy(upfrontDiscount = discount) }
    }

    fun onAdvancePaymentChanged(advance: Double) {
        _formState.update { it.copy(advancePayment = advance) }
    }

    fun saveOrder(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val currentState = _formState.value
            if (currentState.customerName.isNotBlank() && currentState.items.isNotEmpty()) {
                val resultId = orderRepository.saveOrder(currentState)
                if (resultId > 0) {
                    onSuccess(resultId)
                }
            }
        }
    }
}
