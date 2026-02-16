package com.aiassistant.domain.usecase.recurringtask

import com.aiassistant.domain.model.RecurringTask
import com.aiassistant.domain.repository.recurringtask.RecurringTaskRepository
import javax.inject.Inject

class GetRecurringTaskByIdUseCase @Inject constructor(
    private val repository: RecurringTaskRepository
) {
    suspend operator fun invoke(id: Long): RecurringTask? {
        return repository.getById(id)
    }
}
