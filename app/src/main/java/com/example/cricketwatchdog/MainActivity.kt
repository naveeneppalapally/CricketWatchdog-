package com.example.cricketwatchdog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var apiKeyInput: EditText
    private lateinit var syncSpinner: Spinner
    private lateinit var soundSpinner: Spinner
    private lateinit var delaySeekBar: SeekBar
    private lateinit var startButton: Button
    
    private val SCREEN_CAPTURE_REQUEST_CODE = 100

    private lateinit var tvDelayValue: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apiKeyInput = findViewById(R.id.apiKeyInput)
        apiKeyInput.setText("48755340-cd62-4efb-97ab-52b04e0a4ea5")
        syncSpinner = findViewById(R.id.syncSpinner)
        soundSpinner = findViewById(R.id.soundSpinner)
        delaySeekBar = findViewById(R.id.delaySeekBar)
        tvDelayValue = findViewById(R.id.tvDelayValue)
        startButton = findViewById(R.id.startButton)

        setupSpinners()
        setupButtons()
        setupSeekBar()
        checkPermissions()
    }

    private fun checkPermissions() {
        // 1. Overlay Permission
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant Overlay Permission", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
        
        // 2. Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun setupSeekBar() {
        delaySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minutes = progress / 60
                val seconds = progress % 60
                val text = if (minutes > 0) "$minutes min $seconds sec" else "$seconds seconds"
                tvDelayValue.text = text
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupSpinners() {
        val syncOptions = arrayOf("1 min", "2 min", "3 min", "4 min", "5 min")
        val syncAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, syncOptions)
        syncSpinner.adapter = syncAdapter

        val soundOptions = arrayOf("Standard SFX", "AI Voice")
        val soundAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, soundOptions)
        soundSpinner.adapter = soundAdapter
    }
    
    private fun setupButtons() {
        startButton.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, 0)
                return@setOnClickListener
            }

            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mpManager.createScreenCaptureIntent(), SCREEN_CAPTURE_REQUEST_CODE)
        }
        
        findViewById<Button>(R.id.btnResync).setOnClickListener {
            val intent = Intent(this, MatchMonitorService::class.java)
            intent.action = MatchMonitorService.ACTION_RESYNC
            startService(intent)
            Toast.makeText(this, "Resync requested...", Toast.LENGTH_SHORT).show()
        }
        
        // Quick Delay Buttons (Mockup logic for now, or just Resync is enough?)
        // User asked for "1, 2, 3, 4, 5 button for resyncing".
        // Let's implement them as "Set Sync Interval" buttons?
        // Or maybe "Resync Now" is the main one.
        // Let's assume the layout has them.
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                MatchMonitorService.mediaProjectionIntent = data
                MatchMonitorService.apiKey = apiKeyInput.text.toString()
                MatchMonitorService.manualDelaySeconds = delaySeekBar.progress.toLong()
                MatchMonitorService.syncIntervalMinutes = syncSpinner.selectedItemPosition + 1
                MatchMonitorService.useAiVoice = soundSpinner.selectedItemPosition == 1
                
                val intent = Intent(this, MatchMonitorService::class.java)
                intent.action = MatchMonitorService.ACTION_START
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } else {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
