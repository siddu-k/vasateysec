package com.sriox.vasateysec.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.sriox.vasateysec.SupabaseClient
import com.sriox.vasateysec.models.FCMToken
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FCMTokenManager {
    private const val TAG = "FCMTokenManager"
    private const val PREFS_NAME = "fcm_prefs"
    private const val KEY_FCM_TOKEN = "fcm_token"

    /**
     * Initialize FCM and save token to Supabase
     */
    fun initializeFCM(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d(TAG, "FCM Token obtained: ${token.take(20)}...")
                updateFCMToken(context, token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get FCM token", e)
            }
        }
    }

    /**
     * Update FCM token in Supabase
     */
    fun updateFCMToken(context: Context, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser == null) {
                    Log.w(TAG, "No user logged in, cannot save FCM token")
                    return@launch
                }

                val deviceId = getDeviceId(context)
                val deviceName = getDeviceName()

                // Save token locally
                saveTokenLocally(context, token)

                // Check if token already exists
                val existingTokens = try {
                    SupabaseClient.client.from("fcm_tokens")
                        .select {
                            filter {
                                eq("token", token)
                            }
                        }
                        .decodeList<FCMToken>()
                } catch (e: Exception) {
                    Log.w(TAG, "No existing tokens found or error checking: ${e.message}")
                    emptyList()
                }

                if (existingTokens.isEmpty()) {
                    // Insert new token
                    val fcmToken = FCMToken(
                        user_id = currentUser.id,
                        token = token,
                        device_id = deviceId,
                        device_name = deviceName,
                        platform = "android",
                        is_active = true
                    )

                    SupabaseClient.client.from("fcm_tokens").insert(fcmToken)
                    Log.d(TAG, "FCM token saved to Supabase")
                } else {
                    // Update existing token
                    SupabaseClient.client.from("fcm_tokens").update({
                        set("user_id", currentUser.id)
                        set("device_id", deviceId)
                        set("device_name", deviceName)
                        set("is_active", true)
                        set("last_used_at", "now()")
                    }) {
                        filter {
                            eq("token", token)
                        }
                    }
                    Log.d(TAG, "FCM token updated in Supabase")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save FCM token to Supabase", e)
            }
        }
    }

    /**
     * Deactivate FCM token on logout
     */
    fun deactivateFCMToken(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = getTokenLocally(context) ?: return@launch

                SupabaseClient.client.from("fcm_tokens").update({
                    set("is_active", false)
                }) {
                    filter {
                        eq("token", token)
                    }
                }

                clearTokenLocally(context)
                Log.d(TAG, "FCM token deactivated")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to deactivate FCM token", e)
            }
        }
    }

    /**
     * Get FCM tokens for a list of guardian emails
     */
    suspend fun getGuardianTokens(guardianEmails: List<String>): List<Pair<String, String>> {
        return try {
            val tokens = mutableListOf<Pair<String, String>>()

            for (email in guardianEmails) {
                try {
                    // Get user ID from email
                    val users = try {
                        SupabaseClient.client.from("users")
                            .select {
                                filter {
                                    eq("email", email)
                                }
                            }
                            .decodeList<Map<String, String>>()
                    } catch (e: Exception) {
                        Log.w(TAG, "No user found for email $email: ${e.message}")
                        emptyList()
                    }

                    if (users.isNotEmpty()) {
                        val userId = users[0]["id"] ?: continue

                        // Get active FCM tokens for this user
                        val fcmTokens = try {
                            SupabaseClient.client.from("fcm_tokens")
                                .select {
                                    filter {
                                        eq("user_id", userId)
                                        eq("is_active", true)
                                    }
                                }
                                .decodeList<FCMToken>()
                        } catch (e: Exception) {
                            Log.w(TAG, "No FCM tokens found for user $userId: ${e.message}")
                            emptyList()
                        }

                        fcmTokens.forEach { fcmToken ->
                            tokens.add(Pair(email, fcmToken.token))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing guardian email $email", e)
                }
            }

            Log.d(TAG, "Retrieved ${tokens.size} FCM tokens for ${guardianEmails.size} guardians")
            tokens
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get guardian tokens", e)
            emptyList()
        }
    }

    private fun saveTokenLocally(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
    }

    private fun getTokenLocally(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FCM_TOKEN, null)
    }

    private fun clearTokenLocally(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_FCM_TOKEN)
            .apply()
    }

    private fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun getDeviceName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }
    
    /**
     * Get the current user's FCM token
     */
    suspend fun getCurrentUserToken(context: Context): String? {
        return try {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            if (currentUser == null) {
                Log.w(TAG, "No user logged in, cannot get FCM token")
                return null
            }
            
            // First try to get from local storage
            val localToken = getTokenLocally(context)
            if (localToken != null) {
                return localToken
            }
            
            // If not found locally, try to get from FCM
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNotEmpty()) {
                saveTokenLocally(context, token)
                updateFCMToken(context, token)
                token
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current user FCM token", e)
            null
        }
    }
}
