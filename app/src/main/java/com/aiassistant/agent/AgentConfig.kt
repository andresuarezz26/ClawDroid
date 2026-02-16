package com.aiassistant.agent

data class AgentConfig(
    val provider: LLMProvider = LLMProvider.OPENAI,
    val model: String = "gpt-5.2",
    val apiKey: String,
    val temperature: Double = 1.0,
    val maxIterations: Int = 70,
    val agentType: AgentType
)

enum class LLMProvider { OPENAI, ANTHROPIC, GOOGLE }
enum class AgentType { GENERAL, TASK_EXECUTOR, NOTIFICATION_REACTOR}
