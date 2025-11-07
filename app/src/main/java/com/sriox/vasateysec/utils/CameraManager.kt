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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object CameraManager {
    private const val TAG = "CameraManager"
    private const val CAPTURE_TIMEOUT_MS = 10000L // Increased to 10 seconds
    private const val CAMERA_DELAY_MS = 4000L // 4 second delay between cameras to ensure cleanup
    
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
        
        // Try FRONT camera with retry
        try {
            Log.d(TAG, "📸 Step 1: Capturing from FRONT camera...")
            frontPhoto = captureFromCamera(context, CameraCharacteristics.LENS_FACING_FRONT)
            if (frontPhoto != null) {
                Log.d(TAG, "Front camera: ✅ Success (${frontPhoto.length()} bytes)")
            } else {
                Log.e(TAG, "Front camera: ❌ Failed on first attempt, retrying after delay...")
                delay(1000) // Wait 1 second before retry
                frontPhoto = captureFromCamera(context, CameraCharacteristics.LENS_FACING_FRONT)
                if (frontPhoto != null) {
                    Log.d(TAG, "Front camera: ✅ Success on retry (${frontPhoto.length()} bytes)")
                } else {
                    Log.e(TAG, "Front camera: ❌ Failed after retry")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Front camera exception: ${e.javaClass.simpleName} - ${e.message}", e)
        }
        
        // Add delay before back camera to ensure front camera is fully released
        delay(500)
        
        // Try BACK camera
        try {
            Log.d(TAG, "📸 Step 2: Capturing from BACK camera...")
            backPhoto = captureFromCamera(context, CameraCharacteristics.LENS_FACING_BACK)
            Log.d(TAG, "Back camera: ${if (backPhoto != null) "✅ Success (${backPhoto.length()} bytes)" else "❌ Failed"}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Back camera error: ${e.message}", e)
        }
        
        Log.d(TAG, "✅ Photo capture complete: front=${frontPhoto != null}, back=${backPhoto != null}")
        return@withContext CapturedPhotos(frontPhoto, backPhoto)
    }
    
    data class CameraCapture(
        val bitmap: Bitmap?,
        val closeSignal: CompletableDeferred<Unit>
    )
    
    /**
     * Capture photo from specific camera
     */
    private suspend fun captureFromCamera(context: Context, lensFacing: Int): File? {
        val cameraName = if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) "FRONT" else "BACK"
        
        return withTimeoutOrNull(CAPTURE_TIMEOUT_MS) {
            try {
                Log.d(TAG, "[$cameraName] Step 1: Getting camera manager...")
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as AndroidCameraManager
                
                Log.d(TAG, "[$cameraName] Step 2: Finding camera ID...")
                val cameraId = getCameraId(cameraManager, lensFacing)
                if (cameraId == null) {
                    Log.e(TAG, "[$cameraName] ❌ No camera found with lens facing: $lensFacing")
                    return@withTimeoutOrNull null
                }
                Log.d(TAG, "[$cameraName] ✅ Found camera ID: $cameraId")
                
                Log.d(TAG, "[$cameraName] Step 3: Creating image file...")
                val imageFile = createImageFile(context, lensFacing)
                Log.d(TAG, "[$cameraName] ✅ Image file created: ${imageFile.absolutePath}")
                
                Log.d(TAG, "[$cameraName] Step 4: Capturing image (timeout: ${CAPTURE_TIMEOUT_MS}ms)...")
                val capture = captureImage(context, cameraManager, cameraId, lensFacing)
                
                if (capture.bitmap != null) {
                    Log.d(TAG, "[$cameraName] ✅ Bitmap captured: ${capture.bitmap.width}x${capture.bitmap.height}")
                    Log.d(TAG, "[$cameraName] Step 5: Saving to file...")
                    saveBitmapToFile(capture.bitmap, imageFile)
                    Log.d(TAG, "[$cameraName] ✅ SUCCESS - Photo saved: ${imageFile.length()} bytes")
                    
                    // CRITICAL: Wait for camera to actually close before returning
                    Log.d(TAG, "[$cameraName] Step 6: Waiting for camera to ACTUALLY close (onClosed callback)...")
                    val closed = withTimeoutOrNull(5000L) {
                        capture.closeSignal.await()
                        true
                    }
                    
                    if (closed == true) {
                        Log.d(TAG, "[$cameraName] ✅ Camera CONFIRMED closed via onClosed callback")
                        // Add extra delay even after onClosed to ensure hardware fully releases
                        kotlinx.coroutines.delay(1000L)
                        Log.d(TAG, "[$cameraName] ✅ Extra safety delay complete")
                    } else {
                        Log.w(TAG, "[$cameraName] ⚠️ Camera close timeout after 5s - adding extra safety delay")
                        // If onClosed didn't fire, add longer delay for hardware to release
                        kotlinx.coroutines.delay(3000L)
                        Log.d(TAG, "[$cameraName] ✅ Safety delay complete - proceeding")
                    }
                    
                    return@withTimeoutOrNull imageFile
                } else {
                    Log.e(TAG, "[$cameraName] ❌ FAILED - Bitmap is null (camera capture failed)")
                    // Still wait for close signal even on failure
                    withTimeoutOrNull(1000L) {
                        capture.closeSignal.await()
                    }
                    return@withTimeoutOrNull null
                }
            } catch (e: Exception) {
                Log.e(TAG, "[$cameraName] ❌ EXCEPTION: ${e.javaClass.simpleName} - ${e.message}", e)
                return@withTimeoutOrNull null
            }
        } ?: run {
            Log.e(TAG, "[$cameraName] ❌ TIMEOUT after ${CAPTURE_TIMEOUT_MS}ms")
            null
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
    ): CameraCapture = suspendCancellableCoroutine { continuation ->
        val cameraClosedSignal = CompletableDeferred<Unit>()
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
                            continuation.resume(CameraCapture(bitmap, cameraClosedSignal))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing image: ${e.message}", e)
                    if (!resumed) {
                        resumed = true
                        handlerThread.quitSafely()
                        cameraClosedSignal.complete(Unit)
                        continuation.resume(CameraCapture(null, cameraClosedSignal))
                    }
                }
            }, backgroundHandler)
            
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
                if (!resumed) {
                    resumed = true
                    handlerThread.quitSafely()
                    continuation.resume(CameraCapture(null, cameraClosedSignal))
                }
                cameraClosedSignal.complete(Unit)
                // Don't complete cameraClosedSignal here - wait for onClosed callback
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
                                                // Close session, camera, and imageReader to free ALL resources
                                                try {
                                                    session.close()
                                                    camera.close()
                                                    imageReader.close()
                                                    handlerThread.quitSafely()
                                                    Log.d(TAG, "Camera, ImageReader, and Handler closed - waiting for onClosed callback...")
                                                } catch (e: Exception) {
                                                    Log.e(TAG, "Error closing camera resources: ${e.message}")
                                                    cameraClosedSignal.complete(Unit)
                                                }
                                            }
                                        }, backgroundHandler)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Capture failed: ${e.message}", e)
                                        if (!resumed) {
                                            resumed = true
                                            handlerThread.quitSafely()
                                            cameraClosedSignal.complete(Unit)
                                            continuation.resume(CameraCapture(null, cameraClosedSignal))
                                        }
                                    }
                                }
                                
                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    Log.e(TAG, "Camera session configuration failed")
                                    if (!resumed) {
                                        resumed = true
                                        handlerThread.quitSafely()
                                        cameraClosedSignal.complete(Unit)
                                        continuation.resume(CameraCapture(null, cameraClosedSignal))
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
                            cameraClosedSignal.complete(Unit)
                            continuation.resume(CameraCapture(null, cameraClosedSignal))
                        }
                    }
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    try {
                        camera.close()
                        Log.w(TAG, "Camera disconnected")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error closing disconnected camera: ${e.message}")
                    }
                    cameraClosedSignal.complete(Unit)
                    if (!resumed) {
                        resumed = true
                        handlerThread.quitSafely()
                        continuation.resume(CameraCapture(null, cameraClosedSignal))
                    }
                }
                
                override fun onClosed(camera: CameraDevice) {
                    Log.d(TAG, "✅ Camera ACTUALLY CLOSED (onClosed callback)")
                    cameraClosedSignal.complete(Unit)
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    try {
                        camera.close()
                        Log.e(TAG, "Camera error: $error (${getCameraErrorMessage(error)})")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error closing camera after error: ${e.message}")
                    }
                    cameraClosedSignal.complete(Unit)
                    if (!resumed) {
                        resumed = true
                        handlerThread.quitSafely()
                        continuation.resume(CameraCapture(null, cameraClosedSignal))
                    }
                }
                
                private fun getCameraErrorMessage(error: Int): String {
                    return when (error) {
                        CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> "Camera in use"
                        CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> "Max cameras in use"
                        CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> "Camera disabled"
                        CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> "Camera device error"
                        CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> "Camera service error"
                        else -> "Unknown error"
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
                cameraClosedSignal.complete(Unit)
                continuation.resume(CameraCapture(null, cameraClosedSignal))
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
