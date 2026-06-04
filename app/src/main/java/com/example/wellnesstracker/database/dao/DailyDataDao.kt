package com.example.wellnesstracker.database.dao

import androidx.room.*
import com.example.wellnesstracker.model.entities.DailyDataEntity

@Dao
interface DailyDataDao {
    @Query("SELECT * FROM daily_data ORDER BY date DESC")
    suspend fun getAll(): List<DailyDataEntity>

    @Query("SELECT * FROM daily_data WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyDataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: DailyDataEntity)

    @Update
    suspend fun update(data: DailyDataEntity)

    @Delete
    suspend fun delete(data: DailyDataEntity)
}
