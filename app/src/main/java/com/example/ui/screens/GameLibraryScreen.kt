package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.model.DisplayConfig
import com.example.model.InstalledGame
import com.example.model.SessionState
import com.example.model.ShizukuState
import com.example.model.ShizukuStatusInfo
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.HyperCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber

@Composable
fun GameLibraryScreen(
    games: List<InstalledGame>,
    shizukuStatus: ShizukuStatusInfo,
    sessionState: SessionState,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onlyConfigured: Boolean,
    onOnlyConfiguredChange: (Boolean) -> Unit,
    onRequestShizuku: () -> Unit,
    onOpenShizukuApp: () -> Unit,
    onRefresh: () -> Unit,
    onEditProfile: (InstalledGame) -> Unit,
    onLaunchGame: (InstalledGame) -> Unit,
    onEmergencyReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Shizuku Connection Status Banner with Reactive States
        item {
            ShizukuBanner(
                status = shizukuStatus,
                onRequestShizuku = onRequestShizuku,
                onOpenShizukuApp = onOpenShizukuApp,
                onRefresh = onRefresh
            )
        }

        // Active Status & Reset Bar (if altered)
        if (sessionState.isAltered) {
            item {
                ActiveDisplayAlertBar(
                    currentConfig = sessionState.currentConfig,
                    nativeConfig = sessionState.nativeConfig,
                    onReset = onEmergencyReset
                )
            }
        }

        // Search & Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search installed games...", color = TextTertiary) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = HyperCyan)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("game_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HyperCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = CyberSurface,
                        unfocusedContainerColor = CyberSurface
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !onlyConfigured,
                            onClick = { onOnlyConfiguredChange(false) },
                            label = { Text("All (${games.size})", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HyperCyan.copy(alpha = 0.2f),
                                selectedLabelColor = HyperCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = !onlyConfigured,
                                borderColor = if (!onlyConfigured) HyperCyan else CyberCardBorder
                            )
                        )

                        FilterChip(
                            selected = onlyConfigured,
                            onClick = { onOnlyConfiguredChange(true) },
                            label = { Text("Configured", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HyperCyan.copy(alpha = 0.2f),
                                selectedLabelColor = HyperCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = onlyConfigured,
                                borderColor = if (onlyConfigured) HyperCyan else CyberCardBorder
                            )
                        )
                    }

                    IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh games", tint = HyperCyan)
                    }
                }
            }
        }

        // Empty state
        if (games.isEmpty()) {
            item {
                EmptyGamesCard(isFiltered = searchQuery.isNotEmpty() || onlyConfigured)
            }
        } else {
            items(games, key = { it.packageName }) { game ->
                GameCardItem(
                    game = game,
                    onEdit = { onEditProfile(game) },
                    onLaunch = { onLaunchGame(game) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun ShizukuBanner(
    status: ShizukuStatusInfo,
    onRequestShizuku: () -> Unit,
    onOpenShizukuApp: () -> Unit,
    onRefresh: () -> Unit
) {
    val (statusColor, statusTitle) = when (status.state) {
        ShizukuState.CONNECTED -> Pair(NeonLime, "SHIZUKU CONNECTED (PRIVILEGED)")
        ShizukuState.AUTHORIZATION_REQUIRED -> Pair(HyperCyan, "SHIZUKU AUTHORIZATION REQUIRED")
        ShizukuState.CONNECTING -> Pair(ElectricViolet, "CONNECTING TO SHIZUKU...")
        ShizukuState.PERMISSION_DENIED -> Pair(AlertCoral, "SHIZUKU PERMISSION DENIED")
        ShizukuState.NOT_RUNNING -> Pair(WarningAmber, "SHIZUKU NOT RUNNING")
        ShizukuState.CONNECTION_FAILED -> Pair(AlertCoral, "SHIZUKU CONNECTION FAILED")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.8f)),
        modifier = Modifier.fillMaxWidth().testTag("shizuku_status_banner")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusTitle,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Shizuku",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = status.detailMessage.ifEmpty { "Checking Shizuku status..." },
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // Dynamic Action Button based on state
            when (status.state) {
                ShizukuState.AUTHORIZATION_REQUIRED -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRequestShizuku,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HyperCyan,
                            contentColor = CyberBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("authorize_shizuku_button")
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Authorize StretchX", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                ShizukuState.CONNECTING -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { /* In progress */ },
                        enabled = false,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("connecting_shizuku_button")
                    ) {
                        Text("Requesting Authorization...", fontSize = 12.sp)
                    }
                }
                ShizukuState.PERMISSION_DENIED -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenShizukuApp,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AlertCoral,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("open_shizuku_app_button")
                        ) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Shizuku App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onRequestShizuku,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AlertCoral),
                            modifier = Modifier.weight(1f).testTag("retry_auth_button")
                        ) {
                            Text("Retry Prompt", color = AlertCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                ShizukuState.NOT_RUNNING -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenShizukuApp,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HyperCyan,
                                contentColor = CyberBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("start_shizuku_button")
                        ) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Shizuku App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HyperCyan),
                            modifier = Modifier.weight(1f).testTag("check_shizuku_button")
                        ) {
                            Text("Check Again", color = HyperCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                ShizukuState.CONNECTION_FAILED -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AlertCoral,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("retry_connection_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Connection", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                ShizukuState.CONNECTED -> {
                    // Connected: fully authorized, no prompt button needed
                }
            }
        }
    }
}

@Composable
private fun ActiveDisplayAlertBar(
    currentConfig: DisplayConfig,
    nativeConfig: DisplayConfig,
    onReset: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, HyperCyan),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ACTIVE RESOLUTION",
                    color = HyperCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${currentConfig.width}×${currentConfig.height} @ ${currentConfig.densityDpi} DPI",
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Native: ${nativeConfig.width}×${nativeConfig.height}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlertCoral,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("emergency_reset_button")
            ) {
                Text("Reset", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GameCardItem(
    game: InstalledGame,
    onEdit: () -> Unit,
    onLaunch: () -> Unit
) {
    val context = LocalContext.current
    val appIcon: Drawable? = remember(game.packageName) {
        try {
            context.packageManager.getApplicationIcon(game.packageName)
        } catch (_: Exception) {
            null
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (game.hasCustomProfile) HyperCyan.copy(alpha = 0.5f) else CyberCardBorder
        ),
        modifier = Modifier.fillMaxWidth().testTag("game_card_${game.packageName}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    val bitmap = remember(appIcon) {
                        try { appIcon.toBitmap(96, 96).asImageBitmap() } catch (_: Exception) { null }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "${game.appName} Icon",
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        Text(game.appName.take(1), color = HyperCyan, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(game.appName.take(1), color = HyperCyan, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Game Name & Config Badge
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = game.appName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                    if (game.configuredProfile?.isFavorite == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = HyperCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (game.configuredProfile != null) {
                    val p = game.configuredProfile
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${p.scalingMode.label} (${p.projectionMultiplier}× • ${p.customWidth}×${p.customHeight})",
                        color = HyperCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Default Native Display",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Quick Actions: Edit Profile & Launch
            IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Configure Profile",
                    tint = if (game.hasCustomProfile) HyperCyan else TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HyperCyan,
                    contentColor = CyberBackground
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("launch_game_button_${game.packageName}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptyGamesCard(isFiltered: Boolean) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isFiltered) "No matching games found" else "Scanning installed games...",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isFiltered) "Try adjusting your search terms or filters" else "Installed games on your device will appear here automatically",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
