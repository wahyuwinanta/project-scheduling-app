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
import com.cvmmk.databinding.ActivityEditProjectBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditProjectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProjectBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var imageAdapter: ImageAdapter
    private val selectedImageUris = mutableListOf<Uri>()
    private var projectId: Int = -1
    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityEditProjectBinding.inflate(layoutInflater)
            setContentView(binding.root)

            dbHelper = DatabaseHelper(this)
            setupImageRecyclerView()
            setupStatusSpinner()
            setupButtons()

            projectId = intent.getIntExtra("PROJECT_ID", -1)
            if (projectId == -1) {
                Toast.makeText(this, "Invalid project", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            loadProjectData()
        } catch (e: Exception) {
            Log.e("EditProjectActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Failed to load project", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupImageRecyclerView() {
        imageAdapter = ImageAdapter(selectedImageUris, isEditable = true) { uri ->
            selectedImageUris.remove(uri)
            imageAdapter.notifyDataSetChanged()
            Log.d("EditProjectActivity", "Image removed: $uri")
        }
        binding.rvImages.apply {
            layoutManager = LinearLayoutManager(this@EditProjectActivity, LinearLayoutManager.HORIZONTAL, false)
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
                Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            binding.etProjectName.setText(project.name)
            binding.etLocation.setText(project.location)
            binding.etStartDate.setText(project.startDate)
            binding.etProgress.setText(project.progressPercentage.toString())
            binding.etNotes.setText(project.notes)

            val statusPosition = (binding.spinnerStatus.adapter as ArrayAdapter<String>).getPosition(project.status)
            binding.spinnerStatus.setSelection(statusPosition)

            selectedImageUris.clear()
            dbHelper.getProjectImages(projectId).forEach { path ->
                val file = File(path)
                if (file.exists()) {
                    selectedImageUris.add(Uri.fromFile(file))
                    Log.d("EditProjectActivity", "Loaded image: $path")
                } else {
                    Log.w("EditProjectActivity", "Image file not found: $path")
                }
            }
            imageAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e("EditProjectActivity", "Error loading project data: ${e.message}", e)
            Toast.makeText(this, "Failed to load project data", Toast.LENGTH_SHORT).show()
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
            Log.e("EditProjectActivity", "Error capturing image: ${e.message}", e)
            Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data: Intent ->
                data.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        selectedImageUris.add(uri)
                        Log.d("EditProjectActivity", "Gallery image added: $uri")
                    }
                } ?: data.data?.let { uri ->
                    selectedImageUris.add(uri)
                    Log.d("EditProjectActivity", "Single gallery image added: $uri")
                }
                imageAdapter.notifyDataSetChanged()
            }
        } else {
            Log.w("EditProjectActivity", "Gallery result not OK: ${result.resultCode}")
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
                    Log.d("EditProjectActivity", "Image captured and added: $path")
                } else {
                    Log.e("EditProjectActivity", "Captured image file not found: $path")
                }
            }
        } else {
            Log.w("EditProjectActivity", "Camera capture failed")
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
            Log.e("EditProjectActivity", "Error getting real path: ${e.message}", e)
            null
        }
    }

    private fun saveProject() {
        try {
            val name = binding.etProjectName.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val startDate = binding.etStartDate.text.toString().trim()
            val status = binding.spinnerStatus.selectedItem.toString()
            val progress = binding.etProgress.text.toString().toIntOrNull() ?: 0
            val notes = binding.etNotes.text.toString().trim()

            if (name.isEmpty() || location.isEmpty() || startDate.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
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
                        Log.d("EditProjectActivity", "Saved image: $path")
                    }
                }
                Toast.makeText(this, "Project updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to update project", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("EditProjectActivity", "Error saving project: ${e.message}", e)
            Toast.makeText(this, "Error saving project", Toast.LENGTH_SHORT).show()
        }
    }
}