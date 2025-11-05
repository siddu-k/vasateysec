package com.sriox.vasateysec

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sriox.vasateysec.databinding.ActivitySignupBinding
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String
)

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signupButton.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val phone = binding.phoneInput.text.toString().trim()
            val password = binding.passwordInput.text.toString()

            if (validateInputs(name, email, phone, password)) {
                signUp(name, email, phone, password)
            }
        }

        binding.loginPrompt.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(name: String, email: String, phone: String, password: String): Boolean {
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Name is required"
            return false
        }
        binding.nameInputLayout.error = null

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Valid email is required"
            return false
        }
        binding.emailInputLayout.error = null

        if (phone.isEmpty()) {
            binding.phoneInputLayout.error = "Phone number is required"
            return false
        }
        binding.phoneInputLayout.error = null

        if (password.length < 6) {
            binding.passwordInputLayout.error = "Password must be at least 6 characters"
            return false
        }
        binding.passwordInputLayout.error = null

        return true
    }

    private fun signUp(name: String, email: String, phone: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.signupButton.isEnabled = false

        lifecycleScope.launch {
            try {
                // Sign up with Supabase Auth
                SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                // Get the user ID
                val currentUser = SupabaseClient.client.auth.currentUserOrNull()
                    ?: throw Exception("Failed to get user ID after signup")

                // Store additional user data in the database
                SupabaseClient.client.from("users").upsert(
                    mapOf(
                        "id" to currentUser.id,
                        "name" to name,
                        "email" to email,
                        "phone" to phone
                    )
                )

                Toast.makeText(
                    this@SignupActivity,
                    "Sign up successful! Please check your email to verify your account.",
                    Toast.LENGTH_LONG
                ).show()

                // Navigate to login
                startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                finish()

            } catch (e: Exception) {
                Toast.makeText(
                    this@SignupActivity,
                    "Sign up failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.signupButton.isEnabled = true
            }
        }
    }
}
