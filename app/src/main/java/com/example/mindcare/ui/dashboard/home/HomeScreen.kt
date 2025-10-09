package com.example.mindcare.ui.dashboard.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindcare.ui.theme.MindCareBlue
import com.example.mindcare.ui.theme.MindCarePurple

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val diaryEntries by viewModel.diaryEntries.collectAsState()

    // Handling error messages
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            // Display Snackbar prompt
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(top = 48.dp, bottom = 100.dp)
    ) {
        // Header with greeting
        HeaderSection(
            greeting = viewModel.getGreeting(),
            date = viewModel.getFormattedDate(),
            userName = uiState.userName
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Today's Mood Card with 4 mood options in horizontal layout
        TodayMoodCard(
            todayMood = uiState.todayMood,
            onMoodSelected = { mood -> viewModel.saveMood(mood) },
            isLoading = uiState.isLoading,
            showSuccess = uiState.showSuccessMessage
        )

        Spacer(modifier = Modifier.height(24.dp))

        // AI Insights Card
        AiInsightsCard(insights = uiState.aiInsights, recommendations = uiState.recommendations)

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Entries Summary
        RecentEntriesSummary(entries = diaryEntries.take(3))
    }
}

@Composable
private fun HeaderSection(
    greeting: String,
    date: String,
    userName: String
) {
    Column {
        Text(
            text = "$greeting, $userName!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = date,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TodayMoodCard(
    todayMood: String?,
    onMoodSelected: (String) -> Unit,
    isLoading: Boolean,
    showSuccess: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD0D0EE)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Today's Mood",
                    tint = MindCareBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "How are you feeling today?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MindCareBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display the current selected mood
            if (todayMood != null) {
                Text(
                    text = "You're feeling $todayMood today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // 4 mood options - arranged horizontally
            HorizontalMoodOptions(
                selectedMood = todayMood,
                onMoodSelected = onMoodSelected,
                isLoading = isLoading
            )

            Text(
                text = "Tap to record your mood",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                textAlign = TextAlign.Center
            )

            // Success message
            if (showSuccess) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Mood saved successfully",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF33AA33),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun HorizontalMoodOptions(
    selectedMood: String?,
    onMoodSelected: (String) -> Unit,
    isLoading: Boolean
) {
    val moods = listOf(
        MoodOption("Happy", "😊", "Positive and cheerful"),
        MoodOption("Sad", "😢", "Down or unhappy"),
        MoodOption("Anxious", "😰", "Worried or nervous"),
        MoodOption("Calm", "😌", "Relaxed and peaceful")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        moods.forEach { mood ->
            HorizontalMoodOption(
                mood = mood,
                isSelected = selectedMood == mood.name,
                isLoading = isLoading,
                onSelected = { onMoodSelected(mood.name) }
            )
        }
    }
}

@Composable
private fun HorizontalMoodOption(
    mood: MoodOption,
    isSelected: Boolean,
    isLoading: Boolean,
    onSelected: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MindCareBlue.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }

    val borderColor = if (isSelected) {
        MindCareBlue
    } else {
        Color.Gray.copy(alpha = 0.3f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(
                enabled = !isLoading,
                onClick = onSelected
            )
    ) {
        // Mood expression button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(70.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(backgroundColor)
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            if (isLoading && isSelected) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MindCareBlue
                )
            } else {
                Text(
                    text = mood.emoji,
                    fontSize = 32.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mood Name
        Text(
            text = mood.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
        /*
        Text(
            text = mood.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        */
    }
}

@Composable
private fun AiInsightsCard(
    insights: String,
    recommendations: List<RecommendationActivity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "AI Insights",
                    tint = MindCarePurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AI Insights",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MindCarePurple,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display AI analysis text
            Text(
                text = insights,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Three recommended activities - horizontal arrangement
            RecommendationActivities(recommendations = recommendations)
        }
    }
}

@Composable
private fun RecommendationActivities(recommendations: List<RecommendationActivity>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        recommendations.forEach { activity ->
            RecommendationActivityItem(activity = activity)
        }
    }
}

@Composable
private fun RecommendationActivityItem(activity: RecommendationActivity) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MindCarePurple.copy(alpha = 0.1f))
        ) {
            Text(
                text = activity.emoji,
                fontSize = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = activity.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun RecentEntriesSummary(entries: List<com.example.mindcare.data.models.DiaryEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Recent Moods",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (entries.isEmpty()) {
                Text(
                    text = "No mood entries yet. Select a mood above to get started!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                entries.forEach { entry ->
                    RecentEntryItem(entry = entry)
                    if (entries.indexOf(entry) < entries.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentEntryItem(entry: com.example.mindcare.data.models.DiaryEntry) {
    val dateFormat = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
    val moodEmoji = when (entry.mood.toLowerCase()) {
        "happy" -> "😊"
        "calm" -> "😌"
        "sad" -> "😢"
        "anxious" -> "😰"
        else -> "😐"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = moodEmoji,
                fontSize = 24.sp,
                modifier = Modifier.width(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = entry.mood,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dateFormat.format(java.util.Date(entry.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// Data class, used to represent mood options
data class MoodOption(
    val name: String,
    val emoji: String,
    val description: String
)