package com.example.mindcare.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindcare.data.repository.UserRepository
import com.example.mindcare.service.NotificationService
import com.example.mindcare.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val notificationService: NotificationService
) : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth

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
                    // Firebase Auth Login
                    val result = auth.signInWithEmailAndPassword(
                        _uiState.value.email,
                        _uiState.value.password
                    ).await()

                    val firebaseUser = result.user
                    if (firebaseUser != null) {
                        val user = userRepository.getUserByEmail(_uiState.value.email)

                        if (user != null) {
                            userRepository.updateLastLogin(user.id)

                            // Save to Session
                            sessionManager.saveUser(user)
                            sessionManager.saveConfig("notificationsEnabled", "TRUE")
                            notificationService.enableAllNotifications()

                            _uiState.value = _uiState.value.copy(isLoading = false)
                            onSuccess()
                        } else {
                            val userResult = userRepository.createUserFromFirebase(
                                firebaseUser = firebaseUser,
                                name = firebaseUser.displayName ?: "User"
                            )
                            if (userResult.isSuccess) {
                                val newUser = userResult.getOrNull()
                                if (newUser != null) {
                                    // Save to Session
                                    sessionManager.saveUser(newUser)
                                    sessionManager.saveConfig("notificationsEnabled", "TRUE")
                                    notificationService.enableAllNotifications()

                                    _uiState.value = _uiState.value.copy(isLoading = false)
                                    onSuccess()
                                } else {
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        loginError = "Login failed"
                                    )
                                }
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    loginError = userResult.exceptionOrNull()?.message ?: "Login failed"
                                )
                            }
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            loginError = "Firebase authentication failed"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loginError = e.message!!
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