package com.cvmmk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
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

            // Validate session
            val user = sessionManager.getLoggedInUser()
            if (user == null || !sessionManager.isLoggedIn()) {
                Log.e("AdminActivity", "No valid session found")
                Toast.makeText(this, "Pengguna belum masuk", Toast.LENGTH_SHORT).show()
                redirectToLogin()
                return
            }

            // Ensure user is admin
            if (user.role != "admin") {
                Log.e("AdminActivity", "User is not admin: ${user.role}")
                Toast.makeText(this, "Akses ditolak", Toast.LENGTH_SHORT).show()
                sessionManager.clearSession()
                redirectToLogin()
                return
            }

            // Log session details for debugging
            Log.d("AdminActivity", "Session valid for user: ${user.username}, role: ${user.role}, id: ${user.id}")

            // Handle nullable username
            setupUI(user.username ?: "Admin")
            setupClickListeners()
            loadDashboardData()
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat dashboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUI(username: String?) {
        // Set welcome message, handle null username
        binding.tvWelcome.text = getString(R.string.welcome_message, username ?: "Pengguna")
    }

    private fun loadDashboardData() {
        try {
            val stats = databaseHelper.getDashboardStats()
            Log.d("AdminActivity", "Dashboard stats: activeProjects=${stats.activeProjects}, totalWorkers=${stats.totalWorkers}")
            updateStatsBindings(stats.activeProjects, stats.totalWorkers)
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error loading dashboard data: ${e.message}", e)
            try {
                val activeProjectCount = databaseHelper.getActiveProjectCount()
                val workerCount = databaseHelper.getWorkerCount()
                Log.d("AdminActivity", "Fallback: activeProjects=$activeProjectCount, workers=$workerCount")
                updateStatsBindings(activeProjectCount, workerCount)
            } catch (fallbackError: Exception) {
                Log.e("AdminActivity", "Fallback error: ${fallbackError.message}", fallbackError)
                updateStatsBindings(0, 0)
                Toast.makeText(this, "Gagal memuat data dashboard", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStatsBindings(projectCount: Int, workerCount: Int) {
        try {
            binding.cardProjects.findViewById<TextView>(R.id.tv_project_count)?.text = projectCount.toString()
            binding.cardWorkers.findViewById<TextView>(R.id.tv_worker_count)?.text = workerCount.toString()
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error updating stats cards: ${e.message}", e)
            Toast.makeText(this, "Gagal memperbarui statistik", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.btnAddProject.setOnClickListener {
            try {
                startActivity(Intent(this, AddProjectActivity::class.java))
            } catch (e: Exception) {
                Log.e("AdminActivity", "Error starting AddProjectActivity: ${e.message}", e)
                Toast.makeText(this, "Gagal membuka Tambah Proyek", Toast.LENGTH_SHORT).show()
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


        binding.btnManageAccounts.setOnClickListener {
            try {
                startActivity(Intent(this, WorkerAccountsActivity::class.java))
            } catch (e: Exception) {
                Log.e("AdminActivity", "Error starting WorkerAccountsActivity: ${e.message}", e)
                Toast.makeText(this, "Gagal membuka Kelola Akun", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        binding.ivProfile.setOnClickListener {
            Toast.makeText(this, "Fitur profil belum diimplementasikan", Toast.LENGTH_SHORT).show()
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
        }
    }

    private fun logout() {
        try {
            sessionManager.clearSession()
            Log.d("AdminActivity", "Session cleared")
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("AdminActivity", "Error during logout: ${e.message}", e)
            getSharedPreferences("user_session", MODE_PRIVATE).edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }
}