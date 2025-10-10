package com.example.mindcare

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mindcare.service.NotificationPermissionHelper
import com.example.mindcare.ui.auth.login.LoginScreen
import com.example.mindcare.ui.auth.signup.SignUpScreen
import com.example.mindcare.ui.dashboard.DashboardScreen
import com.example.mindcare.ui.welcome.WelcomeScreen
import com.example.mindcare.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var sessionManager: SessionManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
        } else {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationPermissionHelper.hasNotificationPermission(this)) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        /*
        setContent {
            MindCareTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        */
        lifecycleScope.launch {
            val isLoggedIn = sessionManager.isLoggedIn()
            var startDestination = "welcome"
            if (isLoggedIn) {
                startDestination = "dashboard"
            }
            setContent {
                val navController = rememberNavController()
                AppNavigation(navController, startDestination)
            }
        }
    }
}

@Composable
private fun AppNavigation(navController: NavHostController, startDestination: String) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("welcome") { WelcomeScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("signup") { SignUpScreen(navController) }
        composable("dashboard") { DashboardScreen(navController) }
    }
}