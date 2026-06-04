package com.example.wellnesstracker.database.dao

import androidx.room.*
import com.example.wellnesstracker.model.entities.WaterIntakeEntity

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_intake WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): WaterIntakeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(water: WaterIntakeEntity)
}
