package com.anandsundaram.myforest

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.anandsundaram.myforest.ui.theme.MyForestTheme

@Composable
fun FocusScreen(
    modifier: Modifier = Modifier,
    durationMinutes: Float,
    isTimerRunning: Boolean,
    remainingTime: Long,
    growth: Float,
    onDurationChange: (Float) -> Unit,
    onTimerStateChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onTimerStateChange(true)
        } else {
            // Inform the user
        }
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permissions are not needed for API levels below 33.
        }
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations() {
        val intent = Intent().apply {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }

    LaunchedEffect(Unit) {
        if (!isBatteryOptimizationIgnored()) {
            requestIgnoreBatteryOptimizations()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Tree(growth = if (isTimerRunning) growth else 0.3f)
        Spacer(modifier = Modifier.height(32.dp))
        if (isTimerRunning) {
            Text(
                text = "Time Remaining: ${remainingTime / 1000 / 60} minutes",
                style = MaterialTheme.typography.headlineMedium
            )
        } else {
            Text(
                text = "Focus for ${durationMinutes.toInt()} minutes",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Slider(
                value = durationMinutes,
                onValueChange = onDurationChange,
                valueRange = 5f..120f,
                steps = (120 - 5) / 5 - 1
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        if (!isTimerRunning) {
            Button(onClick = {
                val hasUsagePerm = hasUsageStatsPermission()
                val hasNotificationPerm = hasNotificationPermission()

                if (hasUsagePerm && hasNotificationPerm && isBatteryOptimizationIgnored()) {
                    onTimerStateChange(true)
                } else {
                    if (!hasUsagePerm) {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                    if (!hasNotificationPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (!isBatteryOptimizationIgnored()) {
                        requestIgnoreBatteryOptimizations()
                    }
                }
            }) {
                Text("Plant")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FocusScreenPreview() {
    MyForestTheme {
        FocusScreen(
            durationMinutes = 25f,
            isTimerRunning = false,
            remainingTime = 0,
            growth = 0f,
            onDurationChange = {},
            onTimerStateChange = {},
        )
    }
}
