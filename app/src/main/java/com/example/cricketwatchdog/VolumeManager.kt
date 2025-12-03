package com.example.cricketwatchdog

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import java.util.Locale

object VolumeManager {

    private var tts: TextToSpeech? = null

    fun init(context: Context) {
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts?.language = Locale.US
            }
        }
    }

    fun forceAttention(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // 1. Unmute Ringer
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            } catch (e: SecurityException) {
                // Requires DND permission
            }
        }

        // 2. Check Stream Volume
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        
        if (currentVol == 0) {
            val targetVol = (maxVol * 0.7).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
        }

        // 3. Request Focus
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    fun playEventSound(context: Context, event: EventType, useAiVoice: Boolean) {
        if (useAiVoice) {
            val text = when (event) {
                EventType.FOUR -> "It's a Four!"
                EventType.SIX -> "That is a huge Six!"
                EventType.WICKET -> "Wicket! Wicket down!"
                EventType.DRS -> "Decision Review System activated."
                else -> ""
            }
            if (text.isNotEmpty()) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "EVENT_ID")
            }
        } else {
            val soundRes = when (event) {
                EventType.FOUR -> R.raw.cheer
                EventType.SIX -> R.raw.horn
                EventType.WICKET -> R.raw.shatter
                EventType.DRS -> R.raw.beep
                else -> 0
            }
            if (soundRes != 0) {
                MediaPlayer.create(context, soundRes).start()
            }
        }
    }
    
    fun isSystemMuted(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
    }
}
