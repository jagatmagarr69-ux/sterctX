package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DisplayConfig
import com.example.model.InstalledGame
import com.example.model.ProjectionEngine
import com.example.model.ScalingMode
import com.example.model.SessionState
import com.example.model.ShizukuStatusInfo
import com.example.ui.components.AspectRatioCanvasPreview
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.HyperCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResolutionTunerScreen(
    currentConfig: DisplayConfig,
    nativeConfig: DisplayConfig,
    shizukuStatus: ShizukuStatusInfo,
    sessionState: SessionState,
    installedGames: List<InstalledGame>,
    onActivateProjection: (DisplayConfig, Float, ScalingMode, String?) -> Unit,
    onResetToNative: () -> Unit,
    modifier: Modifier = Modifier
) {
    var multiplier by remember { mutableFloatStateOf(1.11f) }
    var selectedMode by remember { mutableStateOf(ScalingMode.STRETCH) }
    var selectedGamePackage by remember {
        mutableStateOf(installedGames.firstOrNull()?.packageName)
    }

    val selectedGame = installedGames.firstOrNull { it.packageName == selectedGamePackage }

    // Real-time calculation matching Cat Mouse Pro reference model
    val projectedTargetConfig = remember(nativeConfig, multiplier, selectedMode) {
        ProjectionEngine.calculateProjection(nativeConfig, multiplier, selectedMode)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Game Session Status Indicator
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (sessionState.isAltered) NeonLime else CyberCardBorder
                ),
                modifier = Modifier.fillMaxWidth().testTag("session_status_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GAME SESSION MANAGER",
                            color = HyperCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (sessionState.isAltered) NeonLime.copy(alpha = 0.2f) else CyberSurfaceVariant
                        ) {
                            Text(
                                text = if (sessionState.isAltered) "PROJECTION ACTIVE" else "STANDBY (NATIVE)",
                                color = if (sessionState.isAltered) NeonLime else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (sessionState.isAltered) {
                            "Active in: ${sessionState.activeGameName ?: "Game"} (${sessionState.currentConfig.longEdge}×${sessionState.currentConfig.shortEdge})"
                        } else {
                            "Native display active. Launcher and system applications are at original ${nativeConfig.longEdge}×${nativeConfig.shortEdge}."
                        },
                        color = TextPrimary,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "StretchX automatically restores native resolution whenever you return to the launcher or switch apps.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Viewport Projection Canvas Preview
        item {
            AspectRatioCanvasPreview(
                nativeConfig = nativeConfig,
                targetConfig = projectedTargetConfig,
                multiplier = multiplier,
                scalingMode = selectedMode
            )
        }

        // Projection Multiplier System (Cat Mouse Pro Reference)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier.fillMaxWidth().testTag("projection_multiplier_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PROJECTION MULTIPLIER",
                                color = HyperCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "projectedWidth = round(${nativeConfig.longEdge} × ${String.format("%.2f", multiplier)}) = ${projectedTargetConfig.longEdge} px",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "${String.format("%.2f", multiplier)}×",
                            color = HyperCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = multiplier,
                        onValueChange = { multiplier = it },
                        valueRange = 1.00f..1.30f,
                        steps = 29, // 0.01 increments
                        colors = SliderDefaults.colors(
                            thumbColor = HyperCyan,
                            activeTrackColor = HyperCyan,
                            inactiveTrackColor = CyberSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("projection_multiplier_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Multiplier Quick Presets
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            1.00f to "1.00× (Native)",
                            1.05f to "1.05× (Subtle)",
                            1.10f to "1.10× (Wider)",
                            1.11f to "1.11× (Ref)",
                            1.15f to "1.15× (Medium)",
                            1.20f to "1.20× (Aggressive)",
                            1.30f to "1.30× (Max)"
                        ).forEach { (m, label) ->
                            val isSelected = Math.abs(multiplier - m) < 0.005f
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) HyperCyan.copy(alpha = 0.2f) else CyberSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) HyperCyan else CyberCardBorder
                                ),
                                modifier = Modifier
                                    .clickable { multiplier = m }
                                    .testTag("preset_multiplier_${String.format("%.2f", m)}")
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) HyperCyan else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Per-Game Scaling Modes
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PER-GAME SCALING MODE",
                        color = HyperCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScalingMode.entries.forEach { mode ->
                            val isSelected = selectedMode == mode
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) HyperCyan.copy(alpha = 0.2f) else CyberSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) HyperCyan else CyberCardBorder
                                ),
                                modifier = Modifier.clickable { selectedMode = mode }
                            ) {
                                Text(
                                    text = mode.label,
                                    color = if (isSelected) HyperCyan else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedMode.description,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Target Game Association
        if (installedGames.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "TARGET APPLICATION",
                            color = HyperCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Associate this projection profile with an installed game for automated session lifecycle activation:",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            installedGames.take(6).forEach { game ->
                                val isSelected = selectedGamePackage == game.packageName
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) HyperCyan.copy(alpha = 0.2f) else CyberSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) HyperCyan else CyberCardBorder
                                    ),
                                    modifier = Modifier.clickable { selectedGamePackage = game.packageName }
                                ) {
                                    Text(
                                        text = game.appName,
                                        color = if (isSelected) HyperCyan else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons: Activate Session vs Restore Display
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        onActivateProjection(
                            projectedTargetConfig,
                            multiplier,
                            selectedMode,
                            selectedGamePackage
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HyperCyan,
                        contentColor = CyberBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("activate_projection_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedGame != null) "Launch Session: ${selectedGame.appName}" else "Activate Projection Session",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                OutlinedButton(
                    onClick = onResetToNative,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCoral),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertCoral),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reset_native_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Restore Display (Reset to Native ${nativeConfig.longEdge}×${nativeConfig.shortEdge})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Technical Architecture Transparency Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = HyperCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Per-Game Architecture & Zero-Black-Bar Guarantee",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Zero Black Bars: By locking the short edge exactly to the physical panel height (${nativeConfig.shortEdge}px) and applying the multiplier horizontally (${projectedTargetConfig.longEdge}px), the projection fills the physical display vertically with zero letterboxing.\n\n2. Launcher & System Preservation: Android AOSP does not provide an isolated per-app logical display API without root. StretchX achieves true per-game isolation through active Game Session Lifecycle Monitoring: while in the game, projection is active; the instant you return to the launcher or switch apps, StretchX automatically restores native resolution (${nativeConfig.longEdge}×${nativeConfig.shortEdge}), keeping your home screen and all other apps completely clean.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
