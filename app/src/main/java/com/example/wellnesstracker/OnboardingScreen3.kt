package com.example.wellnesstracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class OnboardingScreen3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding3)
        findViewById<Button>(R.id.btnFinish).setOnClickListener {
            Prefs(this).setOnboarded(true)
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }
}
