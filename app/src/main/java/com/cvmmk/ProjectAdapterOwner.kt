package com.cvmmk

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cvmmk.databinding.ItemProjectOwnerBinding
import java.io.File

class ProjectAdapterOwner(
    private var projects: MutableList<Project>,
    private val dbHelper: DatabaseHelper,
    private val userRole: String
) : RecyclerView.Adapter<ProjectAdapterOwner.ProjectViewHolder>() {

    class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val projectName: TextView = itemView.findViewById(R.id.tv_project_name)
        val projectLocation: TextView = itemView.findViewById(R.id.tv_project_location)
        val projectDate: TextView = itemView.findViewById(R.id.tv_project_date)
        val projectStatus: TextView = itemView.findViewById(R.id.tv_project_status)
        val projectProgress: TextView = itemView.findViewById(R.id.tv_project_progress)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
        val projectNotes: TextView = itemView.findViewById(R.id.tv_project_notes)
        val workerName: TextView = itemView.findViewById(R.id.tv_worker_name)
        val imagesRecyclerView: RecyclerView = itemView.findViewById(R.id.rv_project_images)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_project_owner, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        val context = holder.itemView.context

        holder.projectName.text = project.name
        holder.projectLocation.text = context.getString(R.string.project_location, project.location)
        holder.projectDate.text = context.getString(R.string.project_start_date, project.startDate)
        holder.projectStatus.text = context.getString(R.string.project_status, project.status)
        holder.projectProgress.text = context.getString(R.string.project_progress, project.progressPercentage)
        holder.progressBar.progress = project.progressPercentage
        holder.projectNotes.text = context.getString(
            R.string.project_notes,
            project.notes ?: context.getString(R.string.no_notes)
        )

        // Fetch worker names
        val workerNames = dbHelper.getAssignmentIdsForProject(project.id)
            .mapNotNull { assignmentId ->
                val cursor = dbHelper.readableDatabase.rawQuery(
                    "SELECT worker_id FROM project_assignments WHERE id = ?",
                    arrayOf(assignmentId.toString())
                )
                try {
                    if (cursor.moveToFirst()) {
                        val workerId = cursor.getInt(cursor.getColumnIndexOrThrow("worker_id"))
                        dbHelper.getWorkerName(workerId)
                    } else {
                        null
                    }
                } finally {
                    cursor.close()
                }
            }.joinToString(", ")

        holder.workerName.text = context.getString(
            R.string.project_workers,
            workerNames.ifEmpty { context.getString(R.string.no_workers) }
        )

        // Load images
        val imageUris = project.imagePaths.mapNotNull { path ->
            val file = File(path)
            if (file.exists()) Uri.fromFile(file) else {
                Log.w("ProjectAdapterOwner", "Image file not found: $path")
                null
            }
        }.toMutableList()

        val imageAdapter = ImageAdapter(imageUris, isEditable = false)
        holder.imagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
            setHasFixedSize(true)
        }

        Log.d("ProjectAdapterOwner", "Loaded ${imageUris.size} images for project ${project.id}")
    }

    override fun getItemCount(): Int = projects.size

    fun updateProjects(newProjects: List<Project>) {
        projects.clear()
        projects.addAll(newProjects)
        notifyDataSetChanged()
    }
}