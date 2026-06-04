package com.example.wellnesstracker.repository

import android.content.Context
import com.example.wellnesstracker.database.AppDatabase
import com.example.wellnesstracker.model.JsonUtils
import com.example.wellnesstracker.model.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WellnessRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)

    // Habits
    suspend fun getHabits(): List<HabitEntity> = withContext(Dispatchers.IO) { db.habitDao().getAll() }
    suspend fun insertHabit(h: HabitEntity) = withContext(Dispatchers.IO) { db.habitDao().insert(h) }
    suspend fun updateHabit(h: HabitEntity) = withContext(Dispatchers.IO) { db.habitDao().update(h) }
    suspend fun deleteHabit(h: HabitEntity) = withContext(Dispatchers.IO) { db.habitDao().delete(h) }

    // Moods
    suspend fun getMoodEntries(): List<MoodEntryEntity> = withContext(Dispatchers.IO) { db.moodDao().getAll() }
    suspend fun insertMood(m: MoodEntryEntity) = withContext(Dispatchers.IO) { db.moodDao().insert(m) }

    // Daily data
    suspend fun getDailyData(date: String): DailyDataEntity? = withContext(Dispatchers.IO) { db.dailyDataDao().getByDate(date) }
    suspend fun insertDailyData(d: DailyDataEntity) = withContext(Dispatchers.IO) { db.dailyDataDao().insert(d) }
    suspend fun getAllDailyData(): List<DailyDataEntity> = withContext(Dispatchers.IO) { db.dailyDataDao().getAll() }

    // Water
    suspend fun getWaterByDate(date: String): WaterIntakeEntity? = withContext(Dispatchers.IO) { db.waterDao().getByDate(date) }
    suspend fun insertWater(w: WaterIntakeEntity) = withContext(Dispatchers.IO) { db.waterDao().insert(w) }

    // Users
    suspend fun getUserByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) { db.userDao().getByEmail(email) }
    suspend fun insertUser(u: UserEntity) = withContext(Dispatchers.IO) { db.userDao().insert(u) }

    // Migration helper: migrate SharedPreferences data JSON into Room entities
    suspend fun migrateFromPrefs(prefObj: Any) {
        // Implementation should be handled by MigrationUtil which has access to Prefs class
    }
}
