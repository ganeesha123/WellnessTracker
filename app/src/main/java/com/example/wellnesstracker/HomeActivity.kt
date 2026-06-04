package com.example.wellnesstracker

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {
    private lateinit var dailyDataService: DailyDataService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupHeader()
        setupBottomNavigation()
        
        // Initialize daily data service for automatic saving at 12 AM
        dailyDataService = DailyDataService(this)
        dailyDataService.startDailyDataSaving()

        // default fragment - set Home as default
        if (savedInstanceState == null) {
            showFragment(HomeFragment())
            findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.menu_home
        }
    }

    private fun setupHeader() {
        val prefs = Prefs(this)
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val ivProfile = findViewById<ImageView>(R.id.ivProfile)
        
        // Set greeting based on time
        tvGreeting.text = getTimeBasedGreeting()
        
        // Set user name
        tvUserName.text = "Hello, ${prefs.getUsername() ?: "User"}!"
        
        // Set profile click listener
        ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun getTimeBasedGreeting(): String {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (currentHour) {
            in 5..11 -> "Good Morning!"
            in 12..17 -> "Good Afternoon!"
            in 18..21 -> "Good Evening!"
            else -> "Good Night!"
        }
    }

    private fun setupBottomNavigation() {
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setOnItemSelectedListener { item ->
            android.util.Log.d("HomeActivity", "Bottom nav item clicked: ${item.itemId}")
            when (item.itemId) {
                R.id.menu_home -> {
                    android.util.Log.d("HomeActivity", "Navigating to Home")
                    showFragment(HomeFragment())
                }
                R.id.menu_habits -> {
                    android.util.Log.d("HomeActivity", "Navigating to Habits")
                    showFragment(HabitsFragment())
                }
                R.id.menu_mood -> {
                    android.util.Log.d("HomeActivity", "Navigating to Mood") 
                    showFragment(MoodFragment())
                }
                R.id.menu_settings -> {
                    android.util.Log.d("HomeActivity", "Navigating to Settings")
                    showFragment(SettingsFragment())
                }
            }
            true
        }
    }

    private fun showFragment(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, f)
            .commit()
        
        // Debug log to check if navigation is working
        android.util.Log.d("HomeActivity", "Switching to fragment: ${f.javaClass.simpleName}")
    }

    // Helper methods for HomeFragment quick actions
    fun navigateToHabitsTab() {
        android.util.Log.d("HomeActivity", "navigateToHabitsTab() called")
        try {
            showFragment(HabitsFragment())
            findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.menu_habits
            android.util.Log.d("HomeActivity", "Successfully navigated to habits tab")
        } catch (e: Exception) {
            android.util.Log.e("HomeActivity", "Error navigating to habits tab: ${e.message}", e)
        }
    }

    fun navigateToMoodTab() {
        showFragment(MoodFragment())
        findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = R.id.menu_mood
    }
    
    fun refreshHomeFragment() {
        // Find and refresh the home fragment if it exists
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
        if (currentFragment is HomeFragment) {
            currentFragment.refreshHomeData()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::dailyDataService.isInitialized) {
            dailyDataService.stopService()
        }
    }
}
