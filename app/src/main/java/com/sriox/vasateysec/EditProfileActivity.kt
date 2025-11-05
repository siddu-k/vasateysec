package com.sriox.vasateysec

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sriox.vasateysec.databinding.ActivityEditProfileBinding
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadProfile()

        binding.saveButton.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()
            val phone = binding.phoneInput.text.toString().trim()
            val wakeWord = binding.wakeWordInput.text.toString().trim()

            if (validateInputs(name, phone, wakeWord)) {
                updateProfile(name, phone, wakeWord)
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

                val userProfile = SupabaseClient.client.from("users")
                    .select {
                        filter {
                            eq("id", currentUser.id)
                        }
                    }
                    .decodeSingle<Map<String, String>>()

                binding.nameInput.setText(userProfile["name"])
                binding.emailInput.setText(userProfile["email"])
                binding.phoneInput.setText(userProfile["phone"])
                binding.wakeWordInput.setText(userProfile["wake_word"] ?: "help me")

            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
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

    private fun updateProfile(name: String, phone: String, wakeWord: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.saveButton.isEnabled = false

        lifecycleScope.launch {
            try {
                if (userId == null) return@launch

                SupabaseClient.client.from("users").update({
                    set("name", name)
                    set("phone", phone)
                    set("wake_word", wakeWord.lowercase())
                }) {
                    filter {
                        eq("id", userId!!)
                    }
                }
                
                // Save to SharedPreferences for quick access
                getSharedPreferences("vasatey_prefs", MODE_PRIVATE).edit()
                    .putString("wake_word", wakeWord.lowercase())
                    .apply()

                Toast.makeText(this@EditProfileActivity, "Profile updated! Wake word: \"$wakeWord\"", Toast.LENGTH_LONG).show()
                finish()

            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.saveButton.isEnabled = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
