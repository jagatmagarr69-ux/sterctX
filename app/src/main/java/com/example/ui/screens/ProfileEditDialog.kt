package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.Dialog
import com.example.model.DisplayConfig
import com.example.model.GameProfile
import com.example.model.InstalledGame
import com.example.model.ProjectionEngine
import com.example.model.ScalingMode
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.HyperCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditDialog(
    game: InstalledGame,
    nativeConfig: DisplayConfig,
    onSave: (GameProfile) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val existing = game.configuredProfile

    var multiplier by remember {
        mutableFloatStateOf(existing?.projectionMultiplier ?: 1.11f)
    }
    var selectedMode by remember {
        mutableStateOf(existing?.scalingMode ?: ScalingMode.STRETCH)
    }

    var autoApply by remember { mutableStateOf(existing?.autoApplyOnLaunch ?: true) }
    var autoRestore by remember { mutableStateOf(existing?.autoRestoreOnExit ?: true) }
    var showBubble by remember { mutableStateOf(existing?.showFloatingBubble ?: true) }
    var isFavorite by remember { mutableStateOf(existing?.isFavorite ?: false) }

    val calculatedTarget = remember(nativeConfig, multiplier, selectedMode) {
        ProjectionEngine.calculateProjection(nativeConfig, multiplier, selectedMode)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("profile_edit_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Game Name & Favorite Star
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = game.appName,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = game.packageName,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Favorite Toggle",
                            tint = if (isFavorite) HyperCyan else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Projection Multiplier Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROJECTION MULTIPLIER",
                        color = HyperCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${String.format("%.2f", multiplier)}×",
                        color = HyperCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Slider(
                    value = multiplier,
                    onValueChange = { multiplier = it },
                    valueRange = 1.00f..1.30f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        thumbColor = HyperCyan,
                        activeTrackColor = HyperCyan,
                        inactiveTrackColor = CyberSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Multiplier Presets
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1.00f, 1.05f, 1.11f, 1.15f, 1.20f, 1.30f).forEach { m ->
                        val isSelected = Math.abs(multiplier - m) < 0.005f
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) HyperCyan.copy(alpha = 0.2f) else CyberSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) HyperCyan else CyberCardBorder
                            ),
                            modifier = Modifier.clickable { multiplier = m }
                        ) {
                            Text(
                                text = "${String.format("%.2f", m)}x",
                                color = if (isSelected) HyperCyan else TextPrimary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Real-time calculation readout
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Target Projection: ${calculatedTarget.longEdge} × ${calculatedTarget.shortEdge}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Native: ${nativeConfig.longEdge}×${nativeConfig.shortEdge} • Zero Top/Bottom Bars",
                            color = HyperCyan,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scaling Mode Chips
                Text(
                    text = "SCALING MODE",
                    color = HyperCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                text = mode.label.substringBefore(" ("),
                                color = if (isSelected) HyperCyan else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { autoApply = !autoApply }
                ) {
                    Checkbox(
                        checked = autoApply,
                        onCheckedChange = { autoApply = it },
                        colors = CheckboxDefaults.colors(checkedColor = HyperCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Apply projection when game launches",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { autoRestore = !autoRestore }
                ) {
                    Checkbox(
                        checked = autoRestore,
                        onCheckedChange = { autoRestore = it },
                        colors = CheckboxDefaults.colors(checkedColor = HyperCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Auto-restore native display when leaving game",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showBubble = !showBubble }
                ) {
                    Checkbox(
                        checked = showBubble,
                        onCheckedChange = { showBubble = it },
                        colors = CheckboxDefaults.colors(checkedColor = HyperCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Show floating in-game controller bubble",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (existing != null) {
                        OutlinedButton(
                            onClick = {
                                onDelete(game.packageName)
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCoral),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AlertCoral)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val newProfile = GameProfile(
                                packageName = game.packageName,
                                appName = game.appName,
                                projectionMultiplier = multiplier,
                                scalingModeName = selectedMode.name,
                                customWidth = calculatedTarget.width,
                                customHeight = calculatedTarget.height,
                                customDpi = calculatedTarget.densityDpi,
                                autoApplyOnLaunch = autoApply,
                                autoRestoreOnExit = autoRestore,
                                showFloatingBubble = showBubble,
                                isFavorite = isFavorite,
                                lastUsedTimestamp = System.currentTimeMillis()
                            )
                            onSave(newProfile)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HyperCyan,
                            contentColor = CyberSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("save_profile_button")
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
