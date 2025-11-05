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

        setupToolbar()
        setupNavigationDrawer()
        setupQuickActions()
        loadUserProfile()
        
        // Initialize FCM
        FCMTokenManager.initializeFCM(this)
        
        // Request all necessary permissions
        requestAllPermissions()
    }

    private fun setupToolbar() {
        // Make sure we're using the Toolbar for the ActionBar
        setSupportActionBar(binding.toolbar)
        
        // Configure the ActionBar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeButtonEnabled(true)
            title = "Vasatey Safety"
            
            // Set the navigation icon
            binding.toolbar.navigationIcon = ContextCompat.getDrawable(
                this@HomeActivity,
                R.drawable.ic_menu
            )
        }
        
        // Handle navigation icon clicks
        binding.toolbar.setNavigationOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        
        // Set the title text appearance
        binding.toolbar.setTitleTextAppearance(this, android.R.style.TextAppearance_Medium)
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white))
    }

    private fun setupNavigationDrawer() {
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)
    }

    private fun setupQuickActions() {
        binding.btnAddGuardian.setOnClickListener {
            startActivity(Intent(this, AddGuardianActivity::class.java))
        }

        binding.btnStartListening.setOnClickListener {
            requestAudioPermission()
        }

        binding.btnAlertHistory.setOnClickListener {
            startActivity(Intent(this, AlertHistoryActivity::class.java))
        }

        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
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

                    binding.welcomeText.text = "Welcome, $userName"

                    // Update navigation header
                    val headerView = binding.navigationView.getHeaderView(0)
                    headerView.findViewById<TextView>(R.id.navHeaderName).text = userName
                    headerView.findViewById<TextView>(R.id.navHeaderEmail).text = userEmail
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
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                // Deactivate FCM token
                FCMTokenManager.deactivateFCMToken(this@HomeActivity)
                
                // Sign out from Supabase
                SupabaseClient.client.auth.signOut()
                
                // Navigate to login
                startActivity(Intent(this@HomeActivity, LoginActivity::class.java))
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

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
