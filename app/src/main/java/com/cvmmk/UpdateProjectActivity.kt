package com.cvmmk

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import android.provider.MediaStore
import android.util.Log
import com.cvmmk.databinding.ActivityUpdateProgressBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class UpdateProjectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateProgressBinding
    private lateinit var dbHelper: DatabaseHelper

    private var workerId: Int = 0
    private var projectId: Int = 0
    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityUpdateProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("UpdateProjectActivity", "Sebelum inisialisasi dbHelper")
        dbHelper = DatabaseHelper(this)
        Log.d("UpdateProjectActivity", "dbHelper berhasil dibuat")

        workerId = intent.getIntExtra("WORKER_ID", 0)
        projectId = intent.getIntExtra("PROJECT_ID", 0)

        // Apply animations
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.tvTitle.startAnimation(slideUp)
        binding.cardProjectInfo.startAnimation(slideUp)
        binding.cardProgressInputs.startAnimation(slideUp)

        // Display project info
        val project = try {
            val projectList = dbHelper.getAllProjects()
            Log.d("UpdateProjectActivity", "Total projects dari DB: ${projectList.size}")
            projectList.find { it.id == projectId }
        } catch (e: Exception) {
            Log.e("UpdateProjectActivity", "Gagal ambil data project: ${e.message}", e)
            null
        }

        project?.let {
            binding.tvProjectInfo.text = "Project: ${it.name}\n" +
                    "Location: ${it.location}\n" +
                    "Start Date: ${it.startDate}\n" +
                    "Status: ${it.status}"
        } ?: run {
            binding.tvProjectInfo.text = "Project not found"
            binding.btnSaveProgress.isEnabled = false
            Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show()
        }


        // Setup slider
        binding.sliderProgress.addOnChangeListener { _, value, _ ->
            binding.tvProgressValue.text = "${value.toInt()}%"
        }

        // Setup image upload
        binding.btnUploadImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Save progress
        binding.btnSaveProgress.setOnClickListener {
            val percentage = binding.sliderProgress.value.toInt()
            val notes = binding.etProgressNotes.text.toString().trim()
            val imagePath = imageUri?.let { uri ->
                val file = createImageFile()
                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                file.absolutePath
            }

            // Validate notes
            binding.tilProgressNotes.error = null
            var isValid = true
            if (notes.isEmpty()) {
                binding.tilProgressNotes.error = "Notes diperlukan"
                isValid = false
            }

            if (!isValid) {
                val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
                binding.tilProgressNotes.startAnimation(shake)
                Toast.makeText(this, "Harap isi catatan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update project progress directly
            val project = dbHelper.getAllProjects().find { it.id == projectId }
            project?.let {
                val result = dbHelper.updateProjectProgress(projectId, percentage, notes, imagePath)
                if (result > 0) {
                    Toast.makeText(this, "Progress updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to update progress", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            binding.ivImagePreview.setImageURI(imageUri)
            binding.ivImagePreview.visibility = android.view.View.VISIBLE
        }
    }
}