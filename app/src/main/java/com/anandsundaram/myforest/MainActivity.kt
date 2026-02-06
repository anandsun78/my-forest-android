package com.anandsundaram.myforest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.anandsundaram.myforest.data.AppDatabase
import com.anandsundaram.myforest.data.RoomFocusRepository
import com.anandsundaram.myforest.data.SharedPrefsFocusPreferences
import com.anandsundaram.myforest.ui.FocusViewModel
import com.anandsundaram.myforest.ui.FocusViewModel.FocusEvent
import com.anandsundaram.myforest.ui.theme.MyForestTheme

sealed class Screen(val route: String, val label: String) {
    object Focus : Screen("focus", "Focus")
    object History : Screen("history", "History")
}

private val navigationItems = listOf(
    Screen.Focus,
    Screen.History,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = SharedPrefsFocusPreferences(
            getSharedPreferences("MyForestPrefs", Context.MODE_PRIVATE)
        )
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "myforest.db"
        ).build()
        val repository = RoomFocusRepository(database.focusSessionDao())

        enableEdgeToEdge()
        setContent {
            MyForestTheme {
                val navController = rememberNavController()
                val viewModel: FocusViewModel = viewModel(
                    factory = FocusViewModel.Factory(preferences, repository)
                )
                val state by viewModel.state.collectAsState()
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is FocusEvent.StartService -> {
                                val intent = Intent(context, FocusService::class.java)
                                    .putExtra(FocusService.EXTRA_DURATION_MS, event.durationMs)
                                ContextCompat.startForegroundService(context, intent)
                            }
                            FocusEvent.StopService -> {
                                context.stopService(Intent(context, FocusService::class.java))
                            }
                        }
                    }
                }

                DisposableEffect(context) {
                    val timerReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            when (intent.action) {
                                FocusService.ACTION_TIMER_TICK -> {
                                    val remaining = intent.getLongExtra(FocusService.EXTRA_REMAINING_MS, 0L)
                                    viewModel.onTimerTick(remaining)
                                }
                                FocusService.ACTION_TIMER_FINISH -> {
                                    viewModel.onTimerFinished()
                                }
                            }
                        }
                    }

                    val filter = IntentFilter().apply {
                        addAction(FocusService.ACTION_TIMER_TICK)
                        addAction(FocusService.ACTION_TIMER_FINISH)
                    }

                    ContextCompat.registerReceiver(
                        context,
                        timerReceiver,
                        filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )

                    onDispose { context.unregisterReceiver(timerReceiver) }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            navigationItems.forEach { screen ->
                                NavigationBarItem(
                                    icon = {
                                        when (screen) {
                                            Screen.Focus -> Icon(Icons.Filled.Home, contentDescription = null)
                                            Screen.History -> Icon(Icons.Filled.DateRange, contentDescription = null)
                                        }
                                    },
                                    label = { Text(screen.label) },
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
                                durationMinutes = state.durationMinutes,
                                isTimerRunning = state.isTimerRunning,
                                remainingTimeMs = state.remainingTimeMs,
                                growth = state.growth,
                                onDurationChange = viewModel::onDurationChange,
                                onTimerStateChange = { shouldStart ->
                                    if (shouldStart) {
                                        viewModel.onPlantClicked()
                                    } else {
                                        viewModel.onStopRequested()
                                    }
                                },
                            )
                        }
                        composable(Screen.History.route) {
                            HistoryScreen(
                                dailyStats = state.dailyStats,
                                totalMinutes = state.totalMinutesAllTime,
                                focusedDays = state.focusedDays
                            )
                        }
                    }
                }
            }
        }
    }
}
