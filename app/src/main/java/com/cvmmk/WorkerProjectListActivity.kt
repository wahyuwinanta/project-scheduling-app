package com.cvmmk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cvmmk.databinding.ActivityWorkerProjectListBinding

class WorkerProjectListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWorkerProjectListBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var projectAdapter: WorkerProjectAdapter
    private var workerId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityWorkerProjectListBinding.inflate(layoutInflater)
            setContentView(binding.root)

            databaseHelper = DatabaseHelper(this)
            sessionManager = SessionManager(this)

            // Get workerId from intent first
            workerId = intent.getIntExtra("WORKER_ID", -1)
            Log.d("WorkerProjectListActivity", "WorkerId from intent: $workerId")

            // Validate session
            val user = sessionManager.getLoggedInUser()

            if (user == null || !sessionManager.isLoggedIn()) {
                Log.e("WorkerProjectListActivity", "User not logged in")
                Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
                redirectToLogin()
                return
            }

            if (user.role != "worker") {
                Log.e("WorkerProjectListActivity", "User is not a worker: ${user.role}")
                Toast.makeText(this, "Akses ditolak", Toast.LENGTH_SHORT).show()
                redirectToLogin()
                return
            }

            // Use workerId from user session if available, otherwise from intent
            if (user.workerId != null && user.workerId > 0) {
                workerId = user.workerId
            }

            if (workerId <= 0) {
                Log.e("WorkerProjectListActivity", "Invalid workerId: $workerId")
                Toast.makeText(this, "ID pekerja tidak valid", Toast.LENGTH_SHORT).show()
                redirectToLogin()
                return
            }

            Log.d("WorkerProjectListActivity", "Final workerId used: $workerId")
            Log.d("WorkerProjectListActivity", "Session valid for user: ${user.username}, role: ${user.role}")

            setupRecyclerView()
            loadProjects()

            // Apply entrance animations
            try {
                val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
                binding.recyclerViewProjects.startAnimation(slideUp)
            } catch (e: Exception) {
                Log.e("WorkerProjectListActivity", "Error loading animations: ${e.message}", e)
            }

        } catch (e: Exception) {
            Log.e("WorkerProjectListActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat proyek", Toast.LENGTH_SHORT).show()
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
            Log.e("WorkerProjectListActivity", "Error redirecting to login: ${e.message}", e)
            finish()
        }
    }

    private fun setupRecyclerView() {
        try {
            binding.recyclerViewProjects.layoutManager = LinearLayoutManager(this)

            projectAdapter = WorkerProjectAdapter(
                projects = emptyList(),
                dbHelper = DatabaseHelper,
                onProgressClick = { project: ProjectWithDetails ->
                    try {
                        val intent = Intent(this, UpdateProjectActivity::class.java).apply {
                            putExtra("PROJECT_ID", project.id)
                            putExtra("PROJECT_NAME", project.name)
                            putExtra("WORKER_ID", workerId)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("WorkerProjectListActivity", "Error starting UpdateProjectActivity: ${e.message}", e)
                        Toast.makeText(this, "Gagal membuka update progress", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            binding.recyclerViewProjects.adapter = projectAdapter
            Log.d("WorkerProjectListActivity", "RecyclerView setup completed")

        } catch (e: Exception) {
            Log.e("WorkerProjectListActivity", "Error setting up RecyclerView: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat daftar proyek", Toast.LENGTH_SHORT).show()
            throw e
        }
    }

    private fun loadProjects() {
        try {
            Log.d("WorkerProjectListActivity", "Loading projects for workerId: $workerId")
            val projects = databaseHelper.getProjectsForWorker(workerId)
            Log.d("WorkerProjectListActivity", "Loaded ${projects.size} projects")

            if (projects.isEmpty()) {
                Log.d("WorkerProjectListActivity", "No projects found for worker $workerId")
                Toast.makeText(this, "Tidak ada proyek yang ditemukan", Toast.LENGTH_SHORT).show()
            }

            projectAdapter.updateProjects(projects)

        } catch (e: Exception) {
            Log.e("WorkerProjectListActivity", "Error loading projects: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat proyek", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (workerId > 0) {
                loadProjects()
            } else {
                Log.e("WorkerProjectListActivity", "Invalid workerId on resume: $workerId")
                redirectToLogin()
            }
        } catch (e: Exception) {
            Log.e("WorkerProjectListActivity", "Error in onResume: ${e.message}", e)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        try {
            val intent = Intent(this, WorkerDashboardActivity::class.java)
            intent.putExtra("WORKER_ID", workerId)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("WorkerProjectListActivity", "Error navigating back: ${e.message}", e)
            finish()
        }
    }
}