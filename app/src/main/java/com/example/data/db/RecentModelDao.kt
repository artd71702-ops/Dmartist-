package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentModelDao {
    @Query("SELECT * FROM recent_models ORDER BY lastUsedTimestamp DESC")
    fun getRecentModels(): Flow<List<RecentModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateModel(model: RecentModelEntity)

    @Query("DELETE FROM recent_models")
    suspend fun clearModels()
}
