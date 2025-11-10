package com.sriox.vasateysec

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sriox.vasateysec.databinding.ActivityEditProfileBinding
import com.sriox.vasateysec.models.UserProfile
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setSupportActionBar(binding.toolbar) // Removed - using gradient header
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupBottomNavigation()
        loadProfile()

        // Highlight Profile nav item
        com.sriox.vasateysec.utils.BottomNavHelper.highlightActiveItem(
            this,
            com.sriox.vasateysec.utils.BottomNavHelper.NavItem.PROFILE
        )

        // Back button
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.saveButton.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()
            val phone = binding.phoneInput.text.toString().trim()
            val wakeWord = binding.wakeWordInput.text.toString().trim()
            val cancelPassword = binding.cancelPasswordInput.text.toString().trim()

            if (validateInputs(name, phone, wakeWord)) {
                updateProfile(name, phone, wakeWord, cancelPassword)
            }
        }

        binding.logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun setupBottomNavigation() {
        val navGuardians = findViewById<android.widget.LinearLayout>(R.id.navGuardians)
        val navHistory = findViewById<android.widget.LinearLayout>(R.id.navHistory)
        val sosButton = findViewById<com.google.android.material.card.MaterialCardView>(R.id.sosButton)
        val navGhistory = findViewById<android.widget.LinearLayout>(R.id.navGhistory)
        val navProfile = findViewById<android.widget.LinearLayout>(R.id.navProfile)

        navGuardians?.setOnClickListener {
            startActivity(android.content.Intent(this, AddGuardianActivity::class.java))
        }
        navHistory?.setOnClickListener {
            startActivity(android.content.Intent(this, AlertHistoryActivity::class.java))
        }
        sosButton?.setOnClickListener {
            com.sriox.vasateysec.utils.SOSHelper.showSOSConfirmation(this)
        }
        navGhistory?.setOnClickListener {
            startActivity(android.content.Intent(this, AlertHistoryActivity::class.java))
        }
        navProfile?.setOnClickListener { /* Already here */ }
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                // Sign out from Supabase
                SupabaseClient.client.auth.signOut()

                // Clear local session
                com.sriox.vasateysec.utils.SessionManager.clearSession()

                // Redirect to login
                val intent = android.content.Intent(this@EditProfileActivity, LoginActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                if (currentUser == null) {
                    Toast.makeText(this@EditProfileActivity, "User not logged in", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                userId = currentUser.id
                android.util.Log.d("EditProfile", "Loading profile for user ID: ${currentUser.id}")

                val userProfile = try {
                    SupabaseClient.client.from("users")
                        .select {
                            filter {
                                eq("id", currentUser.id)
                            }
                        }
                        .decodeSingle<UserProfile>()
                } catch (e: Exception) {
                    android.util.Log.e("EditProfile", "Error loading profile: ${e.message}", e)
                    Toast.makeText(this@EditProfileActivity, "Error loading profile: ${e.message}", Toast.LENGTH_LONG).show()
                    // Create a default profile as a fallback
                    UserProfile(
                        id = currentUser.id,
                        name = currentUser.email?.substringBefore("@") ?: "User",
                        email = currentUser.email ?: "",
                        phone = "",
                        wake_word = "help me",
                        cancel_password = ""
                    )
                }

                // Log the entire profile data for debugging
                android.util.Log.d("EditProfile", "Profile data loaded: $userProfile")
                android.util.Log.d("EditProfile", "Phone value: '${userProfile.phone}'")
                android.util.Log.d("EditProfile", "Name: '${userProfile.name}'")
                android.util.Log.d("EditProfile", "Email: '${userProfile.email}'")
                android.util.Log.d("EditProfile", "Wake word: '${userProfile.wake_word}'")

                binding.nameInput.setText(userProfile.name ?: "")
                binding.emailInput.setText(userProfile.email ?: "")
                binding.phoneInput.setText(userProfile.phone ?: "")
                binding.wakeWordInput.setText(userProfile.wake_word ?: "help me")
                binding.cancelPasswordInput.setText(userProfile.cancel_password ?: "")

            } catch (e: Exception) {
                android.util.Log.e("EditProfile", "Failed to load profile", e)
                Toast.makeText(this@EditProfileActivity, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInputs(name: String, phone: String, wakeWord: String): Boolean {
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Name is required"
            return false
        }
        binding.nameInputLayout.error = null

        if (phone.isEmpty()) {
            binding.phoneInputLayout.error = "Phone number is required"
            return false
        }
        binding.phoneInputLayout.error = null

        if (wakeWord.isEmpty()) {
            binding.wakeWordInputLayout.error = "Wake word is required"
            return false
        }
        if (wakeWord.length < 3) {
            binding.wakeWordInputLayout.error = "Wake word must be at least 3 characters"
            return false
        }
        binding.wakeWordInputLayout.error = null

        return true
    }

    private fun updateProfile(name: String, phone: String, wakeWord: String, cancelPassword: String) {
        // binding.progressBar.visibility = View.VISIBLE // Removed from new layout
        binding.saveButton.isEnabled = false

        lifecycleScope.launch {
            try {
                if (userId == null) return@launch

                SupabaseClient.client.from("users").update({
                    set("name", name)
                    set("phone", phone)
                    set("wake_word", wakeWord.lowercase())
                    if (cancelPassword.isNotEmpty()) {
                        set("cancel_password", cancelPassword)
                    }
                }) {
                    filter {
                        eq("id", userId!!)
                    }
                }

                // Save to SharedPreferences for quick access
                getSharedPreferences("vasatey_prefs", MODE_PRIVATE).edit()
                    .putString("wake_word", wakeWord.lowercase())
                    .apply()

                // Restart VoskWakeWordService to use the new wake word
                try {
                    val serviceIntent = android.content.Intent(this@EditProfileActivity, VoskWakeWordService::class.java)
                    stopService(serviceIntent) // Stop old service
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        startService(serviceIntent) // Start with new wake word
                    }, 500) // Wait 500ms before restarting

                    Toast.makeText(this@EditProfileActivity, "Profile updated! Wake word changed to: \"$wakeWord\"\nService restarting...", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this@EditProfileActivity, "Profile updated! Please restart the app to use new wake word.", Toast.LENGTH_LONG).show()
                }

                finish()

            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                // binding.progressBar.visibility = View.GONE // Removed from new layout
                binding.saveButton.isEnabled = true
            }
        }
    }

    // Removed - using back button in gradient header
    // override fun onSupportNavigateUp(): Boolean {
    //     finish()
    //     return true
    // }
}
