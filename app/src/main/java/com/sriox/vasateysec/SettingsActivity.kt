package com.sriox.vasateysec

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sriox.vasateysec.databinding.ActivitySettingsBinding
import com.sriox.vasateysec.VoskWakeWordService
import com.sriox.vasateysec.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("vasatey_settings", MODE_PRIVATE)

        setupToolbar()
        loadSettings()
        setupSwitches()
        setupWakeWordCard()
        setupBottomNavigation()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Settings"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadSettings() {
        // Load saved settings
        binding.switchPhotoCapture.isChecked = prefs.getBoolean("photo_capture_enabled", true)
        binding.switchLocationTracking.isChecked = prefs.getBoolean("location_tracking_enabled", true)
        binding.switchVoiceAlert.isChecked = prefs.getBoolean("voice_alert_enabled", false)
        binding.switchAutoCall.isChecked = prefs.getBoolean("auto_call_enabled", false)
        binding.switchVibration.isChecked = prefs.getBoolean("vibration_enabled", true)
        binding.switchSound.isChecked = prefs.getBoolean("sound_enabled", true)
    }

    private fun setupSwitches() {
        // Photo Capture
        binding.switchPhotoCapture.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("photo_capture_enabled", isChecked).apply()
            showToast(if (isChecked) "Photo capture enabled" else "Photo capture disabled")
        }

        // Location Tracking
        binding.switchLocationTracking.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("location_tracking_enabled", isChecked).apply()
            showToast(if (isChecked) "Location tracking enabled" else "Location tracking disabled")
        }

        // Voice Alert
        binding.switchVoiceAlert.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("voice_alert_enabled", isChecked).apply()
            // Also update the main prefs for HomeActivity
            getSharedPreferences("vasatey_prefs", MODE_PRIVATE).edit()
                .putBoolean("voice_alert_enabled", isChecked)
                .apply()
            showToast(if (isChecked) "Voice alert enabled" else "Voice alert disabled")
        }

        // Auto Call
        binding.switchAutoCall.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_call_enabled", isChecked).apply()
            showToast(if (isChecked) "Auto call enabled" else "Auto call disabled")
        }

        // Vibration
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply()
            showToast(if (isChecked) "Vibration enabled" else "Vibration disabled")
        }

        // Sound
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
            showToast(if (isChecked) "Sound enabled" else "Sound disabled")
        }
    }
    
    private fun setupWakeWordCard() {
        // Load current wake word from database
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
                        .decodeSingle<Map<String, Any?>>()
                    
                    val wakeWord = userProfile["wake_word"] as? String ?: "help me"
                    binding.currentWakeWord.text = "Current: $wakeWord"
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsActivity", "Error loading wake word: ${e.message}")
            }
        }
        
        // Click listener to change wake word
        binding.wakeWordCard.setOnClickListener {
            showWakeWordDialog()
        }
    }
    
    private fun showWakeWordDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Enter new wake word"
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Change Wake Word")
            .setMessage("Enter a word or phrase to trigger emergency alert")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newWakeWord = input.text.toString().trim()
                if (newWakeWord.isNotEmpty()) {
                    saveWakeWord(newWakeWord)
                } else {
                    showToast("Wake word cannot be empty")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun saveWakeWord(wakeWord: String) {
        lifecycleScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser != null) {
                    SupabaseClient.client.from("users")
                        .update({
                            set("wake_word", wakeWord)
                        }) {
                            filter {
                                eq("id", currentUser.id)
                            }
                        }
                    
                    binding.currentWakeWord.text = "Current: $wakeWord"
                    showToast("Wake word updated to: \"$wakeWord\"")
                    
                    // Restart voice service to use new wake word
                    val serviceIntent = Intent(this@SettingsActivity, VoskWakeWordService::class.java)
                    stopService(serviceIntent)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        startService(serviceIntent)
                    }, 500)
                }
            } catch (e: Exception) {
                showToast("Failed to update wake word: ${e.message}")
            }
        }
    }

    private fun setupBottomNavigation() {
        val navGuardians = findViewById<android.widget.LinearLayout>(R.id.navGuardians)
        val navHistory = findViewById<android.widget.LinearLayout>(R.id.navHistory)
        val sosButton = findViewById<com.google.android.material.card.MaterialCardView>(R.id.sosButton)
        val navGhistory = findViewById<android.widget.LinearLayout>(R.id.navGhistory)
        val navProfile = findViewById<android.widget.LinearLayout>(R.id.navProfile)

        navGuardians?.setOnClickListener {
            startActivity(Intent(this, AddGuardianActivity::class.java))
            finish()
        }

        navHistory?.setOnClickListener {
            startActivity(Intent(this, AlertHistoryActivity::class.java))
            finish()
        }

        sosButton?.setOnClickListener {
            com.sriox.vasateysec.utils.SOSHelper.showSOSConfirmation(this)
        }

        navGhistory?.setOnClickListener {
            startActivity(Intent(this, GuardianMapActivity::class.java))
            finish()
        }

        navProfile?.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
            finish()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
