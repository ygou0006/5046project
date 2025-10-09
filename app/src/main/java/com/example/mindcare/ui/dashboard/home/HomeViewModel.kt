package com.example.mindcare.ui.dashboard.home

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
class HomeViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _diaryEntries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val diaryEntries: StateFlow<List<DiaryEntry>> = _diaryEntries.asStateFlow()

    private var currentUserId: String? = null

    // Recommended activity pool
    private val recommendationPool = mapOf(
        "positive" to listOf(
            RecommendationActivity("Keep it up", "💪"),
            RecommendationActivity("Share joy", "🌟"),
            RecommendationActivity("Stay active", "🚶"),
            RecommendationActivity("Morning walks", "🌅"),
            RecommendationActivity("Gratitude journal", "📔")
        ),
        "negative" to listOf(
            RecommendationActivity("Deep breathing", "🌬️"),
            RecommendationActivity("Talk to someone", "💬"),
            RecommendationActivity("Gentle exercise", "🧘"),
            RecommendationActivity("Listen to music", "🎵"),
            RecommendationActivity("Self-care time", "🛀")
        ),
        "balanced" to listOf(
            RecommendationActivity("Mindful moments", "🧠"),
            RecommendationActivity("Stay hydrated", "💧"),
            RecommendationActivity("Good sleep", "😴"),
            RecommendationActivity("Healthy meals", "🍎"),
            RecommendationActivity("Connect with nature", "🌳")
        ),
        "default" to listOf(
            RecommendationActivity("Start tracking", "📝"),
            RecommendationActivity("Daily reflection", "🤔"),
            RecommendationActivity("Set small goals", "🎯")
        )
    )

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val currentUser = sessionManager.getUser()
            currentUser?.let { user ->
                currentUserId = user.id
                _uiState.value = _uiState.value.copy(
                    userName = user.name,
                    userEmail = user.email
                )
                loadTodayMood(user.id)
                observeUserEntries(user.id)
            }
        }
    }

    private fun observeUserEntries(userId: String) {
        viewModelScope.launch {
            diaryRepository.getEntriesByUser(userId).collect { entries ->
                _diaryEntries.value = entries
                updateAiInsightsAndRecommendations(entries)
            }
        }
    }

    private suspend fun loadTodayMood(userId: String) {
        val todayEntry = diaryRepository.getTodayEntry(userId)
        _uiState.value = _uiState.value.copy(
            todayMood = todayEntry?.mood,
            hasTodayEntry = todayEntry != null
        )
    }

    fun saveMood(mood: String) {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                try {
                    diaryRepository.saveOrUpdateTodayMood(userId, mood)
                    _uiState.value = _uiState.value.copy(
                        todayMood = mood,
                        hasTodayEntry = true,
                        isLoading = false,
                        showSuccessMessage = true
                    )

                    // Hide success message after 3 seconds
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(3000)
                        _uiState.value = _uiState.value.copy(showSuccessMessage = false)
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to save mood: ${e.message}"
                    )
                }
            }
        }
    }

    private fun updateAiInsightsAndRecommendations(entries: List<DiaryEntry>) {
        if (entries.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                aiInsights = "Start tracking your mood to get personalized insights and recommendations for your mental wellbeing.",
                recommendations = getRandomRecommendations("default", 3)
            )
            return
        }

        // Analyze recent emotional records
        val recentEntries = entries.take(7)
        val moodCount = recentEntries.groupingBy { it.mood }.eachCount()

        val mostCommonMood = moodCount.maxByOrNull { it.value }?.key ?: "unknown"
        val positiveMoods = listOf("Happy", "Excited", "Calm")
        val negativeMoods = listOf("Sad", "Anxious", "Angry")

        val positiveCount = positiveMoods.sumOf { moodCount[it] ?: 0 }
        val negativeCount = negativeMoods.sumOf { moodCount[it] ?: 0 }

        val (insight, recommendationType) = when {
            positiveCount > negativeCount * 1.5 -> {
                Pair(
                    "You've been feeling positive lately! Your most common mood is $mostCommonMood. " +
                            "Consider what activities make you feel this way and try to incorporate them more often.",
                    "positive"
                )
            }
            negativeCount > positiveCount * 1.5 -> {
                Pair(
                    "We notice you've been experiencing some challenging emotions recently. " +
                            "Remember that it's okay to not feel okay. Be kind to yourself during this time.",
                    "negative"
                )
            }
            else -> {
                Pair(
                    "Your mood has been balanced recently. Keep tracking to better understand " +
                            "your emotional patterns and what influences them.",
                    "balanced"
                )
            }
        }

        _uiState.value = _uiState.value.copy(
            aiInsights = insight,
            recommendations = getRandomRecommendations(recommendationType, 3)
        )
    }

    private fun getRandomRecommendations(type: String, count: Int): List<RecommendationActivity> {
        val pool = recommendationPool[type] ?: recommendationPool["default"]!!
        return pool.shuffled().take(count)
    }

    fun getGreeting(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            in 18..21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class HomeUiState(
    val userName: String = "",
    val userEmail: String = "",
    val todayMood: String? = null,
    val hasTodayEntry: Boolean = false,
    val aiInsights: String = "Loading your insights...",
    val recommendations: List<RecommendationActivity> = emptyList(),
    val isLoading: Boolean = false,
    val showSuccessMessage: Boolean = false,
    val errorMessage: String? = null
)

// Data class, used to represent recommendation activities
data class RecommendationActivity(
    val title: String,
    val emoji: String
)