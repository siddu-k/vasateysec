package com.sriox.vasateysec

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.sriox.vasateysec.databinding.ActivityHomeBinding
import com.sriox.vasateysec.utils.FCMTokenManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var toggle: ActionBarDrawerToggle

    companion object {
        private const val RECORD_AUDIO_PERMISSION_CODE = 123
        private const val LOCATION_PERMISSION_CODE = 124
        private const val NOTIFICATION_PERMISSION_CODE = 125
        private const val ALL_PERMISSIONS_CODE = 126
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setupToolbar() // Removed - using new gradient header
        // setupNavigationDrawer() // Removed - using bottom nav
        setupQuickActions()
        setupBottomNavigation()
        
        // Highlight home (no specific nav item for home)
        com.sriox.vasateysec.utils.BottomNavHelper.highlightActiveItem(
            this,
            com.sriox.vasateysec.utils.BottomNavHelper.NavItem.NONE
        )
        
        // Ensure session is valid before loading profile
        ensureSessionValid()
        
        loadUserProfile()
        
        // Initialize FCM
        FCMTokenManager.initializeFCM(this)
        
        // Restore voice alert switch state
        restoreVoiceAlertState()
        
        // Request all necessary permissions
        requestAllPermissions()
    }
    
    private fun ensureSessionValid() {
        // Check if we have a local session
        if (!com.sriox.vasateysec.utils.SessionManager.isLoggedIn()) {
            // No local session - redirect to login
            android.util.Log.w("HomeActivity", "No session found, redirecting to login")
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        // We have local session, ensure Supabase session is restored
        lifecycleScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser == null) {
                    android.util.Log.w("HomeActivity", "Supabase session not available, will use local session")
                    // Don't logout - just log the warning
                    // The app will work with local session data
                } else {
                    android.util.Log.d("HomeActivity", "Supabase session active for: ${currentUser.id}")
                }
            } catch (e: Exception) {
                android.util.Log.w("HomeActivity", "Session check failed: ${e.message}")
                // Don't logout on error - continue with local session
            }
        }
    }

    // Removed - using new gradient header without toolbar
    // private fun setupToolbar() { ... }

    // Removed - using bottom navigation instead
    // private fun setupNavigationDrawer() { ... }

    private fun setupQuickActions() {
        // Voice Alert Card Click
        binding.voiceAlertCard.setOnClickListener {
            binding.voiceAlertSwitch.isChecked = !binding.voiceAlertSwitch.isChecked
        }
        
        // Voice Alert Switch
        binding.voiceAlertSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save state to SharedPreferences
            saveVoiceAlertState(isChecked)
            
            if (isChecked) {
                requestAudioPermission()
            } else {
                stopVoiceService()
            }
        }
        
        // Guardians Card
        binding.guardiansCard.setOnClickListener {
            startActivity(Intent(this, AddGuardianActivity::class.java))
        }
        
        // History Card
        binding.historyCard.setOnClickListener {
            startActivity(Intent(this, AlertHistoryActivity::class.java))
        }
        
        // My Alerts Card
        binding.myAlertsCard.setOnClickListener {
            startActivity(Intent(this, MyAlertsActivity::class.java))
        }
        
        // Settings Card
        binding.settingsCard.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // Logout Button
        binding.logoutButton.setOnClickListener {
            showLogoutDialog()
        }
    }
    
    private fun showLogoutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun setupBottomNavigation() {
        // Access bottom nav views via findViewById since they're in an included layout
        val navGuardians = findViewById<android.widget.LinearLayout>(R.id.navGuardians)
        val navHistory = findViewById<android.widget.LinearLayout>(R.id.navHistory)
        val sosButton = findViewById<com.google.android.material.card.MaterialCardView>(R.id.sosButton)
        val navGhistory = findViewById<android.widget.LinearLayout>(R.id.navGhistory)
        val navProfile = findViewById<android.widget.LinearLayout>(R.id.navProfile)
        
        // Guardians
        navGuardians?.setOnClickListener {
            val intent = Intent(this, AddGuardianActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
        
        // History
        navHistory?.setOnClickListener {
            android.util.Log.d("HomeActivity", "History button clicked")
            val intent = Intent(this, AlertHistoryActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
        
        // SOS Button (Center) - Manual Emergency Alert with Confirmation
        sosButton?.setOnClickListener {
            com.sriox.vasateysec.utils.SOSHelper.showSOSConfirmation(this)
        }
        
        // Track - Shows map
        navGhistory?.setOnClickListener {
            android.util.Log.d("HomeActivity", "Track button clicked")
            val intent = Intent(this, GuardianMapActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
        
        // Profile
        navProfile?.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }
    
    private fun saveVoiceAlertState(isEnabled: Boolean) {
        getSharedPreferences("vasatey_prefs", MODE_PRIVATE).edit()
            .putBoolean("voice_alert_enabled", isEnabled)
            .apply()
        android.util.Log.d("HomeActivity", "Voice alert state saved: $isEnabled")
    }
    
    private fun restoreVoiceAlertState() {
        // Check if service is actually running
        val isServiceRunning = isServiceRunning(VoskWakeWordService::class.java)
        val savedState = getSharedPreferences("vasatey_prefs", MODE_PRIVATE)
            .getBoolean("voice_alert_enabled", false)
        
        // Use actual service state, not saved state
        val actualState = isServiceRunning && savedState
        
        // Set switch state without triggering the listener
        binding.voiceAlertSwitch.setOnCheckedChangeListener(null)
        binding.voiceAlertSwitch.isChecked = actualState
        
        // Re-attach the listener after setting the state
        binding.voiceAlertSwitch.setOnCheckedChangeListener { _, checked ->
            saveVoiceAlertState(checked)
            if (checked) {
                requestAudioPermission()
            } else {
                stopVoiceService()
            }
        }
        
        android.util.Log.d("HomeActivity", "Voice alert state restored: $actualState (service running: $isServiceRunning)")
    }
    
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
    
    private fun stopVoiceService() {
        val serviceIntent = Intent(this, VoskWakeWordService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, "Voice alert stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun triggerManualEmergencyAlert() {
        lifecycleScope.launch {
            try {
                // Get current location
                val locationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
                
                if (ActivityCompat.checkSelfPermission(
                        this@HomeActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this@HomeActivity, "Location permission required", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Try to get last known location
                val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                
                val latitude = location?.latitude
                val longitude = location?.longitude
                val accuracy = location?.accuracy
                
                android.util.Log.d("HomeActivity", "Manual SOS: lat=$latitude, lon=$longitude, accuracy=$accuracy")
                
                // Send emergency alert using AlertManager
                val result = com.sriox.vasateysec.utils.AlertManager.sendEmergencyAlert(
                    context = this@HomeActivity,
                    latitude = latitude,
                    longitude = longitude,
                    locationAccuracy = accuracy
                )
                
                if (result.isSuccess) {
                    Toast.makeText(
                        this@HomeActivity,
                        "✅ Emergency alert sent to guardians!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@HomeActivity,
                        "❌ Failed to send alert: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeActivity", "Manual SOS failed", e)
                Toast.makeText(
                    this@HomeActivity,
                    "❌ Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser != null) {
                    val userProfile = SupabaseClient.client.from("users")
                        .select {
                            filter {
                                eq("id", currentUser.id)
                            }
                        }
                        .decodeSingle<Map<String, String>>()

                    val userName = userProfile["name"] ?: "User"
                    val userEmail = userProfile["email"] ?: ""
                    val wakeWord = userProfile["wake_word"] ?: "help me"

                    // Navigation drawer removed - using bottom nav now
                    // val headerView = binding.navigationView.getHeaderView(0)
                    // headerView.findViewById<TextView>(R.id.navHeaderName).text = userName
                    // headerView.findViewById<TextView>(R.id.navHeaderEmail).text = userEmail
                    
                    // Save wake word to SharedPreferences for VoskService
                    getSharedPreferences("vasatey_prefs", MODE_PRIVATE).edit()
                        .putString("wake_word", wakeWord.lowercase())
                        .apply()
                    
                    android.util.Log.d("HomeActivity", "Wake word loaded: $wakeWord")
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                arrayOf(Manifest.permission.RECORD_AUDIO), 
                RECORD_AUDIO_PERMISSION_CODE)
        } else {
            startListeningService()
        }
    }

    private fun startListeningService() {
        val serviceIntent = Intent(this, VoskWakeWordService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Toast.makeText(this, "Voice listening service started", Toast.LENGTH_SHORT).show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_add_guardian -> {
                startActivity(Intent(this, AddGuardianActivity::class.java))
            }
            R.id.nav_start_listening -> {
                requestAudioPermission()
            }
            R.id.nav_alert_history -> {
                startActivity(Intent(this, AlertHistoryActivity::class.java))
            }
            R.id.nav_edit_profile -> {
                startActivity(Intent(this, EditProfileActivity::class.java))
            }
            R.id.nav_logout -> {
                logout()
            }
        }
        // binding.drawerLayout.closeDrawer(GravityCompat.START) // Removed drawer
        return true
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                // Deactivate FCM token
                FCMTokenManager.deactivateFCMToken(this@HomeActivity)
                
                // Sign out from Supabase
                SupabaseClient.client.auth.signOut()
                
                // Clear local session
                com.sriox.vasateysec.utils.SessionManager.clearSession()
                
                // Navigate to login with clear task flags
                val intent = Intent(this@HomeActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Audio permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        
        // Camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        
        // Location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Request all missing permissions at once
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                ALL_PERMISSIONS_CODE
            )
        } else {
            // All permissions granted, request battery optimization exemption
            requestBatteryOptimizationExemption()
        }
    }
    
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
            
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = android.net.Uri.parse("package:$packageName")
                try {
                    startActivity(intent)
                    Toast.makeText(
                        this,
                        "Please disable battery optimization for reliable emergency alerts",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Unable to request battery optimization exemption",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            ALL_PERMISSIONS_CODE -> {
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                if (allGranted) {
                    Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
                    requestBatteryOptimizationExemption()
                } else {
                    Toast.makeText(
                        this,
                        "Some permissions were denied. The app may not work properly.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            RECORD_AUDIO_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startListeningService()
                } else {
                    Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Drawer removed - using bottom nav
        // if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
        //     binding.drawerLayout.closeDrawer(GravityCompat.START)
        // } else {
            super.onBackPressed()
        // }
    }
}
