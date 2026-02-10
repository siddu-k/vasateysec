package com.sriox.vasateysec.config

import com.sriox.vasateysec.BuildConfig

/**
 * Centralized API Configuration
 * 
 * This object provides a single source of truth for all API-related configuration.
 * Values are read from BuildConfig, which is populated from local.properties at build time.
 * 
 * Benefits:
 * - No hardcoded credentials in source code
 * - Easy to update credentials without modifying code
 * - Type-safe access to configuration
 * - Single place to manage all API endpoints
 * 
 * @see local.properties.example for configuration template
 * @see API_CONFIGURATION_GUIDE.md for detailed setup instructions
 */
object ApiConfig {
    
    /**
     * Supabase project URL
     * Example: https://your-project.supabase.co
     */
    val supabaseUrl: String
        get() = BuildConfig.SUPABASE_URL
    
    /**
     * Supabase anonymous (public) key
     * This key is safe to use in client applications as it only allows
     * operations permitted by Row Level Security (RLS) policies
     */
    val supabaseAnonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY
    
    /**
     * FCM notification endpoint URL
     * This is the Vercel/Cloud Function endpoint used to send push notifications
     * Example: https://your-service.vercel.app/api/sendNotification
     */
    val fcmEndpoint: String
        get() = BuildConfig.FCM_ENDPOINT
    
    /**
     * Validates that all required configuration is present
     * Call this during app initialization to fail fast if configuration is missing
     * 
     * @throws IllegalStateException if any required configuration is missing or invalid
     */
    fun validate() {
        require(supabaseUrl.isNotBlank()) { 
            "SUPABASE_URL is not configured. Please check local.properties" 
        }
        require(supabaseAnonKey.isNotBlank()) { 
            "SUPABASE_ANON_KEY is not configured. Please check local.properties" 
        }
        require(fcmEndpoint.isNotBlank()) { 
            "FCM_ENDPOINT is not configured. Please check local.properties" 
        }
        
        // Additional validation for URL format
        require(supabaseUrl.startsWith("https://")) {
            "SUPABASE_URL must start with https://"
        }
        require(fcmEndpoint.startsWith("https://")) {
            "FCM_ENDPOINT must start with https://"
        }
    }
}
