package com.kitchentwenty2.ui.screens.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitchentwenty2.domain.model.MenuItemModel
import com.kitchentwenty2.domain.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuSetupViewModel @Inject constructor(
    private val menuRepository: MenuRepository
) : ViewModel() {

    val menuItems: StateFlow<List<MenuItemModel>> = menuRepository.getAllMenuItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMenuItem(name: String, price: Double) {
        viewModelScope.launch {
            if (name.isNotBlank() && price > 0) {
                menuRepository.addMenuItem(name = name.trim(), defaultPrice = price)
            }
        }
    }

    fun updateMenuItem(item: MenuItemModel) {
        viewModelScope.launch {
            menuRepository.updateMenuItem(item)
        }
    }

    fun deleteMenuItem(itemId: Long) {
        viewModelScope.launch {
            menuRepository.deleteMenuItem(itemId)
        }
    }
}
