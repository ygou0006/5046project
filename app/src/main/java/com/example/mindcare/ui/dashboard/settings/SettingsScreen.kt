package com.example.mindcare.ui.dashboard.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindcare.data.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navControllerMain: NavController,
    navController: NavController,
    innerPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    val uiState by viewModel.uiState
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF6DD5FA), Color(0xFF2980B9)),
                    startY = 0f,
                    endY = 1000f
                )
            )
    ) {
        when (uiState) {
            is ProfileUiState.Loading -> LoadingScreen()
            is ProfileUiState.Success -> ProfileContent(
                navControllerMain = navControllerMain,
                navController = navController,
                viewModel = viewModel,
                coroutineScope = coroutineScope,
                user = (uiState as ProfileUiState.Success).user,
            )
            is ProfileUiState.Error -> ErrorScreen(
                message = (uiState as ProfileUiState.Error).message,
                onRetry = { viewModel.loadUserProfile() }
            )
            else -> {}
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Oops! Something went wrong",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = message,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
        )

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF2980B9)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .padding(top = 16.dp)
                .width(150.dp)
        ) {
            Text("Try Again")
        }
    }
}

@Composable
private fun ProfileContent(
    navControllerMain: NavController,
    navController: NavController,
    viewModel: SettingsViewModel,
    coroutineScope: CoroutineScope,
    user: User,
) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Profile header
        ProfileHeader(user)

        // User information card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                // Section title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACCOUNT INFORMATION",
                        color = Color(0xFF2980B9),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    EditProfile(navController, viewModel, coroutineScope)
                }

                // User details
                ProfileItem(icon = Icons.Default.Face, title = "Full Name", value = user.name)
                ProfileItem(icon = Icons.Default.Email, title = "Email", value = user.email)
            }
        }

        // Settings card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                // Section title
                Text(
                    text = "ACCOUNT SETTINGS",
                    color = Color(0xFF2980B9),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                SettingItemWithSwitch(
                    title = "Enable Notifications",
                    icon = Icons.Default.Notifications,
                    isChecked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.toggleNotifications(enabled)
                    }
                )

                // Settings items
                SettingItem(
                    title = "Logout",
                    icon = Icons.Default.ExitToApp,
                    onClick = { logout(navControllerMain, viewModel, coroutineScope) }
                )
            }
        }

        // App info
        Text(
            text = "MindCare App v1.0",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 24.dp)
        )
    }
}

@Composable
fun ProfileHeader(user: User) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.3f))
                .border(2.dp, Color.White, CircleShape)
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Profile",
                tint = Color.White,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // User name
        Text(
            text = user.name,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProfileItem(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = Color(0xFF2980B9),
            modifier = Modifier.size(24.dp))

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                color = Color.Gray,
                fontSize = 14.sp
            )
            Text(
                text = value,
                color = Color(0xFF2980B9),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // Divider
    Divider(
        color = Color(0x552980B9),
        thickness = 0.8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    )
}

@Composable
fun SettingItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = Color(0xFF2980B9),
            modifier = Modifier.size(24.dp))

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            color = Color(0xFF2980B9),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF2980B9),
            modifier = Modifier.size(24.dp))
    }
}

@Composable
fun Divider(
    color: Color,
    thickness: androidx.compose.ui.unit.Dp,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .height(thickness)
            .background(color)
    )
}

fun logout(
    navController: NavController,
    viewModel: SettingsViewModel,
    coroutineScope: CoroutineScope
) {
    coroutineScope.launch {
        viewModel.loginOut().collect { result ->
            when (result) {
                is ProfileResult.Success -> {
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
                is ProfileResult.Error -> {
                    // Handle error if needed
                }
                else -> {}
            }
        }
    }
}

@Composable
fun SettingItemWithSwitch(
    title: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = Color(0xFF2980B9),
            modifier = Modifier.size(24.dp))

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            color = Color(0xFF2980B9),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2980B9),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
            )
        )
    }

    Divider(
        color = Color(0x552980B9),
        thickness = 0.8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    )
}