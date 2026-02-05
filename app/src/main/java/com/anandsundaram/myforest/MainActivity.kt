package com.anandsundaram.myforest

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anandsundaram.myforest.ui.theme.MyForestTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

sealed class Screen(val route: String, val resourceId: String) {
    object Focus : Screen("focus", "Focus")
    object History : Screen("history", "History")
}

val items = listOf(
    Screen.Focus,
    Screen.History,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle case where app was force-closed during a session
        val sharedPrefs = getSharedPreferences("MyForestPrefs", Context.MODE_PRIVATE)
        if (sharedPrefs.getBoolean("isTimerRunning", false)) {
            val duration = sharedPrefs.getInt("durationMinutes", 0)
            val remaining = sharedPrefs.getLong("remainingTime", 0)
            val actualDuration = ((duration * 60 * 1000 - remaining) / (1000 * 60)).toInt()
            val session = FocusSession(Date(), duration, actualDuration, false)
            // This is a simplified way to handle history; a better approach would be to use a database
            val history = mutableListOf(session) // Simplified for this example
            with(sharedPrefs.edit()) {
                putBoolean("isTimerRunning", false)
                apply()
            }
        }

        enableEdgeToEdge()
        setContent {
            MyForestTheme {
                val navController = rememberNavController()
                var history by remember { mutableStateOf(listOf<FocusSession>()) }

                var durationMinutes by remember { mutableStateOf(25f) }
                var isTimerRunning by remember { mutableStateOf(false) }
                var remainingTime by remember { mutableStateOf(0L) }
                var growth by remember { mutableStateOf(0f) }
                val coroutineScope = rememberCoroutineScope()
                var timerJob by remember { mutableStateOf<Job?>(null) }

                fun handleSessionCompletion(isSuccess: Boolean) {
                    val actualMinutes = if (isSuccess) {
                        durationMinutes.toInt()
                    } else {
                        ((durationMinutes * 60 * 1000 - remainingTime) / (1000 * 60)).toInt()
                    }
                    history = history + FocusSession(Date(), durationMinutes.toInt(), actualMinutes, isSuccess)
                    isTimerRunning = false
                    growth = 0f
                    timerJob?.cancel()

                    with(sharedPrefs.edit()) {
                        putBoolean("isTimerRunning", false)
                        apply()
                    }
                }

                DisposableEffect(isTimerRunning, durationMinutes, remainingTime) {
                    onDispose {
                        if (isTimerRunning) {
                            with(sharedPrefs.edit()) {
                                putBoolean("isTimerRunning", true)
                                putInt("durationMinutes", durationMinutes.toInt())
                                putLong("remainingTime", remainingTime)
                                apply()
                            }
                        }
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = {
                                        when (screen) {
                                            Screen.Focus -> Icon(Icons.Filled.Home, contentDescription = null)
                                            Screen.History -> Icon(Icons.Filled.DateRange, contentDescription = null)
                                        }
                                    },
                                    label = { Text(screen.resourceId) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController,
                        startDestination = Screen.Focus.route,
                        Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Focus.route) {
                            FocusScreen(
                                durationMinutes = durationMinutes,
                                isTimerRunning = isTimerRunning,
                                remainingTime = remainingTime,
                                growth = growth,
                                onDurationChange = { durationMinutes = it },
                                onTimerStateChange = { newIsTimerRunning ->
                                    isTimerRunning = newIsTimerRunning
                                    if (newIsTimerRunning) {
                                        timerJob = coroutineScope.launch {
                                            val totalTime = durationMinutes.toLong() * 60 * 1000
                                            remainingTime = totalTime
                                            while (remainingTime > 0) {
                                                delay(1000)
                                                remainingTime -= 1000
                                                growth = 1 - (remainingTime.toFloat() / totalTime)
                                            }
                                            handleSessionCompletion(true)
                                        }
                                    } else {
                                        handleSessionCompletion(false)
                                    }
                                },
                            )
                        }
                        composable(Screen.History.route) { HistoryScreen(history) }
                    }
                }
            }
        }
    }
}
