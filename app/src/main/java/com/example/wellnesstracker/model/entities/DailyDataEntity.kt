package com.example.wellnesstracker.model.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_data")
data class DailyDataEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val habitsCompletedJson: String = "[]",
    val moodEntriesJson: String = "[]",
    val notes: String = "",
    val timestamp: Long = 0L
)
