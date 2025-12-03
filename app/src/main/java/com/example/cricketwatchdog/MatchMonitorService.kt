package com.example.cricketwatchdog

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MatchMonitorService : LifecycleService() {

    companion object {
        var mediaProjectionIntent: Intent? = null
        var apiKey: String = ""
        var manualDelaySeconds: Long = 0
        var syncIntervalMinutes: Int = 1
        var useAiVoice: Boolean = false
        
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_RESYNC = "ACTION_RESYNC"
    }

    private var monitoringJob: Job? = null
    private var syncJob: Job? = null
    private var mediaProjection: MediaProjection? = null
    private val autoSyncEngine = AutoSyncEngine()
    private lateinit var overlayManager: OverlayManager
    private var globalDelaySeconds: Long = 0
    private var lastProcessedBall: String = ""

    override fun onCreate() {
        super.onCreate()
        VolumeManager.init(this)
        overlayManager = OverlayManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoring()
            ACTION_RESYNC -> triggerManualResync()
        }

        return START_STICKY
    }

    private var lastRuns = -1
    private var lastWickets = -1
    private var isFastOcrMode = true // Default to OCR mode to save API

    private fun startMonitoring() {
        val notification = createNotification()
        startForeground(1, notification)

        if (mediaProjectionIntent != null) {
            val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mpManager.getMediaProjection(
                android.app.Activity.RESULT_OK,
                mediaProjectionIntent!!
            )
        }

        monitoringJob?.cancel()
        monitoringJob = lifecycleScope.launch {
            while (true) {
                performOcrScan()
                delay(2000) // Fast scan every 2s
            }
        }
    }
    
    private suspend fun performOcrScan() {
        if (mediaProjection == null) return

        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getRealMetrics(metrics)
        
        val score = autoSyncEngine.getScreenScore(
            mediaProjection!!,
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi
        )

        if (score != null) {
            // First run? Initialize
            if (lastRuns == -1) {
                lastRuns = score.runs
                lastWickets = score.wickets
                return
            }

            // Detect Events
            val diffRuns = score.runs - lastRuns
            val diffWickets = score.wickets - lastWickets

            var event = EventType.NONE

            if (diffRuns == 4) {
                event = EventType.FOUR
            } else if (diffRuns == 6) {
                event = EventType.SIX
            }

            if (diffWickets > 0) {
                event = EventType.WICKET
            }

            if (event != EventType.NONE) {
                // Trigger Alert Immediately (No Delay needed for OCR as it's live on screen)
                VolumeManager.forceAttention(this@MatchMonitorService)
                VolumeManager.playEventSound(this@MatchMonitorService, event, useAiVoice)
                overlayManager.showFlash(event)
            }

            // Update state
            lastRuns = score.runs
            lastWickets = score.wickets
        }
    }

    private fun triggerManualResync() {
        // Reset OCR state
        lastRuns = -1
        lastWickets = -1
    }

    // Removed old performSync as it's replaced by performOcrScan
    private suspend fun performSync(matches: List<MatchInfo>) {}

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        syncJob?.cancel()
        mediaProjection?.stop()
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "MonitorChannel",
                "Match Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "MonitorChannel")
            .setContentTitle("Cricket Watchdog")
            .setContentText("Monitoring match...")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .build()
    }
}
