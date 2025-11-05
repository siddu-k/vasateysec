package com.sriox.vasateysec.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationManager {
    private const val TAG = "LocationManager"

    /**
     * Get current location
     */
    suspend fun getCurrentLocation(context: Context): Location? {
        Log.d(TAG, "getCurrentLocation called")
        
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "Location permission not granted")
            return null
        }
        
        Log.d(TAG, "Location permission granted, getting location...")

        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        // First try to get last known location immediately (fast)
        try {
            val lastLocation = fusedLocationClient.lastLocation.result
            if (lastLocation != null) {
                Log.d(TAG, "Using last known location: ${lastLocation.latitude}, ${lastLocation.longitude}, accuracy: ${lastLocation.accuracy}m")
                return lastLocation
            } else {
                Log.d(TAG, "No last known location available")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not get last known location: ${e.message}")
        }

        // If no last known location, try to get current location with timeout
        return suspendCancellableCoroutine { continuation ->
            try {
                Log.d(TAG, "Requesting current location with high accuracy...")
                val cancellationTokenSource = CancellationTokenSource()

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d(TAG, "Current location obtained: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")
                        continuation.resume(location)
                    } else {
                        Log.w(TAG, "Current location is null")
                        continuation.resume(null)
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get current location: ${e.message}", e)
                    continuation.resume(null)
                }

                continuation.invokeOnCancellation {
                    Log.d(TAG, "Location request cancelled")
                    cancellationTokenSource.cancel()
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception getting location", e)
                continuation.resume(null)
            } catch (e: Exception) {
                Log.e(TAG, "Exception getting location: ${e.message}", e)
                continuation.resume(null)
            }
        }
    }

    private fun getLastKnownLocation(
        context: Context,
        fusedLocationClient: FusedLocationProviderClient,
        continuation: kotlin.coroutines.Continuation<Location?>
    ) {
        try {
            if (!hasLocationPermission(context)) {
                continuation.resume(null)
                return
            }

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d(TAG, "Last known location: ${location.latitude}, ${location.longitude}")
                    } else {
                        Log.w(TAG, "Last known location is also null")
                    }
                    continuation.resume(location)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get last known location", e)
                    continuation.resume(null)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting last location", e)
            continuation.resume(null)
        }
    }

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}
