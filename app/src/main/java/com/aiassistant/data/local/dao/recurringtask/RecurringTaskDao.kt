package com.aiassistant.data.local.dao.recurringtask

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import com.aiassistant.data.local.entity.recurringtask.RecurringTaskEntity
import com.aiassistant.data.local.entity.recurringtask.TaskExecutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTaskDao {

    @Insert
    suspend fun insert(task: RecurringTaskEntity): Long

    @Update
    suspend fun update(task: RecurringTaskEntity)

    @Query("SELECT * FROM recurring_tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RecurringTaskEntity>>

    @Query("SELECT * FROM recurring_tasks ORDER BY createdAt DESC")
    suspend fun getAll(): List<RecurringTaskEntity>

    @Query("SELECT * FROM recurring_tasks WHERE id = :id")
    suspend fun getById(id: Long): RecurringTaskEntity?

    @Query("SELECT * FROM recurring_tasks WHERE enabled = 1")
    suspend fun getEnabled(): List<RecurringTaskEntity>

    @Query("UPDATE recurring_tasks SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE recurring_tasks SET lastRunAt = :lastRunAt, lastRunStatus = :status, lastRunSummary = :summary WHERE id = :id")
    suspend fun updateLastRun(id: Long, lastRunAt: Long, status: String, summary: String?)

    @Query("DELETE FROM recurring_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun insertExecution(execution: TaskExecutionEntity)

    @Query("SELECT * FROM task_executions WHERE taskId = :taskId ORDER BY executedAt DESC LIMIT :limit")
    suspend fun getRecentExecutions(taskId: Long, limit: Int): List<TaskExecutionEntity>
}
