package com.example.mindcare.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindcare.data.repository.UserRepository
import com.example.mindcare.service.NotificationService
import com.example.mindcare.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val notificationService: NotificationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            emailError = validateEmail(email)
        )
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = validatePassword(password)
        )
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }

    fun login(onSuccess: () -> Unit) {
        if (isFormValid()) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            viewModelScope.launch {
                try {
                    // Verify user credentials
                    val user = userRepository.getUserByEmail(_uiState.value.email)

                    if (user != null && user.password == _uiState.value.password) {
                        // Update last login time
                        userRepository.updateLastLogin(user.id)

                        // Save to Session
                        sessionManager.saveUser(user)
                        sessionManager.saveConfig("notificationsEnabled", "TRUE")
                        notificationService.enableAllNotifications()

                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onSuccess()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            loginError = "Invalid email or password"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loginError = "Login failed: ${e.message}"
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                emailError = validateEmail(_uiState.value.email),
                passwordError = validatePassword(_uiState.value.password)
            )
        }
    }

    private fun isFormValid(): Boolean {
        return validateEmail(_uiState.value.email).isEmpty() &&
                validatePassword(_uiState.value.password).isEmpty()
    }

    private fun validateEmail(email: String): String {
        return when {
            email.isBlank() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
            else -> ""
        }
    }

    private fun validatePassword(password: String): String {
        return if (password.isBlank()) "Password is required" else ""
    }

    fun resetErrors() {
        _uiState.value = _uiState.value.copy(
            emailError = "",
            passwordError = "",
            loginError = ""
        )
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: String = "",
    val passwordError: String = "",
    val loginError: String = ""
)