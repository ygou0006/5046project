package com.example.mindcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mindcare.ui.dashboard.DashboardScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            val isLoggedIn = true //sessionManager.isLoggedIn()
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
        composable("welcome") {/* WelcomeScreen(navController) */}
        composable("login") {/* LoginScreen(navController) */}
        composable("register") {/* RegisterScreen(navController) */}
        composable("dashboard") { DashboardScreen(navController) }
    }
}