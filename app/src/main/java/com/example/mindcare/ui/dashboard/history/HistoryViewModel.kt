package com.example.mindcare.ui.history

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
class HistoryViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _allEntries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    private var currentUserId: String? = null

    // Pagination
    private val pageSize = 10
    private var currentPage = 0
    private var canLoadMore = true

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val currentUser = sessionManager.getUser()
            currentUser?.let { user ->
                currentUserId = user.id
                observeUserEntries(user.id)
            }
        }
    }

    private fun observeUserEntries(userId: String) {
        viewModelScope.launch {
            diaryRepository.getEntriesByUser(userId).collect { entries ->
                _allEntries.value = entries.sortedByDescending { it.date }
                applySearchAndPagination()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applySearchAndPagination()
    }

    fun loadMoreEntries() {
        if (!canLoadMore || _uiState.value.isLoadingMore) return

        currentPage++
        applySearchAndPagination()
    }

    fun refreshEntries() {
        currentPage = 0
        canLoadMore = true
        applySearchAndPagination()
    }

    private fun applySearchAndPagination() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            // 应用搜索过滤
            val filteredEntries = if (_uiState.value.searchQuery.isBlank()) {
                _allEntries.value
            } else {
                _allEntries.value.filter { entry ->
                    entry.mood.contains(_uiState.value.searchQuery, ignoreCase = true) ||
                            entry.note.contains(_uiState.value.searchQuery, ignoreCase = true)
                }
            }

            // 应用分页
            val totalItems = filteredEntries.size
            val startIndex = currentPage * pageSize
            val endIndex = minOf(startIndex + pageSize, totalItems)

            val displayedEntries = if (startIndex < totalItems) {
                filteredEntries.subList(0, endIndex)
            } else {
                emptyList()
            }

            canLoadMore = endIndex < totalItems

            _uiState.value = _uiState.value.copy(
                displayedEntries = displayedEntries,
                totalEntries = totalItems,
                isLoadingMore = false,
                showEmptyState = displayedEntries.isEmpty() && _uiState.value.searchQuery.isBlank(),
                showNoResults = displayedEntries.isEmpty() && _uiState.value.searchQuery.isNotBlank()
            )
        }
    }

    fun selectEntryForEditing(entry: DiaryEntry) {
        _uiState.value = _uiState.value.copy(
            selectedEntry = entry,
            showEditDialog = true,
            editMood = entry.mood,
            editNote = entry.note
        )
    }

    fun updateEditMood(mood: String) {
        _uiState.value = _uiState.value.copy(editMood = mood)
    }

    fun updateEditNote(note: String) {
        _uiState.value = _uiState.value.copy(editNote = note)
    }

    fun saveEditedEntry() {
        val selectedEntry = _uiState.value.selectedEntry ?: return
        val updatedEntry = selectedEntry.copy(
            mood = _uiState.value.editMood,
            note = _uiState.value.editNote
        )

        viewModelScope.launch {
            try {
                diaryRepository.updateEntry(updatedEntry)
                _uiState.value = _uiState.value.copy(
                    showEditDialog = false,
                    showSuccessMessage = true
                )

                // 3秒后隐藏成功消息
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _uiState.value = _uiState.value.copy(showSuccessMessage = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update entry: ${e.message}"
                )
            }
        }
    }

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            try {
                diaryRepository.deleteEntry(entry)
                _uiState.value = _uiState.value.copy(
                    showEditDialog = false,
                    showDeleteSuccess = true
                )

                // Hide success message after 3 seconds
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _uiState.value = _uiState.value.copy(showDeleteSuccess = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete entry: ${e.message}"
                )
            }
        }
    }

    fun dismissEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccessMessages() {
        _uiState.value = _uiState.value.copy(
            showSuccessMessage = false,
            showDeleteSuccess = false
        )
    }

    fun formatDate(date: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date(date))
    }

    fun formatTime(date: Long): String {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return timeFormat.format(Date(date))
    }

    fun getMoodEmoji(mood: String): String {
        return when (mood.toLowerCase()) {
            "happy" -> "😊"
            "sad" -> "😢"
            "anxious" -> "😰"
            "calm" -> "😌"
            else -> "😐"
        }
    }
}

data class HistoryUiState(
    val searchQuery: String = "",
    val displayedEntries: List<DiaryEntry> = emptyList(),
    val totalEntries: Int = 0,
    val isLoadingMore: Boolean = false,
    val showEmptyState: Boolean = false,
    val showNoResults: Boolean = false,
    val showEditDialog: Boolean = false,
    val selectedEntry: DiaryEntry? = null,
    val editMood: String = "",
    val editNote: String = "",
    val showSuccessMessage: Boolean = false,
    val showDeleteSuccess: Boolean = false,
    val errorMessage: String? = null
)