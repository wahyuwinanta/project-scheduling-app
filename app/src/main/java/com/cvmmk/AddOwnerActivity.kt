package com.cvmmk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cvmmk.databinding.ActivityAddOwnerBinding

class AddOwnerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddOwnerBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private var adminId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddOwnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            // Initialize components
            dbHelper = DatabaseHelper(this)
            sessionManager = SessionManager(this)

            // Validate admin session
            val user = sessionManager.getLoggedInUser()
            if (user == null || !sessionManager.isLoggedIn() || user.role != "admin") {
                Log.e("AddOwnerActivity", "Invalid session or user is not an admin")
                Toast.makeText(this, "Silakan login sebagai admin terlebih dahulu", Toast.LENGTH_SHORT).show()
                redirectToLogin()
                return
            }
            adminId = user.id
            Log.d("AddOwnerActivity", "Admin ID: $adminId, username: ${user.username}")

            // Apply entrance animations
            applyAnimations()

            // Setup save button
            binding.btnSaveOwner.setOnClickListener {
                saveOwner()
            }
        } catch (e: Exception) {
            Log.e("AddOwnerActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat halaman", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun redirectToLogin() {
        try {
            sessionManager.clearSession()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("AddOwnerActivity", "Error redirecting to login: ${e.message}", e)
            finish()
        }
    }

    private fun applyAnimations() {
        try {
            val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            binding.cardInputs.startAnimation(slideUp)
            binding.btnSaveOwner.startAnimation(slideUp)
        } catch (e: Exception) {
            Log.e("AddOwnerActivity", "Error applying animations: ${e.message}", e)
        }
    }

    private fun saveOwner() {
        try {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            var isValid = true

            // Clear previous errors
            binding.tilUsername.error = null
            binding.tilPassword.error = null

            // Validate inputs
            if (username.isEmpty()) {
                binding.tilUsername.error = "Username wajib diisi"
                isValid = false
            }
            if (password.isEmpty()) {
                binding.tilPassword.error = "Kata sandi wajib diisi"
                isValid = false
            } else if (password.length < 6) {
                binding.tilPassword.error = "Kata sandi harus minimal 6 karakter"
                isValid = false
            }

            if (!isValid) {
                shakeInvalidFields()
                Toast.makeText(this, "Harap isi semua kolom dengan benar", Toast.LENGTH_SHORT).show()
                return
            }

            // Add owner
            val ownerId = dbHelper.addOwner(adminId, username, password)
            if (ownerId != -1L) {
                Log.d("AddOwnerActivity", "Owner added successfully, ID: $ownerId")
                // Animate save button
                binding.btnSaveOwner.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(100)
                    .withEndAction {
                        binding.btnSaveOwner.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
                Toast.makeText(this, "Akun owner berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                binding.tilUsername.error = "Username mungkin sudah digunakan"
                shakeInvalidFields()
                Toast.makeText(this, "Gagal menambahkan akun owner", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("AddOwnerActivity", "Error saving owner: ${e.message}", e)
            Toast.makeText(this, "Kesalahan: Gagal menyimpan akun", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shakeInvalidFields() {
        try {
            val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
            if (binding.etUsername.text.isNullOrEmpty()) binding.tilUsername.startAnimation(shake)
            if (binding.etPassword.text.isNullOrEmpty() || binding.etPassword.text.toString().length < 6) binding.tilPassword.startAnimation(shake)
        } catch (e: Exception) {
            Log.e("AddOwnerActivity", "Error shaking invalid fields: ${e.message}", e)
        }
    }
}