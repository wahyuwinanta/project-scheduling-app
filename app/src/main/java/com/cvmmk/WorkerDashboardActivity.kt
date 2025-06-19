package com.cvmmk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cvmmk.databinding.ActivityWorkerDashboardBinding

class WorkerDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWorkerDashboardBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private var workerId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityWorkerDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)

            dbHelper = DatabaseHelper(this)
            sessionManager = SessionManager(this)

            // Get workerId from intent
            workerId = intent.getIntExtra("WORKER_ID", 0)
            Log.d("WorkerDashboardActivity", "WorkerId from intent: $workerId")

            // Validate session and workerId
            val user = sessionManager.getLoggedInUser()
            if (user == null || !sessionManager.isLoggedIn() || user.role != "worker") {
                Log.e("WorkerDashboardActivity", "Invalid session")
                Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
                redirectToLogin()
                return
            }

            // Use workerId from user session if available
            if (user.workerId != null && user.workerId > 0) {
                workerId = user.workerId
            }

            if (workerId <= 0) {
                Log.e("WorkerDashboardActivity", "Invalid workerId: $workerId")
                Toast.makeText(this, "ID pekerja tidak valid", Toast.LENGTH_SHORT).show()
                redirectToLogin()
                return
            }

            Log.d("WorkerDashboardActivity", "Using workerId: $workerId for user: ${user.username}")

            setupUI()
            loadWorkerData()
            setupClickListeners()
            animateViews()

        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat dashboard", Toast.LENGTH_SHORT).show()
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
            Log.e("WorkerDashboardActivity", "Error redirecting to login: ${e.message}", e)
            finish()
        }
    }

    private fun setupUI() {
        try {
            // Set status bar color
            window.statusBarColor = ContextCompat.getColor(this, R.color.blue_primary)

            // Hide action bar for custom toolbar
            supportActionBar?.hide()
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error setting up UI: ${e.message}", e)
        }
    }

    private fun loadWorkerData() {
        try {
            val worker = dbHelper.getWorkerById(workerId)
            if (worker != null) {
                binding.tvWorkerName.text = worker.name ?: "Nama tidak tersedia"
                binding.tvWorkerRole.text = worker.role ?: "Posisi tidak tersedia"
                Log.d("WorkerDashboardActivity", "Loaded worker data: ${worker.name}, ${worker.role}")
            } else {
                Log.e("WorkerDashboardActivity", "Worker not found for ID: $workerId")
                binding.tvWorkerName.text = "Pekerja tidak ditemukan"
                binding.tvWorkerRole.text = ""
                Toast.makeText(this, "Data pekerja tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error loading worker data: ${e.message}", e)
            binding.tvWorkerName.text = "Error memuat data"
            binding.tvWorkerRole.text = ""
        }
    }

    private fun setupClickListeners() {
        try {
            binding.cardViewProjects.setOnClickListener {
                Log.d("WorkerDashboardActivity", "Projects card clicked, workerId: $workerId")
                animateCardClick(binding.cardViewProjects) {
                    try {
                        val intent = Intent(this, WorkerProjectListActivity::class.java)
                        intent.putExtra("WORKER_ID", workerId)
                        Log.d("WorkerDashboardActivity", "Starting WorkerProjectListActivity with workerId: $workerId")
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("WorkerDashboardActivity", "Error starting WorkerProjectListActivity: ${e.message}", e)
                        Toast.makeText(this, "Gagal membuka daftar proyek", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            binding.btnLogout.setOnClickListener {
                showLogoutDialog()
            }
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error setting up click listeners: ${e.message}", e)
        }
    }

    private fun animateViews() {
        try {
            val slideInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
            val fadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)

            binding.layoutHeader.startAnimation(slideInAnimation)
            binding.layoutCards.startAnimation(fadeInAnimation)
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error animating views: ${e.message}", e)
            // Don't crash if animations fail
        }
    }

    private fun animateCardClick(view: android.view.View, action: () -> Unit) {
        try {
            val scaleDown = android.view.animation.ScaleAnimation(
                1.0f, 0.95f, 1.0f, 0.95f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
            )
            scaleDown.duration = 100
            scaleDown.fillAfter = true

            val scaleUp = android.view.animation.ScaleAnimation(
                0.95f, 1.0f, 0.95f, 1.0f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
            )
            scaleUp.duration = 100
            scaleUp.startOffset = 100

            view.startAnimation(scaleDown)
            view.postDelayed({
                view.startAnimation(scaleUp)
                action()
            }, 200)
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error animating card click: ${e.message}", e)
            // Execute action immediately if animation fails
            action()
        }
    }

    private fun showLogoutDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Apakah Anda yakin ingin keluar?")
                .setPositiveButton("Ya") { _, _ ->
                    logout()
                }
                .setNegativeButton("Batal", null)
                .show()
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error showing logout dialog: ${e.message}", e)
            logout() // Fallback to direct logout
        }
    }

    private fun logout() {
        try {
            // Clear session using SessionManager
            sessionManager.clearSession()

            // Navigate back to login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error during logout: ${e.message}", e)
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Show logout dialog when back is pressed
        showLogoutDialog()
    }
}