package com.example.wellnesstracker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class MoodFragment : Fragment() {
    private val prefs by lazy { Prefs(requireContext()) }
    private var selectedEmoji = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_mood, container, false)
        
        val emojiContainer = root.findViewById<LinearLayout>(R.id.emojiContainer)
        val etNote = root.findViewById<EditText>(R.id.etNote)
        val btnSaveMood = root.findViewById<Button>(R.id.btnSaveMood)

        setupEmojiSelector(emojiContainer)
        
        btnSaveMood.setOnClickListener {
            android.util.Log.d("MoodFragment", "Save mood button clicked")
            android.util.Log.d("MoodFragment", "Selected emoji: '$selectedEmoji'")
            if (selectedEmoji.isNotEmpty()) {
                val note = etNote.text.toString().trim()
                android.util.Log.d("MoodFragment", "Saving mood: emoji='$selectedEmoji', note='$note'")
                saveMoodEntry(selectedEmoji, note)
                etNote.text.clear()
                selectedEmoji = ""
                updateEmojiSelection(emojiContainer)
                
                // Refresh weekly mood history to show new data
                val weeklyHistoryContainer = root.findViewById<LinearLayout>(R.id.weeklyMoodContainer)
                if (weeklyHistoryContainer != null) {
                    displayWeeklyMoodHistory(weeklyHistoryContainer)
                }
                
                // Refresh home page to update today's mood
                val parentActivity = requireActivity()
                if (parentActivity is HomeActivity) {
                    parentActivity.refreshHomeFragment()
                }
                
                Toast.makeText(requireContext(), "Mood saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please select a mood first", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize weekly mood history section
        val weeklyHistoryContainer = root.findViewById<LinearLayout>(R.id.weeklyMoodContainer)
        if (weeklyHistoryContainer != null) {
            displayWeeklyMoodHistory(weeklyHistoryContainer)
        }
        
        return root
    }

    private fun setupEmojiSelector(container: LinearLayout) {
        val emojis = listOf(
            "😢" to "Sad", 
            "😔" to "Down", 
            "😐" to "Neutral", 
            "🙂" to "Good", 
            "😃" to "Great"
        )
        
        container.removeAllViews()
        
        for ((emoji, mood) in emojis) {
            val emojiLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(16, 16, 16, 16)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    android.util.Log.d("MoodFragment", "Emoji clicked: $emoji")
                    selectedEmoji = emoji
                    android.util.Log.d("MoodFragment", "Selected emoji set to: '$selectedEmoji'")
                    updateEmojiSelection(container)
                }
            }
            
            val emojiButton = TextView(requireContext()).apply {
                text = emoji
                textSize = 28f
                gravity = android.view.Gravity.CENTER
                try {
                    background = resources.getDrawable(R.drawable.circle_background, null)
                } catch (e: Exception) {
                    setBackgroundColor(android.graphics.Color.parseColor("#E8E8E8"))
                }
                setPadding(24, 24, 24, 24)
            }
            
            val moodLabel = TextView(requireContext()).apply {
                text = mood
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.parseColor("#7F8C8D"))
            }
            
            emojiLayout.addView(emojiButton)
            emojiLayout.addView(moodLabel)
            container.addView(emojiLayout)
        }
    }

    private fun updateEmojiSelection(container: LinearLayout) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i) as LinearLayout
            val emojiButton = child.getChildAt(0) as TextView
            
            if (emojiButton.text == selectedEmoji) {
                emojiButton.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            } else {
                try {
                    emojiButton.background = resources.getDrawable(R.drawable.circle_background, null)
                } catch (e: Exception) {
                    // Fallback to simple background
                    emojiButton.setBackgroundColor(android.graphics.Color.parseColor("#E8E8E8"))
                }
            }
        }
        
        // Enable/disable save button based on selection
        val btnSaveMood = view?.findViewById<Button>(R.id.btnSaveMood)
        btnSaveMood?.isEnabled = selectedEmoji.isNotEmpty()
        android.util.Log.d("MoodFragment", "Updated save button enabled: ${selectedEmoji.isNotEmpty()}, selected: '$selectedEmoji'")
    }

    private fun saveMoodEntry(emoji: String, note: String) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = Date()
            
            val entry = MoodEntry(
                emoji = emoji,
                date = dateFormat.format(now),
                time = timeFormat.format(now),
                note = note
            )
            
            android.util.Log.d("MoodFragment", "Created mood entry: ${entry.emoji} on ${entry.date} at ${entry.time}")
            
            val entries = prefs.getMoodEntries()
            android.util.Log.d("MoodFragment", "Current mood entries count: ${entries.size}")
            entries.add(0, entry) // Add to beginning
            prefs.saveMoodEntries(entries)
            android.util.Log.d("MoodFragment", "Saved mood entries, new count: ${entries.size}")
            
            // Also save to daily data for persistence
            val dailyDataService = DailyDataService(requireContext())
            dailyDataService.saveDailyDataManually()
            android.util.Log.d("MoodFragment", "Saved to daily data service")
            
        } catch (e: Exception) {
            android.util.Log.e("MoodFragment", "Error saving mood entry: ${e.message}", e)
        }
    }


    
    private fun displayWeeklyMoodHistory(container: LinearLayout) {
        container.removeAllViews()
        
        try {
            val dailyDataService = DailyDataService(requireContext())
            val weeklyData = dailyDataService.getWeeklyMoodHistory()
            
            android.util.Log.d("MoodFragment", "Displaying ${weeklyData.size} weeks of mood history")
            
            for ((index, week) in weeklyData.withIndex()) {
                android.util.Log.d("MoodFragment", "Week $index - Start: ${week.weekStartDate}, Entries: ${week.totalEntries}, Daily moods: ${week.dailyMoods.size}")
                val weekView = createWeeklyMoodView(week)
                container.addView(weekView)
            }
        } catch (e: Exception) {
            android.util.Log.e("MoodFragment", "Error displaying weekly mood history: ${e.message}", e)
        }
    }
    
    private fun createWeeklyMoodView(weekData: WeeklyMoodSummary): View {
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            elevation = 2f
        }
        
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
        }
        
        // Week header
        val headerText = TextView(requireContext()).apply {
            text = "Week of ${formatWeekDate(weekData.weekStartDate)}"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#2C3E50"))
        }
        layout.addView(headerText)
        
        // Daily moods grid
        val daysLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 12)
        }
        
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val calendar = java.util.Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        
        try {
            calendar.time = dateFormat.parse(weekData.weekStartDate) ?: java.util.Date()
        } catch (e: Exception) {
            calendar.time = java.util.Date()
        }
        
        for (i in 0..6) {
            val dayContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = android.view.Gravity.CENTER
                setPadding(8, 8, 8, 8)
            }
            
            val dayLabel = TextView(requireContext()).apply {
                text = dayNames[i]
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.parseColor("#7F8C8D"))
            }
            dayContainer.addView(dayLabel)
            
            val currentDate = dateFormat.format(calendar.time)
            val moodEntry = weekData.dailyMoods[currentDate]
            
            val moodView = TextView(requireContext()).apply {
                text = moodEntry?.emoji ?: "⭕"
                textSize = 24f
                gravity = android.view.Gravity.CENTER
                setPadding(4, 8, 4, 8)
            }
            dayContainer.addView(moodView)
            
            daysLayout.addView(dayContainer)
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        
        layout.addView(daysLayout)
        
        // Week summary
        if (weekData.totalEntries > 0) {
            val summaryText = TextView(requireContext()).apply {
                text = "Most frequent mood: ${weekData.averageMood} (${weekData.totalEntries} days tracked)"
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#95A5A6"))
            }
            layout.addView(summaryText)
        } else {
            val noDataText = TextView(requireContext()).apply {
                text = "No mood data for this week"
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#BDC3C7"))
                gravity = android.view.Gravity.CENTER
            }
            layout.addView(noDataText)
        }
        
        cardView.addView(layout)
        return cardView
    }
    
    private fun formatWeekDate(dateString: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: java.util.Date())
        } catch (e: Exception) {
            dateString
        }
    }
}
