package com.example.mindcare.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindcare.ui.theme.MindCareBlue
import com.example.mindcare.ui.theme.MindCarePurple

@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()

    // Check if it scrolls to the bottom
    LaunchedEffect(lazyListState.layoutInfo.visibleItemsInfo) {
        if (lazyListState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
            val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.last()
            if (lastVisibleItem.index >= lazyListState.layoutInfo.totalItemsCount - 3) {
                viewModel.loadMoreEntries()
            }
        }
    }

    // Handling error and success messages
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            // Display Snackbar
        }
    }

    LaunchedEffect(uiState.showSuccessMessage) {
        if (uiState.showSuccessMessage) {
            // Display success prompt
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(top = 48.dp, bottom = 100.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Combined Stats and Search Section
            StatsAndSearchSection(
                totalEntries = uiState.totalEntries,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { query -> viewModel.updateSearchQuery(query) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // List Content Area
            when {
                uiState.showEmptyState -> EmptyStateSection()
                uiState.showNoResults -> NoResultsSection(searchQuery = uiState.searchQuery)
                else -> EntriesListSection(
                    entries = uiState.displayedEntries,
                    lazyListState = lazyListState,
                    onEntryClick = { entry -> viewModel.selectEntryForEditing(entry) },
                    isLoadingMore = uiState.isLoadingMore,
                    viewModel = viewModel
                )
            }
        }

        // Success message
        if (uiState.showSuccessMessage) {
            SuccessMessage(
                message = "Entry updated successfully!",
                onDismiss = { viewModel.clearSuccessMessages() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (uiState.showDeleteSuccess) {
            SuccessMessage(
                message = "Entry deleted successfully!",
                onDismiss = { viewModel.clearSuccessMessages() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Edit dialog
    if (uiState.showEditDialog) {
        EditEntryDialog(
            selectedEntry = uiState.selectedEntry,
            editMood = uiState.editMood,
            editNote = uiState.editNote,
            onMoodChange = { mood -> viewModel.updateEditMood(mood) },
            onNoteChange = { note -> viewModel.updateEditNote(note) },
            onSave = { viewModel.saveEditedEntry() },
            onDelete = {
                uiState.selectedEntry?.let { viewModel.deleteEntry(it) }
            },
            onDismiss = { viewModel.dismissEditDialog() },
            viewModel = viewModel
        )
    }
}

@Composable
private fun StatsAndSearchSection(
    totalEntries: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Stats Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Journey",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalEntries entries",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MindCareBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Stats Icon with background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MindCareBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Statistics",
                        tint = MindCareBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider
            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Icon with background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MindCarePurple.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MindCarePurple,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Search Field
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Search by mood or notes...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = MindCarePurple
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateSection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MindCareBlue.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "No entries",
                tint = MindCareBlue,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Entries Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start recording your moods to see your history here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NoResultsSection(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MindCarePurple.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "No results",
                tint = MindCarePurple,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Results Found",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No entries match \"$searchQuery\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EntriesListSection(
    entries: List<com.example.mindcare.data.models.DiaryEntry>,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    onEntryClick: (com.example.mindcare.data.models.DiaryEntry) -> Unit,
    isLoadingMore: Boolean,
    viewModel: HistoryViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(entries) { entry ->
            HistoryEntryItem(
                entry = entry,
                onClick = { onEntryClick(entry) },
                viewModel = viewModel
            )
        }

        // Loading more indicator
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MindCareBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryItem(
    entry: com.example.mindcare.data.models.DiaryEntry,
    onClick: () -> Unit,
    viewModel: HistoryViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when (entry.mood) {
                "Happy" -> Color(0xFFE2FFE2)
                "Sad" -> Color(0xFFE6E2FF)
                "Anxious" -> Color(0xFFFFE2E2)
                "Calm" -> Color(0xFFF1FFF1)
                else -> Color(0xFFF5F5F5)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mood emoji with background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFFEFEFE), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.getMoodEmoji(entry.mood),
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Main information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.mood,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (entry.note.isNotBlank()) entry.note else " ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${viewModel.formatDate(entry.date)} • ${viewModel.formatTime(entry.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Edit icon with background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.Transparent, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EditEntryDialog(
    selectedEntry: com.example.mindcare.data.models.DiaryEntry?,
    editMood: String,
    editNote: String,
    onMoodChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: HistoryViewModel
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Entry",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Column {
                // Date display (non-editable)
                selectedEntry?.let { entry ->
                    Text(
                        text = viewModel.formatDate(entry.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Mood selection
                Text(
                    text = "Mood",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                HorizontalMoodOptions(
                    selectedMood = editMood,
                    onMoodSelected = onMoodChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Notes editing
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = editNote,
                    onValueChange = onNoteChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = {
                        Text("Add your thoughts...")
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Row {
                // Delete button
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }

                Spacer(modifier = Modifier.weight(1f))

                // Cancel button
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                // Save button
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = MindCareBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save")
                }
            }
        }
    )
}

@Composable
private fun HorizontalMoodOptions(
    selectedMood: String,
    onMoodSelected: (String) -> Unit
) {
    val moods = listOf(
        MoodOption("Happy", "😊"),
        MoodOption("Sad", "😢"),
        MoodOption("Anxious", "😰"),
        MoodOption("Calm", "😌")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        moods.forEach { mood ->
            HorizontalMoodOption(
                mood = mood,
                isSelected = selectedMood == mood.name,
                onSelected = { onMoodSelected(mood.name) }
            )
        }
    }
}

@Composable
private fun HorizontalMoodOption(
    mood: MoodOption,
    isSelected: Boolean,
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
            .width(70.dp)
            .clickable(onClick = onSelected)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Text(
                text = mood.emoji,
                fontSize = 28.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = mood.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun SuccessMessage(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White
                )
            }
        }
    }
}

// Data class for representing mood options
data class MoodOption(
    val name: String,
    val emoji: String
)