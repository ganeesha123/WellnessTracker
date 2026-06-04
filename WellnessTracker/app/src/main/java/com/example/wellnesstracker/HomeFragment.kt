package com.example.wellnesstracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private val prefs by lazy { Prefs(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        
        val tvCurrentDate = root.findViewById<TextView>(R.id.tvCurrentDate)
        val tvHabitsProgress = root.findViewById<TextView>(R.id.tvHabitsProgress)
        val tvTodayMood = root.findViewById<TextView>(R.id.tvTodayMood)
        val tvStreak = root.findViewById<TextView>(R.id.tvStreak)
        val btnQuickAddHabit = root.findViewById<Button>(R.id.btnQuickAddHabit)
        val btnQuickLogMood = root.findViewById<Button>(R.id.btnQuickLogMood)
        val weeklyChartContainer = root.findViewById<LinearLayout>(R.id.weeklyChartContainer)
        
        // Water tracking elements
        val tvWaterProgress = root.findViewById<TextView>(R.id.tvWaterProgress)
        val btnAddGlass = root.findViewById<Button>(R.id.btnAddGlass)
        val waterGlasses = listOf(
            root.findViewById<LinearLayout>(R.id.waterGlass1),
            root.findViewById<LinearLayout>(R.id.waterGlass2),
            root.findViewById<LinearLayout>(R.id.waterGlass3),
            root.findViewById<LinearLayout>(R.id.waterGlass4),
            root.findViewById<LinearLayout>(R.id.waterGlass5),
            root.findViewById<LinearLayout>(R.id.waterGlass6),
            root.findViewById<LinearLayout>(R.id.waterGlass7),
            root.findViewById<LinearLayout>(R.id.waterGlass8)
        )

        // Set current date
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        tvCurrentDate.text = dateFormat.format(Date())

        // Load and display statistics
        loadStatistics(tvHabitsProgress, tvTodayMood, tvStreak)
        loadWeeklyChart(weeklyChartContainer)
        loadWaterProgress(tvWaterProgress, waterGlasses)

        // Quick action buttons
        btnQuickAddHabit.setOnClickListener {
            android.util.Log.d("HomeFragment", "Quick add habit button clicked")
            // Navigate to habits tab and show add habit dialog
            val parentActivity = requireActivity()
            android.util.Log.d("HomeFragment", "Parent activity: ${parentActivity::class.simpleName}")
            if (parentActivity is HomeActivity) {
                android.util.Log.d("HomeFragment", "Calling navigateToHabitsTab()")
                parentActivity.navigateToHabitsTab()
            } else {
                android.util.Log.e("HomeFragment", "Parent activity is not HomeActivity!")
            }
        }

        btnQuickLogMood.setOnClickListener {
            // Navigate to mood tab
            val parentActivity = requireActivity()
            if (parentActivity is HomeActivity) {
                parentActivity.navigateToMoodTab()
            }
        }

        // Water tracking button
        btnAddGlass.setOnClickListener {
            val waterIntake = prefs.addWaterGlass()
            loadWaterProgress(tvWaterProgress, waterGlasses)
            android.util.Log.d("HomeFragment", "Added glass. Current: ${waterIntake.glassesConsumed}/${waterIntake.targetGlasses}")
        }

        return root
    }
    
    fun refreshHomeData() {
        // Public method to refresh home data when called from other fragments
        view?.let { root ->
            val tvHabitsProgress = root.findViewById<TextView>(R.id.tvHabitsProgress)
            val tvTodayMood = root.findViewById<TextView>(R.id.tvTodayMood)
            val tvStreak = root.findViewById<TextView>(R.id.tvStreak)
            val weeklyChartContainer = root.findViewById<LinearLayout>(R.id.weeklyChartContainer)
            val tvWaterProgress = root.findViewById<TextView>(R.id.tvWaterProgress)
            val waterGlasses = listOf(
                root.findViewById<LinearLayout>(R.id.waterGlass1),
                root.findViewById<LinearLayout>(R.id.waterGlass2),
                root.findViewById<LinearLayout>(R.id.waterGlass3),
                root.findViewById<LinearLayout>(R.id.waterGlass4),
                root.findViewById<LinearLayout>(R.id.waterGlass5),
                root.findViewById<LinearLayout>(R.id.waterGlass6),
                root.findViewById<LinearLayout>(R.id.waterGlass7),
                root.findViewById<LinearLayout>(R.id.waterGlass8)
            )
            
            loadStatistics(tvHabitsProgress, tvTodayMood, tvStreak)
            loadWeeklyChart(weeklyChartContainer)
            loadWaterProgress(tvWaterProgress, waterGlasses)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        view?.let { root ->
            val tvHabitsProgress = root.findViewById<TextView>(R.id.tvHabitsProgress)
            val tvTodayMood = root.findViewById<TextView>(R.id.tvTodayMood)
            val tvStreak = root.findViewById<TextView>(R.id.tvStreak)
            val weeklyChartContainer = root.findViewById<LinearLayout>(R.id.weeklyChartContainer)
            val tvWaterProgress = root.findViewById<TextView>(R.id.tvWaterProgress)
            val waterGlasses = listOf(
                root.findViewById<LinearLayout>(R.id.waterGlass1),
                root.findViewById<LinearLayout>(R.id.waterGlass2),
                root.findViewById<LinearLayout>(R.id.waterGlass3),
                root.findViewById<LinearLayout>(R.id.waterGlass4),
                root.findViewById<LinearLayout>(R.id.waterGlass5),
                root.findViewById<LinearLayout>(R.id.waterGlass6),
                root.findViewById<LinearLayout>(R.id.waterGlass7),
                root.findViewById<LinearLayout>(R.id.waterGlass8)
            )
            
            loadStatistics(tvHabitsProgress, tvTodayMood, tvStreak)
            loadWeeklyChart(weeklyChartContainer)
            loadWaterProgress(tvWaterProgress, waterGlasses)
        }
    }

    private fun loadStatistics(habitsProgressView: TextView, moodView: TextView, streakView: TextView) {
        // Load habits progress
        val habits = prefs.getHabits()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        if (habits.isNotEmpty()) {
            val completedToday = habits.count { it.completedDates.contains(today) }
            val progress = (completedToday * 100) / habits.size
            habitsProgressView.text = "$progress%"
        } else {
            habitsProgressView.text = "0%"
        }

        // Load today's mood
        val moods = prefs.getMoodEntries()
        val todayMood = moods.firstOrNull { entry ->
            entry.date == today
        }
        
        moodView.text = todayMood?.emoji ?: "😐"

        // Calculate streak
        val streak = calculateStreak(habits)
        streakView.text = streak.toString()
    }

    private fun calculateStreak(habits: List<Habit>): Int {
        if (habits.isEmpty()) return 0
        
        var streak = 0
        val calendar = Calendar.getInstance()
        
        // Check consecutive days with completed habits
        for (i in 0..6) { // Check last 7 days
            val checkDate = Calendar.getInstance()
            checkDate.add(Calendar.DAY_OF_YEAR, -i)
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(checkDate.time)
            
            val hasCompletedHabit = habits.any { habit ->
                habit.completedDates.contains(dateString)
            }
            
            if (hasCompletedHabit) {
                streak++
            } else {
                break
            }
        }
        
        return streak
    }

    private fun loadWaterProgress(progressView: TextView, glasses: List<LinearLayout>) {
        val waterIntake = prefs.getTodayWaterIntake()
        
        // Update progress text
        progressView.text = "${waterIntake.glassesConsumed}/${waterIntake.targetGlasses}"
        
        // Update glass visuals
        glasses.forEachIndexed { index, glass ->
            if (index < waterIntake.glassesConsumed) {
                // Filled glass
                glass.background = resources.getDrawable(R.drawable.water_glass_filled, null)
                val droplet = glass.getChildAt(0) as TextView
                droplet.alpha = 1.0f
            } else {
                // Empty glass
                glass.background = resources.getDrawable(R.drawable.water_glass_empty, null)
                val droplet = glass.getChildAt(0) as TextView
                droplet.alpha = 0.3f
            }
        }
        
        android.util.Log.d("HomeFragment", "Water progress loaded: ${waterIntake.glassesConsumed}/${waterIntake.targetGlasses}")
    }

    private fun loadWeeklyChart(container: LinearLayout) {
        container.removeAllViews()
        
        val habits = prefs.getHabits()
        val weeklyData = mutableListOf<Pair<String, Int>>() // Store date and percentage
        
        // Calculate completion percentage for each day of the week (last 7 days)
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault()) // Short day name (Mon, Tue, etc.)
        val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault()) // Day number (1, 2, etc.)
        val today = dateFormat.format(Date())
        
        android.util.Log.d("HomeFragment", "Loading weekly chart for today: $today")
        
        for (i in 6 downTo 0) {
            val checkDate = Calendar.getInstance()
            checkDate.add(Calendar.DAY_OF_YEAR, -i)
            val dateString = dateFormat.format(checkDate.time)
            val dayName = dayFormat.format(checkDate.time)
            val dayNumber = dayNumberFormat.format(checkDate.time)
            
            val percentage = if (habits.isNotEmpty()) {
                val completedHabits = habits.count { habit ->
                    habit.completedDates.contains(dateString)
                }
                (completedHabits * 100) / habits.size
            } else {
                0
            }
            
            weeklyData.add(Pair("$dayName\n$dayNumber", percentage))
            android.util.Log.d("HomeFragment", "Date: $dateString ($dayName $dayNumber) - Percentage: $percentage%")
        }
        
        // Create bar chart with day labels
        weeklyData.forEachIndexed { index, (dayLabel, percentage) ->
            val barContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM
                layoutParams = LinearLayout.LayoutParams(0, 140.dpToPx(), 1f).apply {
                    setMargins(4, 0, 4, 0)
                }
            }
            
            // Add percentage label on top of bar
            val percentageLabel = TextView(requireContext()).apply {
                text = "$percentage%"
                textSize = 10f
                setTextColor(android.graphics.Color.parseColor("#2C3E50"))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 0, 0, 4)
            }
            
            // Create the bar with proper height calculation
            val maxBarHeight = 80 // Maximum height for 100%
            val barHeight = ((percentage * maxBarHeight) / 100).coerceAtLeast(4) // Minimum 4px height
            val bar = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    barHeight.dpToPx()
                )
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(getBarColor(percentage))
                    cornerRadius = 8f
                }
            }
            
            // Add day label at the bottom
            val dayLabelView = TextView(requireContext()).apply {
                text = dayLabel
                textSize = 10f
                setTextColor(android.graphics.Color.parseColor("#7F8C8D"))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 4, 0, 0)
            }
            
            // Check if this is today and highlight it
            val dateString = dateFormat.format(Calendar.getInstance().apply { 
                add(Calendar.DAY_OF_YEAR, -(6 - index)) 
            }.time)
            if (dateString == today) {
                dayLabelView.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                dayLabelView.setTypeface(null, android.graphics.Typeface.BOLD)
                percentageLabel.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                percentageLabel.setTypeface(null, android.graphics.Typeface.BOLD)
                android.util.Log.d("HomeFragment", "Today's bar highlighted: $dateString - $percentage%")
            }
            
            barContainer.addView(percentageLabel)
            barContainer.addView(bar)
            barContainer.addView(dayLabelView)
            container.addView(barContainer)
        }
    }
    
    private fun getBarColor(percentage: Int): Int {
        return when {
            percentage >= 80 -> android.graphics.Color.parseColor("#4CAF50") // Green
            percentage >= 60 -> android.graphics.Color.parseColor("#FF9800") // Orange
            percentage >= 40 -> android.graphics.Color.parseColor("#FFC107") // Yellow
            percentage > 0 -> android.graphics.Color.parseColor("#F44336") // Red
            else -> android.graphics.Color.parseColor("#E0E0E0") // Gray for 0%
        }
    }
    
    private fun Int.dpToPx(): Int {
        val density = requireContext().resources.displayMetrics.density
        return (this * density).toInt()
    }
}