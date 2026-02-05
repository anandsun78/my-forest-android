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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anandsundaram.myforest.ui.theme.MyForestTheme
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

        val sharedPrefs = getSharedPreferences("MyForestPrefs", Context.MODE_PRIVATE)

        enableEdgeToEdge()
        setContent {
            MyForestTheme {
                val navController = rememberNavController()
                var history by remember { mutableStateOf(listOf<FocusSession>()) }

                val initialDuration = remember {
                    val stored = sharedPrefs.all["durationMinutes"]
                    when (stored) {
                        is Float -> stored
                        is Int -> stored.toFloat()
                        is Long -> stored.toFloat()
                        is Double -> stored.toFloat()
                        else -> 25f
                    }
                }
                var durationMinutes by rememberSaveable { mutableStateOf(initialDuration) }
                var isTimerRunning by rememberSaveable { mutableStateOf(false) }
                var remainingTime by rememberSaveable { mutableStateOf(0L) }
                var growth by rememberSaveable { mutableStateOf(0f) }

                val context = LocalContext.current

                fun handleSessionCompletion(isSuccess: Boolean) {
                    val actualMinutes = if (isSuccess) {
                        durationMinutes.toInt()
                    } else {
                        ((durationMinutes * 60 * 1000 - remainingTime) / (1000 * 60)).toInt()
                    }
                    history = history + FocusSession(Date(), durationMinutes.toInt(), actualMinutes, isSuccess)
                    isTimerRunning = false
                    growth = 0f

                    with(sharedPrefs.edit()) {
                        putBoolean("isTimerRunning", false)
                        apply()
                    }
                }

                DisposableEffect(context) {
                    val timerReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            when (intent.action) {
                                FocusService.ACTION_TIMER_TICK -> {
                                    remainingTime = intent.getLongExtra("remainingTime", 0L)
                                    val totalDuration = durationMinutes.toLong() * 60 * 1000
                                    if (totalDuration > 0) {
                                        growth = 1f - (remainingTime.toFloat() / totalDuration)
                                    }
                                }
                                FocusService.ACTION_TIMER_FINISH -> {
                                    handleSessionCompletion(true)
                                }
                            }
                        }
                    }

                    val filter = IntentFilter().apply {
                        addAction(FocusService.ACTION_TIMER_TICK)
                        addAction(FocusService.ACTION_TIMER_FINISH)
                    }

                    ContextCompat.registerReceiver(context, timerReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

                    onDispose {
                        context.unregisterReceiver(timerReceiver)
                    }
                }
                fun startFocusSession() {
                    isTimerRunning = true
                    remainingTime = durationMinutes.toLong() * 60 * 1000

                    val intent = Intent(this, FocusService::class.java)
                    intent.putExtra("duration", remainingTime)
                    startService(intent)
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
                                onDurationChange = {
                                    durationMinutes = it
                                    sharedPrefs.edit().putFloat("durationMinutes", it).apply()
                                },
                                onTimerStateChange = { newIsTimerRunning ->
                                    if (newIsTimerRunning) {
                                        startFocusSession()
                                    } else {
                                        stopService(Intent(this@MainActivity, FocusService::class.java))
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
