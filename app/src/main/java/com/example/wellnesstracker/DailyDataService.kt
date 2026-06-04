package com.example.wellnesstracker

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class DailyDataService(private val context: Context) {
    private val prefs = Prefs(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    
    fun startDailyDataSaving() {
        android.util.Log.d("DailyDataService", "Starting daily data saving service")
        
        // Calculate time until next 12 AM (midnight)
        val calendar = Calendar.getInstance()
        val now = calendar.time
        
        // Set to next midnight (12 AM)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_MONTH, 1) // Next day's midnight
        
        val timeUntilMidnight = calendar.timeInMillis - now.time
        
        android.util.Log.d("DailyDataService", "Time until next midnight: ${timeUntilMidnight / 1000 / 60} minutes")
        
        // Schedule daily data saving at 12 AM every day
        executor.scheduleAtFixedRate({
            saveCurrentDayData()
        }, timeUntilMidnight, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS)
        
        // Also save data immediately for current day if it doesn't exist
        saveCurrentDayData()
    }
    
    private fun saveCurrentDayData() {
        try {
            val today = dateFormat.format(Date())
            android.util.Log.d("DailyDataService", "Saving daily data for: $today")
            
            // Get current day's data or create new
            val existingData = prefs.getDailyData(today)
            val dailyData = existingData ?: DailyData(today)
            
            // Collect current habits completion status
            val habits = prefs.getHabits()
            val completedHabits = mutableListOf<String>()
            
            for (habit in habits) {
                if (habit.completedDates.contains(today)) {
                    completedHabits.add(habit.name)
                }
            }
            
            // Update daily data
            dailyData.habitsCompleted.clear()
            dailyData.habitsCompleted.addAll(completedHabits)
            
            // Get today's mood entries
            val allMoods = prefs.getMoodEntries()
            val todayMoods = allMoods.filter { it.date == today }.toMutableList()
            dailyData.moodEntries.clear()
            dailyData.moodEntries.addAll(todayMoods)

            // Update timestamp
            dailyData.timestamp = System.currentTimeMillis()

            // Save the daily data
            prefs.saveDailyData(dailyData)

            // Reset water intake for the new day (water tracking resets daily)
            prefs.resetDailyWater(today)
            
            android.util.Log.d("DailyDataService", "Daily data saved: ${completedHabits.size} habits, ${todayMoods.size} moods")
            
        } catch (e: Exception) {
            android.util.Log.e("DailyDataService", "Error saving daily data: ${e.message}", e)
        }
    }
    
    fun saveDailyDataManually() {
        saveCurrentDayData()
    }
    
    fun getWeeklyMoodHistory(): List<WeeklyMoodSummary> {
        val weeklyData = mutableListOf<WeeklyMoodSummary>()
        val calendar = Calendar.getInstance()
        
        // Go back 4 weeks from today
        for (weekOffset in 0..3) {
            // Set to Monday of the week
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.WEEK_OF_YEAR, -weekOffset)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            
            val weekStart = dateFormat.format(calendar.time)
            android.util.Log.d("DailyDataService", "Getting week data for offset $weekOffset, week start: $weekStart")
            val weekData = prefs.getWeeklyMoodData(weekStart)
            android.util.Log.d("DailyDataService", "Week data - Start: ${weekData.weekStartDate}, Entries: ${weekData.totalEntries}")
            weeklyData.add(weekData)
        }
        
        return weeklyData
    }
    
    fun stopService() {
        executor.shutdown()
    }
}