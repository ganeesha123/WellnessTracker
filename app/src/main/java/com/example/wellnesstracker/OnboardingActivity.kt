package com.example.wellnesstracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Simple linear onboarding flow using separate activities for brevity
        val pref = Prefs(this)
        if (pref.isOnboarded()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        // Launch first onboarding screen
        startActivity(Intent(this, OnboardingScreen1::class.java))
        finish()
    }
}
