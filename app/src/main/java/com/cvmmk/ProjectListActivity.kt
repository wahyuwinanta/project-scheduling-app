package com.cvmmk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cvmmk.databinding.ActivityProjectListBinding

class ProjectListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProjectListBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityProjectListBinding.inflate(layoutInflater)
            setContentView(binding.root)

            dbHelper = DatabaseHelper(this)

            // Get user ID from Intent (adjust based on how you pass user ID)

            val userId = intent.getIntExtra("USER_ID", -1)
            if (userId == -1) {
                Log.e("ProjectListActivity", "No user ID provided")
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Get user role
            val user = dbHelper.getUserById(userId)
            val userRole = user?.role ?: "unknown"
            if (userRole == "unknown") {
                Log.e("ProjectListActivity", "Invalid user role")
                Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Apply entrance animations
            try {
                val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
                binding.rvProjects.startAnimation(slideUp)
                binding.tvTitle.startAnimation(slideUp)
            } catch (e: Exception) {
                Log.e("ProjectListActivity", "Error loading animations: ${e.message}", e)
            }

            setupRecyclerView(userRole)
        } catch (e: Exception) {
            Log.e("ProjectListActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Failed to load projects", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView(userRole: String) {
        try {
            binding.rvProjects.layoutManager = LinearLayoutManager(this)
            val projects = dbHelper.getAllProjects()

            adapter = ProjectAdapter(
                projects = projects.toMutableList(),
                dbHelper = dbHelper,
                userRole = userRole, // Pass user role
                onEditClick = { project ->
                    try {
                        val intent = Intent(this, EditProjectActivity::class.java).apply {
                            putExtra("PROJECT_ID", project.id)
                            putExtra("PROJECT_NAME", project.name)
                            putExtra("PROJECT_LOCATION", project.location)
                            putExtra("PROJECT_START_DATE", project.startDate)
                            putExtra("PROJECT_STATUS", project.status)
                            putExtra("PROJECT_PROGRESS", project.progressPercentage)
                            putExtra("PROJECT_NOTES", project.notes)
                            putStringArrayListExtra("PROJECT_IMAGE_PATHS", ArrayList(project.imagePaths)) // Pass all image paths
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("ProjectListActivity", "Error opening EditProjectActivity: ${e.message}", e)
                        Toast.makeText(this, "Error opening edit screen", Toast.LENGTH_SHORT).show()
                    }
                },
                onDeleteClick = { project ->
                    try {
                        AlertDialog.Builder(this)
                            .setTitle(R.string.confirm_deletion_title)
                            .setMessage(getString(R.string.confirm_deletion_message, project.name))
                            .setPositiveButton(R.string.delete) { _, _ ->
                                try {
                                    val result = dbHelper.deleteProject(project.id)
                                    if (result > 0) {
                                        Toast.makeText(this, getString(R.string.project_deleted_success, project.name), Toast.LENGTH_SHORT).show()
                                        refreshRecyclerView()
                                    } else {
                                        Toast.makeText(this, R.string.project_deletion_failed, Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("ProjectListActivity", "Error deleting project: ${e.message}", e)
                                    Toast.makeText(this, "Error deleting project", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .setCancelable(true)
                            .show()
                    } catch (e: Exception) {
                        Log.e("ProjectListActivity", "Error showing delete dialog: ${e.message}", e)
                    }
                },
                
            )
            binding.rvProjects.adapter = adapter
        } catch (e: Exception) {
            Log.e("ProjectListActivity", "Error setting up RecyclerView: ${e.message}", e)
            Toast.makeText(this, "Failed to load project list", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshRecyclerView() {
        try {
            val projects = dbHelper.getAllProjects()
            adapter.updateProjects(projects)
        } catch (e: Exception) {
            Log.e("ProjectListActivity", "Error refreshing RecyclerView: ${e.message}", e)
            Toast.makeText(this, "Failed to refresh projects", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRecyclerView()
    }


}