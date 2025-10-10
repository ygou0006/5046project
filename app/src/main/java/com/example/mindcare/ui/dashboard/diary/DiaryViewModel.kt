package com.example.mindcare.ui.dashboard.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindcare.data.models.DiaryEntry
import com.example.mindcare.data.repository.DiaryRepository
import com.example.mindcare.utils.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    // Mood options list
    val moodOptions = listOf(
        MoodOption("Happy", "😊"),
        MoodOption("Sad", "😢"),
        MoodOption("Anxious", "😰"),
        MoodOption("Calm", "😌")
    )

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = sessionManager.getUser()
            currentUserId = user?.id
        }
    }

    fun updateSelectedDate(date: Long) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            formattedDate = formatDate(date)
        )
    }

    fun updateSelectedMood(mood: String) {
        _uiState.value = _uiState.value.copy(selectedMood = mood)
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun saveEntry(onSuccess: () -> Unit) {
        currentUserId?.let { userId ->
            if (_uiState.value.selectedDate == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Please select a date"
                )
                return
            }

            if (_uiState.value.selectedMood.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Please select a mood"
                )
                return
            }

            _uiState.value = _uiState.value.copy(isLoading = true)

            viewModelScope.launch {
                try {
                    val entry = DiaryEntry(
                        userId = userId,
                        date = _uiState.value.selectedDate!!,
                        mood = _uiState.value.selectedMood,
                        note = _uiState.value.note
                    )

                    diaryRepository.insertEntry(entry)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showSuccessMessage = true,
                        errorMessage = null
                    )

                    // Hide success message and reset form after 3 seconds
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(3000)
                        resetForm()
                        onSuccess()
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to save entry: ${e.message}"
                    )
                }
            }
        } ?: run {
            _uiState.value = _uiState.value.copy(
                errorMessage = "User not found. Please log in again."
            )
        }
    }

    fun resetForm() {
        _uiState.value = DiaryUiState(
            formattedDate = formatDate(System.currentTimeMillis())
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(showSuccessMessage = false)
    }

    private fun formatDate(date: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date(date))
    }
}

data class DiaryUiState(
    val selectedDate: Long? = null,
    val formattedDate: String = "",
    val selectedMood: String = "",
    val note: String = "",
    val isLoading: Boolean = false,
    val showSuccessMessage: Boolean = false,
    val errorMessage: String? = null
) {
    val isFormValid: Boolean
        get() = selectedDate != null && selectedMood.isNotBlank()
}

// Data class, used to represent mood options
data class MoodOption(
    val name: String,
    val emoji: String
)