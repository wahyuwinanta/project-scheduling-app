package com.cvmmk

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cvmmk.databinding.ItemWorkerProjectBinding
import java.io.File

class WorkerProjectAdapter(
    private var projects: List<ProjectWithDetails>,
    private val dbHelper: DatabaseHelper.Companion,
    private val onProgressClick: (ProjectWithDetails) -> Unit
) : RecyclerView.Adapter<WorkerProjectAdapter.ProjectViewHolder>() {

    class ProjectViewHolder(val binding: ItemWorkerProjectBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemWorkerProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProjectViewHolder(binding)
    }

    @SuppressLint("StringFormatInvalid")
    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        with(holder.binding) {
            // Apply slide-up animation
            root.startAnimation(AnimationUtils.loadAnimation(root.context, R.anim.slide_up))

            // Bind project details
            tvProjectName.text = root.context.getString(R.string.project_name, project.name ?: "Unknown")
            tvLocation.text = root.context.getString(R.string.project_location, project.location ?: "N/A")
            tvStartDate.text = root.context.getString(R.string.project_start_date, project.startDate ?: "N/A")
            tvStatus.text = root.context.getString(R.string.project_status, project.status ?: "Not Started")
            tvProgress.text = root.context.getString(R.string.project_progress, project.progressPercentage)
            tvNotes.text = root.context.getString(R.string.project_notes, project.notes ?: root.context.getString(R.string.no_notes))
            tvWorkers.text = root.context.getString(R.string.project_workers, project.workerName ?: "None")

            // Load images
            val imageUris = project.imagePaths.mapNotNull { path ->
                val file = File(path)
                if (file.exists()) Uri.fromFile(file) else {
                    Log.w("WorkerProjectAdapter", "Image file not found: $path")
                    null
                }
            }.toMutableList()

            val imageAdapter = ImageAdapter(imageUris, isEditable = false)
            rvProjectImages.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = imageAdapter
                setHasFixedSize(true)
            }

            // Set click listener for update button
            btnUpdate.setOnClickListener {
                onProgressClick(project)
            }

            Log.d("WorkerProjectAdapter", "Bound project ${project.id} with ${imageUris.size} images")
        }
    }

    override fun getItemCount(): Int = projects.size

    fun updateProjects(newProjects: List<ProjectWithDetails>) {
        projects = newProjects
        notifyDataSetChanged()
        Log.d("WorkerProjectAdapter", "Updated with ${projects.size} projects")
    }
}