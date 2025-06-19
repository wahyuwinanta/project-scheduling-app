package com.cvmmk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cvmmk.databinding.ActivityOwnerAccountsBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class OwnerAccountsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOwnerAccountsBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: OwnerAccountAdapter
    private var adminId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            // Initialize components
            dbHelper = DatabaseHelper(this)
            sessionManager = SessionManager(this)

            // Validate admin session
            val user = sessionManager.getLoggedInUser()
            if (user == null || !sessionManager.isLoggedIn() || user.role != "admin") {
                Log.e("OwnerAccountsActivity", "Invalid session or user is not an admin")
                Toast.makeText(this, "Silakan login sebagai admin terlebih dahulu", Toast.LENGTH_SHORT).show()
                redirectToLogin()
                return
            }
            adminId = user.id
            Log.d("OwnerAccountsActivity", "Admin ID: $adminId, username: ${user.username}")

            // Set up RecyclerView
            binding.rvOwnerAccounts.layoutManager = LinearLayoutManager(this)
            loadOwnerAccounts()

            // Back button
            binding.btnBack.setOnClickListener {
                finish()
            }
        } catch (e: Exception) {
            Log.e("OwnerAccountsActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat daftar akun", Toast.LENGTH_SHORT).show()
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
            Log.e("OwnerAccountsActivity", "Error redirecting to login: ${e.message}", e)
            finish()
        }
    }

    private fun loadOwnerAccounts() {
        try {
            val owners = dbHelper.getAllOwners(adminId)
            if (::adapter.isInitialized) {
                adapter.updateData(owners)
                Log.d("OwnerAccountsActivity", "Updated adapter with ${owners.size} owners")
            } else {
                adapter = OwnerAccountAdapter(
                    owners,
                    onEditClick = { owner -> showEditDialog(owner) },
                    onDeleteClick = { owner ->
                        AlertDialog.Builder(this)
                            .setTitle("Hapus Akun")
                            .setMessage("Apakah Anda yakin ingin menghapus akun ${owner.username}?")
                            .setPositiveButton("Hapus") { _, _ ->
                                try {
                                    val success = dbHelper.deleteOwner(adminId, owner.id)
                                    if (success) {
                                        Toast.makeText(this, "Akun berhasil dihapus", Toast.LENGTH_SHORT).show()
                                        loadOwnerAccounts()
                                    } else {
                                        Toast.makeText(this, "Gagal menghapus akun", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("OwnerAccountsActivity", "Error deleting owner: ${e.message}", e)
                                    Toast.makeText(this, "Kesalahan: Gagal menghapus akun", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton("Batal", null)
                            .show()
                    }
                )
                binding.rvOwnerAccounts.adapter = adapter
                Log.d("OwnerAccountsActivity", "Initialized adapter with ${owners.size} owners")
            }
        } catch (e: Exception) {
            Log.e("OwnerAccountsActivity", "Error loading owners: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat daftar akun", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDialog(owner: User) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_owner_account, null)
        val tilUsername = dialogView.findViewById<TextInputLayout>(R.id.til_username)
        val etUsername = dialogView.findViewById<TextInputEditText>(R.id.et_username)
        val tilPassword = dialogView.findViewById<TextInputLayout>(R.id.til_password)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.et_password)

        // Pre-fill fields
        etUsername.setText(owner.username)
        etPassword.setText("") // Don't pre-fill password for security

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Akun Owner")
            .setView(dialogView)
            .setPositiveButton("Simpan", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                try {
                    val newUsername = etUsername.text.toString().trim()
                    val newPassword = etPassword.text.toString().trim()

                    // Clear previous errors
                    tilUsername.error = null
                    tilPassword.error = null

                    // Validation
                    if (newUsername.isEmpty()) {
                        tilUsername.error = "Username wajib diisi"
                        return@setOnClickListener
                    }
                    if (newPassword.isNotEmpty() && newPassword.length < 6) {
                        tilPassword.error = "Kata sandi harus minimal 6 karakter"
                        return@setOnClickListener
                    }

                    // Check if username is changed and already taken
                    if (newUsername != owner.username && dbHelper.isUsernameTaken(newUsername, owner.id)) {
                        tilUsername.error = "Username sudah digunakan"
                        return@setOnClickListener
                    }

                    // Update owner (only update password if provided)
                    val finalPassword = if (newPassword.isNotEmpty()) newPassword else null
                    val updated = dbHelper.updateOwner(adminId, owner.id, newUsername, finalPassword ?: owner.username ?: "")
                    if (updated > 0) {
                        Toast.makeText(this, "Akun berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        loadOwnerAccounts()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Gagal memperbarui akun", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("OwnerAccountsActivity", "Error updating owner: ${e.message}", e)
                    Toast.makeText(this, "Kesalahan: Gagal memperbarui akun", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }
}