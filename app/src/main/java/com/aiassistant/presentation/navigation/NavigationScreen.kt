package com.aiassistant.presentation.navigation

import kotlinx.serialization.Serializable

sealed class NavigationScreen {

  @Serializable
  object Main: NavigationScreen()

  @Serializable
  object TelegramSettings: NavigationScreen()

  @Serializable
  object RecurringTaskList: NavigationScreen()

  @Serializable
  data class RecurringTaskDetail(val taskId: Long): NavigationScreen()
}