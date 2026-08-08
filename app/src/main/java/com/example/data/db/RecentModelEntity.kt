package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_models")
data class RecentModelEntity(
    @PrimaryKey
    val modelId: String,
    val displayName: String,
    val provider: String,
    val description: String,
    val lastUsedTimestamp: Long = System.currentTimeMillis()
)
