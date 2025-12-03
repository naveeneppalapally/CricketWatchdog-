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
