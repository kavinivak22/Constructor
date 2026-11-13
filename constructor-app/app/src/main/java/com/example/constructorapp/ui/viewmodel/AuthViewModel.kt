package com.example.constructorapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.remote.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfileDto?>(null)
    val userProfile: StateFlow<UserProfileDto?> = _userProfile.asStateFlow()

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun parseFriendlyErrorMessage(rawError: String): String {
        return try {
            val json = JSONObject(rawError)
            val msg = json.optString("error_description", json.optString("msg", json.optString("message", json.optString("error", ""))))
            if (msg.contains("Invalid login credentials", ignoreCase = true)) {
                "Invalid email or password. Please check your credentials and try again."
            } else if (msg.contains("already registered", ignoreCase = true)) {
                "This email is already registered. Please sign in instead."
            } else if (msg.contains("password should be at least", ignoreCase = true)) {
                "Password must be at least 6 characters long."
            } else if (msg.isNotBlank()) {
                msg
            } else {
                rawError
            }
        } catch (_: Exception) {
            if (rawError.contains("Invalid login credentials", ignoreCase = true)) {
                "Invalid email or password. Please check your credentials and try again."
            } else if (rawError.contains("already registered", ignoreCase = true)) {
                "This email is already registered. Please sign in instead."
            } else {
                rawError
            }
        }
    }

    fun login(
        email: String,
        password: String,
        onLoginSuccess: () -> Unit,
        onNeedsCompany: (userId: String, email: String) -> Unit = { _, _ -> },
        onAccessDenied: () -> Unit = {}
    ) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill all required fields")
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val response = SupabaseClient.api.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    SupabaseClient.sessionManager.saveTokens(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        userId = body.user.id,
                        email = body.user.email ?: email.trim()
                    )

                    // Verify User Status & Company Membership (exact webapp logic)
                    val profileRes = try {
                        SupabaseClient.api.getUserProfile(
                            authHeader = "Bearer ${body.accessToken}",
                            idFilter = "eq.${body.user.id}"
                        )
                    } catch (_: Exception) { null }

                    val profile = profileRes?.body()?.firstOrNull()
                    _userProfile.value = profile

                    if (profile != null) {
                        if (profile.status.equals("inactive", ignoreCase = true)) {
                            _uiState.value = AuthUiState.AccessDenied(body.user.id, profile.email ?: email.trim())
                            onAccessDenied()
                            return@launch
                        }

                        if (profile.companyId.isNullOrBlank()) {
                            _uiState.value = AuthUiState.NeedsCompany(body.user.id, profile.email ?: email.trim(), profile.displayName ?: "")
                            onNeedsCompany(body.user.id, profile.email ?: email.trim())
                            return@launch
                        }

                        // Save company id
                        SupabaseClient.companyId = profile.companyId
                    } else {
                        // User record doesn't exist yet in public.users -> needs company registration
                        _uiState.value = AuthUiState.NeedsCompany(body.user.id, body.user.email ?: email.trim(), "")
                        onNeedsCompany(body.user.id, body.user.email ?: email.trim())
                        return@launch
                    }

                    _uiState.value = AuthUiState.Success
                    onLoginSuccess()
                } else {
                    val rawError = response.errorBody()?.string() ?: "Login failed"
                    _uiState.value = AuthUiState.Error(parseFriendlyErrorMessage(rawError))
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(parseFriendlyErrorMessage(e.localizedMessage ?: "Unknown error occurred"))
            }
        }
    }

    fun signup(
        email: String,
        password: String,
        displayName: String,
        onLoginSuccess: () -> Unit,
        onNeedsCompany: (userId: String, email: String) -> Unit,
        onEmailSent: (email: String) -> Unit
    ) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill out all fields")
            return
        }

        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters long")
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val metadata = mapOf(
                    "full_name" to displayName.trim(),
                    "displayName" to displayName.trim(),
                    "display_name" to displayName.trim(),
                    "name" to displayName.trim()
                )

                val response = SupabaseClient.api.signup(
                    SignupRequest(
                        email = email.trim(),
                        password = password,
                        data = metadata
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (!body.accessToken.isNullOrBlank() && body.user != null) {
                        // Immediate session (email confirmation disabled or auto-confirmed)
                        SupabaseClient.sessionManager.saveTokens(
                            accessToken = body.accessToken,
                            refreshToken = body.refreshToken ?: "",
                            userId = body.user.id,
                            email = body.user.email ?: email.trim()
                        )

                        // Newly created user definitely needs to register or join a company
                        _uiState.value = AuthUiState.NeedsCompany(body.user.id, email.trim(), displayName.trim())
                        onNeedsCompany(body.user.id, email.trim())
                    } else {
                        // Email verification link sent
                        _uiState.value = AuthUiState.EmailVerificationSent(email.trim())
                        onEmailSent(email.trim())
                    }
                } else {
                    val rawError = response.errorBody()?.string() ?: "Signup failed"
                    _uiState.value = AuthUiState.Error(parseFriendlyErrorMessage(rawError))
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(parseFriendlyErrorMessage(e.localizedMessage ?: "Failed to create account"))
            }
        }
    }

    fun registerCompany(
        companyName: String,
        address: String,
        phone: String,
        website: String,
        businessType: String,
        companySize: String,
        displayName: String,
        onSuccess: () -> Unit
    ) {
        val userId = SupabaseClient.userId
        val userEmail = SupabaseClient.userEmail

        if (userId.isNullOrBlank()) {
            _uiState.value = AuthUiState.Error("You must be logged in to register a company.")
            return
        }

        if (companyName.isBlank() || address.isBlank() || phone.isBlank() || businessType.isBlank() || companySize.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill out all required company details.")
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                // 1. Create the company
                val companyReq = CompanyCreateRequest(
                    name = companyName.trim(),
                    address = address.trim(),
                    phone = phone.trim(),
                    website = website.trim().ifBlank { null },
                    businessType = businessType,
                    companySize = companySize,
                    ownerId = userId
                )

                val compResponse = SupabaseClient.api.createCompany(
                    authHeader = SupabaseClient.authHeader,
                    company = companyReq
                )

                if (!compResponse.isSuccessful || compResponse.body().isNullOrEmpty()) {
                    val err = compResponse.errorBody()?.string() ?: "Failed to create company (${compResponse.code()})"
                    _uiState.value = AuthUiState.Error(err)
                    return@launch
                }

                val createdCompany = compResponse.body()!!.first()
                val companyId = createdCompany.id
                SupabaseClient.companyId = companyId

                // 2. Upsert user profile as Admin with company_id
                val userDto = UserProfileDto(
                    id = userId,
                    email = userEmail,
                    displayName = displayName.ifBlank { userEmail?.substringBefore("@") ?: "Admin" },
                    companyId = companyId,
                    role = "admin",
                    status = "active",
                    phone = phone.trim()
                )

                val userResponse = SupabaseClient.api.upsertUserProfile(
                    authHeader = SupabaseClient.authHeader,
                    user = userDto
                )

                if (!userResponse.isSuccessful) {
                    val userErr = userResponse.errorBody()?.string() ?: "Company created, but failed to link administrator user"
                    _uiState.value = AuthUiState.Error(userErr)
                    return@launch
                }

                _userProfile.value = userDto
                _uiState.value = AuthUiState.Success
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Failed to complete company setup")
            }
        }
    }

    fun forgotPassword(email: String, onDone: () -> Unit) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your email address")
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch {
            try {
                val res = SupabaseClient.api.resetPassword(PasswordResetRequest(email.trim()))
                _uiState.value = AuthUiState.PasswordResetSent(email.trim())
                onDone()
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Failed to request password reset")
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        SupabaseClient.sessionManager.clear()
        SupabaseClient.companyId = null
        _userProfile.value = null
        _uiState.value = AuthUiState.Idle
        onSignedOut()
    }
}

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    object Success : AuthUiState
    data class EmailVerificationSent(val email: String) : AuthUiState
    data class NeedsCompany(val userId: String, val email: String, val displayName: String = "") : AuthUiState
    data class AccessDenied(val userId: String, val email: String) : AuthUiState
    data class PasswordResetSent(val email: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
