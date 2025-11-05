package com.sriox.vasateysec

import android.app.Application

class VasateyApplication : Application() {
    
    companion object {
        lateinit var instance: VasateyApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Supabase client with application context
        SupabaseClient.initialize(this)
    }
}
