package com.aiassistant.presentation.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class DrawerItem {
    Chat, RecurringTasks, Settings
}

@Composable
fun AppDrawerContent(
    selectedItem: DrawerItem,
    onItemSelected: (DrawerItem) -> Unit
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "ClawDroid",
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Chat") },
            label = { Text("Chat") },
            selected = selectedItem == DrawerItem.Chat,
            onClick = { onItemSelected(DrawerItem.Chat) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Refresh, contentDescription = "Recurring Tasks") },
            label = { Text("Recurring Tasks") },
            selected = selectedItem == DrawerItem.RecurringTasks,
            onClick = { onItemSelected(DrawerItem.RecurringTasks) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = selectedItem == DrawerItem.Settings,
            onClick = { onItemSelected(DrawerItem.Settings) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}
