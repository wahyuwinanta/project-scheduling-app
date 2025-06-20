package com.cvmmk

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.cvmmk.databinding.ActivityUpdateProjectBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UpdateProjectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateProjectBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var imageAdapter: ImageAdapter
    private val selectedImageUris = mutableListOf<Uri>()
    private var projectId: Int = -1
    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityUpdateProjectBinding.inflate(layoutInflater)
            setContentView(binding.root)

            dbHelper = DatabaseHelper(this)
            setupImageRecyclerView()
            setupStatusSpinner()
            setupButtons()

            projectId = intent.getIntExtra("PROJECT_ID", -1)
            if (projectId == -1) {
                Toast.makeText(this, "Proyek tidak valid", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            loadProjectData()
        } catch (e: Exception) {
            Log.e("UpdateProjectActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat proyek", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupImageRecyclerView() {
        imageAdapter = ImageAdapter(selectedImageUris, isEditable = true) { uri ->
            selectedImageUris.remove(uri)
            imageAdapter.notifyDataSetChanged()
            Log.d("UpdateProjectActivity", "Image removed: $uri")
        }
        binding.rvImages.apply {
            layoutManager = LinearLayoutManager(this@UpdateProjectActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupStatusSpinner() {
        val statuses = arrayOf("Belum Dimulai", "Sedang Berjalan", "Selesai")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnAddImage.setOnClickListener {
            pickImageFromGallery()
        }
        binding.btnCaptureImage.setOnClickListener {
            captureImage()
        }
        binding.btnSaveProject.setOnClickListener {
            saveProject()
        }
    }

    private fun loadProjectData() {
        try {
            val project = dbHelper.getProjectById(projectId)
            if (project == null) {
                Toast.makeText(this, "Proyek tidak ditemukan", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Display non-editable fields
            binding.tvProjectName.text = project.name
            binding.tvLocation.text = project.location
            binding.tvStartDate.text = project.startDate
            binding.tvWorkers.text = dbHelper.getAssignmentIdsForProject(projectId)
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
                }.joinToString(", ").ifEmpty { getString(R.string.no_workers) }

            // Editable fields
            binding.etProgress.setText(project.progressPercentage.toString())
            binding.etNotes.setText(project.notes)
            binding.progressBar.progress = project.progressPercentage

            val statusPosition = (binding.spinnerStatus.adapter as ArrayAdapter<String>).getPosition(project.status)
            binding.spinnerStatus.setSelection(statusPosition)

            selectedImageUris.clear()
            dbHelper.getProjectImages(projectId).forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    selectedImageUris.add(Uri.fromFile(file))
                    Log.d("UpdateProjectActivity", "Loaded image: $path")
                } else {
                    Log.w("UpdateProjectActivity", "Image file not found: $path")
                }
            }
            imageAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e("UpdateProjectActivity", "Error loading project data: ${e.message}", e)
            Toast.makeText(this, "Gagal memuat data proyek", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        galleryLauncher.launch(intent)
    }

    private fun captureImage() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            currentPhotoPath = file.absolutePath

            val photoUri = FileProvider.getUriForFile(
                this,
                "com.cvmmk.fileprovider",
                file
            )
            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            Log.e("UpdateProjectActivity", "Error capturing image: ${e.message}", e)
            Toast.makeText(this, "Gagal membuka kamera", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data: Intent ->
                data.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        selectedImageUris.add(uri)
                        Log.d("UpdateProjectActivity", "Gallery image added: $uri")
                    }
                } ?: data.data?.let { uri ->
                    selectedImageUris.add(uri)
                    Log.d("UpdateProjectActivity", "Single gallery image added: $uri")
                }
                imageAdapter.notifyDataSetChanged()
            }
        } else {
            Log.w("UpdateProjectActivity", "Gallery result not OK: ${result.resultCode}")
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    val uri = Uri.fromFile(file)
                    selectedImageUris.add(uri)
                    imageAdapter.notifyDataSetChanged()
                    Log.d("UpdateProjectActivity", "Image captured and added: $path")
                } else {
                    Log.e("UpdateProjectActivity", "Captured image file not found: $path")
                }
            }
        } else {
            Log.w("UpdateProjectActivity", "Camera capture failed")
        }
    }

    private fun getRealPathFromUri(uri: Uri): String? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("UpdateProjectActivity", "Error getting real path: ${e.message}", e)
            null
        }
    }

    private fun saveProject() {
        try {
            val status = binding.spinnerStatus.selectedItem.toString()
            val progress = binding.etProgress.text.toString().toIntOrNull() ?: 0
            val notes = binding.etNotes.text.toString().trim()

            if (progress < 0 || progress > 100) {
                Toast.makeText(this, "Progres harus antara 0 dan 100", Toast.LENGTH_SHORT).show()
                return
            }

            val updated = dbHelper.updateProject(
                projectId = projectId,
                status = status,
                progressPercentage = progress,
                notes = if (notes.isEmpty()) null else notes
            )

            if (updated) {
                dbHelper.deleteProjectImages(projectId)
                selectedImageUris.forEach { uri ->
                    getRealPathFromUri(uri)?.let { path ->
                        dbHelper.addProjectImage(projectId, path)
                        Log.d("UpdateProjectActivity", "Saved image: $path")
                    }
                }
                Toast.makeText(this, "Proyek berhasil diperbarui", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Gagal memperbarui proyek", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("UpdateProjectActivity", "Error saving project: ${e.message}", e)
            Toast.makeText(this, "Error menyimpan proyek", Toast.LENGTH_SHORT).show()
        }
    }
}