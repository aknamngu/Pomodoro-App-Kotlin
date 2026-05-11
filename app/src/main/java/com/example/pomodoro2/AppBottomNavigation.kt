package com.example.pomodoro2

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pomodoro2.ui.theme.MauNhanTym

@Composable
fun AppBottomNavigation(
    currentRoute: String,
    onNavigateToTasks: () -> Unit,
    onNavigateToMotivation: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    language: String
) {
    val items = listOf(
        NavigationItem(
            route = "tasks",
            icon = Icons.Default.List,
            label = if (language == "vi") "Nhiệm vụ" else "Tasks",
            onClick = onNavigateToTasks
        ),
        NavigationItem(
            route = "motivation",
            icon = Icons.Default.Park,
            label = if (language == "vi") "Vườn" else "Garden",
            onClick = onNavigateToMotivation
        ),
        NavigationItem(
            route = "focus",
            icon = Icons.Default.Timer,
            label = if (language == "vi") "Tập trung" else "Focus",
            onClick = onNavigateToFocus
        ),
        NavigationItem(
            route = "history",
            icon = Icons.Default.History,
            label = if (language == "vi") "Lịch sử" else "History",
            onClick = onNavigateToHistory
        ),
        NavigationItem(
            route = "settings",
            icon = Icons.Default.Settings,
            label = if (language == "vi") "Cài đặt" else "Settings",
            onClick = onNavigateToSettings
        )
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = item.onClick,
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(text = item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MauNhanTym,
                    selectedTextColor = MauNhanTym,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    indicatorColor = MauNhanTym.copy(alpha = 0.1f)
                )
            )
        }
    }
}

private data class NavigationItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val onClick: () -> Unit
)
