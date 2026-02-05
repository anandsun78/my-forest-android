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

class FocusService : Service() {

    private var monitoringTimer: java.util.Timer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var countDownTimer: CountDownTimer? = null

    companion object {
        const val CHANNEL_ID = "FocusServiceChannel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val duration = intent?.getLongExtra("duration", 0) ?: 0

        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Mode")
            .setContentText("Remaining: ${duration / 1000 / 60} minutes")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)

        startMonitoring()

        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateNotification("Remaining: ${millisUntilFinished / 1000 / 60} minutes")
            }

            override fun onFinish() {
                stopSelf()
            }
        }.start()

        return START_STICKY
    }

    override fun onDestroy() {
        stopMonitoring()
        countDownTimer?.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        monitoringTimer = java.util.Timer()
        monitoringTimer?.schedule(object : java.util.TimerTask() {
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
            .build()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
