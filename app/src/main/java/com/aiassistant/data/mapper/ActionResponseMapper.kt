package com.aiassistant.data.mapper

import com.aiassistant.domain.model.Action
import com.aiassistant.domain.model.ActionResponse
import com.aiassistant.domain.model.ActionType
import com.aiassistant.domain.model.ScrollDirection
import com.aiassistant.domain.model.TaskStatus
import com.aiassistant.data.remote.dto.ChatCompletionResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class RawActionResponse(
    val thought: String,
    val status: String,
    val actions: List<RawAction>
)

@Serializable
private data class RawAction(
    val type: String,
    val index: Int? = null,
    @SerialName("package") val packageName: String? = null,
    val text: String? = null,
    val direction: String? = null
)

@Singleton
class ActionResponseMapper @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun toDomain(response: ChatCompletionResponse): ActionResponse {
        val content = response.choices.first().message.content
        val raw = json.decodeFromString<RawActionResponse>(content)

        return ActionResponse(
            thought = raw.thought,
            status = when (raw.status) {
                "done" -> TaskStatus.DONE
                "failed" -> TaskStatus.FAILED
                else -> TaskStatus.ACTING
            },
            actions = raw.actions.map { mapAction(it) }
        )
    }

    private fun mapAction(raw: RawAction): Action {
        return Action(
            type = when (raw.type) {
                "launch" -> ActionType.LAUNCH
                "click" -> ActionType.CLICK
                "setText" -> ActionType.SET_TEXT
                "scroll" -> ActionType.SCROLL
                "back" -> ActionType.BACK
                "home" -> ActionType.HOME
                else -> throw IllegalArgumentException("Unknown action: ${raw.type}")
            },
            index = raw.index,
            packageName = raw.packageName,
            text = raw.text,
            direction = raw.direction?.let { ScrollDirection.valueOf(it.uppercase()) }
        )
    }
}