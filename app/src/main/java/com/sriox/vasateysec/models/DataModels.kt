package com.sriox.vasateysec.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String
)

@Serializable
data class Guardian(
    val id: String? = null,
    val user_id: String,
    val guardian_email: String,
    val guardian_user_id: String? = null,
    val status: String = "active"
)

@Serializable
data class FCMToken(
    val id: String? = null,
    val user_id: String,
    val token: String,
    val device_id: String? = null,
    val device_name: String? = null,
    val platform: String = "android",
    val is_active: Boolean = true
)

@Serializable
data class AlertHistory(
    val id: String? = null,
    val user_id: String,
    val user_name: String,
    val user_email: String,
    val user_phone: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location_accuracy: Float? = null,
    val alert_type: String = "voice_help",
    val status: String = "sent",
    val created_at: String? = null
)

@Serializable
data class AlertRecipient(
    val id: String? = null,
    val alert_id: String,
    val guardian_email: String,
    val guardian_user_id: String? = null,
    val fcm_token: String? = null,
    val notification_sent: Boolean = false,
    val notification_delivered: Boolean = false,
    val viewed_at: String? = null
)

@Serializable
data class Notification(
    val id: String? = null,
    val alert_id: String,
    val token: String,
    val title: String,
    val body: String,
    val recipient_email: String,
    val is_self_alert: Boolean = false,
    val status: String = "pending"
)
