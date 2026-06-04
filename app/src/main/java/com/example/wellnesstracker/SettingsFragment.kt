package com.example.wellnesstracker

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {
    private val prefs by lazy { Prefs(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        
        val etInterval = root.findViewById<EditText>(R.id.etInterval)
        val btnSaveInterval = root.findViewById<Button>(R.id.btnSaveInterval)
        val resetDataOption = root.findViewById<LinearLayout>(R.id.resetDataOption)
        val aboutOption = root.findViewById<LinearLayout>(R.id.aboutOption)

        // Load current interval
        etInterval.setText(prefs.getHydrationIntervalMinutes().toString())

        // Save hydration interval
        btnSaveInterval.setOnClickListener {
            val intervalText = etInterval.text.toString().trim()
            if (intervalText.isNotEmpty()) {
                val minutes = intervalText.toIntOrNull()
                if (minutes != null && minutes > 0) {
                    prefs.setHydrationIntervalMinutes(minutes)
                    scheduleHydrationReminder(minutes)
                    Toast.makeText(requireContext(), "Reminders scheduled for every $minutes minutes", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please enter an interval", Toast.LENGTH_SHORT).show()
            }
        }

        // Reset data option
        resetDataOption.setOnClickListener {
            showResetDataDialog()
        }

        // About option
        aboutOption.setOnClickListener {
            showAboutDialog()
        }

        return root
    }

    private fun scheduleHydrationReminder(minutes: Int) {
        val context = requireContext()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, HydrationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Cancel any existing alarms
        alarmManager.cancel(pendingIntent)
        
        // Schedule new repeating alarm
        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000)
        val intervalMillis = minutes * 60 * 1000L
        
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            intervalMillis,
            pendingIntent
        )
    }

    private fun showResetDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset All Data")
            .setMessage("This will permanently delete all your habits and mood entries. This action cannot be undone.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Reset") { _, _ ->
                resetAllData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetAllData() {
        prefs.clearAllData()
        Toast.makeText(requireContext(), "All data has been reset", Toast.LENGTH_LONG).show()
        
        // Refresh the current fragment if it's habits or mood
        val activity = requireActivity()
        if (activity is HomeActivity) {
            // Trigger refresh of other fragments by recreating activity
            activity.recreate()
        }
    }

    private fun showAboutDialog() {
        val aboutMessage = """
            WellnessHub v1.0.0
            
            A comprehensive wellness tracking app designed to help you maintain healthy habits and track your emotional well-being.
            
            Features:
            • Daily Habit Tracking
            • Mood Journal with Emoji Selector
            • Hydration Reminders
            • Progress Statistics
            • Data Export & Sharing
            
            Developed for IT2010 Mobile Application Development
            
            © 2023 WellnessHub
        """.trimIndent()
        
        AlertDialog.Builder(requireContext())
            .setTitle("About WellnessHub")
            .setMessage(aboutMessage)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("OK", null)
            .show()
    }
}
