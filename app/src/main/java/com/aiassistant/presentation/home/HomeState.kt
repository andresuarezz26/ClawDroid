package com.aiassistant.presentation.home

data class HomeState(
  val isLoading: Boolean = false,
  val messages: List<String> = listOf(),
  val inputText: String = "",
  val isExecuting: Boolean = false,)
