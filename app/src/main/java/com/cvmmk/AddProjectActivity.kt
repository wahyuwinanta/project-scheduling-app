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
import com.cvmmk.databinding.ActivityAddProjectBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.app.DatePickerDialog
import android.app.AlertDialog

import android.view.View
import android.widget.TextView

import android.view.ViewGroup

import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView


class AddProjectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddProjectBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var imageAdapter: ImageAdapter
    private val selectedImageUris = mutableListOf<Uri>()
    private var currentPhotoPath: String? = null
    private var selectedWorkers = mutableListOf<Worker>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityAddProjectBinding.inflate(layoutInflater)
            setContentView(binding.root)
            dbHelper = DatabaseHelper(this)
            setupImageRecyclerView()
            setupStatusSpinner()
            setupWorkerSpinner()
            setupDatePicker()
            setupButtons()
        } catch (e: Exception) {
            Log.e("AddProjectActivity", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Gagal menginisialisasi", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupImageRecyclerView() {
        imageAdapter = ImageAdapter(selectedImageUris, isEditable = true) { uri ->
            selectedImageUris.remove(uri)
            imageAdapter.notifyDataSetChanged()
            Log.d("AddProjectActivity", "Image removed: $uri")
        }
        binding.rvImages.apply {
            layoutManager = LinearLayoutManager(this@AddProjectActivity, LinearLayoutManager.HORIZONTAL, false)
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

    private fun setupWorkerSpinner() {
        dbHelper.debugWorkersTable()
        val workers = dbHelper.getAllWorkers()
        // Tambah TextView untuk menampilkan pekerja terpilih
        val selectedWorkersText = TextView(this).apply {
            id = View.generateViewId()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = "Pekerja yang dipilih: Tidak ada"
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(16, 8, 16, 8)
        }
        // Tambah TextView ke NestedScrollView
        val rootView = binding.root as NestedScrollView
        val innerLayout = rootView.getChildAt(0) as ViewGroup // Biasanya LinearLayout atau ConstraintLayout
        innerLayout.addView(selectedWorkersText)
        if (workers.isEmpty()) {
            Log.w("AddProjectActivity", "No workers found, disabling button")
            Toast.makeText(this, R.string.no_workers_available, Toast.LENGTH_SHORT).show()
            binding.btnSelectWorkers.isEnabled = false
        } else {
            Log.d("AddProjectActivity", "Workers found: ${workers.map { it.name }}")
            binding.btnSelectWorkers.isEnabled = true
            val selected = BooleanArray(workers.size) { false }
            binding.btnSelectWorkers.setOnClickListener {
                val workerNames = workers.map { "${it.name} (${it.role ?: "No Role"})" }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle(R.string.select_workers)
                    .setMultiChoiceItems(workerNames, selected) { _, which, isChecked ->
                        selected[which] = isChecked
                    }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        selectedWorkers.clear()
                        selectedWorkers.addAll(workers.filterIndexed { index, _ -> selected[index] })
                        Log.d("AddProjectActivity", "Selected workers: ${selectedWorkers.map { it.name }}")
                        selectedWorkersText.text = if (selectedWorkers.isEmpty()) {
                            "Pekerja yang dipilih: Tidak ada"
                        } else {
                            "Pekerja yang dipilih: ${selectedWorkers.joinToString { it.name }}"
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun setupDatePicker() {
        binding.etStartDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay)
                    }
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    binding.etStartDate.setText(dateFormat.format(selectedDate.time))
                },
                year, month, day
            )
            datePickerDialog.show()
        }
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
            Log.e("AddProjectActivity", "Error capturing image: ${e.message}", e)
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
                        Log.d("AddProjectActivity", "Gallery image added: $uri")
                    }
                } ?: data.data?.let { uri ->
                    selectedImageUris.add(uri)
                    Log.d("AddProjectActivity", "Single gallery image added: $uri")
                }
                imageAdapter.notifyDataSetChanged()
            }
        } else {
            Log.w("AddProjectActivity", "Gallery result not OK: ${result.resultCode}")
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
                    Log.d("AddProjectActivity", "Image captured and added: $path")
                } else {
                    Log.e("AddProjectActivity", "Captured image file not found: $path")
                }
            }
        } else {
            Log.w("AddProjectActivity", "Camera capture failed")
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
            Log.e("AddProjectActivity", "Error getting real path: ${e.message}", e)
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
                Toast.makeText(this, "Harap isi semua kolom wajib", Toast.LENGTH_SHORT).show()
                return
            }

            if (selectedWorkers.isEmpty()) {
                Toast.makeText(this, "Harap pilih setidaknya satu pekerja", Toast.LENGTH_SHORT).show()
                return
            }

            val projectId = dbHelper.addProject(
                name = name,
                location = location,
                startDate = startDate,
                status = status,
                progressPercentage = progress,
                notes = if (notes.isEmpty()) null else notes
            )

            if (projectId > 0) {
                // Save images
                selectedImageUris.forEach { uri ->
                    getRealPathFromUri(uri)?.let { path ->
                        dbHelper.addProjectImage(projectId.toInt(), path)
                        Log.d("AddProjectActivity", "Saved image: $path")
                    }
                }
                // Save worker assignments
                selectedWorkers.forEach { worker ->
                    dbHelper.assignWorkerToProject(worker.id, projectId.toInt())
                    Log.d("AddProjectActivity", "Assigned worker ${worker.name} to project $projectId")
                }
                Toast.makeText(this, "Proyek berhasil disimpan", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Gagal menyimpan proyek", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("AddProjectActivity", "Error saving project: ${e.message}", e)
            Toast.makeText(this, "Gagal menyimpan proyek: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}