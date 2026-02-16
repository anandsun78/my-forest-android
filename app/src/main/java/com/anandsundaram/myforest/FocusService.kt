package com.anandsundaram.myforest

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.util.Timer
import java.util.TimerTask

class FocusService : Service() {

    private var monitoringTimer: Timer? = null
    private var countdownTimer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastNotifiedMinute: Long = -1

    companion object {
        const val CHANNEL_ID = "FocusServiceChannel"
        const val ACTION_TIMER_TICK = "com.anandsundaram.myforest.TIMER_TICK"
        const val ACTION_TIMER_FINISH = "com.anandsundaram.myforest.TIMER_FINISH"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_REMAINING_MS = "extra_remaining_ms"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val duration = intent?.getLongExtra(EXTRA_DURATION_MS, 0) ?: 0
        if (duration <= 0L) {
            sendTimerFinish()
            stopSelf()
            return START_NOT_STICKY
        }
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Mode")
            .setContentText("Remaining: ${duration / 1000 / 60} minutes")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOnlyAlertOnce(true)
            .build()

        startForeground(1, notification)

        startMonitoring()
        startCountdown(duration)

        return START_STICKY
    }

    private fun startCountdown(duration: Long) {
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(duration, 500) {
            override fun onTick(millisUntilFinished: Long) {
                sendTimerTick(millisUntilFinished)

                val remainingMinutes = (millisUntilFinished / 1000 / 60)
                if (remainingMinutes != lastNotifiedMinute) {
                    lastNotifiedMinute = remainingMinutes
                    updateNotification("Remaining: $remainingMinutes minutes")
                }
            }

            override fun onFinish() {
                sendTimerFinish()
                stopSelf()
            }
        }.start()
    }

    override fun onDestroy() {
        stopMonitoring()
        countdownTimer?.cancel()
        lastNotifiedMinute = -1
        super.onDestroy()
    }

    private fun startMonitoring() {
        monitoringTimer?.cancel()
        monitoringTimer = Timer()
        monitoringTimer?.schedule(object : TimerTask() {
            override fun run() {
                handler.post { bringAppToFront() }
            }
        }, 0, 500) // Check every 500ms
    }

    private fun stopMonitoring() {
        monitoringTimer?.cancel()
    }

    private fun bringAppToFront() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguardManager.isKeyguardLocked) {
            return // Don't do anything if the screen is locked.
        }

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000, time)
        if (stats != null) {
            val topStat = stats.maxByOrNull { it.lastTimeUsed }
            val currentApp = topStat?.packageName
            if (currentApp != null && currentApp != packageName) {
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Focus Service Channel",
                NotificationManager.IMPORTANCE_LOW // Use low importance to prevent sound
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Mode")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOnlyAlertOnce(true)
            .build()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    private fun sendTimerTick(remainingTime: Long) {
        val intent = Intent(ACTION_TIMER_TICK).setPackage(packageName)
        intent.putExtra(EXTRA_REMAINING_MS, remainingTime)
        sendBroadcast(intent)
    }

    private fun sendTimerFinish() {
        val intent = Intent(ACTION_TIMER_FINISH).setPackage(packageName)
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
