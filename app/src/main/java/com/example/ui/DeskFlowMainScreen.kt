package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ClientConnectScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HostScreen
import com.example.ui.screens.RemoteViewerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DeskCyanPrimary

enum class AppTab(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
) {
    HOST("Share Screen", Icons.Filled.Cast, Icons.Outlined.Cast, "tab_host"),
    CLIENT("Remote", Icons.Filled.Laptop, Icons.Outlined.Laptop, "tab_client"),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History, "tab_history"),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

@Composable
fun DeskFlowMainScreen(
    viewModel: DeskFlowViewModel,
    onRequestScreenCapture: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AppTab.HOST) }
    var isViewingRemoteSession by remember { mutableStateOf(false) }
    val isClientConnected by viewModel.viewerClient.isConnected.collectAsState()

    if (isViewingRemoteSession || isClientConnected) {
        // Full screen interactive remote viewer
        RemoteViewerScreen(
            viewModel = viewModel,
            onCloseViewer = {
                isViewingRemoteSession = false
                viewModel.disconnectRemoteClient()
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    AppTab.values().forEach { tab ->
                        val selected = selectedTab == tab
                        NavigationBarItem(
                            modifier = Modifier.testTag(tab.testTag),
                            selected = selected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    tint = if (selected) DeskCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    color = if (selected) DeskCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = DeskCyanPrimary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_switch"
                ) { target ->
                    when (target) {
                        AppTab.HOST -> HostScreen(
                            viewModel = viewModel,
                            onRequestScreenCapture = onRequestScreenCapture
                        )
                        AppTab.CLIENT -> ClientConnectScreen(
                            viewModel = viewModel,
                            onNavigateToViewer = { isViewingRemoteSession = true }
                        )
                        AppTab.HISTORY -> HistoryScreen(
                            viewModel = viewModel
                        )
                        AppTab.SETTINGS -> SettingsScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
