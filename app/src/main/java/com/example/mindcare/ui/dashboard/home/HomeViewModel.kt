package com.example.mindcare.ui.dashboard.home

import android.content.Context
import android.net.Uri
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val sessionManager: SessionManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _diaryEntries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val diaryEntries: StateFlow<List<DiaryEntry>> = _diaryEntries.asStateFlow()

    private var currentUserId: String? = null

    // Heart rate analysis data
    private val _heartRateRecords = MutableStateFlow<List<HeartRateRecord>>(emptyList())
    private val _currentHeartRateIndex = MutableStateFlow(0)

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

    // Heart Rate Analysis Functions
    fun loadHeartRateDataFromFile() {
        _uiState.value = _uiState.value.copy(
            errorMessage = "Please select a CSV file using the file picker"
        )
    }

    /**
     * Load heart rate data from selected file URI
     */
    fun loadHeartRateDataFromUri(uri: Uri) {
        _uiState.value = _uiState.value.copy(isLoadingHeartRateData = true)

        viewModelScope.launch {
            try {
                val result = loadHeartRateDataFromContentUri(uri)
                if (result.isSuccess) {
                    val records = result.getOrThrow()
                    _heartRateRecords.value = records
                    _currentHeartRateIndex.value = 0

                    _uiState.value = _uiState.value.copy(
                        heartRateRecords = records,
                        currentHeartRateIndex = 0,
                        currentHeartRateRecord = if (records.isNotEmpty()) records[0] else null,
                        isLoadingHeartRateData = false,
                        errorMessage = null
                    )
                } else {
                    throw result.exceptionOrNull() ?: Exception("Unknown error occurred")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingHeartRateData = false,
                    errorMessage = "Failed to load file: ${e.message}"
                )
            }
        }
    }

    /**
     * Load heart rate data from content URI with comprehensive error handling
     */
    private fun loadHeartRateDataFromContentUri(uri: Uri): Result<List<HeartRateRecord>> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val lines = reader.readLines()

                if (lines.isEmpty()) {
                    return Result.failure(Exception("File is empty"))
                }

                val result = parseCsvLines(lines)
                if (result.isSuccess) {
                    val records = result.getOrThrow()
                    if (records.isEmpty()) {
                        Result.failure(Exception("No valid records found in the file. Please check the CSV format."))
                    } else {
                        Result.success(records)
                    }
                } else {
                    result
                }
            } ?: Result.failure(Exception("Cannot open file from URI"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse CSV lines into HeartRateRecord objects with flexible column support
     */
    private fun parseCsvLines(lines: List<String>): Result<List<HeartRateRecord>> {
        val records = mutableListOf<HeartRateRecord>()
        var validRecords = 0
        var invalidRecords = 0
        val errorMessages = mutableListOf<String>()

        // Validate file has content
        if (lines.isEmpty()) {
            return Result.failure(Exception("File is empty"))
        }

        // Detect header and determine column structure
        val header = lines[0].trim().lowercase()
        val hasResultColumn = header.contains("result")

        val minRequiredColumns = 8 // Age, Gender, Heart rate, Systolic BP, Diastolic BP, Blood sugar, CK-MB, Troponin
        val expectedColumns = if (hasResultColumn) 9 else 8

        // Check if we have enough columns
        val firstDataLine = if (lines.size > 1) lines[1].split(",") else emptyList()
        if (firstDataLine.size < minRequiredColumns) {
            return Result.failure(Exception("CSV file must have at least $minRequiredColumns columns: Age, Gender, Heart rate, Systolic BP, Diastolic BP, Blood sugar, CK-MB, Troponin"))
        }

        // Process data lines
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isNotEmpty()) {
                try {
                    val values = line.split(",")

                    // Validate minimum number of columns
                    if (values.size < minRequiredColumns) {
                        invalidRecords++
                        errorMessages.add("Line ${i + 1}: Insufficient columns. Expected at least $minRequiredColumns, got ${values.size}")
                        continue
                    }

                    // Parse required fields
                    val age = try { values[0].toInt() } catch (e: NumberFormatException) {
                        throw Exception("Line ${i + 1}: Invalid age format '${values[0]}'")
                    }

                    val gender = try { values[1].toInt() } catch (e: NumberFormatException) {
                        throw Exception("Line ${i + 1}: Invalid gender format '${values[1]}'. Expected 0 or 1")
                    }

                    if (gender != 0 && gender != 1) {
                        throw Exception("Line ${i + 1}: Invalid gender value '$gender'. Expected 0 (Female) or 1 (Male)")
                    }

                    val heartRate = try { values[2].toInt() } catch (e: NumberFormatException) {
                        throw Exception("Line ${i + 1}: Invalid heart rate format '${values[2]}'")
                    }

                    val systolicBP = try { values[3].toInt() } catch (e: NumberFormatException) {
                        throw Exception("Line ${i + 1}: Invalid systolic BP format '${values[3]}'")
                    }

                    val diastolicBP = try { values[4].toInt() } catch (e: NumberFormatException) {
                        throw Exception("Line ${i + 1}: Invalid diastolic BP format '${values[4]}'")
                    }

                    val bloodSugar = try { values[5].toInt() } catch (e: NumberFormatException) {
                        throw Exception("Line ${i + 1}: Invalid blood sugar format '${values[5]}'")
                    }

                    val ckMb = try { values[6].toDouble() } catch (e: NumberFormatException) {
                        throw Exception("Line ${i + 1}: Invalid CK-MB format '${values[6]}'")
                    }

                    val troponin = try { values[7].toDouble() } catch (e: NumberFormatException) {
                        throw Exception("Line ${i + 1}: Invalid troponin format '${values[7]}'")
                    }

                    // Parse optional result field
                    val result = if (hasResultColumn && values.size > 8) {
                        val resultValue = values[8].trim().lowercase()
                        if (resultValue == "positive" || resultValue == "negative") {
                            resultValue
                        } else {
                            null // Invalid result value, treat as not provided
                        }
                    } else {
                        null // Result column not present
                    }

                    // Validate value ranges
                    if (age < 0 || age > 150) {
                        throw Exception("Line ${i + 1}: Age $age is out of reasonable range (0-150)")
                    }

                    if (heartRate < 30 || heartRate > 250) {
                        throw Exception("Line ${i + 1}: Heart rate $heartRate is out of reasonable range (30-250)")
                    }

                    if (systolicBP < 50 || systolicBP > 250) {
                        throw Exception("Line ${i + 1}: Systolic BP $systolicBP is out of reasonable range (50-250)")
                    }

                    if (diastolicBP < 30 || diastolicBP > 150) {
                        throw Exception("Line ${i + 1}: Diastolic BP $diastolicBP is out of reasonable range (30-150)")
                    }

                    if (bloodSugar < 30 || bloodSugar > 600) {
                        throw Exception("Line ${i + 1}: Blood sugar $bloodSugar is out of reasonable range (30-600)")
                    }

                    val (analysisResult, recommendation) = analyzeHeartRateRecord(
                        age, gender, heartRate, systolicBP, diastolicBP,
                        bloodSugar, ckMb, troponin, result
                    )

                    records.add(HeartRateRecord(
                        age = age,
                        gender = gender,
                        heartRate = heartRate,
                        systolicBP = systolicBP,
                        diastolicBP = diastolicBP,
                        bloodSugar = bloodSugar,
                        ckMb = ckMb,
                        troponin = troponin,
                        result = result,
                        analysisResult = analysisResult,
                        recommendation = recommendation
                    ))
                    validRecords++

                } catch (e: Exception) {
                    invalidRecords++
                    errorMessages.add(e.message ?: "Line ${i + 1}: Unknown parsing error")
                    continue
                }
            }
        }

        // Create summary message
        val summaryMessage = buildString {
            append("Loaded $validRecords valid records")
            if (invalidRecords > 0) {
                append(", skipped $invalidRecords invalid records")
            }
            if (!hasResultColumn) {
                append(" (Result column not found - using AI analysis)")
            }
            if (errorMessages.isNotEmpty()) {
                append(". Errors: ${errorMessages.take(3).joinToString("; ")}")
                if (errorMessages.size > 3) {
                    append(" and ${errorMessages.size - 3} more errors")
                }
            }
        }

        // If no valid records were found, return failure
        return if (validRecords == 0) {
            Result.failure(Exception("No valid records found. $summaryMessage"))
        } else {
            // Store summary in state for display
            _uiState.value = _uiState.value.copy(
                errorMessage = if (invalidRecords > 0 || !hasResultColumn) summaryMessage else null
            )
            Result.success(records)
        }
    }

    /**
     * Analyze heart rate record and provide recommendations in English
     * Now supports records without result column
     */
    private fun analyzeHeartRateRecord(
        age: Int, gender: Int, heartRate: Int, systolicBP: Int, diastolicBP: Int,
        bloodSugar: Int, ckMb: Double, troponin: Double, result: String?
    ): Pair<String, String> {
        val analysis = StringBuilder()
        val recommendation = StringBuilder()

        var riskScore = 0
        var abnormalFindings = 0

        // Analyze heart rate
        when {
            heartRate < 60 -> {
                analysis.append("Low heart rate (bradycardia). ")
                recommendation.append("Consider consulting a doctor for low heart rate. Try light exercises to improve circulation. ")
                riskScore++
                abnormalFindings++
            }
            heartRate > 100 -> {
                analysis.append("High heart rate (tachycardia). ")
                recommendation.append("Practice deep breathing exercises. Listen to calming music to relax. Avoid caffeine. ")
                riskScore++
                abnormalFindings++
            }
            else -> {
                analysis.append("Normal heart rate. ")
            }
        }

        // Analyze blood pressure
        when {
            systolicBP < 90 || diastolicBP < 60 -> {
                analysis.append("Low blood pressure. ")
                recommendation.append("Stay hydrated and increase salt intake slightly. Stand up slowly to avoid dizziness. ")
                riskScore++
                abnormalFindings++
            }
            systolicBP > 140 || diastolicBP > 90 -> {
                analysis.append("High blood pressure. ")
                recommendation.append("Reduce sodium intake. Practice stress management techniques. Monitor regularly. ")
                riskScore += 2
                abnormalFindings++
            }
            else -> {
                analysis.append("Normal blood pressure. ")
            }
        }

        // Analyze blood sugar
        when {
            bloodSugar < 70 -> {
                analysis.append("Low blood sugar. ")
                recommendation.append("Have a small snack with carbohydrates. Drink fruit juice. Rest and recheck levels. ")
                riskScore++
                abnormalFindings++
            }
            bloodSugar > 140 -> {
                analysis.append("High blood sugar. ")
                recommendation.append("Monitor carbohydrate intake. Stay physically active. Drink plenty of water. ")
                riskScore += 2
                abnormalFindings++
            }
            else -> {
                analysis.append("Normal blood sugar. ")
            }
        }

        // Analyze cardiac markers
        if (ckMb > 5.0) {
            analysis.append("Elevated CK-MB levels detected. ")
            recommendation.append("Consult a healthcare professional for elevated cardiac markers. Avoid strenuous exercise. ")
            riskScore += 3
            abnormalFindings++
        }

        if (troponin > 0.1) {
            analysis.append("Elevated troponin levels detected. ")
            recommendation.append("Seek immediate medical attention. This may indicate heart muscle damage. ")
            riskScore += 3
            abnormalFindings++
        }

        // Age-specific recommendations
        when {
            age < 30 -> recommendation.append("Maintain regular exercise and healthy diet. ")
            age in 30..50 -> recommendation.append("Monitor your health parameters regularly. ")
            age > 50 -> recommendation.append("Regular health check-ups are recommended. ")
        }

        // Add general wellness advice based on overall assessment
        if (result == "positive" || riskScore >= 3) {
            recommendation.append("Consider meditation or yoga for stress reduction. Get adequate sleep. ")
        } else if (result == "negative" || riskScore == 0) {
            recommendation.append("Continue with your healthy habits. Regular monitoring is beneficial. ")
        } else if (abnormalFindings > 0) {
            recommendation.append("Monitor your health closely and consider consulting a healthcare provider. ")
        }

        // Final analysis conclusion
        val finalResult = result ?: when {
            riskScore >= 3 -> "High risk - Abnormal findings detected"
            riskScore >= 1 -> "Moderate risk - Some abnormal findings"
            else -> "Low risk - Normal findings"
        }

        analysis.append("Overall assessment: $finalResult")

        return Pair(analysis.toString(), recommendation.toString())
    }

    fun nextHeartRateRecord() {
        val currentIndex = _currentHeartRateIndex.value
        val records = _heartRateRecords.value

        if (currentIndex < records.size - 1) {
            val newIndex = currentIndex + 1
            _currentHeartRateIndex.value = newIndex
            _uiState.value = _uiState.value.copy(
                currentHeartRateIndex = newIndex,
                currentHeartRateRecord = records[newIndex]
            )
        }
    }

    fun previousHeartRateRecord() {
        val currentIndex = _currentHeartRateIndex.value
        val records = _heartRateRecords.value

        if (currentIndex > 0) {
            val newIndex = currentIndex - 1
            _currentHeartRateIndex.value = newIndex
            _uiState.value = _uiState.value.copy(
                currentHeartRateIndex = newIndex,
                currentHeartRateRecord = records[newIndex]
            )
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

// Data classes remain the same
data class HomeUiState(
    val userName: String = "",
    val userEmail: String = "",
    val todayMood: String? = null,
    val hasTodayEntry: Boolean = false,
    val aiInsights: String = "Loading your insights...",
    val recommendations: List<RecommendationActivity> = emptyList(),
    val isLoading: Boolean = false,
    val showSuccessMessage: Boolean = false,
    val errorMessage: String? = null,
    // Heart rate analysis state
    val heartRateRecords: List<HeartRateRecord> = emptyList(),
    val currentHeartRateIndex: Int = 0,
    val currentHeartRateRecord: HeartRateRecord? = null,
    val isLoadingHeartRateData: Boolean = false
)

data class HeartRateRecord(
    val age: Int,
    val gender: Int,
    val heartRate: Int,
    val systolicBP: Int,
    val diastolicBP: Int,
    val bloodSugar: Int,
    val ckMb: Double,
    val troponin: Double,
    val result: String?, // Make result nullable
    val analysisResult: String,
    val recommendation: String
)

// Data class, used to represent recommendation activities
data class RecommendationActivity(
    val title: String,
    val emoji: String
)