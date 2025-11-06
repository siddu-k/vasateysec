package com.sriox.vasateysec

import android.app.Application
import com.sriox.vasateysec.utils.SessionManager

class VasateyApplication : Application() {
    
    companion object {
        lateinit var instance: VasateyApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize SessionManager first
        SessionManager.initialize(this)
        
        // Initialize Supabase client with application context
        SupabaseClient.initialize(this)
    }
}
