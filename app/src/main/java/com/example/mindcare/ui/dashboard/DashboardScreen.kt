package com.example.mindcare.ui.dashboard

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mindcare.ui.dashboard.home.HomeScreen
import com.example.mindcare.ui.dashboard.settings.SettingsScreen

@Composable
fun DashboardScreen(
    navControllerMain: NavController
) {
    val context = LocalContext.current
    BackHandler {
        (context as Activity).finish()
    }

    val navController = rememberNavController()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MyBottomBar(navController)
        }
    ){ innerPadding ->
        Column {
            MyNavHost(navControllerMain, navController, innerPadding)
        }
    }
}

@Composable
fun MyBottomBar(navController: NavHostController){
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(
        "Home",
        "Diary",
        "History",
        "Trends",
        "Settings"
    )

    NavigationBar{
        items.forEachIndexed { index,item ->
            NavigationBarItem(
                icon = {
                    when(item){
                        "Home" -> Icon(Icons.Filled.Home, contentDescription = "Home")
                        "Diary" -> Icon(Icons.Filled.Create, contentDescription = "Diary")
                        "History" -> Icon(Icons.Filled.List, contentDescription = "History")
                        "Trends" -> Icon(Icons.Filled.DateRange, contentDescription = "Trends")
                        "Settings" -> Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                label = {Text(item)},
                selected = selectedItem == index,
                onClick = {
                    selectedItem = index
                    navController.navigate(item)
                }
            )
        }
    }
}

@Composable
fun MyNavHost(navControllerMain: NavController, navController: NavHostController, innerPadding: PaddingValues) {
    NavHost(
        navController = navController,
        startDestination = "Home"
    ) {
        composable("Home") {
            HomeScreen(navController = navController)
        }
        composable("Diary") {
        }
        composable("History") {
        }
        composable("Trends") {
        }
        composable("Settings") {
            SettingsScreen(navControllerMain = navControllerMain, navController = navController, innerPadding = innerPadding)
        }
    }
}