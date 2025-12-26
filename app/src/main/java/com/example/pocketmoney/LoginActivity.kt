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

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegisterLink: TextView
    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sharedPrefManager = SharedPrefManager(this)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegisterLink = findViewById(R.id.tvRegisterLink)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty()) {
                etUsername.error = "Username is required"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                etPassword.error = "Password is required"
                return@setOnClickListener
            }

            val user = authenticateUser(username, password)
            if (user != null) {
                sharedPrefManager.setLoggedInUser(username)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
        }

        tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun authenticateUser(username: String, password: String): User? {
        val users = loadUsers()
        return users.find { it.username == username && it.password == password }
    }

    private fun loadUsers(): List<User> {
        val file = File(filesDir, "users.json")
        if (!file.exists()) return emptyList()
        val json = file.readText()
        val type = object : TypeToken<List<User>>() {}.type
        return Gson().fromJson(json, type) ?: emptyList()
    }
}