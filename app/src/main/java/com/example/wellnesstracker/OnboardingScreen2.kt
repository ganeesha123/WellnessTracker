package com.example.wellnesstracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class OnboardingScreen2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding2)
        findViewById<Button>(R.id.btnNext).setOnClickListener {
            startActivity(Intent(this, OnboardingScreen3::class.java))
            finish()
        }
    }
}
