package com.aiassistant.presentation.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aiassistant.presentation.chat.ChatScreen
import com.aiassistant.presentation.recurring_task_detail.RecurringTaskDetailScreen
import com.aiassistant.presentation.recurring_task_list.RecurringTaskListScreen
import com.aiassistant.presentation.settings.TelegramSettingsScreen
import kotlinx.coroutines.launch

@Composable
fun NavigationStack() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedDrawerItem by rememberSaveable { mutableStateOf(DrawerItem.Chat) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                selectedItem = selectedDrawerItem,
                onItemSelected = { item ->
                    scope.launch { drawerState.close() }
                    if (item == selectedDrawerItem) return@AppDrawerContent
                    selectedDrawerItem = item
                    when (item) {
                        DrawerItem.Chat -> navController.navigate(NavigationScreen.Main) {
                            popUpTo(NavigationScreen.Main) { inclusive = true }
                        }
                        DrawerItem.RecurringTasks -> navController.navigate(NavigationScreen.RecurringTaskList) {
                            popUpTo(NavigationScreen.Main)
                        }
                        DrawerItem.Settings -> navController.navigate(NavigationScreen.TelegramSettings) {
                            popUpTo(NavigationScreen.Main)
                        }
                    }
                }
            )
        }
    ) {
        NavHost(navController = navController, startDestination = NavigationScreen.Main) {
            composable<NavigationScreen.Main> {
                selectedDrawerItem = DrawerItem.Chat
                ChatScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable<NavigationScreen.TelegramSettings> {
                selectedDrawerItem = DrawerItem.Settings
                TelegramSettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<NavigationScreen.RecurringTaskList> {
                selectedDrawerItem = DrawerItem.RecurringTasks
                RecurringTaskListScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateToDetail = { taskId ->
                        navController.navigate(NavigationScreen.RecurringTaskDetail(taskId))
                    }
                )
            }
            composable<NavigationScreen.RecurringTaskDetail> {
                RecurringTaskDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
