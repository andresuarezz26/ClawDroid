package com.aiassistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aiassistant.data.local.entity.TaskLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskLogDao {
    @Insert
    suspend fun insert(entity: TaskLogEntity)

    @Query("SELECT * FROM task_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<TaskLogEntity>>
}