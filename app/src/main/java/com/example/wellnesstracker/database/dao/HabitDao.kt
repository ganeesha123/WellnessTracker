package com.example.wellnesstracker.database.dao

import androidx.room.*
import com.example.wellnesstracker.model.entities.HabitEntity

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY id ASC")
    suspend fun getAll(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)
}
