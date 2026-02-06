package com.anandsundaram.myforest.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions ORDER BY startTimestamp DESC")
    fun observeSessions(): Flow<List<FocusSessionEntity>>
}
