package com.kitchentwenty2.ui.screens.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitchentwenty2.domain.model.MenuItemModel
import com.kitchentwenty2.domain.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuSetupViewModel @Inject constructor(
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val _addNameError = MutableStateFlow<String?>(null)
    val addNameError: StateFlow<String?> = _addNameError.asStateFlow()

    private val _editNameError = MutableStateFlow<String?>(null)
    val editNameError: StateFlow<String?> = _editNameError.asStateFlow()

    val menuItems: StateFlow<List<MenuItemModel>> = menuRepository.getAllMenuItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMenuItem(name: String, price: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            if (name.isNotBlank() && price > 0) {
                val normalizedName = name.trim()
                if (menuRepository.existsByName(normalizedName)) {
                    _addNameError.value = "A menu item with this dish name already exists"
                    return@launch
                }
                _addNameError.value = null
                val id = menuRepository.addMenuItem(name = normalizedName, defaultPrice = price)
                if (id > 0) onSuccess()
            }
        }
    }

    fun updateMenuItem(item: MenuItemModel, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val normalizedName = item.name.trim()
            if (menuRepository.existsByName(normalizedName, excludeId = item.menuItemId)) {
                _editNameError.value = "A menu item with this dish name already exists"
                return@launch
            }
            _editNameError.value = null
            menuRepository.updateMenuItem(item.copy(name = normalizedName))
            onSuccess()
        }
    }

    fun deleteMenuItem(itemId: Long) {
        viewModelScope.launch {
            menuRepository.deleteMenuItem(itemId)
        }
    }
}
