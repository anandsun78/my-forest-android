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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    object Onboarding : Screen("onboarding", "Onboarding")
    object Permissions : Screen("permissions", "Permissions")
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
                var onboardingCompleted by remember { mutableStateOf(preferences.isOnboardingCompleted()) }
                var hasAllPermissions by remember { mutableStateOf(hasAllRequiredPermissions(this@MainActivity)) }
                val viewModel: FocusViewModel = viewModel(
                    factory = FocusViewModel.Factory(preferences, repository)
                )
                val state by viewModel.state.collectAsState()
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

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

                DisposableEffect(lifecycleOwner, onboardingCompleted) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasAllPermissions = hasAllRequiredPermissions(context)
                            if (onboardingCompleted && !hasAllPermissions) {
                                navController.navigate(Screen.Permissions.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                Scaffold(
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        val showBottomBar = currentDestination?.hierarchy?.any {
                            it.route == Screen.Onboarding.route || it.route == Screen.Permissions.route
                        } != true

                        if (showBottomBar) {
                            NavigationBar {
                                navigationItems.forEach { screen ->
                                    NavigationBarItem(
                                        icon = {
                                            when (screen) {
                                                Screen.Focus -> Icon(Icons.Filled.Home, contentDescription = null)
                                                Screen.History -> Icon(Icons.Filled.DateRange, contentDescription = null)
                                                Screen.Onboarding -> { }
                                                Screen.Permissions -> { }
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
                    }
                ) { innerPadding ->
                    NavHost(
                        navController,
                        startDestination = when {
                            !onboardingCompleted -> Screen.Onboarding.route
                            !hasAllPermissions -> Screen.Permissions.route
                            else -> Screen.Focus.route
                        },
                        Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Onboarding.route) {
                            OnboardingScreen(
                                onGetStarted = {
                                    preferences.setOnboardingCompleted(true)
                                    onboardingCompleted = true
                                    hasAllPermissions = hasAllRequiredPermissions(context)
                                    navController.navigate(
                                        if (hasAllPermissions) Screen.Focus.route else Screen.Permissions.route
                                    ) {
                                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(Screen.Permissions.route) {
                            PermissionsScreen(
                                onPermissionsSatisfied = {
                                    hasAllPermissions = true
                                    navController.navigate(Screen.Focus.route) {
                                        popUpTo(Screen.Permissions.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
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
