package com.example.wellnesstracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class LaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        // wait 3 seconds then proceed to onboarding screen 1
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, OnboardingScreen1::class.java))
            finish()
        }, 3000)
    }
}