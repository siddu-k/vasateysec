package com.sriox.vasateysec.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val wake_word: String? = null,
    val cancel_password: String? = null
)
