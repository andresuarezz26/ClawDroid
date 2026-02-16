package com.aiassistant.domain.usecase.recurringtask

import com.aiassistant.domain.model.RecurringTask
import com.aiassistant.domain.repository.recurringtask.RecurringTaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

class ObserveRecurringTasksUseCase @Inject constructor(
    private val repository: RecurringTaskRepository
) {
    operator fun invoke(): Flow<List<RecurringTask>> {
        return repository.observeAll().flowOn(Dispatchers.IO)
    }
}
