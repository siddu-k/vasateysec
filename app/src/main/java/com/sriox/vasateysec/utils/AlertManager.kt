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
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
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
        locationAccuracy: Float? = null,
        frontPhotoFile: File? = null,
        backPhotoFile: File? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "sendEmergencyAlert called with location: lat=$latitude, lon=$longitude, accuracy=$locationAccuracy")
            
            // Try to get current user from Supabase auth
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            
            // If Supabase auth is not available (e.g., when app is closed), use SessionManager
            val userId: String
            val userName: String
            val userEmail: String
            val userPhone: String
            
            if (currentUser != null) {
                // Supabase session is available
                Log.d(TAG, "Using Supabase session for user: ${currentUser.id}")
                userId = currentUser.id
                
                // Get user profile from database
                val userProfile = SupabaseClient.client.from("users")
                    .select {
                        filter {
                            eq("id", currentUser.id)
                        }
                    }
                    .decodeSingle<UserProfile>()

                userName = userProfile.name ?: "Unknown"
                userEmail = userProfile.email ?: ""
                userPhone = userProfile.phone ?: ""
            } else {
                // Fallback to SessionManager (for when app is closed but service is running)
                Log.d(TAG, "Supabase session not available, using SessionManager")
                
                if (!SessionManager.isLoggedIn()) {
                    return@withContext Result.failure(Exception("User not logged in"))
                }
                
                userId = SessionManager.getUserId() ?: return@withContext Result.failure(Exception("User ID not found in session"))
                userName = SessionManager.getUserName() ?: "Unknown"
                userEmail = SessionManager.getUserEmail() ?: ""
                
                // Get user phone from database using userId
                userPhone = try {
                    val userProfile = SupabaseClient.client.from("users")
                        .select {
                            filter {
                                eq("id", userId)
                            }
                        }
                        .decodeSingle<UserProfile>()
                    userProfile.phone ?: ""
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch phone from database: ${e.message}")
                    ""
                }
            }
            
            Log.d(TAG, "Sending alert for user: $userName ($userEmail)")

            // Upload photos to Supabase Storage if available
            Log.d(TAG, "========================================")
            Log.d(TAG, "📤 Photo Upload Process Starting...")
            Log.d(TAG, "Front photo file: ${if (frontPhotoFile != null) "${frontPhotoFile.absolutePath} (exists: ${frontPhotoFile.exists()}, size: ${frontPhotoFile.length()})" else "NULL"}")
            Log.d(TAG, "Back photo file: ${if (backPhotoFile != null) "${backPhotoFile.absolutePath} (exists: ${backPhotoFile.exists()}, size: ${backPhotoFile.length()})" else "NULL"}")
            Log.d(TAG, "========================================")
            
            var frontPhotoUrl: String? = null
            var backPhotoUrl: String? = null
            
            // Upload both photos in parallel with timeout (max 15 seconds total)
            try {
                withTimeout(15000L) {
                    // Launch both uploads in parallel
                    val frontJob = async {
                        if (frontPhotoFile != null && frontPhotoFile.exists()) {
                            Log.d(TAG, "📤 Uploading front camera photo...")
                            val url = uploadPhotoToStorage(userId, frontPhotoFile, "front")
                            Log.d(TAG, if (url != null) "✅ Front photo uploaded: $url" else "❌ Front photo upload failed")
                            url
                        } else {
                            Log.w(TAG, "⚠️ Skipping front photo upload - file is null or doesn't exist")
                            null
                        }
                    }
                    
                    val backJob = async {
                        if (backPhotoFile != null && backPhotoFile.exists()) {
                            Log.d(TAG, "📤 Uploading back camera photo...")
                            val url = uploadPhotoToStorage(userId, backPhotoFile, "back")
                            Log.d(TAG, if (url != null) "✅ Back photo uploaded: $url" else "❌ Back photo upload failed")
                            url
                        } else {
                            Log.w(TAG, "⚠️ Skipping back photo upload - file is null or doesn't exist")
                            null
                        }
                    }
                    
                    // Wait for both uploads to complete
                    frontPhotoUrl = frontJob.await()
                    backPhotoUrl = backJob.await()
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "⏱️ Photo upload timeout after 15 seconds - proceeding without photos")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Photo upload error: ${e.message}", e)
            }
            
            Log.d(TAG, "========================================")
            Log.d(TAG, "📤 Photo Upload Complete")
            Log.d(TAG, "Front URL: ${frontPhotoUrl ?: "NONE"}")
            Log.d(TAG, "Back URL: ${backPhotoUrl ?: "NONE"}")
            Log.d(TAG, "========================================")

            // Create alert history record - use the model but it will skip null id
            val alertHistory = AlertHistory(
                user_id = userId,
                user_name = userName,
                user_email = userEmail,
                user_phone = userPhone,
                latitude = latitude,
                longitude = longitude,
                location_accuracy = locationAccuracy,
                alert_type = "voice_help",
                status = "sent",
                front_photo_url = frontPhotoUrl,
                back_photo_url = backPhotoUrl
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
                Log.d(TAG, "Current user ID: $userId")
                Log.d(TAG, "Fetching guardians for user: $userId")
                
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
                            eq("user_id", userId)
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
                        Log.w(TAG, "Query returned empty list - no guardians found for user $userId")
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
                val tokenPairs = FCMTokenManager.getGuardianTokens(guardianEmails)
                
                // Convert List<Pair<String, String>> to Map<String, Map<String, String>>
                // where the key is guardian_user_id and value contains token and email
                val tokensMap = mutableMapOf<String, Map<String, String>>()
                for ((email, token) in tokenPairs) {
                    // Get guardian user info for this email
                    val guardianInfo = guardians.find { it.guardian_email == email }
                    if (guardianInfo != null) {
                        val guardianUserId = guardianInfo.guardian_user_id ?: email
                        tokensMap[guardianUserId] = mapOf(
                            "token" to token,
                            "email" to email,
                            "name" to email.substringBefore("@")
                        )
                    }
                }
                Log.d(TAG, "Retrieved ${tokensMap.size} guardian tokens")
                tokensMap
            } catch (e: Exception) {
                Log.e(TAG, "Error getting guardian tokens", e)
                emptyMap<String, Map<String, String>>()
            }
            
            // Only send to guardians, NOT to self
            if (guardianTokens.isEmpty()) {
                Log.w(TAG, "No FCM tokens found for guardians")
                return@withContext Result.failure(Exception("No active guardians found with the app installed"))
            }
            
            Log.d(TAG, "Sending alerts to ${guardianTokens.size} guardians (excluding self)")

            // Send notifications to each guardian (NOT to self)
            var successCount = 0
            
            // Process each guardian token
            for ((guardianUserId, tokenInfo) in guardianTokens) {
                val guardianEmail = tokenInfo["email"] as? String ?: ""
                val token = tokenInfo["token"] as? String ?: continue
                
                val title = "🚨 $userName needs help!"
                val body = "$userName has triggered an emergency alert. Tap to view their location."
                
                Log.d(TAG, "========================================")
                Log.d(TAG, "📤 Sending notification to GUARDIAN: $guardianEmail")
                Log.d(TAG, "Photo URLs being sent:")
                Log.d(TAG, "  Front: ${frontPhotoUrl ?: "NONE"}")
                Log.d(TAG, "  Back: ${backPhotoUrl ?: "NONE"}")
                Log.d(TAG, "========================================")
                
                val success = sendNotificationToSupabase(
                    alertId = alertId,
                    token = token,
                    title = title,
                    body = body,
                    userEmail = userEmail,
                    guardianEmail = guardianEmail,
                    userName = userName,
                    userPhone = userPhone,
                    latitude = latitude,
                    longitude = longitude,
                    frontPhotoUrl = frontPhotoUrl,
                    backPhotoUrl = backPhotoUrl
                )

                if (success) {
                    successCount++
                    Log.d(TAG, "Successfully sent notification to guardian: $guardianEmail")
                } else {
                    Log.e(TAG, "Failed to send notification to guardian: $guardianEmail")
                }
            }
            
            // Create alert recipient records for all guardian notifications
            for ((guardianUserId, tokenInfo) in guardianTokens) {
                try {
                    val recipient = AlertRecipient(
                        alert_id = alertId,
                        guardian_email = tokenInfo["email"] as? String ?: "",
                        guardian_user_id = guardianUserId, // Add guardian user ID for querying received alerts
                        fcm_token = tokenInfo["token"] as? String ?: "",
                        notification_sent = true,
                        notification_delivered = false
                    )
                    
                    SupabaseClient.client
                        .from("alert_recipients")
                        .insert(recipient)
                    
                    Log.d(TAG, "Created alert recipient record for guardian: $guardianUserId")
                        
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
        userEmail: String,
        guardianEmail: String,
        userName: String,
        userPhone: String,
        latitude: Double?,
        longitude: Double?,
        frontPhotoUrl: String? = null,
        backPhotoUrl: String? = null
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
                        "email": "$userEmail",
                        "isSelfAlert": false,
                        "fullName": "$userName",
                        "phoneNumber": "$userPhone",
                        "lastKnownLatitude": ${latitude ?: "null"},
                        "lastKnownLongitude": ${longitude ?: "null"},
                        "frontPhotoUrl": ${if (frontPhotoUrl != null) "\"$frontPhotoUrl\"" else "null"},
                        "backPhotoUrl": ${if (backPhotoUrl != null) "\"$backPhotoUrl\"" else "null"}
                    }
                """.trimIndent()
                
                Log.d(TAG, "========================================")
                Log.d(TAG, "📤 JSON Payload being sent to Vercel:")
                Log.d(TAG, jsonBody)
                Log.d(TAG, "========================================")
                
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
                    Log.d(TAG, "Notification sent successfully to guardian $guardianEmail via Vercel")
                    Log.d(TAG, "Response: $responseBody")
                    true
                } else {
                    Log.e(TAG, "Failed to send notification to guardian $guardianEmail. Status: ${response.code}")
                    Log.e(TAG, "Response: $responseBody")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send notification via Vercel", e)
                false
            }
        }
    }
    
    /**
     * Upload photo to Supabase Storage
     */
    private suspend fun uploadPhotoToStorage(userId: String, photoFile: File, cameraType: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                val fileName = "emergency_${userId}_${cameraType}_${timestamp}.jpg"
                val bucketName = "emergency-photos"
                
                Log.d(TAG, "========================================")
                Log.d(TAG, "📤 Uploading $cameraType photo to Supabase Storage")
                Log.d(TAG, "Bucket: $bucketName")
                Log.d(TAG, "File: $fileName")
                Log.d(TAG, "Size: ${photoFile.length()} bytes")
                Log.d(TAG, "========================================")
                
                // Upload file to Supabase Storage
                val bucket = SupabaseClient.client.storage.from(bucketName)
                
                // Use proper upload with content type
                bucket.upload(
                    path = fileName,
                    data = photoFile.readBytes(),
                    upsert = true
                )
                
                // Get public URL
                val publicUrl = bucket.publicUrl(fileName)
                Log.d(TAG, "✅ Photo uploaded successfully!")
                Log.d(TAG, "URL: $publicUrl")
                Log.d(TAG, "========================================")
                
                return@withContext publicUrl
            } catch (e: Exception) {
                Log.e(TAG, "========================================")
                Log.e(TAG, "❌ Failed to upload $cameraType photo to storage")
                Log.e(TAG, "Error: ${e.message}")
                Log.e(TAG, "Stack trace:", e)
                Log.e(TAG, "========================================")
                return@withContext null
            }
        }
    }
}
