package com.cvmmk

import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cvmmk.databinding.ActivityAddWorkerBinding
import com.google.android.material.textfield.TextInputLayout

class AddWorkerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddWorkerBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddWorkerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // Apply entrance animations
        applyAnimations()

        // Save worker button click listener
        binding.btnSaveWorker.setOnClickListener {
            saveWorker()
        }
    }

    private fun applyAnimations() {
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.cardInputs.startAnimation(slideUp)
        binding.btnSaveWorker.startAnimation(slideUp)
    }

    private fun saveWorker() {
        val name = binding.etWorkerName.text.toString().trim()
        val role = binding.etWorkerRole.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        var isValid = true
        if (name.isEmpty()) {
            binding.tilWorkerName.error = "Worker name is required"
            isValid = false
        }
        if (role.isEmpty()) {
            binding.tilWorkerRole.error = "Role is required"
            isValid = false
        }
        if (username.isEmpty()) {
            binding.tilUsername.error = "Username is required"
            isValid = false
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }
        if (!isValid) {
            shakeInvalidFields()
            Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            Log.d("AddWorkerActivity", "Adding worker: name=$name, role=$role")
            val workerId = dbHelper.addWorker(name, role)
            if (workerId != -1L) {
                Log.d("AddWorkerActivity", "Worker added, ID=$workerId, adding user: username=$username")
                val userResult = dbHelper.addUser(username, password, "worker", workerId.toInt())
                if (userResult != -1L) {
                    db.setTransactionSuccessful()
                    Log.d("AddWorkerActivity", "User added, ID=$userResult")
                    binding.btnSaveWorker.animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .setDuration(100)
                        .withEndAction {
                            binding.btnSaveWorker.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()
                        }
                        .start()
                    Toast.makeText(this, "Worker and user account added successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    binding.tilUsername.error = "Username may already exist"
                    shakeInvalidFields()
                    Toast.makeText(this, "Failed to add user account", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Failed to add worker", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("AddWorkerActivity", "Error saving worker: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            db.endTransaction()
        }
    }

    private fun shakeInvalidFields() {
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
        if (binding.etWorkerName.text.isNullOrEmpty()) binding.tilWorkerName.startAnimation(shake)
        if (binding.etWorkerRole.text.isNullOrEmpty()) binding.tilWorkerRole.startAnimation(shake)
        if (binding.etUsername.text.isNullOrEmpty()) binding.tilUsername.startAnimation(shake)
        if (binding.etPassword.text.isNullOrEmpty() || binding.etPassword.text.toString().length < 6) binding.tilPassword.startAnimation(shake)
    }
}