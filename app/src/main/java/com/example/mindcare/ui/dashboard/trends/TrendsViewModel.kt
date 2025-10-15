package com.example.mindcare.ui.dashboard.trends

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
class TrendsViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private val allEntries = mutableListOf<DiaryEntry>()

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
                allEntries.clear()
                allEntries.addAll(entries.sortedBy { it.date })
                analyzeTrends()
            }
        }
    }

    private fun analyzeTrends() {
        if (allEntries.isEmpty()) {
            _uiState.value = TrendsUiState(showEmptyState = true)
            return
        }

        // Mood distribution statistics
        val moodDistribution = allEntries.groupingBy { it.mood }.eachCount()

        // Recent 30 days mood trend
        val last30DaysEntries = allEntries.filter {
            it.date >= getDateDaysAgo(30)
        }

        // Weekly analysis
        val weeklyMoodData = getWeeklyMoodData()

        // Time of day analysis (Morning, Afternoon, Evening)
        val timeOfDayData = getTimeOfDayData()

        // Mood change trend (by day)
        val dailyMoodTrend = getDailyMoodTrend(last30DaysEntries)

        // Mood stability analysis
        val moodStability = calculateMoodStability()

        // Positive mood ratio
        val positiveRatio = calculatePositiveRatio()

        // Line chart data
        val lineChartData = getLineChartData()

        _uiState.value = TrendsUiState(
            moodDistribution = moodDistribution,
            weeklyMoodData = weeklyMoodData,
            timeOfDayData = timeOfDayData,
            dailyMoodTrend = dailyMoodTrend,
            lineChartData = lineChartData,
            totalEntries = allEntries.size,
            averageMoodScore = calculateAverageMoodScore(),
            mostFrequentMood = moodDistribution.maxByOrNull { it.value }?.key ?: "Unknown",
            moodStability = moodStability,
            positiveRatio = positiveRatio,
            longestStreak = calculateLongestStreak(),
            currentStreak = calculateCurrentStreak(),
            showEmptyState = false
        )
    }

    private fun getDateDaysAgo(days: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        return calendar.timeInMillis
    }

    private fun getWeeklyMoodData(): Map<String, Map<String, Int>> {
        val weeklyData = mutableMapOf<String, MutableMap<String, Int>>()

        allEntries.forEach { entry ->
            val weekKey = getWeekKey(entry.date)
            val mood = entry.mood

            if (!weeklyData.containsKey(weekKey)) {
                weeklyData[weekKey] = mutableMapOf()
            }

            weeklyData[weekKey]?.let { weekMap ->
                weekMap[mood] = (weekMap[mood] ?: 0) + 1
            }
        }

        return weeklyData
    }

    private fun getWeekKey(date: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        calendar.firstDayOfWeek = Calendar.MONDAY

        val weekStart = calendar.clone() as Calendar
        weekStart.add(Calendar.DAY_OF_WEEK, calendar.get(Calendar.DAY_OF_WEEK) * -1 + 2)

        val weekEnd = weekStart.clone() as Calendar
        weekEnd.add(Calendar.DAY_OF_WEEK, 6)

        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        return "${dateFormat.format(weekStart.time)} - ${dateFormat.format(weekEnd.time)}"
    }

    private fun getTimeOfDayData(): Map<String, Int> {
        val timeData = mutableMapOf(
            "Morning" to 0,
            "Afternoon" to 0,
            "Evening" to 0,
            "Night" to 0
        )

        allEntries.forEach { entry ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = entry.date
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            when {
                hour in 6..11 -> timeData["Morning"] = timeData["Morning"]!! + 1
                hour in 12..17 -> timeData["Afternoon"] = timeData["Afternoon"]!! + 1
                hour in 18..21 -> timeData["Evening"] = timeData["Evening"]!! + 1
                else -> timeData["Night"] = timeData["Night"]!! + 1
            }
        }

        return timeData
    }

    private fun getDailyMoodTrend(entries: List<DiaryEntry>): Map<String, String> {
        val dailyTrend = mutableMapOf<String, String>()
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        entries.groupBy {
            dateFormat.format(Date(it.date))
        }.forEach { (date, dayEntries) ->
            val mostFrequentMood = dayEntries.groupingBy { it.mood }.eachCount()
                .maxByOrNull { it.value }?.key ?: "Unknown"
            dailyTrend[date] = mostFrequentMood
        }

        return dailyTrend
    }

    private fun getLineChartData(): List<LineChartData> {
        if (allEntries.isEmpty()) return emptyList()

        // Get last 14 days data
        val last14Days = getLastNDays(14)
        val moodScores = mapOf(
            "Happy" to 4f,
            "Calm" to 3f,
            "Sad" to 2f,
            "Anxious" to 1f
        )

        // Pre-group data by day to avoid repeated calculations
        val entriesByDay = allEntries.groupBy { entry ->
            val calendar = Calendar.getInstance().apply { timeInMillis = entry.date }
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }

        val result = mutableListOf<LineChartData>()
        var previousScore = 2.5f // Default neutral score

        last14Days.forEachIndexed { index, date ->
            val dayEntries = entriesByDay[date] ?: emptyList()

            val currentScore = if (dayEntries.isNotEmpty()) {
                dayEntries.map { moodScores[it.mood] ?: 2.5f }.average().toFloat()
            } else {
                // If no records, use previous day's score
                previousScore
            }

            result.add(
                LineChartData(
                    date = date,
                    score = currentScore,
                    hasData = dayEntries.isNotEmpty(),
                    mood = if (dayEntries.isNotEmpty()) {
                        dayEntries.groupingBy { it.mood }.eachCount()
                            .maxByOrNull { it.value }?.key ?: "Unknown"
                    } else {
                        "No Data"
                    }
                )
            )

            previousScore = currentScore
        }

        return result
    }

    private fun getLastNDays(days: Int): List<Long> {
        val calendar = Calendar.getInstance()
        return List(days) { index ->
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -index)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }.reversed()
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun calculateAverageMoodScore(): Float {
        val moodScores = mapOf(
            "Happy" to 4f,
            "Calm" to 3f,
            "Sad" to 2f,
            "Anxious" to 1f
        )

        return if (allEntries.isNotEmpty()) {
            allEntries.map { moodScores[it.mood] ?: 2.5f }.average().toFloat()
        } else {
            0f
        }
    }

    private fun calculateMoodStability(): Float {
        if (allEntries.size < 2) return 1f

        val moodScores = mapOf(
            "Happy" to 4,
            "Calm" to 3,
            "Sad" to 2,
            "Anxious" to 1
        )

        var stabilityScore = 0f
        for (i in 1 until allEntries.size) {
            val prevScore = moodScores[allEntries[i-1].mood] ?: 2
            val currScore = moodScores[allEntries[i].mood] ?: 2
            stabilityScore += 1f - (Math.abs(prevScore - currScore) / 3f)
        }

        return stabilityScore / (allEntries.size - 1)
    }

    private fun calculatePositiveRatio(): Float {
        val positiveMoods = listOf("Happy", "Calm")
        val positiveEntries = allEntries.count { it.mood in positiveMoods }

        return if (allEntries.isNotEmpty()) {
            positiveEntries.toFloat() / allEntries.size
        } else {
            0f
        }
    }

    private fun calculateLongestStreak(): Int {
        if (allEntries.isEmpty()) return 0

        val positiveMoods = listOf("Happy", "Calm")
        var longestStreak = 0
        var currentStreak = 0

        val sortedEntries = allEntries.sortedBy { it.date }

        for (entry in sortedEntries) {
            if (entry.mood in positiveMoods) {
                currentStreak++
                longestStreak = maxOf(longestStreak, currentStreak)
            } else {
                currentStreak = 0
            }
        }

        return longestStreak
    }

    private fun calculateCurrentStreak(): Int {
        if (allEntries.isEmpty()) return 0

        val positiveMoods = listOf("Happy", "Calm")
        val sortedEntries = allEntries.sortedByDescending { it.date }
        var currentStreak = 0

        for (entry in sortedEntries) {
            if (entry.mood in positiveMoods) {
                currentStreak++
            } else {
                break
            }
        }

        return currentStreak
    }

    fun getMoodColor(mood: String): String {
        return when (mood) {
            "Happy" -> "#4CAF50"
            "Calm" -> "#2196F3"
            "Sad" -> "#FF9800"
            "Anxious" -> "#F44336"
            else -> "#9E9E9E"
        }
    }

    fun getMoodEmoji(mood: String): String {
        return when (mood) {
            "Happy" -> "😊"
            "Calm" -> "😌"
            "Sad" -> "😢"
            "Anxious" -> "😰"
            else -> "😐"
        }
    }

    fun formatPercentage(value: Float): String {
        return "${(value * 100).toInt()}%"
    }

    fun formatShortDate(date: Long): String {
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        return dateFormat.format(Date(date))
    }

    fun formatDate(date: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        return dateFormat.format(Date(date))
    }
}

data class TrendsUiState(
    val moodDistribution: Map<String, Int> = emptyMap(),
    val weeklyMoodData: Map<String, Map<String, Int>> = emptyMap(),
    val timeOfDayData: Map<String, Int> = emptyMap(),
    val dailyMoodTrend: Map<String, String> = emptyMap(),
    val lineChartData: List<LineChartData> = emptyList(),
    val totalEntries: Int = 0,
    val averageMoodScore: Float = 0f,
    val mostFrequentMood: String = "",
    val moodStability: Float = 0f,
    val positiveRatio: Float = 0f,
    val longestStreak: Int = 0,
    val currentStreak: Int = 0,
    val showEmptyState: Boolean = false
)

data class LineChartData(
    val date: Long,
    val score: Float,
    val hasData: Boolean,
    val mood: String
)