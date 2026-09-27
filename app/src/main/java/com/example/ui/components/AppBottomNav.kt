package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag

enum class NavScreen(val title: String, val icon: ImageVector, val tag: String) {
    VOUCHERS("Vouchers", Icons.Default.ConfirmationNumber, "nav_vouchers"),
    GENERATE("Generate", Icons.Default.AddCircle, "nav_generate"),
    PRINTER("Printer", Icons.Default.Print, "nav_printer"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings")
}

@Composable
fun AppBottomNav(
    currentScreen: NavScreen,
    onScreenSelected: (NavScreen) -> Unit
) {
    NavigationBar {
        NavScreen.values().forEach { screen ->
            NavigationBarItem(
                selected = currentScreen == screen,
                onClick = { onScreenSelected(screen) },
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                modifier = Modifier.testTag(screen.tag)
            )
        }
    }
}

@Composable
fun AppBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val screens = NavScreen.values()
    NavigationBar {
        screens.forEachIndexed { index, screen ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                modifier = Modifier.testTag(screen.tag)
            )
        }
    }
}
