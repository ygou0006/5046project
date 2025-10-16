package com.example.mindcare.ui.dashboard.diary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindcare.data.models.DiaryEntry
import com.example.mindcare.data.repository.DiaryRepository
import com.example.mindcare.utils.SessionManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.InputStream
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

    // Text recognizer instance
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Track model download state
    private var isModelDownloaded = false

    // Mood options list
    val moodOptions = listOf(
        MoodOption("Happy", "😊"),
        MoodOption("Sad", "😢"),
        MoodOption("Anxious", "😰"),
        MoodOption("Calm", "😌")
    )

    init {
        loadCurrentUser()
        checkModelAvailability()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = sessionManager.getUser()
            currentUserId = user?.id
        }
    }

    /**
     * Check if text recognition model is available
     */
    private fun checkModelAvailability() {
        viewModelScope.launch {
            try {
                // Try to create a test image to check model availability
                val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                val testImage = InputImage.fromBitmap(testBitmap, 0)

                // This will trigger model download if not available
                textRecognizer.process(testImage)
                    .addOnSuccessListener {
                        // Model is available
                        isModelDownloaded = true
                        _uiState.value = _uiState.value.copy(isModelDownloaded = true)
                    }
                    .addOnFailureListener { exception ->
                        // Model needs download
                        isModelDownloaded = false
                        _uiState.value = _uiState.value.copy(
                            isModelDownloaded = false,
                            showDownloadDialog = true
                        )
                    }
            } catch (e: Exception) {
                isModelDownloaded = false
                _uiState.value = _uiState.value.copy(
                    isModelDownloaded = false,
                    showDownloadDialog = true
                )
            }
        }
    }

    /**
     * Manually trigger model download
     */
    fun downloadTextRecognitionModel() {
        _uiState.value = _uiState.value.copy(
            isDownloadingModel = true,
            downloadProgress = 0,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                // Simulate download progress
                for (progress in 0..100 step 10) {
                    _uiState.value = _uiState.value.copy(downloadProgress = progress)
                    kotlinx.coroutines.delay(200)
                }

                // Create a test image to trigger model download
                val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                val testImage = InputImage.fromBitmap(testBitmap, 0)

                textRecognizer.process(testImage)
                    .addOnSuccessListener {
                        // Model downloaded successfully
                        isModelDownloaded = true
                        _uiState.value = _uiState.value.copy(
                            isDownloadingModel = false,
                            isModelDownloaded = true,
                            downloadProgress = 100,
                            showDownloadDialog = false,
                            errorMessage = null
                        )
                    }
                    .addOnFailureListener { exception ->
                        // Download failed
                        _uiState.value = _uiState.value.copy(
                            isDownloadingModel = false,
                            errorMessage = "Download failed: ${exception.message}",
                            showDownloadDialog = true
                        )
                    }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloadingModel = false,
                    errorMessage = "Download initialization failed: ${e.message}",
                    showDownloadDialog = true
                )
            }
        }
    }

    /**
     * Process image from URI and extract text using ML Kit
     */
    fun processImageFromUri(context: Context, uri: Uri) {
        // Check if model is downloaded
        if (!isModelDownloaded) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Text recognition model not available. Please download it first.",
                showDownloadDialog = true
            )
            return
        }

        _uiState.value = _uiState.value.copy(isProcessingImage = true)

        viewModelScope.launch {
            try {
                // Convert URI to bitmap
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    // Create InputImage from bitmap
                    val image = InputImage.fromBitmap(bitmap, 0)

                    // Process image with ML Kit
                    val result = textRecognizer.process(image).await()

                    // Extract recognized text
                    val recognizedText = result.text

                    if (recognizedText.isNotEmpty()) {
                        // Update note with recognized text
                        _uiState.value = _uiState.value.copy(
                            note = if (uiState.value.note.isNotEmpty()) {
                                "${uiState.value.note}\n\n--- Extracted Text ---\n$recognizedText"
                            } else {
                                recognizedText
                            },
                            isProcessingImage = false,
                            showSuccessMessage = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "No text found in the image",
                            isProcessingImage = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to load image",
                        isProcessingImage = false
                    )
                }
            } catch (e: Exception) {
                if (e.message?.contains("downloaded") == true ||
                    e.message?.contains("model") == true ||
                    e.message?.contains("unavailable") == true) {
                    // Model not available, prompt for download
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Text recognition model needs to be downloaded",
                        showDownloadDialog = true,
                        isProcessingImage = false
                    )
                    isModelDownloaded = false
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Text recognition failed: ${e.message}",
                        isProcessingImage = false
                    )
                }
            }
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

    fun hideDownloadDialog() {
        _uiState.value = _uiState.value.copy(showDownloadDialog = false)
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

    fun clearNote() {
        _uiState.value = _uiState.value.copy(note = "")
    }
}

data class DiaryUiState(
    val selectedDate: Long? = null,
    val formattedDate: String = "",
    val selectedMood: String = "",
    val note: String = "",
    val isLoading: Boolean = false,
    val isProcessingImage: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val isModelDownloaded: Boolean = false,
    val downloadProgress: Int = 0,
    val showDownloadDialog: Boolean = false,
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