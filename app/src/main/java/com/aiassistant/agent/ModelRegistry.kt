package com.aiassistant.agent

data class ModelInfo(val id: String, val displayName: String, val provider: LLMProvider)

object ModelRegistry {
    val models = listOf(
        ModelInfo("gpt-5.2", "GPT-5.2", LLMProvider.OPENAI),
        ModelInfo("gpt-5", "GPT-5", LLMProvider.OPENAI),
        ModelInfo("gpt-5-mini", "GPT-5 Mini", LLMProvider.OPENAI),
        ModelInfo("gpt-4o", "GPT-4o", LLMProvider.OPENAI),
        ModelInfo("gpt-4o-mini", "GPT-4o Mini", LLMProvider.OPENAI),
        ModelInfo("gpt-4-turbo", "GPT-4 Turbo", LLMProvider.OPENAI),
        ModelInfo("claude-3-5-sonnet", "Claude 3.5 Sonnet", LLMProvider.ANTHROPIC),
        ModelInfo("claude-3-opus", "Claude 3 Opus", LLMProvider.ANTHROPIC),
        ModelInfo("claude-3-haiku", "Claude 3 Haiku", LLMProvider.ANTHROPIC),
        ModelInfo("sonnet-4-5", "Claude 4.5 Sonnet", LLMProvider.ANTHROPIC),
        ModelInfo("gemini-2.5-pro", "Gemini 2.5 Pro", LLMProvider.GOOGLE),
        ModelInfo("gemini-2.0-flash", "Gemini 2.0 Flash", LLMProvider.GOOGLE),
        ModelInfo("gemini-3-pro", "Gemini 3.0 Pro", LLMProvider.GOOGLE),
    )

    val defaultModel = models.first()

    fun modelsByProvider(): Map<LLMProvider, List<ModelInfo>> = models.groupBy { it.provider }

    fun findModel(id: String): ModelInfo? = models.find { it.id == id }
}
