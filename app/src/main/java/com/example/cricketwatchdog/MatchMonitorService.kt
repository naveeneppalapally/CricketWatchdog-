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

    private var currentMatchId: String? = null

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
            val api = MatchAPI.create()
            
            while (true) {
                try {
                    val matchData = api.getScore(apiKey)
                    
                    // Auto-Detect Match if not set
                    if (currentMatchId == null) {
                        // We need to sync first to find the match
                        performSync(matchData.data)
                    }

                    val match = matchData.data.find { it.id == currentMatchId } ?: matchData.data.firstOrNull()
                    
                    if (match != null) {
                        currentMatchId = match.id // Lock on if fallback used
                        val score = match.score?.firstOrNull()
                        if (score != null) {
                            val currentBallId = "${score.o}" // Unique ID for ball
                            
                            if (currentBallId != lastProcessedBall) {
                                // Real logic would be:
                                // val event = detectEvent(oldScore, newScore)
                                val event = EventType.NONE // Replace with real detection
                                
                                if (event != EventType.NONE) {
                                    val totalDelay = if (globalDelaySeconds > 0) globalDelaySeconds else manualDelaySeconds
                                    delay(totalDelay * 1000)

                                    VolumeManager.forceAttention(this@MatchMonitorService)
                                    VolumeManager.playEventSound(this@MatchMonitorService, event, useAiVoice)
                                    overlayManager.showFlash(event)
                                }
                                
                                lastProcessedBall = currentBallId
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000) // Poll every 5s
            }
        }

        startSyncLoop()
    }
    
    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = lifecycleScope.launch {
            while (true) {
                // We need to fetch data to pass to sync
                try {
                    val api = MatchAPI.create()
                    val matchData = api.getScore(apiKey)
                    performSync(matchData.data)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(syncIntervalMinutes * 60 * 1000L)
            }
        }
    }
    
    private fun triggerManualResync() {
        lifecycleScope.launch {
             try {
                val api = MatchAPI.create()
                val matchData = api.getScore(apiKey)
                performSync(matchData.data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun performSync(matches: List<MatchInfo>) {
        if (mediaProjection == null) return

        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getRealMetrics(metrics)
        
        val screenOver = autoSyncEngine.getScreenOver(
            mediaProjection!!,
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi
        )

        if (screenOver != null) {
            // 1. Auto-Detect Match if needed
            if (currentMatchId == null) {
                // Find match with closest over
                val bestMatch = matches.minByOrNull { match ->
                    val matchOver = match.score?.firstOrNull()?.o ?: -100.0
                    kotlin.math.abs(matchOver - screenOver)
                }
                
                if (bestMatch != null) {
                    val matchOver = bestMatch.score?.firstOrNull()?.o ?: 0.0
                    if (kotlin.math.abs(matchOver - screenOver) < 5.0) { // Tolerance of 5 overs
                        currentMatchId = bestMatch.id
                    }
                }
            }

            // 2. Calculate Delay for current match
            val match = matches.find { it.id == currentMatchId }
            if (match != null) {
                val apiOver = match.score?.firstOrNull()?.o ?: 0.0
                
                // Calculate Delay
                val apiBalls = (apiOver.toInt() * 6) + ((apiOver * 10).toInt() % 10)
                val screenBalls = (screenOver.toInt() * 6) + ((screenOver * 10).toInt() % 10)
                
                val diffBalls = apiBalls - screenBalls
                globalDelaySeconds = if (diffBalls > 0) diffBalls * 20L else 0L
            }
        } else {
            // Ad Detected or Failed
             delay(30000) 
             // We don't recurse here to avoid infinite loops in this simplified logic
        }
    }

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
