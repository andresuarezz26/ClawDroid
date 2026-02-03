package com.aiassistant.domain.usecase

import com.aiassistant.domain.model.UINode
import com.aiassistant.domain.repository.ScreenRepository
import javax.inject.Inject

class CaptureScreenUseCase @Inject constructor(
    private val screenRepository: ScreenRepository
) {
    suspend operator fun invoke(): List<UINode> = screenRepository.captureScreen()
}