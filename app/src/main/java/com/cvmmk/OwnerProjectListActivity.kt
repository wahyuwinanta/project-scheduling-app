package com.cvmmk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cvmmk.databinding.ActivityOwnerProjectListBinding

class OwnerProjectListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOwnerProjectListBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ProjectAdapterOwner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityOwnerProjectListBinding.inflate(layoutInflater)
            setContentView(binding.root)

            dbHelper = DatabaseHelper(this)

            // Get user ID from Intent
            val userId = intent.getIntExtra("USER_ID", -1)
            if (userId == -1) {
                Log.e("OwnerProjectListActivity", "No user ID provided")
                Toast.makeText(this, "User tidak login", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Get user role
            val user = dbHelper.getUserById(userId)
            val userRole = user?.role ?: "unknown"
            if (userRole != "owner") {
                Log.e("OwnerProjectListActivity", "Invalid user role: $userRole")
                Toast.makeText(this, "Akses hanya untuk pemilik", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Apply entrance animations
            try {
                val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
                binding.rvProjects.startAnimation(slideUp)
                binding.tvTitle.startAnimation(slideUp)
            } catch (e: Exception) {
                Log.e("OwnerProjectListActivity", "Error loading animations: ${e.message}", e)
            }

            setupRecyclerView(userRole)
        } catch (e: Exception) {
            Log.e("OwnerProjectListActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat daftar proyek", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupRecyclerView(userRole: String) {
        try {
            binding.rvProjects.layoutManager = LinearLayoutManager(this)
            val projects = dbHelper.getAllProjects()
            Log.d("OwnerProjectListActivity", "Loaded ${projects.size} projects")

            adapter = ProjectAdapterOwner(
                projects = projects.toMutableList(),
                dbHelper = dbHelper,
                userRole = userRole
            )
            binding.rvProjects.adapter = adapter
        } catch (e: Exception) {
            Log.e("OwnerProjectListActivity", "Error setting up RecyclerView: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat daftar proyek", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshRecyclerView() {
        try {
            val projects = dbHelper.getAllProjects()
            adapter.updateProjects(projects)
            Log.d("OwnerProjectListActivity", "Refreshed with ${projects.size} projects")
        } catch (e: Exception) {
            Log.e("OwnerProjectListActivity", "Error refreshing RecyclerView: ${e.message}", e)
            Toast.makeText(this, "Gagal memperbarui daftar proyek", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRecyclerView()
    }
}