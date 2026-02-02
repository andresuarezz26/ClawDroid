package com.aiassistant.presentation.home

sealed interface HomeIntent {

  data class UpdateInput(val input: String) : HomeIntent
  object RunCommand : HomeIntent
}