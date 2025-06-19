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

        // Inisialisasi dengan error handling
        initializeComponents()

        // Cek session yang ada
        if (checkExistingSession()) {
            return // Jika ada session valid dan berhasil redirect, keluar
        }

        // Setup UI jika tidak ada session
        setupUI()
    }

    private fun initializeComponents() {
        try {
            dbHelper = DatabaseHelper(this)
            sessionManager = SessionManager(this)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error initializing components: ${e.message}")
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
                    // Session tidak valid, hapus
                    sessionManager.clearSession()
                }
            }
            false
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error checking session: ${e.message}")
            // Jika ada error, hapus session yang mungkin corrupt
            try {
                sessionManager.clearSession()
            } catch (clearError: Exception) {
                Log.e("LoginActivity", "Error clearing session: ${clearError.message}")
            }
            false
        }
    }

    private fun validateUserInDatabase(user: User): Boolean {
        return try {
            // Cek apakah username masih ada di database
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT id, role FROM users WHERE username = ?",
                arrayOf(user.username)
            )
            val isValid = if (cursor.moveToFirst()) {
                val dbUserId = cursor.getInt(0)
                val dbUserRole = cursor.getString(1)
                dbUserId == user.id && dbUserRole == user.role
            } else {
                false
            }
            cursor.close()
            isValid
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error validating user: ${e.message}")
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
            Log.e("LoginActivity", "Error setting up UI: ${e.message}")
            // Jika gagal setup UI, coba restart activity
            recreate()
        }
    }

    private fun setupClickListeners() {
        try {
            binding.btnLogin.setOnClickListener {
                performLogin()
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error setting up click listeners: ${e.message}")
        }
    }

    private fun setupPasswordToggle() {
        try {
            binding.ivTogglePassword.setOnClickListener {
                togglePasswordVisibility()
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error setting up password toggle: ${e.message}")
        }
    }

    private fun togglePasswordVisibility() {
        try {
            if (isPasswordVisible) {
                // Sembunyikan password
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.ic_eye_closed)
                isPasswordVisible = false
            } else {
                // Tampilkan password
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.ic_eye_open)
                isPasswordVisible = true
            }

            // Pindahkan cursor ke akhir teks
            binding.etPassword.setSelection(binding.etPassword.text.length)

        } catch (e: Exception) {
            Log.e("LoginActivity", "Error toggling password visibility: ${e.message}")
        }
    }

    private fun performLogin() {
        try {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            hideError()

            if (username.isEmpty() || password.isEmpty()) {
                showError("Harap isi semua kolom")
                return
            }

            val user = dbHelper.authenticateUser(username, password)
            if (user != null) {
                handleLoginSuccess(user)
            } else {
                showError("Username atau password salah")
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error during login: ${e.message}")
            showError("Terjadi kesalahan saat login")
        }
    }

    private fun handleLoginSuccess(user: User) {
        try {
            sessionManager.saveSession(user)
            Log.d("LoginActivity", "Session saved for user: ${user.username}, role: ${user.role}, id: ${user.id}")
            redirectToMainActivity(user)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error handling login success: ${e.message}")
            showError("Terjadi kesalahan saat menyimpan session")
        }
    }

    private fun redirectToMainActivity(user: User) {
        try {
            val intent = when (user.role) {
                "admin" -> Intent(this, AdminActivity::class.java)
                "worker" -> {
                    Intent(this, WorkerDashboardActivity::class.java).apply {
                        putExtra("WORKER_ID", user.workerId)
                    }
                }
                else -> {
                    showError("Peran tidak valid")
                    return
                }
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error redirecting: ${e.message}")
            showError("Terjadi kesalahan saat membuka halaman utama")
        }
    }

    private fun showError(message: String) {
        try {
            if (::binding.isInitialized) {
                binding.tvErrorMessage.text = message
                binding.tvErrorMessage.visibility = android.view.View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error showing error: ${e.message}")
        }
    }

    private fun hideError() {
        try {
            if (::binding.isInitialized) {
                binding.tvErrorMessage.visibility = android.view.View.GONE
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error hiding error: ${e.message}")
        }
    }
}