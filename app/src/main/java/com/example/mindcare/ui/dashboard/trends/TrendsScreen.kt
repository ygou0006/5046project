package com.example.mindcare.ui.dashboard.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindcare.ui.theme.MindCareBlue
import com.example.mindcare.ui.theme.MindCarePurple

@Composable
fun TrendsScreen(
    navController: NavController,
    viewModel: TrendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showEmptyState) {
        EmptyTrendsState()
    } else {
        TrendsContent(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
private fun EmptyTrendsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.List,
            contentDescription = "No data",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Data Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start recording your moods to see trends and insights",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TrendsContent(
    uiState: TrendsUiState,
    viewModel: TrendsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(top = 48.dp, bottom = 100.dp)
    ) {
        // Title
        Text(
            text = "Your Mood Trends",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Overview stats card
        OverviewStatsCard(uiState = uiState, viewModel = viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // Mood trend line chart
        MoodTrendLineChartCard(uiState = uiState, viewModel = viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // Mood distribution chart
        MoodDistributionCard(uiState = uiState, viewModel = viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // Mood stability card
        MoodStabilityCard(uiState = uiState, viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // Time of day analysis card
        TimeOfDayCard(uiState = uiState)

        Spacer(modifier = Modifier.height(16.dp))

        // Streak card
        StreakCard(uiState = uiState)
    }
}

@Composable
private fun OverviewStatsCard(
    uiState: TrendsUiState,
    viewModel: TrendsViewModel
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
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Overview",
                    tint = Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    title = "Total Entries",
                    value = uiState.totalEntries.toString(),
                    icon = Icons.Default.AddCircle
                )
                StatItem(
                    title = "Avg Mood Score",
                    value = "%.1f".format(uiState.averageMoodScore),
                    icon = Icons.Default.Menu
                )
                StatItem(
                    title = "Most Frequent",
                    value = viewModel.getMoodEmoji(uiState.mostFrequentMood),
                    icon = Icons.Default.Face
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MindCareBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun MoodTrendLineChartCard(
    uiState: TrendsUiState,
    viewModel: TrendsViewModel
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
                    imageVector = Icons.Default.Share,
                    contentDescription = "Mood Trend",
                    tint = Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "14-Day Mood Trend",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.lineChartData.isNotEmpty()) {
                MoodTrendLineChart(
                    lineChartData = uiState.lineChartData,
                    viewModel = viewModel
                )
            } else {
                Text(
                    text = "No data available for trend analysis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun MoodTrendLineChart(
    lineChartData: List<LineChartData>,
    viewModel: TrendsViewModel
) {
    val chartHeight = 200.dp
    val horizontalPadding = 32.dp
    val verticalPadding = 24.dp
    val legendHeight = 40.dp

    // Fixed score range 1.0-4.0
    val minScore = 1.0f
    val maxScore = 4.0f
    val scoreRange = maxScore - minScore

    // Define colors for mood scores
    val getScoreColor = { score: Float ->
        when {
            score >= 3.5f -> Color(0xFF4CAF50)
            score >= 2.5f -> Color(0xFF8BC34A)
            score >= 1.5f -> Color(0xFFFF9800)
            else -> Color(0xFFF44336)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight + verticalPadding * 2 + legendHeight)
    ) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(legendHeight)
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Mood Score:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Row {
                    LegendItem(color = Color(0xFF4CAF50), label = "4.0")
                    LegendItem(color = Color(0xFF8BC34A), label = "3.0")
                    LegendItem(color = Color(0xFFFF9800), label = "2.0")
                    LegendItem(color = Color(0xFFF44336), label = "1.0")
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight + verticalPadding * 2)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw fixed value grid lines (1.0, 2.0, 3.0, 4.0)
                val fixedScores = listOf(1.0f, 2.0f, 3.0f, 4.0f)

                fixedScores.forEach { score ->
                    // Calculate y coordinate position
                    val y = canvasHeight * (1 - (score - minScore) / scoreRange)

                    // Horizontal grid lines
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 3f
                    )

                    // Y-axis labels
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            "%.1f".format(score),
                            -50f, // Adjust label position
                            y + 5f, // Fine-tune vertical position
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#666666")
                                textSize = 28f
                                textAlign = android.graphics.Paint.Align.LEFT
                            }
                        )
                    }
                }

                // Draw line chart
                if (lineChartData.size > 1) {
                    val pointSpacing = canvasWidth / (lineChartData.size - 1)

                    // Draw connecting lines
                    var i1 = 0
                    while (i1 < lineChartData.size) {
                        var currentPoint = lineChartData[i1++]
                        while (!currentPoint.hasData && i1 < lineChartData.size) {
                            currentPoint = lineChartData[i1++]
                        }
                        var i2 = i1
                        if (i2 >= lineChartData.size) break
                        var nextPoint = lineChartData[i2++]
                        while (!nextPoint.hasData && i2 < lineChartData.size) {
                            nextPoint = lineChartData[i2++]
                        }

                        if (nextPoint.hasData) {
                            val x1 = (i1 - 1) * pointSpacing
                            val y1 = canvasHeight * (1 - (currentPoint.score - minScore) / scoreRange)
                            val x2 = (i2 - 1) * pointSpacing
                            val y2 = canvasHeight * (1 - (nextPoint.score - minScore) / scoreRange)

                            drawLine(
                                color = MindCarePurple.copy(alpha = 0.7f),
                                start = Offset(x1, y1),
                                end = Offset(x2, y2),
                                strokeWidth = 6f
                            )
                        }
                        i1 = i2 - 1
                    }

                    // Draw data points
                    for (i in lineChartData.indices) {
                        val point = lineChartData[i]
                        if (point.hasData) {
                            val x = i * pointSpacing
                            val y = canvasHeight * (1 - (point.score - minScore) / scoreRange)

                            // Draw data point
                            drawCircle(
                                color = getScoreColor(point.score),
                                radius = 10f,
                                center = Offset(x, y)
                            )
                        }
                    }

                    // Draw X-axis labels in Canvas
                    lineChartData.forEachIndexed { index, data ->
                        if (index % 2 == 0) {
                            val x = index * pointSpacing
                            val y = canvasHeight + 50f // X-axis label position (below chart)

                            drawContext.canvas.nativeCanvas.apply {
                                drawText(
                                    viewModel.formatShortDate(data.date),
                                    x - 40f, // Adjust position to center
                                    y,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.parseColor("#666666")
                                        textSize = 28f
                                        textAlign = android.graphics.Paint.Align.LEFT
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun MoodDistributionCard(
    uiState: TrendsUiState,
    viewModel: TrendsViewModel
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
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Mood Distribution",
                    tint = Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Mood Distribution",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.moodDistribution.isNotEmpty()) {
                MoodDistributionBarChart(
                    moodDistribution = uiState.moodDistribution,
                    viewModel = viewModel
                )
            } else {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun MoodDistributionBarChart(
    moodDistribution: Map<String, Int>,
    viewModel: TrendsViewModel
) {
    val totalEntries = moodDistribution.values.sum()

    Column {
        moodDistribution.entries.sortedByDescending { it.value }.forEach { (mood, count) ->
            val percentage = if (totalEntries > 0) count.toFloat() / totalEntries else 0f

            MoodDistributionItem(
                mood = mood,
                emoji = viewModel.getMoodEmoji(mood),
                count = count,
                percentage = percentage,
                color = Color(android.graphics.Color.parseColor(viewModel.getMoodColor(mood))),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun MoodDistributionItem(
    mood: String,
    emoji: String,
    count: Int,
    percentage: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mood emoji and name
        Row(
            modifier = Modifier.width(100.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 20.sp,
                modifier = Modifier.width(32.dp)
            )
            Text(
                text = mood,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .background(
                    color = color.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .background(
                        color = color,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Percentage and count
        Text(
            text = "${(percentage * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(40.dp)
        )

        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.width(40.dp)
        )
    }
}

@Composable
private fun MoodStabilityCard(uiState: TrendsUiState, viewModel: TrendsViewModel) {
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
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Mood Stability",
                    tint = Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Emotional Well-being",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StabilityMetric(
                    title = "Mood Stability",
                    value = viewModel.formatPercentage(uiState.moodStability),
                    description = "How consistent your moods are"
                )
                StabilityMetric(
                    title = "Positive Ratio",
                    value = viewModel.formatPercentage(uiState.positiveRatio),
                    description = "Percentage of positive moods"
                )
            }
        }
    }
}

@Composable
private fun StabilityMetric(
    title: String,
    value: String,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MindCareBlue,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimeOfDayCard(uiState: TrendsUiState) {
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
                    imageVector = Icons.Default.Info,
                    contentDescription = "Time of Day",
                    tint = Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Time of Day Analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.timeOfDayData.isNotEmpty()) {
                TimeOfDayChart(timeOfDayData = uiState.timeOfDayData)
            } else {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun TimeOfDayChart(timeOfDayData: Map<String, Int>) {
    val totalEntries = timeOfDayData.values.sum()

    Column {
        timeOfDayData.entries.forEach { (time, count) ->
            val percentage = if (totalEntries > 0) count.toFloat() / totalEntries else 0f

            TimeOfDayItem(
                time = time,
                count = count,
                percentage = percentage,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TimeOfDayItem(
    time: String,
    count: Int,
    percentage: Float,
    modifier: Modifier = Modifier
) {
    val color = when (time) {
        "Morning" -> Color(0xFFFFB74D)
        "Afternoon" -> Color(0xFF4FC3F7)
        "Evening" -> Color(0xFF9575CD)
        "Night" -> Color(0xFF4A6572)
        else -> Color.Gray
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(100.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .background(
                    color = color.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .background(
                        color = color,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${(percentage * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(40.dp)
        )

        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.width(40.dp)
        )
    }
}

@Composable
private fun StreakCard(uiState: TrendsUiState) {
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
                    imageVector = Icons.Default.Star,
                    contentDescription = "Streaks",
                    tint = Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Positive Streaks",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StreakItem(
                    title = "Current Streak",
                    value = uiState.currentStreak.toString(),
                    subtitle = "days"
                )
                StreakItem(
                    title = "Longest Streak",
                    value = uiState.longestStreak.toString(),
                    subtitle = "days"
                )
            }
        }
    }
}

@Composable
private fun StreakItem(
    title: String,
    value: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            color = MindCareBlue,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}