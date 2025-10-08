package com.example.mindcare.ui.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindcare.data.repository.UserRepository
import com.example.mindcare.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            nameError = validateName(name)
        )
    }

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

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = confirmPassword,
            confirmPasswordError = validateConfirmPassword(confirmPassword)
        )
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isConfirmPasswordVisible = !_uiState.value.isConfirmPasswordVisible
        )
    }

    fun updateTermsAccepted(accepted: Boolean) {
        _uiState.value = _uiState.value.copy(
            isTermsAccepted = accepted,
            termsError = if (!accepted) "You must accept the terms" else ""
        )
    }

    fun signUp(onSuccess: () -> Unit) {
        if (isFormValid()) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            viewModelScope.launch {
                try {
                    val result = userRepository.createUser(
                        email = _uiState.value.email,
                        password = _uiState.value.password,
                        name = _uiState.value.name
                    )

                    if (result.isSuccess) {
                        val user = result.getOrNull()
                        if (user != null) {
                            // Save to Session
                            sessionManager.saveUser(user)
                            sessionManager.saveConfig("notificationsEnabled", "TRUE")

                            _uiState.value = _uiState.value.copy(isLoading = false)
                            onSuccess()
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                signUpError = "User creation failed"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            signUpError = result.exceptionOrNull()?.message ?: "Sign up failed"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        signUpError = "Sign up failed: ${e.message}"
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                nameError = validateName(_uiState.value.name),
                emailError = validateEmail(_uiState.value.email),
                passwordError = validatePassword(_uiState.value.password),
                confirmPasswordError = validateConfirmPassword(_uiState.value.confirmPassword),
                termsError = if (!_uiState.value.isTermsAccepted) "You must accept the terms" else ""
            )
        }
    }

    private fun isFormValid(): Boolean {
        return validateName(_uiState.value.name).isEmpty() &&
                validateEmail(_uiState.value.email).isEmpty() &&
                validatePassword(_uiState.value.password).isEmpty() &&
                validateConfirmPassword(_uiState.value.confirmPassword).isEmpty() &&
                _uiState.value.isTermsAccepted
    }

    private fun validateName(name: String): String {
        return when {
            name.isBlank() -> "Full name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            else -> ""
        }
    }

    private fun validateEmail(email: String): String {
        return when {
            email.isBlank() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
            else -> ""
        }
    }

    private fun validatePassword(password: String): String {
        return when {
            password.isBlank() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> ""
        }
    }

    private fun validateConfirmPassword(confirmPassword: String): String {
        return when {
            confirmPassword.isBlank() -> "Please confirm your password"
            confirmPassword != _uiState.value.password -> "Passwords do not match"
            else -> ""
        }
    }

    fun resetErrors() {
        _uiState.value = _uiState.value.copy(
            nameError = "",
            emailError = "",
            passwordError = "",
            confirmPasswordError = "",
            termsError = "",
            signUpError = ""
        )
    }
}

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isTermsAccepted: Boolean = false,
    val isLoading: Boolean = false,
    val nameError: String = "",
    val emailError: String = "",
    val passwordError: String = "",
    val confirmPasswordError: String = "",
    val termsError: String = "",
    val signUpError: String = ""
)