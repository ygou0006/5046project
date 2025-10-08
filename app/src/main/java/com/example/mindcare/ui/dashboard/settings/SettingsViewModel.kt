package com.example.mindcare.ui.dashboard.settings

import android.annotation.SuppressLint
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindcare.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.mindcare.data.models.User
import com.example.mindcare.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    // UI status
    private val _uiState = mutableStateOf<ProfileUiState>(ProfileUiState.Loading)
    val uiState: State<ProfileUiState> = _uiState

    // Modify the status of user information
    private val _editState = mutableStateOf<EditState>(EditState.Idle)
    val editState: State<EditState> = _editState

    // Current user information for editing
    var editableUser by mutableStateOf(User(id = "", email = "", password = "", name = "", createdAt = 0, lastLoginAt = 0))
        private set
    var editableUserBak by mutableStateOf(User(id = "", email = "", password = "", name = "", createdAt = 0, lastLoginAt = 0))
        private set

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // Load user information
    fun loadUserProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = ProfileUiState.Loading
            try {
                val user = sessionManager.getUser()
                if (user != null) {
                    editableUser = user
                    editableUser = editableUser.copy(password = "")
                    editableUserBak = user
                    _uiState.value = ProfileUiState.Success(user)
                } else {
                    _uiState.value = ProfileUiState.Error("User not found")
                }

                if (sessionManager.getConfig("notificationsEnabled") == "TRUE") {
                    _notificationsEnabled.value = true
                } else {
                    _notificationsEnabled.value = false
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    e.message ?: "Failed to load profile"
                )
            }
        }
    }

    // Update user information
    fun updateUserField(field: String, value: String) {
        editableUser = when (field) {
            "email" -> editableUser.copy(email = value)
            "password" -> editableUser.copy(password = value)
            "name" -> editableUser.copy(name = value)
            else -> { return }
        }
    }

    // Save user information
    fun saveUserProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            _editState.value = EditState.Loading
            try {
                if (editableUser.password == "") {
                    editableUser = editableUser.copy(password = editableUserBak.password)
                }
                userRepository.updateUser(editableUser)
                sessionManager.saveUser(editableUser)
                _editState.value = EditState.Success
                _uiState.value = ProfileUiState.Success(editableUser)
            } catch (e: Exception) {
                _editState.value = EditState.Error(
                    e.message ?: "Failed to save profile"
                )
            }
        }
    }

    @SuppressLint("NewApi")
    fun loginOut(): Flow<ProfileResult> = flow {
        try {
            sessionManager.clearSession()
            emit(ProfileResult.Success)
        } catch (e: Exception) {
            emit(ProfileResult.Error(e.message ?: "Login out failed"))
        } finally {
        }
    }.flowOn(Dispatchers.IO)

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        sessionManager.saveConfig("notificationsEnabled", if (enabled) "TRUE" else "FALSE")
    }
}

// UI status
sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: User) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

// Edit status
sealed class EditState {
    object Idle : EditState()
    object Loading : EditState()
    object Success : EditState()
    data class Error(val message: String) : EditState()
}

sealed class ProfileResult {
    object Success : ProfileResult()
    data class Error(val message: String) : ProfileResult()
}