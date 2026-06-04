package com.example.wellnesstracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.wellnesstracker.model.entities.*
import com.example.wellnesstracker.database.dao.*

@Database(
    entities = [
        HabitEntity::class,
        MoodEntryEntity::class,
        DailyDataEntity::class,
        WaterIntakeEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun moodDao(): MoodDao
    abstract fun dailyDataDao(): DailyDataDao
    abstract fun waterDao(): WaterDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wellness_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
