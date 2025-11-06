package com.sriox.vasateysec.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager as AndroidCameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object CameraManager {
    private const val TAG = "CameraManager"
    private const val CAPTURE_TIMEOUT_MS = 5000L
    
    data class CapturedPhotos(
        val frontPhoto: File?,
        val backPhoto: File?
    )
    
    /**
     * Capture emergency photos from both cameras
     */
    suspend fun captureEmergencyPhotos(context: Context): CapturedPhotos = withContext(Dispatchers.IO) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "📸 EMERGENCY PHOTO CAPTURE STARTED")
        Log.d(TAG, "========================================")
        
        // Check camera permission
        val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Camera permission: ${if (hasCameraPermission) "✅ GRANTED" else "❌ DENIED"}")
        
        if (!hasCameraPermission) {
            Log.e(TAG, "❌ CANNOT CAPTURE - Camera permission not granted!")
            Log.e(TAG, "========================================")
            return@withContext CapturedPhotos(null, null)
        }
        
        var frontPhoto: File? = null
        var backPhoto: File? = null
        
        try {
            // Capture from front camera
            frontPhoto = captureFromCamera(context, CameraCharacteristics.LENS_FACING_FRONT)
            Log.d(TAG, "Front camera: ${if (frontPhoto != null) "✅ Success" else "❌ Failed"}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Front camera error: ${e.message}", e)
        }
        
        try {
            // Capture from back camera
            backPhoto = captureFromCamera(context, CameraCharacteristics.LENS_FACING_BACK)
            Log.d(TAG, "Back camera: ${if (backPhoto != null) "✅ Success" else "❌ Failed"}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Back camera error: ${e.message}", e)
        }
        
        Log.d(TAG, "✅ Photo capture complete: front=${frontPhoto != null}, back=${backPhoto != null}")
        return@withContext CapturedPhotos(frontPhoto, backPhoto)
    }
    
    /**
     * Capture photo from specific camera
     */
    private suspend fun captureFromCamera(context: Context, lensFacing: Int): File? {
        return withTimeoutOrNull(CAPTURE_TIMEOUT_MS) {
            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as AndroidCameraManager
                val cameraId = getCameraId(cameraManager, lensFacing) ?: return@withTimeoutOrNull null
                
                Log.d(TAG, "📷 Capturing from ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "front" else "back"} camera...")
                
                val imageFile = createImageFile(context, lensFacing)
                val bitmap = captureImage(context, cameraManager, cameraId, lensFacing)
                
                if (bitmap != null) {
                    saveBitmapToFile(bitmap, imageFile)
                    Log.d(TAG, "✅ Saved ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "front" else "back"} photo: ${imageFile.absolutePath}")
                    return@withTimeoutOrNull imageFile
                } else {
                    Log.w(TAG, "⚠️ Failed to capture from ${if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "front" else "back"} camera")
                    return@withTimeoutOrNull null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error capturing photo: ${e.message}", e)
                return@withTimeoutOrNull null
            }
        }
    }
    
    /**
     * Capture image using Camera2 API
     */
    private suspend fun captureImage(
        context: Context,
        cameraManager: AndroidCameraManager,
        cameraId: String,
        lensFacing: Int
    ): Bitmap? = suspendCancellableCoroutine { continuation ->
        var resumed = false
        
        try {
            val handlerThread = HandlerThread("CameraBackground")
            handlerThread.start()
            val backgroundHandler = Handler(handlerThread.looper)
            
            val imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
            
            imageReader.setOnImageAvailableListener({ reader ->
                try {
                    val image = reader.acquireLatestImage()
                    if (image != null) {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        image.close()
                        
                        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        
                        // Rotate front camera image
                        if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                            bitmap = rotateBitmap(bitmap, 270f)
                        }
                        
                        if (!resumed) {
                            resumed = true
                            handlerThread.quitSafely()
                            continuation.resume(bitmap)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing image: ${e.message}", e)
                    if (!resumed) {
                        resumed = true
                        handlerThread.quitSafely()
                        continuation.resume(null)
                    }
                }
            }, backgroundHandler)
            
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
                if (!resumed) {
                    resumed = true
                    handlerThread.quitSafely()
                    continuation.resume(null)
                }
                return@suspendCancellableCoroutine
            }
            
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    try {
                        val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        captureRequestBuilder.addTarget(imageReader.surface)
                        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                        
                        camera.createCaptureSession(
                            listOf(imageReader.surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    try {
                                        session.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                                            override fun onCaptureCompleted(
                                                session: CameraCaptureSession,
                                                request: CaptureRequest,
                                                result: TotalCaptureResult
                                            ) {
                                                // Image will be processed in ImageReader callback
                                                session.close()
                                                camera.close()
                                            }
                                        }, backgroundHandler)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Capture failed: ${e.message}", e)
                                        if (!resumed) {
                                            resumed = true
                                            handlerThread.quitSafely()
                                            continuation.resume(null)
                                        }
                                    }
                                }
                                
                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    Log.e(TAG, "Camera session configuration failed")
                                    if (!resumed) {
                                        resumed = true
                                        handlerThread.quitSafely()
                                        continuation.resume(null)
                                    }
                                }
                            },
                            backgroundHandler
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating capture session: ${e.message}", e)
                        if (!resumed) {
                            resumed = true
                            handlerThread.quitSafely()
                            continuation.resume(null)
                        }
                    }
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    if (!resumed) {
                        resumed = true
                        handlerThread.quitSafely()
                        continuation.resume(null)
                    }
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    Log.e(TAG, "Camera error: $error")
                    if (!resumed) {
                        resumed = true
                        handlerThread.quitSafely()
                        continuation.resume(null)
                    }
                }
            }, backgroundHandler)
            
            continuation.invokeOnCancellation {
                handlerThread.quitSafely()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera: ${e.message}", e)
            if (!resumed) {
                resumed = true
                continuation.resume(null)
            }
        }
    }
    
    /**
     * Get camera ID for specified lens facing
     */
    private fun getCameraId(cameraManager: AndroidCameraManager, lensFacing: Int): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) == lensFacing
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting camera ID: ${e.message}", e)
            null
        }
    }
    
    /**
     * Create temporary image file
     */
    private fun createImageFile(context: Context, lensFacing: Int): File {
        val timestamp = System.currentTimeMillis()
        val prefix = if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "front" else "back"
        return File(context.cacheDir, "emergency_${prefix}_${timestamp}.jpg")
    }
    
    /**
     * Save bitmap to file
     */
    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.flush()
            }
            Log.d(TAG, "✅ Saved image: ${file.absolutePath}, size: ${file.length()} bytes")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save image: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Rotate bitmap
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Check camera permission
     */
    private fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == 
                PackageManager.PERMISSION_GRANTED
    }
}
