package com.cvmmk

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cvmmk.databinding.ActivityWorkerAccountsBinding

class WorkerAccountsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWorkerAccountsBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: WorkerAccountAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkerAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // Set up RecyclerView
        binding.rvWorkerAccounts.layoutManager = LinearLayoutManager(this)
        loadWorkerAccounts()

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadWorkerAccounts() {
        val accounts = dbHelper.getWorkerAccounts()
        if (::adapter.isInitialized) {
            adapter.updateData(accounts) // ✅ update data di adapter yang sudah ada
        } else {
            adapter = WorkerAccountAdapter(accounts,
                onEditClick = { account -> showEditDialog(account) },
                onDeleteClick = { account ->
                    AlertDialog.Builder(this)
                        .setTitle("Delete Account")
                        .setMessage("Are you sure you want to delete ${account.workerName}'s account?")
                        .setPositiveButton("Delete") { _, _ ->
                            val success = dbHelper.deleteWorkerAccount(account.userId, account.workerId)
                            if (success) {
                                Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                loadWorkerAccounts()
                            } else {
                                Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            )
            binding.rvWorkerAccounts.adapter = adapter // ✅ Harus di sini, bukan di dalam onDeleteClick
        }
    }




    private fun showEditDialog(account: WorkerAccount) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_worker_account, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_worker_name)
        val etRole = dialogView.findViewById<EditText>(R.id.et_worker_role)
        val etUsername = dialogView.findViewById<EditText>(R.id.et_username)
        val etPassword = dialogView.findViewById<EditText>(R.id.et_password)

        // Pre-fill fields
        etName.setText(account.workerName)
        etRole.setText(account.workerRole)
        etUsername.setText(account.username)
        etPassword.setText(account.password)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Account")
            .setView(dialogView)
            .setPositiveButton("Save", null) // Manual handler
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val newName = etName.text.toString().trim()
                val newRole = etRole.text.toString().trim()
                val newUsername = etUsername.text.toString().trim()
                val newPassword = etPassword.text.toString().trim()

                // Gunakan nilai lama jika tidak diubah
                val finalName = if (newName.isNotEmpty()) newName else account.workerName ?: ""
                val finalRole = if (newRole.isNotEmpty()) newRole else account.workerRole ?: ""
                val finalUsername = if (newUsername.isNotEmpty()) newUsername else account.username ?: ""
                val finalPassword = if (newPassword.isNotEmpty()) newPassword else account.password ?: ""

                // Validasi: username dan password wajib diisi
                if (finalUsername.isEmpty() || finalPassword.isEmpty()) {
                    Toast.makeText(this, "Username and Password cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Cek apakah username diubah, dan sudah dipakai user lain
                if (finalUsername != account.username && dbHelper.isUsernameTaken(finalUsername, account.userId)) {
                    Toast.makeText(this, "Username already exists, please choose another", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val db = dbHelper.writableDatabase
                db.beginTransaction()
                try {
                    val workerUpdated = dbHelper.updateWorker(account.workerId, finalName, finalRole)
                    val userUpdated = dbHelper.updateUserAccount(account.userId, finalUsername, finalPassword)
                    Log.d("UPDATE_CHECK", "workerUpdated: $workerUpdated, userUpdated: $userUpdated")

                    if (workerUpdated >= 0 && userUpdated >= 0) {
                        db.setTransactionSuccessful()
                        Toast.makeText(this, "Account updated successfully", Toast.LENGTH_SHORT).show()
                        loadWorkerAccounts()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Failed to update account", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    db.endTransaction()
                }
            }
        }

        dialog.show()
    }

}