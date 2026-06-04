package com.example.wellnesstracker

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvProfileName: TextView
    private lateinit var tvJoinDate: TextView
    private lateinit var tvTotalHabits: TextView
    private lateinit var tvMoodEntries: TextView
    private lateinit var tvStreak: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"

        initViews()
        loadProfileData()
        setupClickListeners()
    }

    private fun initViews() {
        tvProfileName = findViewById(R.id.tvProfileName)
        tvJoinDate = findViewById(R.id.tvJoinDate)
        tvTotalHabits = findViewById(R.id.tvTotalHabits)
        tvMoodEntries = findViewById(R.id.tvMoodEntries)
        tvStreak = findViewById(R.id.tvStreak)
        
        // Setup back button
        findViewById<android.widget.ImageView>(R.id.ivBackButton).setOnClickListener {
            finish()
        }
    }

    private fun loadProfileData() {
        val prefs = Prefs(this)
        
        // Load user name
        tvProfileName.text = prefs.getUsername() ?: "User"
        
        // Load join date (simulate with current date)
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvJoinDate.text = "Member since ${dateFormat.format(Date())}"
        
        // Load statistics
        val habits = prefs.getHabits()
        tvTotalHabits.text = habits.size.toString()
        
        val moods = prefs.getMoodEntries()
        tvMoodEntries.text = moods.size.toString()
        
        // Calculate streak (simplified)
        tvStreak.text = calculateStreak(habits).toString()
    }

    private fun calculateStreak(habits: List<Habit>): Int {
        if (habits.isEmpty()) return 0
        
        var streak = 0
        val today = Calendar.getInstance()
        
        // Simple streak calculation - count consecutive days with completed habits
        for (i in 0..6) { // Check last 7 days
            val checkDate = Calendar.getInstance()
            checkDate.add(Calendar.DAY_OF_YEAR, -i)
            
            val hasCompletedHabit = habits.any { habit ->
                habit.completedDates.any { dateStr ->
                    val completedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                    completedDate?.let {
                        val cal = Calendar.getInstance()
                        cal.time = it
                        cal.get(Calendar.YEAR) == checkDate.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == checkDate.get(Calendar.DAY_OF_YEAR)
                    } ?: false
                }
            }
            
            if (hasCompletedHabit) {
                streak++
            } else {
                break
            }
        }
        
        return streak
    }

    private fun setupClickListeners() {
        findViewById<android.widget.LinearLayout>(R.id.changePasswordOption).setOnClickListener {
            showChangePasswordDialog()
        }
        
        findViewById<android.widget.LinearLayout>(R.id.exportDataOption).setOnClickListener {
            exportData()
        }
        
        findViewById<android.widget.LinearLayout>(R.id.logoutOption).setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showChangePasswordDialog() {
        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setMessage("This feature will be available in future updates.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun exportData() {
        val prefs = Prefs(this)
        val habits = prefs.getHabits()
        val moods = prefs.getMoodEntries()
        
        val exportData = StringBuilder()
        exportData.append("=== WellnessHub Data Export ===\n\n")
        
        exportData.append("HABITS:\n")
        habits.forEach { habit ->
            exportData.append("- ${habit.name}\n")
            exportData.append("  Completed: ${habit.completedDates.size} times\n")
        }
        
        exportData.append("\nMOOD ENTRIES:\n")
        moods.forEach { mood ->
            exportData.append("- ${mood.date}: ${mood.emoji} (${mood.note})\n")
        }
        
        // Simple share intent
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, exportData.toString())
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "WellnessHub Data Export")
        startActivity(Intent.createChooser(shareIntent, "Export Data"))
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        val prefs = Prefs(this)
        prefs.setLoggedIn(false)
        
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}