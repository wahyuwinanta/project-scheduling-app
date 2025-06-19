package com.cvmmk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cvmmk.databinding.ActivityOwnerDashboardBinding

class OwnerDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOwnerDashboardBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Initialize view binding
            binding = ActivityOwnerDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Initialize database and session manager
            dbHelper = DatabaseHelper(this)
            sessionManager = SessionManager(this)

            // Validate session
            val user = sessionManager.getLoggedInUser()
            if (user == null || !sessionManager.isLoggedIn() || user.role != "owner") {
                Log.e("OwnerDashboardActivity", "Invalid session or user is not an owner")
                Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
                redirectToLogin()
                return
            }

            // Set userId from session
            userId = user.id
            Log.d("OwnerDashboardActivity", "UserId from session: $userId, username: ${user.username}")

            // Setup UI and load data
            setupUI()
            loadOwnerData()
            loadDashboardStats()
            setupClickListeners()
            animateViews()

        } catch (e: Exception) {
            Log.e("OwnerDashboardActivity", "Error in onCreate: ${e.message}", e)
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
            Log.e("OwnerDashboardActivity", "Error redirecting to login: ${e.message}", e)
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
            Log.e("OwnerDashboardActivity", "Error setting up UI: ${e.message}", e)
        }
    }

    private fun loadOwnerData() {
        try {
            val user = dbHelper.getUserById(userId)
            if (user != null && user.role == "owner") {
                binding.tvWorkerName.text = user.username ?: "Nama tidak tersedia"
                binding.tvOwnerRole.text = "Owner"
                Log.d("OwnerDashboardActivity", "Loaded owner data: ${user.username}")
            } else {
                Log.e("OwnerDashboardActivity", "Owner not found for ID: $userId")
                binding.tvWorkerName.text = "Pemilik tidak ditemukan"
                binding.tvOwnerRole.text = ""
                Toast.makeText(this, "Data pemilik tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("OwnerDashboardActivity", "Error loading owner data: ${e.message}", e)
            binding.tvWorkerName.text = "Error memuat data"
            binding.tvOwnerRole.text = ""
        }
    }

    private fun loadDashboardStats() {
        try {
            val stats = dbHelper.getDashboardStats()
            // Assuming the empty CardView is for total projects
            binding.tvTotalProjects.text = stats.totalProjects.toString()
            Log.d("OwnerDashboardActivity", "Loaded stats: ${stats.totalProjects} projects")
        } catch (e: Exception) {
            Log.e("OwnerDashboardActivity", "Error loading dashboard stats: ${e.message}", e)
            binding.tvTotalProjects.text = "N/A"
        }
    }

    private fun setupClickListeners() {
        try {
            binding.cardViewProjects.setOnClickListener {
                Log.d("OwnerDashboardActivity", "Projects card clicked, userId: $userId")
                animateCardClick(binding.cardViewProjects) {
                    try {
                        val intent = Intent(this, OwnerProjectListActivity::class.java)
                        intent.putExtra("USER_ID", userId)
                        Log.d("OwnerDashboardActivity", "Starting OwnerProjectListActivity with userId: $userId")
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("OwnerDashboardActivity", "Error starting OwnerProjectListActivity: ${e.message}", e)
                        Toast.makeText(this, "Gagal membuka daftar proyek", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            binding.btnLogout.setOnClickListener {
                showLogoutDialog()
            }
        } catch (e: Exception) {
            Log.e("OwnerDashboardActivity", "Error setting up click listeners: ${e.message}", e)
        }
    }

    private fun animateViews() {
        try {
            val slideInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
            val fadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)

            binding.layoutHeader.startAnimation(slideInAnimation)
            binding.layoutCards.startAnimation(fadeInAnimation)
        } catch (e: Exception) {
            Log.e("OwnerDashboardActivity", "Error animating views: ${e.message}", e)
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
            Log.e("OwnerDashboardActivity", "Error animating card click: ${e.message}", e)
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
            Log.e("OwnerDashboardActivity", "Error showing logout dialog: ${e.message}", e)
            logout()
        }
    }

    private fun logout() {
        try {
            sessionManager.clearSession()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("OwnerDashboardActivity", "Error during logout: ${e.message}", e)
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        showLogoutDialog()
    }
}