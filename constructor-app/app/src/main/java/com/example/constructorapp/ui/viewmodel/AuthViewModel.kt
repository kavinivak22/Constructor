package com.example.constructorapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.remote.LoginRequest
import com.example.constructorapp.data.remote.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill all fields")
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val response = SupabaseClient.api.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    SupabaseClient.authToken = body.accessToken
                    SupabaseClient.userId = body.user.id
                    SupabaseClient.userEmail = body.user.email

                    _uiState.value = AuthUiState.Success
                    onSuccess()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Login failed"
                    _uiState.value = AuthUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
}

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}
