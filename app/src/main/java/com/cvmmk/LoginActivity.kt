package com.cvmmk

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.cvmmk.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize components (DatabaseHelper and SessionManager)
        if (!initializeComponents()) {
            showFatalError("Gagal menginisialisasi komponen aplikasi")
            return
        }

        // Check for existing valid session
        if (checkExistingSession()) {
            return // Exit if session is valid and redirect succeeds
        }

        // Setup UI for login
        setupUI()
    }

    private fun initializeComponents(): Boolean {
        return try {
            dbHelper = DatabaseHelper(this)
            sessionManager = SessionManager(this)
            true
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error initializing components: ${e.message}", e)
            false
        }
    }

    private fun checkExistingSession(): Boolean {
        return try {
            if (sessionManager.isLoggedIn()) {
                val user = sessionManager.getLoggedInUser()
                if (user != null && validateUserInDatabase(user)) {
                    redirectToMainActivity(user)
                    return true
                } else {
                    // Invalid session, clear it
                    sessionManager.clearSession()
                    Log.w("LoginActivity", "Invalid session detected, cleared")
                }
            }
            false
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error checking session: ${e.message}", e)
            sessionManager.clearSession()
            false
        }
    }

    private fun validateUserInDatabase(user: User): Boolean {
        return try {
            // Verify user exists in database with matching ID and role
            val dbUser = dbHelper.getUserById(user.id)
            dbUser != null && dbUser.username == user.username && dbUser.role == user.role
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error validating user: ${e.message}", e)
            false
        }
    }

    private fun setupUI() {
        try {
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)
            setupClickListeners()
            setupPasswordToggle()
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error setting up UI: ${e.message}", e)
            showFatalError("Gagal menyiapkan antarmuka pengguna")
        }
    }

    private fun setupClickListeners() {
        try {
            binding.btnLogin.setOnClickListener {
                performLogin()
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error setting up click listeners: ${e.message}", e)
            showError("Gagal mengatur tombol login")
        }
    }

    private fun setupPasswordToggle() {
        try {
            binding.ivTogglePassword.setOnClickListener {
                togglePasswordVisibility()
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error setting up password toggle: ${e.message}", e)
            showError("Gagal mengatur toggle kata sandi")
        }
    }

    private fun togglePasswordVisibility() {
        try {
            if (isPasswordVisible) {
                // Hide password
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.ic_eye_closed)
                isPasswordVisible = false
            } else {
                // Show password
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.ic_eye_open)
                isPasswordVisible = true
            }
            // Move cursor to end of text
            binding.etPassword.setSelection(binding.etPassword.text.length)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error toggling password visibility: ${e.message}", e)
            showError("Gagal mengubah visibilitas kata sandi")
        }
    }

    private fun performLogin() {
        try {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            hideError()

            if (username.isEmpty() || password.isEmpty()) {
                showError("Harap isi username dan kata sandi")
                return
            }

            val user = dbHelper.authenticateUser(username, password)
            if (user != null) {
                handleLoginSuccess(user)
            } else {
                showError("Username atau kata sandi salah")
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error during login: ${e.message}", e)
            showError("Gagal login: Terjadi kesalahan pada sistem")
        }
    }

    private fun handleLoginSuccess(user: User) {
        try {
            sessionManager.saveSession(user)
            Log.d("LoginActivity", "Session saved for user: ${user.username}, role: ${user.role}, id: ${user.id}")
            redirectToMainActivity(user)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error handling login success: ${e.message}", e)
            showError("Gagal menyimpan sesi login")
        }
    }

    private fun redirectToMainActivity(user: User) {
        try {
            val intent = when (user.role) {
                "admin" -> Intent(this, AdminActivity::class.java)
                "worker" -> Intent(this, WorkerDashboardActivity::class.java).apply {
                    putExtra("WORKER_ID", user.workerId)
                }
                "owner" -> Intent(this, OwnerDashboardActivity::class.java)
                else -> {
                    showError("Peran pengguna tidak dikenali")
                    return
                }
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error redirecting to main activity: ${e.message}", e)
            showError("Gagal membuka halaman utama")
        }
    }

    private fun showError(message: String) {
        try {
            if (::binding.isInitialized) {
                binding.tvErrorMessage.text = message
                binding.tvErrorMessage.visibility = android.view.View.VISIBLE
            } else {
                Log.w("LoginActivity", "Binding not initialized, cannot show error: $message")
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error showing error message: ${e.message}", e)
        }
    }

    private fun showFatalError(message: String) {
        try {
            if (::binding.isInitialized) {
                binding.tvErrorMessage.text = "$message. Silakan coba lagi nanti."
                binding.tvErrorMessage.visibility = android.view.View.VISIBLE
                binding.btnLogin.isEnabled = false
            }
            Log.e("LoginActivity", "Fatal error: $message")
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error showing fatal error: ${e.message}", e)
        }
    }

    private fun hideError() {
        try {
            if (::binding.isInitialized) {
                binding.tvErrorMessage.visibility = android.view.View.GONE
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error hiding error message: ${e.message}", e)
        }
    }
}