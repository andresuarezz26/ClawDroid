package com.aiassistant.agent

data class AgentConfig(
    val provider: LLMProvider = LLMProvider.OPENAI,
    val model: String = "gpt-5-mini",
    val apiKey: String,
    val temperature: Double = 1.0,
    val maxIterations: Int = 50
)

enum class LLMProvider { OPENAI, ANTHROPIC, GOOGLE }
