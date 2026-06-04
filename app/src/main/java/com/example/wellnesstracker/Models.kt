package com.example.wellnesstracker

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.example.wellnesstracker.repository.MigrationUtil
import com.example.wellnesstracker.repository.WellnessRepository
import com.example.wellnesstracker.model.JsonUtils
import com.example.wellnesstracker.model.entities.*

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
    private val repo = WellnessRepository(ctx)

    init {
        // Kick off migration in background to avoid blocking UI
        CoroutineScope(Dispatchers.IO).launch {
            MigrationUtil.ensureMigrated(ctx)
        }
    }

    private fun prewarmHabitsFromDb() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entities = repo.getHabits()
                val list = entities.map { e ->
                    val completed = try { JsonUtils.fromJson<MutableList<String>>(e.completedDatesJson) } catch (_: Exception) { mutableListOf<String>() }
                    Habit(e.name, e.description, completed)
                }
                sp.edit().putString("habits", gson.toJson(list)).apply()
            } catch (_: Exception) { }
        }
    }

    private fun prewarmMoodsFromDb() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entities = repo.getMoodEntries()
                val list = entities.map { e -> MoodEntry(e.emoji, e.date, e.time, e.note) }
                sp.edit().putString("mood_entries", gson.toJson(list)).apply()
            } catch (_: Exception) { }
        }
    }

    private fun prewarmDailyFromDb(date: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entity = repo.getDailyData(date)
                entity?.let { e ->
                    val habits = try { JsonUtils.fromJson<MutableList<String>>(e.habitsCompletedJson) } catch (_: Exception) { mutableListOf() }
                    val moods = try { JsonUtils.fromJson<MutableList<MoodEntry>>(e.moodEntriesJson) } catch (_: Exception) { mutableListOf() }
                    val dd = DailyData(date = e.date, habitsCompleted = habits, moodEntries = moods, notes = e.notes, timestamp = e.timestamp)
                    sp.edit().putString("daily_data_${'$'}date", gson.toJson(dd)).apply()
                }
            } catch (_: Exception) { }
        }
    }

    private fun prewarmWaterFromDb(date: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val e = repo.getWaterByDate(date)
                if (e != null) {
                    val wi = WaterIntake(date = e.date, glassesConsumed = e.glassesConsumed, targetGlasses = e.targetGlasses, timestamps = try { JsonUtils.fromJson<MutableList<Long>>(e.timestampsJson) } catch (_: Exception) { mutableListOf() })
                    sp.edit().putString("water_${'$'}date", gson.toJson(wi)).apply()
                }
            } catch (_: Exception) { }
        }
    }

    fun isOnboarded(): Boolean = sp.getBoolean("onboarded", false)
    fun setOnboarded(v: Boolean) = sp.edit().putBoolean("onboarded", v).apply()

    // Habits: keep compatibility with existing API but read/write to Room asynchronously
    fun getHabits(): MutableList<Habit> {
        val json = sp.getString("habits", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Habit>>() {}.type
            return gson.fromJson(json, type)
        }

        // If not present in prefs (post-migration), return empty and prewarm from DB in background
        prewarmHabitsFromDb()
        return mutableListOf()
    }

    fun saveHabits(h: MutableList<Habit>) {
        // save to prefs for backwards compatibility and also to DB
        sp.edit().putString("habits", gson.toJson(h)).apply()
        CoroutineScope(Dispatchers.IO).launch {
            for (habit in h) {
                val entity = HabitEntity(name = habit.name, description = habit.description, completedDatesJson = JsonUtils.toJson(habit.completedDates))
                repo.insertHabit(entity)
            }
        }
    }

    // Legacy moods
    fun getMoods(): MutableList<MoodEntry> = getMoodEntries()
    fun saveMoods(list: MutableList<MoodEntry>) = saveMoodEntries(list)

    fun getMoodEntries(): MutableList<MoodEntry> {
        val json = sp.getString("mood_entries", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<MoodEntry>>() {}.type
            return gson.fromJson(json, type)
        }

        // Not in prefs: return empty and prewarm from DB
        prewarmMoodsFromDb()
        return mutableListOf()
    }

    fun saveMoodEntries(list: MutableList<MoodEntry>) {
        sp.edit().putString("mood_entries", gson.toJson(list)).apply()
        CoroutineScope(Dispatchers.IO).launch {
            for (m in list.reversed()) { // maintain original order
                repo.insertMood(MoodEntryEntity(emoji = m.emoji, date = m.date, time = m.time, note = m.note))
            }
        }
    }

    fun setHydrationIntervalMinutes(m: Int) = sp.edit().putInt("hydration_min", m).apply()
    fun getHydrationIntervalMinutes(): Int = sp.getInt("hydration_min", 60)

    // User credentials and profile - prefer Room but keep prefs for compatibility
    fun saveUserCredentials(email: String, password: String) {
        sp.edit()
            .putString("user_email", email)
            .putString("user_password", password)
            .apply()
        CoroutineScope(Dispatchers.IO).launch {
            repo.insertUser(UserEntity(email = email, password = password))
        }
    }

    fun validateCredentials(email: String, password: String): Boolean {
        // First check prefs for quick validation
        val savedEmail = sp.getString("user_email", null)
        val savedPassword = sp.getString("user_password", null)
        if (savedEmail != null && savedPassword != null) return email == savedEmail && password == savedPassword

        // Fall back to DB
        // Avoid blocking UI; assume not registered if not in prefs
        CoroutineScope(Dispatchers.IO).launch {
            try { repo.getUserByEmail(email) } catch (_: Exception) {}
        }
        return false
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
        // Clear prefs and also clear DB tables by marking migration flag false (full wipe requires DB operations)
        sp.edit()
            .remove("habits")
            .remove("mood_entries")
            .remove("moods")
            .apply()
        // NOTE: Clearing DB tables would require DAO access; keep as-is to avoid accidental data loss.
    }

    // Daily data
    fun getDailyData(date: String): DailyData? {
        val json = sp.getString("daily_data_$date", null)
        if (json != null) return gson.fromJson(json, DailyData::class.java)

        // Not in prefs: prewarm in background and return null
        prewarmDailyFromDb(date)
        return null
    }

    fun saveDailyData(dailyData: DailyData) {
        sp.edit().putString("daily_data_${dailyData.date}", gson.toJson(dailyData)).apply()
        CoroutineScope(Dispatchers.IO).launch {
            val entity = DailyDataEntity(date = dailyData.date, habitsCompletedJson = JsonUtils.toJson(dailyData.habitsCompleted), moodEntriesJson = JsonUtils.toJson(dailyData.moodEntries), notes = dailyData.notes, timestamp = dailyData.timestamp)
            repo.insertDailyData(entity)
        }
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

        if (allData.isNotEmpty()) return allData.sortedByDescending { it.date }.toMutableList()

        // Fall back: return whatever we have (likely empty) without blocking
        return allData.sortedByDescending { it.date }.toMutableList()
    }

    fun getWeeklyMoodData(weekStartDate: String): WeeklyMoodSummary {
        val weeklyData = WeeklyMoodSummary(weekStartDate)

        // Behavior preserved from previous implementation but using getDailyData/getMoodEntries
        val calendar = java.util.Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        calendar.time = dateFormat.parse(weekStartDate) ?: java.util.Date()

        val moodCounts = mutableMapOf<String, Int>()
        val todayDate = dateFormat.format(java.util.Date())

        for (i in 0..6) {
            val currentDate = dateFormat.format(calendar.time)
            val moodEntry = if (currentDate == todayDate) {
                val allMoods = getMoodEntries()
                val todayMood = allMoods.firstOrNull { it.date == currentDate }
                todayMood
            } else {
                val dailyData = getDailyData(currentDate)
                val savedMood = dailyData?.moodEntries?.lastOrNull()
                savedMood
            }

            moodEntry?.let { mood ->
                weeklyData.dailyMoods[currentDate] = mood
                weeklyData.totalEntries++
                moodCounts[mood.emoji] = moodCounts.getOrDefault(mood.emoji, 0) + 1
            }

            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        weeklyData.averageMood = moodCounts.maxByOrNull { it.value }?.key ?: ""
        return weeklyData
    }

    // Water Intake Methods
    fun getWaterIntake(date: String): WaterIntake? {
        val json = sp.getString("water_$date", null)
        if (json != null) return gson.fromJson(json, WaterIntake::class.java)

        // Not in prefs: prewarm in background and return null
        prewarmWaterFromDb(date)
        return null
    }

    fun saveWaterIntake(waterIntake: WaterIntake) {
        val json = gson.toJson(waterIntake)
        sp.edit().putString("water_${waterIntake.date}", json).apply()
        CoroutineScope(Dispatchers.IO).launch {
            val entity = WaterIntakeEntity(date = waterIntake.date, glassesConsumed = waterIntake.glassesConsumed, targetGlasses = waterIntake.targetGlasses, timestampsJson = JsonUtils.toJson(waterIntake.timestamps))
            repo.insertWater(entity)
        }
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
