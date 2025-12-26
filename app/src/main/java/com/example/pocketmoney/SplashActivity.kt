package com.example.pocketmoney

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import android.widget.ImageView

class SplashActivity : AppCompatActivity() {

    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        sharedPrefManager = SharedPrefManager(this)

        // Load the transparent GIF using Glide
        val ivSplashLogo: ImageView = findViewById(R.id.ivSplashLogo)
        Glide.with(this)
            .asGif()
            .load(R.drawable.splash_logo)
            .into(ivSplashLogo)

        // Delay for 3 seconds before redirecting
        Handler(Looper.getMainLooper()).postDelayed({
            if (!sharedPrefManager.isOnboardingCompleted()) {
                startActivity(Intent(this, OnboardingActivity::class.java))
            } else if (!sharedPrefManager.isLoggedIn()) {
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        }, 3000)
    }
}