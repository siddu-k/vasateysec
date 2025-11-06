package com.sriox.vasateysec.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sriox.vasateysec.HelpRequestActivity
import com.sriox.vasateysec.R
import com.sriox.vasateysec.utils.FCMTokenManager

class VasateyFCMService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "VasateyFCMService"
        private const val CHANNEL_ID = "guardian_alert_channel"
        private const val CHANNEL_NAME = "Guardian Alerts"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: ${token.take(20)}...")
        
        // Save token to Supabase
        FCMTokenManager.updateFCMToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "Message received from: ${message.from}")
        
        // Always handle data payload (works for both app open and closed)
        // This ensures we show the same rich notification in both cases
        message.data.let { data ->
            if (data.isNotEmpty()) {
                Log.d(TAG, "Message data payload: $data")
                handleDataMessage(data)
            }
        }
        
        // Also check if there's a notification payload (when app is in foreground)
        message.notification?.let { notification ->
            Log.d(TAG, "Message notification payload: title=${notification.title}, body=${notification.body}")
            // We handle it via data payload above, so we can ignore this
            // This prevents duplicate notifications
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "Emergency Alert"
        val body = data["body"] ?: "Someone needs help"
        val fullName = data["fullName"] ?: ""
        val email = data["email"] ?: ""
        val phoneNumber = data["phoneNumber"] ?: ""
        // Backend may send 'lastKnownLatitude'/'lastKnownLongitude' OR 'latitude'/'longitude'
        val latitudeStr = data["lastKnownLatitude"] ?: data["latitude"] ?: ""
        val longitudeStr = data["lastKnownLongitude"] ?: data["longitude"] ?: ""
        
        Log.d(TAG, "Raw location data from FCM: lastKnownLatitude=${data["lastKnownLatitude"]}, latitude=${data["latitude"]}")
        Log.d(TAG, "Parsed location strings: latitudeStr='$latitudeStr', longitudeStr='$longitudeStr'")
        
        val alertType = data["alertType"] ?: "emergency"
        val timestamp = data["timestamp"] ?: ""
        val isSelfAlert = data["isSelfAlert"]?.toBoolean() ?: false
        
        Log.d(TAG, "Notification data: lat=$latitudeStr, lon=$longitudeStr, isSelf=$isSelfAlert")
        Log.d(TAG, "All FCM data keys: ${data.keys}")

        // Don't show notification for self alerts (user already knows they triggered it)
        if (isSelfAlert) {
            Log.d(TAG, "Skipping notification display for self alert")
            return
        }

        // Create intent to open EmergencyAlertViewerActivity - standalone, no login required
        // Use explicit class name to ensure it opens directly
        val intent = Intent(this, com.sriox.vasateysec.EmergencyAlertViewerActivity::class.java).apply {
            // CLEAR_TASK removes all other activities, NEW_TASK creates new task
            // This ensures ONLY EmergencyAlertViewerActivity opens
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("fullName", fullName)
            putExtra("email", email)
            putExtra("phoneNumber", phoneNumber)
            putExtra("latitude", latitudeStr)
            putExtra("longitude", longitudeStr)
            putExtra("alertType", alertType)
            putExtra("timestamp", timestamp)
            putExtra("fromNotification", true)
        }
        
        Log.d(TAG, "========================================")
        Log.d(TAG, "Creating notification with intent:")
        Log.d(TAG, "Action: ${intent.action}")
        Log.d(TAG, "Package: ${intent.`package`}")
        Log.d(TAG, "Flags: ${intent.flags}")
        Log.d(TAG, "Data: name=$fullName, lat=$latitudeStr, lon=$longitudeStr")
        Log.d(TAG, "========================================")

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        Log.d(TAG, "PendingIntent created successfully")

        // Build rich notification with detailed information
        val locationText = if (latitudeStr.isNotEmpty() && longitudeStr.isNotEmpty()) {
            "📍 Location: Lat $latitudeStr, Lon $longitudeStr"
        } else {
            "📍 Location: Not available"
        }
        
        val detailedText = buildString {
            append("🚨 EMERGENCY ALERT 🚨\n\n")
            append("👤 Name: $fullName\n")
            append("📧 Email: $email\n")
            append("📞 Phone: $phoneNumber\n")
            append("$locationText\n\n")
            append("⚠️ This person needs immediate help!\n")
            append("Tap to view details and location on map.")
        }
        
        // Build notification with InboxStyle for better data display
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚨 Emergency Alert from $fullName")
            .setContentText("$fullName needs help! Tap to view location.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(detailedText)
                .setBigContentTitle("🚨 EMERGENCY ALERT")
                .setSummaryText("Tap to respond"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .setOnlyAlertOnce(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(android.graphics.Color.RED)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        
        Log.d(TAG, "Rich notification displayed with full alert details")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Receive emergency alerts from people you're protecting"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
