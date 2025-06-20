package com.cvmmk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.cvmmk.databinding.ActivityWorkerDashboardBinding

data class Notification(val id: Int, val title: String, val message: String, val timestamp: Long)
data class ProjectAssignment(val id: Int, val projectId: Int, val workerId: Int, val isNew: Boolean)

class NotificationAdapter(private val notifications: List<Notification>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    class ViewHolder(itemView: android.view.View) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        val title: android.widget.TextView = itemView.findViewById(android.R.id.text1)
        val message: android.widget.TextView = itemView.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        holder.title.text = notification.title
        holder.message.text = notification.message
    }

    override fun getItemCount() = notifications.size
}

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
            checkNewAssignments()
            loadStats()
            loadRecentNotifications()
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
            window.statusBarColor = ContextCompat.getColor(this, R.color.blue_primary)
            supportActionBar?.hide()
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error setting up UI: ${e.message}", e)
        }
    }

    private fun loadWorkerData() {
        try {
            val worker = dbHelper.getWorkerById(workerId)
            Log.d("WorkerDashboardActivity", "Worker data: $worker")
            if (worker != null) {
                binding.tvWorkerName.text = worker.name ?: "Nama tidak tersedia"
                binding.tvWorkerRole.text = worker.role ?: "Posisi tidak tersedia"
            } else {
                Log.e("WorkerDashboardActivity", "Worker not found for ID: $workerId")
                binding.tvWorkerName.text = "Pekerja tidak ditemukan"
                binding.tvWorkerRole.text = ""
                Toast.makeText(this, "Data pekerja tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error loading worker data: ${e.message}, stackTrace=${e.stackTraceToString()}")
            binding.tvWorkerName.text = "Error memuat data"
            binding.tvWorkerRole.text = ""
        }
    }

    private fun checkNewAssignments() {
        try {
            val lastCheckTime = sessionManager.getLastAssignmentCheckTime()
            val newAssignments = dbHelper.getNewProjectAssignmentsForWorker(workerId, lastCheckTime)
            Log.d("WorkerDashboardActivity", "New assignments: ${newAssignments.size}")
            sessionManager.setLastAssignmentCheckTime(System.currentTimeMillis())
            if (newAssignments.isNotEmpty()) {
                binding.cardQuickNotification.visibility = View.VISIBLE
                val projectCount = newAssignments.size
                binding.tvNotificationTitle.text = "Tugas Baru Tersedia"
                binding.tvNotificationMessage.text = getString(
                    R.string.new_project_notification,
                    projectCount,
                    if (projectCount > 1) "tugas baru" else "tugas baru"
                )
            } else {
                binding.cardQuickNotification.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error checking assignments: ${e.message}", e)
            binding.cardQuickNotification.visibility = View.GONE
        }
    }

    private fun loadStats() {
        try {
            val completedCount = dbHelper.getCompletedTaskCount(workerId)
            val pendingCount = dbHelper.getPendingTaskCount(workerId)
            binding.tvCompletedCount.text = completedCount.toString()
            binding.tvPendingCount.text = pendingCount.toString()
            Log.d("WorkerDashboardActivity", "Stats: Completed=$completedCount, Pending=$pendingCount")
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error loading stats: ${e.message}", e)
            binding.tvCompletedCount.text = "0"
            binding.tvPendingCount.text = "0"
        }
    }

    private fun loadRecentNotifications() {
        try {
            val notifications = dbHelper.getRecentNotifications(workerId)
            Log.d("WorkerDashboardActivity", "Found ${notifications.size} recent notifications")
            binding.rvNotifications.layoutManager = LinearLayoutManager(this)
            binding.rvNotifications.adapter = NotificationAdapter(notifications)
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error loading notifications: ${e.message}", e)
        }
    }

    private fun setupClickListeners() {
        try {


            // Projects card click
            binding.cardViewProjects.setOnClickListener {
                Log.d("WorkerDashboardActivity", "Projects card clicked")
                animateCardClick(binding.cardViewProjects) {
                    val intent = Intent(this, WorkerProjectListActivity::class.java)
                    intent.putExtra("WORKER_ID", workerId)
                    startActivity(intent)
                }
            }



            // Logout button click
            binding.btnLogout.setOnClickListener {
                showLogoutDialog()
            }
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error setting up click listeners: ${e.message}", e)
        }
    }

    private fun animateViews() {
        try {
            val slideIn = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
            val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            binding.layoutHeader.startAnimation(slideIn)
            binding.cardQuickNotification.startAnimation(fadeIn)
            binding.layoutCards.startAnimation(fadeIn)
            binding.rvNotifications.startAnimation(fadeIn)
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error animating views: ${e.message}", e)
        }
    }

    private fun animateCardClick(view: View, action: () -> Unit) {
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
            action()
        }
    }

    private fun showLogoutDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Apakah Anda yakin ingin keluar?")
                .setPositiveButton("Ya") { _, _ -> logout() }
                .setNegativeButton("Batal", null)
                .show()
        } catch (e: Exception) {
            Log.e("WorkerDashboardActivity", "Error showing logout dialog: ${e.message}", e)
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
            Log.e("WorkerDashboardActivity", "Error during logout: ${e.message}", e)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        checkNewAssignments()
        loadStats()
        loadRecentNotifications()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        showLogoutDialog()
    }
}