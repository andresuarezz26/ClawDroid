package com.aiassistant.agent.agent_strategy

import ai.koog.agents.core.tools.ToolRegistry

/**
 * Create different agents according to the use case. In this app we can have many type of agents
 * using different tools and with different objectives.
 */
interface AgentStrategy {

  fun systemPrompt(): String

  fun toolRegistry(): ToolRegistry
}