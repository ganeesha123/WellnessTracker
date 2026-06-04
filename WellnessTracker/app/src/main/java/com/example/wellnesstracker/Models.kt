package com.example.wellnesstracker

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Habit(
    var name: String, 
    var description: String = "",
    var completedDates: MutableList<String> = mutableListOf()
)

data class MoodEntry(
    var emoji: String,
    var date: String,
    var time: String,
    var note: String = ""
)

data class DailyData(
    var date: String, // Format: "yyyy-MM-dd"
    var habitsCompleted: MutableList<String> = mutableListOf(), // List of completed habit names
    var moodEntries: MutableList<MoodEntry> = mutableListOf(),
    var notes: String = "",
    var timestamp: Long = System.currentTimeMillis() // When this daily data was saved
)

data class WeeklyMoodSummary(
    var weekStartDate: String, // Format: "yyyy-MM-dd" (Monday)
    var dailyMoods: MutableMap<String, MoodEntry> = mutableMapOf(), // date -> mood entry
    var averageMood: String = "",
    var totalEntries: Int = 0
)

data class WaterIntake(
    var date: String, // Format: "yyyy-MM-dd"
    var glassesConsumed: Int = 0,
    var targetGlasses: Int = 8,
    var timestamps: MutableList<Long> = mutableListOf() // When each glass was consumed
)

class Prefs(private val ctx: Context) {
    private val sp = ctx.getSharedPreferences("wt_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun isOnboarded(): Boolean = sp.getBoolean("onboarded", false)
    fun setOnboarded(v: Boolean) = sp.edit().putBoolean("onboarded", v).apply()

    fun getHabits(): MutableList<Habit> {
        val json = sp.getString("habits", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Habit>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveHabits(h: MutableList<Habit>) {
        sp.edit().putString("habits", gson.toJson(h)).apply()
    }

    // Legacy mood support
    fun getMoods(): MutableList<MoodEntry> {
        val json = sp.getString("moods", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<MoodEntry>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveMoods(list: MutableList<MoodEntry>) {
        sp.edit().putString("moods", gson.toJson(list)).apply()
    }

    // New mood entry methods
    fun getMoodEntries(): MutableList<MoodEntry> {
        val json = sp.getString("mood_entries", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<MoodEntry>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveMoodEntries(list: MutableList<MoodEntry>) {
        sp.edit().putString("mood_entries", gson.toJson(list)).apply()
    }

    fun setHydrationIntervalMinutes(m: Int) = sp.edit().putInt("hydration_min", m).apply()
    fun getHydrationIntervalMinutes(): Int = sp.getInt("hydration_min", 60)

    // User credentials and profile
    fun saveUserCredentials(email: String, password: String) {
        sp.edit()
            .putString("user_email", email)
            .putString("user_password", password)
            .apply()
    }

    fun validateCredentials(email: String, password: String): Boolean {
        val savedEmail = sp.getString("user_email", null)
        val savedPassword = sp.getString("user_password", null)
        return email == savedEmail && password == savedPassword
    }

    fun isUserRegistered(): Boolean {
        return sp.getString("user_email", null) != null
    }

    fun getUsername(): String? {
        val email = sp.getString("user_email", null)
        return email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
    }

    fun setLoggedIn(loggedIn: Boolean) {
        sp.edit().putBoolean("logged_in", loggedIn).apply()
    }

    fun isLoggedIn(): Boolean = sp.getBoolean("logged_in", false)

    fun clearAllData() {
        sp.edit()
            .remove("habits")
            .remove("mood_entries")
            .remove("moods")
            .apply()
    }

    // Daily data persistence methods
    fun getDailyData(date: String): DailyData? {
        val json = sp.getString("daily_data_$date", null) ?: return null
        return gson.fromJson(json, DailyData::class.java)
    }

    fun saveDailyData(dailyData: DailyData) {
        sp.edit().putString("daily_data_${dailyData.date}", gson.toJson(dailyData)).apply()
    }

    fun getAllDailyData(): MutableList<DailyData> {
        val allData = mutableListOf<DailyData>()
        val allKeys = sp.all.keys.filter { it.startsWith("daily_data_") }
        
        for (key in allKeys) {
            sp.getString(key, null)?.let { json ->
                try {
                    val dailyData = gson.fromJson(json, DailyData::class.java)
                    allData.add(dailyData)
                } catch (e: Exception) {
                    android.util.Log.e("Prefs", "Error parsing daily data for key $key: ${e.message}")
                }
            }
        }
        
        return allData.sortedByDescending { it.date }.toMutableList()
    }

    fun getWeeklyMoodData(weekStartDate: String): WeeklyMoodSummary {
        val weeklyData = WeeklyMoodSummary(weekStartDate)
        
        // Get data for 7 days starting from weekStartDate
        val calendar = java.util.Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        calendar.time = dateFormat.parse(weekStartDate) ?: java.util.Date()
        
        val moodCounts = mutableMapOf<String, Int>()
        
        val todayDate = dateFormat.format(java.util.Date())
        android.util.Log.d("WeeklyMood", "Getting weekly mood data for week starting: $weekStartDate, today: $todayDate")
        
        for (i in 0..6) {
            val currentDate = dateFormat.format(calendar.time)
            
            // Check if this is today - if so, get current mood entries instead of daily data
            val moodEntry = if (currentDate == todayDate) {
                // For today, get the latest mood from current mood entries
                val allMoods = getMoodEntries()
                val todayMood = allMoods.firstOrNull { it.date == currentDate }
                android.util.Log.d("WeeklyMood", "Today ($currentDate): Found ${allMoods.size} total moods, today's mood: ${todayMood?.emoji}")
                todayMood
            } else {
                // For other days, get from saved daily data
                val dailyData = getDailyData(currentDate)
                val savedMood = dailyData?.moodEntries?.lastOrNull()
                android.util.Log.d("WeeklyMood", "Date ($currentDate): Saved mood: ${savedMood?.emoji}")
                savedMood
            }
            
            moodEntry?.let { mood ->
                weeklyData.dailyMoods[currentDate] = mood
                weeklyData.totalEntries++
                moodCounts[mood.emoji] = moodCounts.getOrDefault(mood.emoji, 0) + 1
                android.util.Log.d("WeeklyMood", "Added mood ${mood.emoji} for date $currentDate")
            }
            
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        
        // Calculate average mood (most frequent)
        weeklyData.averageMood = moodCounts.maxByOrNull { it.value }?.key ?: ""
        
        return weeklyData
    }

    // Water Intake Methods
    fun getWaterIntake(date: String): WaterIntake? {
        val json = sp.getString("water_$date", null) ?: return null
        return gson.fromJson(json, WaterIntake::class.java)
    }

    fun saveWaterIntake(waterIntake: WaterIntake) {
        val json = gson.toJson(waterIntake)
        sp.edit().putString("water_${waterIntake.date}", json).apply()
    }

    fun getTodayWaterIntake(): WaterIntake {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return getWaterIntake(today) ?: WaterIntake(date = today)
    }

    fun addWaterGlass(): WaterIntake {
        val waterIntake = getTodayWaterIntake()
        if (waterIntake.glassesConsumed < waterIntake.targetGlasses) {
            waterIntake.glassesConsumed++
            waterIntake.timestamps.add(System.currentTimeMillis())
            saveWaterIntake(waterIntake)
        }
        return waterIntake
    }

    fun resetDailyWater(date: String) {
        val waterIntake = WaterIntake(date = date)
        saveWaterIntake(waterIntake)
    }
}
