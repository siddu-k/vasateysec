package com.sriox.vasateysec.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null
)
