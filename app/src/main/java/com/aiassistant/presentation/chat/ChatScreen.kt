package com.aiassistant.presentation.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {

  val state = viewModel.state.collectAsStateWithLifecycle().value

  ChatScreenContent(
    inputText = state.inputText,
    isExecuting = state.isExecuting,
    onInputTextChange = { newText -> viewModel.processIntent(ChatIntent.UpdateInput(newText)) },
    onRunCommand = { viewModel.processIntent(ChatIntent.RunCommand) })
}

@Composable
private fun ChatScreenContent(
  modifier: Modifier = Modifier,
  inputText: String = "",
  isExecuting: Boolean = false,
  onInputTextChange: (String) -> Unit = {},
  onRunCommand: () -> Unit = {}
) {

  Column {
    Text("Home Screen")

    Row(modifier.padding(16.dp)) {
      CommandInput(modifier = modifier, inputText, onInputTextChange, onRunCommand, !isExecuting)
    }

  }
}

@Composable
fun CommandInput(
  modifier: Modifier = Modifier,
  value: String,
  onValueChange: (String) -> Unit,
  onSend: () -> Unit,
  enabled: Boolean
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    TextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier.weight(1f),
      placeholder = { Text("Type a command...") },
      enabled = enabled,
      singleLine = true,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
      keyboardActions = KeyboardActions(onSend = { onSend() })
    )
    Spacer(Modifier.width(8.dp))
    IconButton(onClick = onSend, enabled = enabled && value.isNotBlank()) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.Send,
        contentDescription = "Execute"
      )
    }
  }
}


@Preview
@Composable
fun HomeScreenPreview() {
  ChatScreenContent()
}