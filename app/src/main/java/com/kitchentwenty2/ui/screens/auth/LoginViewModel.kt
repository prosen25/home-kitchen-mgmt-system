package com.kitchentwenty2.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitchentwenty2.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun signInWithEmailPassword() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Email and password are required.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signInWithEmailPassword(email, password)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Unable to sign in with email and password."
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Google sign-in was cancelled or failed.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signInWithGoogleIdToken(idToken)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Unable to sign in with Google."
                )
            }
        }
    }
}
