package com.aiassistant.presentation.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

  private val _state = MutableStateFlow(HomeState())
  val state: StateFlow<HomeState> = _state.asStateFlow()

  fun processIntent(intent: HomeIntent) {
    when (intent) {
      is HomeIntent.UpdateInput -> {
        _state.update {
          it.copy(inputText = intent.input)
        }
        // Handle load data intent
      }
      HomeIntent.RunCommand -> {

      }
    }
  }
}