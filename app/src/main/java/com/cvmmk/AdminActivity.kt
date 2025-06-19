package com.cvmmk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cvmmk.databinding.ActivityAdminBinding

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityAdminBinding.inflate(layoutInflater)
            setContentView(binding.root)

            databaseHelper = DatabaseHelper(this)
            sessionManager = SessionManager(this)

            // Validate admin session
            val user = sessionManager.getLoggedInUser()
            if (user == null || !sessionManager.isLoggedIn() || user.role != "admin") {
                Log.e("AdminActivity", "Invalid session or user is not admin: ${user?.role}")
                Toast.makeText(this, "Silakan login sebagai admin", Toast.LENGTH_SHORT).show()
                redirectToLogin()
                return
            }

            Log.d("AdminActivity", "Session valid for user: ${user.username}, role: ${user.role}, id: ${user.id}")

            // Setup UI and data
            setupUI(user.username ?: "Admin")
            setupClickListeners()
            loadDashboardData()
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat dashboard", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI(username: String) {
        try {
            binding.tvWelcome.text = getString(R.string.welcome_message, username)
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error setting up UI: ${e.message}", e)
            binding.tvWelcome.text = getString(R.string.welcome_message, "Pengguna")
        }
    }

    private fun loadDashboardData() {
        try {
            val stats = databaseHelper.getDashboardStats()
            Log.d("AdminActivity", "Dashboard stats: activeProjects=${stats.activeProjects}, totalWorkers=${stats.totalWorkers}")
            binding.tvProjectCount.text = stats.activeProjects.toString()
            binding.tvWorkerCount.text = stats.totalWorkers.toString()
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error loading dashboard data: ${e.message}", e)
            binding.tvProjectCount.text = "0"
            binding.tvWorkerCount.text = "0"
            Toast.makeText(this, "Gagal memuat statistik dashboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        try {
            binding.btnAddProject.setOnClickListener {
                try {
                    startActivity(Intent(this, AddProjectActivity::class.java))
                } catch (e: Exception) {
                    Log.e("AdminActivity", "Error starting AddProjectActivity: ${e.message}", e)
                    Toast.makeText(this, "Gagal membuka Tambah Proyek", Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnViewProjects.setOnClickListener {
                try {
                    val intent = Intent(this, ProjectListActivity::class.java).apply {
                        putExtra("USER_ID", sessionManager.getLoggedInUser()?.id ?: -1)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("AdminActivity", "Error starting ProjectListActivity: ${e.message}", e)
                    Toast.makeText(this, "Gagal membuka Daftar Proyek", Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnAddWorker.setOnClickListener {
                try {
                    startActivity(Intent(this, AddWorkerActivity::class.java))
                } catch (e: Exception) {
                    Log.e("AdminActivity", "Error starting AddWorkerActivity: ${e.message}", e)
                    Toast.makeText(this, "Gagal membuka Tambah Pekerja", Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnManageAccounts.setOnClickListener {
                try {
                    startActivity(Intent(this, WorkerAccountsActivity::class.java))
                } catch (e: Exception) {
                    Log.e("AdminActivity", "Error starting WorkerAccountsActivity: ${e.message}", e)
                    Toast.makeText(this, "Gagal membuka Kelola Akun Pekerja", Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnAddOwner.setOnClickListener {
                try {
                    startActivity(Intent(this, AddOwnerActivity::class.java))
                } catch (e: Exception) {
                    Log.e("AdminActivity", "Error starting AddOwnerActivity: ${e.message}", e)
                    Toast.makeText(this, "Gagal membuka Tambah Owner", Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnManageOwners.setOnClickListener {
                try {
                    startActivity(Intent(this, OwnerAccountsActivity::class.java))
                } catch (e: Exception) {
                    Log.e("AdminActivity", "Error starting OwnerAccountsActivity: ${e.message}", e)
                    Toast.makeText(this, "Gagal membuka Kelola Akun Owner", Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnLogout.setOnClickListener {
                showLogoutDialog()
            }

            binding.ivProfile.setOnClickListener {
                Toast.makeText(this, "Fitur profil belum diimplementasikan", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error setting up click listeners: ${e.message}", e)
            Toast.makeText(this, "Gagal mengatur tombol", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLogoutDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle(R.string.confirm_logout_title)
                .setMessage(R.string.confirm_logout_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    logout()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error showing logout dialog: ${e.message}", e)
            Toast.makeText(this, "Gagal menampilkan dialog keluar", Toast.LENGTH_SHORT).show()
            logout()
        }
    }

    private fun logout() {
        try {
            sessionManager.clearSession()
            Log.d("AdminActivity", "Session cleared")
            redirectToLogin()
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error during logout: ${e.message}", e)
            Toast.makeText(this, "Gagal keluar dari sesi", Toast.LENGTH_SHORT).show()
            redirectToLogin()
        }
    }

    private fun redirectToLogin() {
        try {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error redirecting to login: ${e.message}", e)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }
}