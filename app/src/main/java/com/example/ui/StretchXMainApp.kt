package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SafetyCountdownBanner
import com.example.ui.screens.GameLibraryScreen
import com.example.ui.screens.ProfileEditDialog
import com.example.ui.screens.ResolutionTunerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShizukuDiagnosticsScreen
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.HyperCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.StretchXViewModel

enum class NavigationTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    GAMES("Games", Icons.Default.SportsEsports),
    TUNER("Tuner", Icons.Default.Tune),
    SHIZUKU("Shizuku", Icons.Default.Security),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StretchXMainApp(
    viewModel: StretchXViewModel
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavigationTab.GAMES) }

    val filteredGames by viewModel.filteredGames.collectAsState()
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val onlyConfigured by viewModel.onlyConfiguredFilter.collectAsState()
    val editingGame by viewModel.editingGame.collectAsState()
    val nativeConfig by viewModel.nativeConfig.collectAsState()
    val currentConfig by viewModel.currentConfig.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "StretchX",
                        color = HyperCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp
                    )
                },
                actions = {
                    if (sessionState.isAltered) {
                        IconButton(
                            onClick = { viewModel.revertResolutionNow() },
                            modifier = Modifier.testTag("appbar_emergency_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Emergency Reset Display",
                                tint = AlertCoral
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(imageVector = tab.icon, contentDescription = tab.title)
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberBackground,
                            selectedTextColor = HyperCyan,
                            indicatorColor = HyperCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CyberBackground)
        ) {
            when (currentTab) {
                NavigationTab.GAMES -> {
                    GameLibraryScreen(
                        games = filteredGames,
                        shizukuStatus = shizukuStatus,
                        sessionState = sessionState,
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.searchQuery.value = it },
                        onlyConfigured = onlyConfigured,
                        onOnlyConfiguredChange = { viewModel.onlyConfiguredFilter.value = it },
                        onRequestShizuku = { viewModel.requestShizukuAuth() },
                        onOpenShizukuApp = { viewModel.openShizukuApp() },
                        onRefresh = { viewModel.refreshAll() },
                        onEditProfile = { viewModel.openProfileEditor(it) },
                        onLaunchGame = { viewModel.launchGameWithProfile(it) },
                        onEmergencyReset = { viewModel.revertResolutionNow() }
                    )
                }
                NavigationTab.TUNER -> {
                    ResolutionTunerScreen(
                        currentConfig = currentConfig,
                        nativeConfig = nativeConfig,
                        shizukuStatus = shizukuStatus,
                        sessionState = sessionState,
                        installedGames = filteredGames,
                        onActivateProjection = { config, multiplier, mode, pkg ->
                            viewModel.activateProjection(config, multiplier, mode, pkg)
                        },
                        onResetToNative = { viewModel.revertResolutionNow() }
                    )
                }
                NavigationTab.SHIZUKU -> {
                    ShizukuDiagnosticsScreen(
                        status = shizukuStatus,
                        nativeConfig = nativeConfig,
                        currentConfig = currentConfig,
                        onRequestAuth = { viewModel.requestShizukuAuth() },
                        onOpenShizukuApp = { viewModel.openShizukuApp() },
                        onRefresh = { viewModel.refreshAll() }
                    )
                }
                NavigationTab.SETTINGS -> {
                    SettingsScreen(
                        onRequestOverlayPermission = { viewModel.requestOverlayPermission(it) },
                        onEmergencyReset = { viewModel.revertResolutionNow() }
                    )
                }
            }

            // Safety Countdown Banner overlay
            SafetyCountdownBanner(
                sessionState = sessionState,
                onKeep = { viewModel.confirmAndKeepResolution() },
                onRevert = { viewModel.revertResolutionNow() }
            )

            // Game Profile Editor Dialog
            editingGame?.let { game ->
                ProfileEditDialog(
                    game = game,
                    nativeConfig = nativeConfig,
                    onSave = { profile -> viewModel.saveProfile(profile) },
                    onDelete = { pkg -> viewModel.deleteProfile(pkg) },
                    onDismiss = { viewModel.closeProfileEditor() }
                )
            }
        }
    }
}
