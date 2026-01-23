package com.sriox.vasateysec

import android.content.Context
import com.sriox.vasateysec.config.ApiConfig
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

object SupabaseClient {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    lateinit var client: SupabaseClientType
        private set
    
    fun initialize(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = ApiConfig.supabaseUrl,
            supabaseKey = ApiConfig.supabaseAnonKey
        ) {
            // Configure HTTP client with increased timeouts
            httpEngine = OkHttp.create {
                preconfigured = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .callTimeout(60, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build()
            }
            
            install(Auth) {
                // Enable session persistence with SharedPreferences
                scheme = "app"
                host = "supabase.com"
                // Session will be automatically saved and restored
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }
            install(Postgrest) {
                serializer = io.github.jan.supabase.serializer.KotlinXSerializer(json)
            }
            install(Storage)
        }
    }
}
