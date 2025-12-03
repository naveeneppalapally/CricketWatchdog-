package com.example.cricketwatchdog

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.regex.Pattern
import kotlin.coroutines.resume

enum class SyncStatus {
    SUCCESS, FAILED, AD_DETECTED
}

class AutoSyncEngine {

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    data class ScreenScore(val runs: Int, val wickets: Int, val overs: Double)

    @SuppressLint("WrongConstant")
    suspend fun getScreenScore(
        mediaProjection: MediaProjection,
        width: Int,
        height: Int,
        density: Int
    ): ScreenScore? = suspendCancellableCoroutine { continuation ->

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            null
        )

        val handler = Handler(Looper.getMainLooper())
        
        handler.postDelayed({
            try {
                val image = imageReader?.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val bitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    image.close()

                    // Crop to bottom 20% (Score area)
                    val croppedBitmap = Bitmap.createBitmap(
                        bitmap,
                        0,
                        (height * 0.8).toInt(),
                        width,
                        (height * 0.2).toInt()
                    )

                    val inputImage = InputImage.fromBitmap(croppedBitmap, 0)

                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val text = visionText.text
                            // Regex for "145/3" or "145-3" and "14.2"
                            val scorePattern = Pattern.compile("(\\d+)[/|-](\\d+)")
                            val overPattern = Pattern.compile("(\\d+)\\.(\\d+)")
                            
                            val scoreMatcher = scorePattern.matcher(text)
                            val overMatcher = overPattern.matcher(text)

                            var runs = -1
                            var wickets = -1
                            var overs = -1.0

                            if (scoreMatcher.find()) {
                                runs = scoreMatcher.group(1)?.toIntOrNull() ?: -1
                                wickets = scoreMatcher.group(2)?.toIntOrNull() ?: -1
                            }
                            
                            if (overMatcher.find()) {
                                overs = overMatcher.group(0)?.toDoubleOrNull() ?: -1.0
                            }

                            if (runs != -1 && wickets != -1) {
                                continuation.resume(ScreenScore(runs, wickets, overs))
                            } else {
                                continuation.resume(null)
                            }
                            cleanup()
                        }
                        .addOnFailureListener {
                            continuation.resume(null)
                            cleanup()
                        }
                } else {
                    continuation.resume(null)
                    cleanup()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resume(null)
                cleanup()
            }
        }, 1000)
    }

    private fun cleanup() {
        virtualDisplay?.release()
        imageReader?.close()
        virtualDisplay = null
        imageReader = null
    }
}
