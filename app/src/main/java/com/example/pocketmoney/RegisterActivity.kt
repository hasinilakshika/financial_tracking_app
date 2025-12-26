package com.example.pocketmoney

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLoginLink: TextView
    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize SharedPrefManager
        sharedPrefManager = SharedPrefManager(this)

        // Initialize views before using them
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLoginLink = findViewById(R.id.tvLoginLink)

        // Now set up the listeners
        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Validate username
            if (username.isEmpty()) {
                etUsername.error = "Username is required"
                return@setOnClickListener
            }

            // Validate password
            if (password.isEmpty()) {
                etPassword.error = "Password is required"
                return@setOnClickListener
            }

            // Validate confirm password
            if (confirmPassword.isEmpty()) {
                etConfirmPassword.error = "Confirm Password is required"
                return@setOnClickListener
            }

            // Check if passwords match
            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Load existing users
            val users = loadUsers().toMutableList()
            if (users.any { it.username == username }) {
                etUsername.error = "Username already exists"
                return@setOnClickListener
            }

            // Register the new user
            val newUser = User(username, password)
            users.add(newUser)
            saveUsers(users)
            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()

            // Redirect to LoginActivity for all users
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadUsers(): List<User> {
        val file = File(filesDir, "users.json")
        if (!file.exists()) return emptyList()
        val json = file.readText()
        val type = object : TypeToken<List<User>>() {}.type
        return Gson().fromJson(json, type) ?: emptyList()
    }

    private fun saveUsers(users: List<User>) {
        val file = File(filesDir, "users.json")
        val json = Gson().toJson(users)
        file.writeText(json)
    }
}