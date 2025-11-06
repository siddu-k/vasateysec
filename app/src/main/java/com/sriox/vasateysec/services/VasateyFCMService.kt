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
        
        // Handle data payload
        message.data.let { data ->
            if (data.isNotEmpty()) {
                Log.d(TAG, "Message data payload: $data")
                handleDataMessage(data)
            }
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "Emergency Alert"
        val body = data["body"] ?: "Someone needs help"
        val fullName = data["fullName"] ?: ""
        val email = data["email"] ?: ""
        val phoneNumber = data["phoneNumber"] ?: ""
        // Backend sends 'lastKnownLatitude' and 'lastKnownLongitude'
        val latitudeStr = data["lastKnownLatitude"] ?: data["latitude"] ?: ""
        val longitudeStr = data["lastKnownLongitude"] ?: data["longitude"] ?: ""
        val alertType = data["alertType"] ?: "emergency"
        val timestamp = data["timestamp"] ?: ""
        val isSelfAlert = data["isSelfAlert"]?.toBoolean() ?: false
        
        Log.d(TAG, "Notification data: lat=$latitudeStr, lon=$longitudeStr, isSelf=$isSelfAlert")

        // Don't show notification for self alerts (user already knows they triggered it)
        if (isSelfAlert) {
            Log.d(TAG, "Skipping notification display for self alert")
            return
        }

        // Create intent to open HelpRequestActivity
        val intent = Intent(this, HelpRequestActivity::class.java).apply {
            // Use SINGLE_TOP to avoid clearing the activity stack
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("fullName", fullName)
            putExtra("email", email)
            putExtra("phoneNumber", phoneNumber)
            putExtra("latitude", latitudeStr)
            putExtra("longitude", longitudeStr)
            putExtra("alertType", alertType)
            putExtra("timestamp", timestamp)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$body\n\nFrom: $fullName\nPhone: $phoneNumber"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
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
