package com.example.cricketwatchdog

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    fun showFlash(type: EventType) {
        // Only flash if System Volume is 0 (Muted)
        if (!VolumeManager.isSystemMuted(context)) {
            return
        }

        val color = when (type) {
            EventType.FOUR, EventType.SIX -> Color.GREEN
            EventType.WICKET -> Color.RED
            EventType.DRS -> Color.parseColor("#FFA500") // Orange
            else -> return
        }
        
        val text = when (type) {
            EventType.FOUR -> "4 RUNS!"
            EventType.SIX -> "6 RUNS!"
            EventType.WICKET -> "WICKET!"
            EventType.DRS -> "DRS!"
            else -> ""
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val layout = FrameLayout(context)
        layout.setBackgroundColor(color)
        layout.alpha = 0.7f // Semi-transparent flash

        val textView = TextView(context)
        textView.text = text
        textView.textSize = 100f
        textView.setTextColor(Color.WHITE)
        textView.gravity = Gravity.CENTER
        
        val textParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        textParams.gravity = Gravity.CENTER
        layout.addView(textView, textParams)

        try {
            windowManager.addView(layout, params)
            
            CoroutineScope(Dispatchers.Main).launch {
                delay(5000)
                try {
                    windowManager.removeView(layout)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
