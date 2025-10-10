package com.example.mindcare.ui.dashboard.diary

import android.app.DatePickerDialog
import android.content.Context
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindcare.ui.theme.MindCareBlue
import java.util.*

@Composable
fun DiaryScreen(
    navController: NavController,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Handling error messages
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            // Display Snackbar
        }
    }

    // Processing success message
    LaunchedEffect(uiState.showSuccessMessage) {
        if (uiState.showSuccessMessage) {
            // Successful prompt
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp).padding(top = 48.dp)
    ) {
        // Title
        Text(
            text = "Diary Form",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Date and Mood Selection Section
        DateAndMoodSelectionSection(
            formattedDate = uiState.formattedDate,
            onDateClick = { showDatePicker(context, viewModel) },
            selectedMood = uiState.selectedMood,
            moodOptions = viewModel.moodOptions,
            onMoodSelected = { mood -> viewModel.updateSelectedMood(mood) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Note input section
        NoteInputSection(
            note = uiState.note,
            onNoteChange = { note -> viewModel.updateNote(note) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Save button
        SaveButtonSection(
            isEnabled = uiState.isFormValid && !uiState.isLoading,
            isLoading = uiState.isLoading,
            onSave = { viewModel.saveEntry { } }
        )

        // Error message
        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            ErrorMessage(
                message = uiState.errorMessage!!,
                onDismiss = { viewModel.clearError() }
            )
        }

        // Success message
        if (uiState.showSuccessMessage) {
            Spacer(modifier = Modifier.height(16.dp))
            SuccessMessage(
                onDismiss = { viewModel.clearSuccessMessage() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateAndMoodSelectionSection(
    formattedDate: String,
    onDateClick: () -> Unit,
    selectedMood: String,
    moodOptions: List<MoodOption>,
    onMoodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var moodExpanded by remember { mutableStateOf(false) }
    val selectedOption = moodOptions.find { it.name == selectedMood }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Card Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Date and Mood",
                    tint = MindCareBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Date & Mood Selection",
                    style = MaterialTheme.typography.titleMedium,
                    color = MindCareBlue,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Selection
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select Date",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedButton(
                    onClick = onDateClick,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MindCareBlue)
                ) {
                    Text(
                        text = if (formattedDate.isNotEmpty()) formattedDate else "Tap to select date",
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Mood Selection
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "How are you feeling?",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = moodExpanded,
                    onExpandedChange = { moodExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedOption?.let { "${it.emoji} ${it.name}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(30.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Mood",
                                tint = Color(0xFFCCCCCC)
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = moodExpanded)
                        },
                        placeholder = { Text("Select your mood") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 18.sp,
                            color = MindCareBlue
                        )
                    )

                    // Drop-down menu
                    ExposedDropdownMenu(
                        expanded = moodExpanded,
                        onDismissRequest = { moodExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        moodOptions.forEach { mood ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = mood.emoji,
                                            fontSize = 20.sp,
                                            modifier = Modifier.width(40.dp)
                                        )
                                        Text(text = mood.name)
                                    }
                                },
                                onClick = {
                                    onMoodSelected(mood.name)
                                    moodExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteInputSection(
    note: String,
    onNoteChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Add Note",
                    tint = MindCareBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Additional Notes (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MindCareBlue,
                    fontWeight = MaterialTheme.typography.titleMedium.fontWeight
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text(
                        text = "Write your thoughts",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 5
            )
        }
    }
}

@Composable
private fun SaveButtonSection(
    isEnabled: Boolean,
    isLoading: Boolean,
    onSave: () -> Unit
) {
    Button(
        onClick = onSave,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = isEnabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MindCareBlue,
            disabledContainerColor = MindCareBlue.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Saving...")
        } else {
            Text("Save Entry", fontSize = 18.sp,)
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun SuccessMessage(
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Entry saved successfully!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    }
}

// Display date selector
private fun showDatePicker(context: Context, viewModel: DiaryViewModel) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
            viewModel.updateSelectedDate(selectedCalendar.timeInMillis)
        },
        year,
        month,
        day
    )

    // Set the optional maximum date to today
    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
    datePickerDialog.show()
}