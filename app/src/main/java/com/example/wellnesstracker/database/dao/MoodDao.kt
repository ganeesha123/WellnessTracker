package com.example.wellnesstracker.database.dao

import androidx.room.*
import com.example.wellnesstracker.model.entities.MoodEntryEntity

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries ORDER BY id DESC")
    suspend fun getAll(): List<MoodEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mood: MoodEntryEntity): Long

    @Delete
    suspend fun delete(mood: MoodEntryEntity)
}
