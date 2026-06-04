package com.example.wellnesstracker.model.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_intake")
data class WaterIntakeEntity(
    @PrimaryKey val date: String,
    val glassesConsumed: Int = 0,
    val targetGlasses: Int = 8,
    val timestampsJson: String = "[]"
)
