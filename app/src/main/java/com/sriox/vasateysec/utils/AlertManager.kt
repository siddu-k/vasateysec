package com.sriox.vasateysec.utils

import android.content.Context
import android.util.Log
import com.sriox.vasateysec.SupabaseClient
import com.sriox.vasateysec.models.AlertHistory
import com.sriox.vasateysec.models.AlertRecipient
import com.sriox.vasateysec.models.Guardian
import com.sriox.vasateysec.models.UserProfile
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Returning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object AlertManager {
    private const val TAG = "AlertManager"

    /**
     * Send emergency alert to all guardians
     */
    suspend fun sendEmergencyAlert(
        context: Context,
        latitude: Double?,
        longitude: Double?,
        locationAccuracy: Float? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "sendEmergencyAlert called with location: lat=$latitude, lon=$longitude, accuracy=$locationAccuracy")
            
            // Get current user
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User not logged in"))

            // Get user profile
            val userProfile = SupabaseClient.client.from("users")
                .select {
                    filter {
                        eq("id", currentUser.id)
                    }
                }
                .decodeSingle<UserProfile>()

            val userName = userProfile.name ?: "Unknown"
            val userEmail = userProfile.email ?: ""
            val userPhone = userProfile.phone ?: ""

            // Create alert history record - use the model but it will skip null id
            val alertHistory = AlertHistory(
                user_id = currentUser.id,
                user_name = userName,
                user_email = userEmail,
                user_phone = userPhone,
                latitude = latitude,
                longitude = longitude,
                location_accuracy = locationAccuracy,
                alert_type = "voice_help",
                status = "sent"
            )
            
            Log.d(TAG, "Creating alert history with location: lat=$latitude, lon=$longitude")

            val insertResponse = SupabaseClient.client.from("alert_history")
                .insert(alertHistory) {
                    select()
                }

            val insertedAlert = try {
                insertResponse.decodeSingle<AlertHistory>()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode inserted alert: ${e.message}", e)
                return@withContext Result.failure(Exception("Failed to create alert: ${e.message}"))
            }

            val alertId = insertedAlert.id 
                ?: return@withContext Result.failure(Exception("Alert created but no ID returned"))
            
            Log.d(TAG, "Alert created with ID: $alertId, returned location: lat=${insertedAlert.latitude}, lon=${insertedAlert.longitude}")

            // Get guardians with better error handling
            val guardians = try {
                Log.d(TAG, "Current user ID: ${currentUser.id}")
                Log.d(TAG, "Fetching guardians for user: ${currentUser.id}")
                
                // TEST: Query ALL guardians first to see if query works
                Log.d(TAG, "TEST: Fetching ALL guardians from database...")
                Log.d(TAG, "TEST: Supabase URL: ${SupabaseClient.client.supabaseUrl}")
                
                val allGuardiansResponse = SupabaseClient.client.from("guardians").select()
                Log.d(TAG, "TEST: Response object type: ${allGuardiansResponse.javaClass.simpleName}")
                
                val allGuardians = try {
                    val result = allGuardiansResponse.decodeList<Guardian>()
                    Log.d(TAG, "TEST: Decode successful!")
                    result
                } catch (e: Exception) {
                    Log.e(TAG, "TEST: Failed to decode guardians")
                    Log.e(TAG, "TEST: Error: ${e.message}")
                    Log.e(TAG, "TEST: Error class: ${e.javaClass.name}")
                    Log.e(TAG, "TEST: Stack trace:", e)
                    emptyList()
                }
                Log.d(TAG, "TEST: Total guardians in DB: ${allGuardians.size}")
                allGuardians.forEach { g ->
                    Log.d(TAG, "TEST: Guardian - user_id: ${g.user_id}, email: ${g.guardian_email}, status: ${g.status}")
                }
                
                // Now query guardians for the current user specifically
                val response = SupabaseClient.client.from("guardians")
                    .select {
                        filter {
                            eq("user_id", currentUser.id)
                            eq("status", "active")
                        }
                    }
                
                Log.d(TAG, "Guardians query executed, attempting to decode...")
                
                // Try to decode the response, handle empty results gracefully
                try {
                    val result = response.decodeList<Guardian>()
                    Log.d(TAG, "Raw result: $result")
                    Log.d(TAG, "Successfully fetched ${result.size} guardians")
                    if (result.isEmpty()) {
                        Log.w(TAG, "Query returned empty list - no guardians found for user ${currentUser.id}")
                    } else {
                        result.forEach { guardian ->
                            Log.d(TAG, "Guardian: ${guardian.guardian_email}, status: ${guardian.status}")
                        }
                    }
                    result
                } catch (jsonError: Exception) {
                    Log.e(TAG, "JSON decode error: ${jsonError.message}", jsonError)
                    Log.e(TAG, "Error type: ${jsonError.javaClass.simpleName}")
                    emptyList<Guardian>()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching guardians: ${e.message}", e)
                Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Stack trace:", e)
                emptyList<Guardian>()
            }
                
            if (guardians.isEmpty()) {
                Log.w(TAG, "No active guardians found in the database")
                return@withContext Result.failure(Exception("No guardians found. Please add guardians first."))
            }

            val guardianEmails = guardians.map { it.guardian_email }
            Log.d(TAG, "Sending alerts to ${guardianEmails.size} guardians")

            // Get FCM tokens for guardians and self with better error handling
            val guardianTokens = try {
                Log.d(TAG, "Getting FCM tokens for ${guardianEmails.size} guardians")
                @Suppress("UNCHECKED_CAST")
                val tokens = FCMTokenManager.getGuardianTokens(guardianEmails) as? Map<String, Map<String, String>>
                    ?: emptyMap()
                Log.d(TAG, "Retrieved ${tokens.size} guardian tokens")
                tokens
            } catch (e: Exception) {
                Log.e(TAG, "Error getting guardian tokens", e)
                emptyMap<String, Map<String, String>>()
            }
            
            val selfToken = try {
                FCMTokenManager.getCurrentUserToken(context).also {
                    Log.d(TAG, if (it != null) "Retrieved self FCM token" else "No self FCM token available")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting self FCM token", e)
                null
            }
            
            // Combine guardian tokens and self token
            val allTokens: Map<String, Map<String, String>> = if (selfToken != null) {
                val combined = mutableMapOf<String, Map<String, String>>()
                combined.putAll(guardianTokens)
                combined[currentUser.id] = mapOf(
                    "token" to selfToken,
                    "email" to userEmail,
                    "name" to userName
                )
                combined
            } else {
                guardianTokens
            }

            if (allTokens.isEmpty()) {
                Log.w(TAG, "No FCM tokens found for guardians or self")
                return@withContext Result.failure(Exception("No active guardians found with the app installed"))
            }

            // Send notifications to each guardian
            var successCount = 0
            
            // Process each token in the map
            for ((userId, tokenInfo) in allTokens) {
                val isSelf = userId == currentUser.id
                val title = if (isSelf) "🆘 Help is on the way!" else "🚨 $userName needs help!"
                val body = if (isSelf) 
                    "We've alerted your guardians. Stay where you are, help is coming!" 
                else 
                    "$userName has triggered an emergency alert. Tap to view their location."
                
                val token = tokenInfo["token"] as? String ?: continue
                
                val success = sendNotificationToSupabase(
                    alertId = alertId,
                    token = token,
                    title = title,
                    body = body,
                    recipientEmail = if (isSelf) userEmail else tokenInfo["email"] as? String ?: "",
                    isSelfAlert = isSelf,
                    userName = userName,
                    userPhone = userPhone,
                    latitude = latitude,
                    longitude = longitude
                )

                if (success) {
                    successCount++
                }
            }
            
            // Create alert recipient records for all notifications
            for ((userId, tokenInfo) in allTokens) {
                try {
                    val isSelf = userId == currentUser.id
                    val recipient = AlertRecipient(
                        alert_id = alertId,
                        guardian_email = if (isSelf) userEmail else tokenInfo["email"] as? String ?: "",
                        fcm_token = tokenInfo["token"] as? String ?: "",
                        notification_sent = true,
                        notification_delivered = false
                    )
                    
                    SupabaseClient.client
                        .from("alert_recipients")
                        .insert(recipient)
                        
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create alert recipient record", e)
                }
            }

            if (successCount > 0) {
                Result.success("Alert sent to $successCount recipient(s)")
            } else {
                Result.failure(Exception("Failed to send alerts to any recipients"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send emergency alert", e)
            Result.failure(e)
        }
    }

    /**
     * Send notification via Vercel endpoint
     */
    @Suppress("DEPRECATION")
    private suspend fun sendNotificationToSupabase(
        alertId: String,
        token: String,
        title: String,
        body: String,
        recipientEmail: String,
        isSelfAlert: Boolean,
        userName: String,
        userPhone: String,
        latitude: Double?,
        longitude: Double?
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Call Vercel endpoint directly to send FCM notification
                val client = okhttp3.OkHttpClient()
                
                val jsonBody = """
                    {
                        "token": "$token",
                        "title": "$title",
                        "body": "$body",
                        "email": "$recipientEmail",
                        "isSelfAlert": $isSelfAlert,
                        "fullName": "$userName",
                        "phoneNumber": "$userPhone",
                        "lastKnownLatitude": ${latitude ?: "null"},
                        "lastKnownLongitude": ${longitude ?: "null"}
                    }
                """.trimIndent()
                
                val requestBody = okhttp3.RequestBody.create(
                    null,
                    jsonBody
                )
                
                val request = okhttp3.Request.Builder()
                    .url("https://vasatey-notify-msg.vercel.app/api/sendNotification")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Notification sent successfully to $recipientEmail via Vercel")
                    Log.d(TAG, "Response: $responseBody")
                    true
                } else {
                    Log.e(TAG, "Failed to send notification to $recipientEmail. Status: ${response.code}")
                    Log.e(TAG, "Response: $responseBody")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send notification via Vercel", e)
                false
            }
        }
    }
}
