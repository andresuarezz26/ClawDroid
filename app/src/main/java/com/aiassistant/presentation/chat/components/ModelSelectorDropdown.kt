package com.aiassistant.presentation.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiassistant.agent.LLMProvider
import com.aiassistant.agent.ModelInfo

@Composable
fun ModelSelectorDropdown(
    selectedModelDisplayName: String,
    isExpanded: Boolean,
    enabled: Boolean,
    hasAnyApiKey: Boolean,
    availableModels: List<ModelInfo>,
    onToggle: () -> Unit,
    onSelectModel: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(enabled = enabled && hasAnyApiKey) { onToggle() }
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (hasAnyApiKey) selectedModelDisplayName else "No API key detected",
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    !hasAnyApiKey -> MaterialTheme.colorScheme.error
                    enabled -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            if (hasAnyApiKey) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select model",
                    tint = if (enabled) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.widthIn(min = 200.dp)
        ) {
            val grouped = availableModels.groupBy { it.provider }
            val providers = grouped.keys.toList()
            providers.forEachIndexed { index, provider ->
                // Provider header
                DropdownMenuItem(
                    text = {
                        Text(
                            text = providerLabel(provider),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {},
                    enabled = false
                )
                // Models for this provider
                grouped[provider]?.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = model.displayName,
                                style = if (model.displayName == selectedModelDisplayName)
                                    MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                else MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = { onSelectModel(model.id) }
                    )
                }
                // Divider between groups (not after the last)
                if (index < providers.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun providerLabel(provider: LLMProvider): String = when (provider) {
    LLMProvider.OPENAI -> "OpenAI"
    LLMProvider.ANTHROPIC -> "Anthropic"
    LLMProvider.GOOGLE -> "Google"
}
