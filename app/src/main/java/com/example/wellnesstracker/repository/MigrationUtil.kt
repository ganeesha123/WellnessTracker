package com.example.wellnesstracker.repository

import android.content.Context
import com.example.wellnesstracker.model.JsonUtils
import com.example.wellnesstracker.model.entities.*
import com.example.wellnesstracker.Habit
import com.example.wellnesstracker.MoodEntry
import com.example.wellnesstracker.DailyData
import com.example.wellnesstracker.WaterIntake
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MigrationUtil {
    private const val MIGRATED_KEY = "prefs_migrated_to_room"

    suspend fun ensureMigrated(context: Context) {
        withContext(Dispatchers.IO) {
            val sp = context.getSharedPreferences("wt_prefs", Context.MODE_PRIVATE)
            val already = sp.getBoolean(MIGRATED_KEY, false)
            if (already) return@withContext

            try {
                val repo = WellnessRepository(context)
                val gson = Gson()

                // Habits
                sp.getString("habits", null)?.let { json ->
                    val type = object : TypeToken<MutableList<Habit>>() {}.type
                    val habits: MutableList<Habit> = gson.fromJson(json, type)
                    for (h in habits) {
                        val entity = HabitEntity(name = h.name, description = h.description, completedDatesJson = JsonUtils.toJson(h.completedDates))
                        repo.insertHabit(entity)
                    }
                }

                // Mood entries (new key first, then legacy fallback)
                val moodsJson = sp.getString("mood_entries", null) ?: sp.getString("moods", null)
                moodsJson?.let { json ->
                    val type = object : TypeToken<MutableList<MoodEntry>>() {}.type
                    val moods: MutableList<MoodEntry> = gson.fromJson(json, type)
                    for (m in moods.reversed()) {
                        repo.insertMood(MoodEntryEntity(emoji = m.emoji, date = m.date, time = m.time, note = m.note))
                    }
                }

                // Daily data: iterate keys
                val keys = sp.all.keys.filter { it.startsWith("daily_data_") }
                for (key in keys) {
                    sp.getString(key, null)?.let { json ->
                        try {
                            val d = gson.fromJson(json, DailyData::class.java)
                            val entity = DailyDataEntity(
                                date = d.date,
                                habitsCompletedJson = JsonUtils.toJson(d.habitsCompleted),
                                moodEntriesJson = JsonUtils.toJson(d.moodEntries),
                                notes = d.notes,
                                timestamp = d.timestamp
                            )
                            repo.insertDailyData(entity)
                        } catch (_: Exception) { }
                    }
                }

                // Water: migrate today's value if present
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                sp.getString("water_$today", null)?.let { json ->
                    try {
                        val w = gson.fromJson(json, WaterIntake::class.java)
                        repo.insertWater(
                            WaterIntakeEntity(
                                date = w.date,
                                glassesConsumed = w.glassesConsumed,
                                targetGlasses = w.targetGlasses,
                                timestampsJson = JsonUtils.toJson(w.timestamps)
                            )
                        )
                    } catch (_: Exception) { }
                }

                // Users
                val email = sp.getString("user_email", null)
                val password = sp.getString("user_password", null)
                if (!email.isNullOrEmpty()) {
                    repo.insertUser(UserEntity(email = email, password = password ?: ""))
                }

                sp.edit().putBoolean(MIGRATED_KEY, true).apply()
            } catch (e: Exception) {
                android.util.Log.e("MigrationUtil", "Migration failed: ${e.message}")
            }
        }
    }
}
