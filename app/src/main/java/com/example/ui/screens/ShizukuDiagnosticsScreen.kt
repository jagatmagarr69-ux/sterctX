package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DisplayConfig
import com.example.model.ShizukuState
import com.example.model.ShizukuStatusInfo
import com.example.shizuku.ShizukuManager
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
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@Composable
fun ShizukuDiagnosticsScreen(
    status: ShizukuStatusInfo,
    nativeConfig: DisplayConfig,
    currentConfig: DisplayConfig,
    onRequestAuth: () -> Unit,
    onOpenShizukuApp: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var diagnosticLog by remember { mutableStateOf<String?>(null) }
    var isTestingPing by remember { mutableStateOf(false) }

    val (statusColor, statusTitle) = when (status.state) {
        ShizukuState.CONNECTED -> Pair(NeonLime, "SHIZUKU CONNECTED")
        ShizukuState.AUTHORIZATION_REQUIRED -> Pair(HyperCyan, "AUTHORIZATION REQUIRED")
        ShizukuState.CONNECTING -> Pair(ElectricViolet, "CONNECTING...")
        ShizukuState.PERMISSION_DENIED -> Pair(AlertCoral, "PERMISSION DENIED")
        ShizukuState.NOT_RUNNING -> Pair(WarningAmber, "NOT RUNNING")
        ShizukuState.CONNECTION_FAILED -> Pair(AlertCoral, "CONNECTION FAILED")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor),
                modifier = Modifier.fillMaxWidth().testTag("shizuku_diagnostics_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = statusTitle,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        }

                        IconButton(onClick = onRefresh) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = HyperCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = status.detailMessage,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DiagnosticMetric("STATE", status.state.displayName)
                        DiagnosticMetric("VERSION", if (status.version > 0) "v${status.version}" else "N/A")
                        DiagnosticMetric(
                            "PRIVILEGE",
                            if (status.isRoot) "Root (UID 0)"
                            else if (status.uid >= 0) "ADB (UID ${status.uid})"
                            else "None"
                        )
                    }

                    // Dynamic Buttons
                    when (status.state) {
                        ShizukuState.AUTHORIZATION_REQUIRED -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRequestAuth,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HyperCyan,
                                    contentColor = CyberBackground
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("diag_authorize_button")
                            ) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Request Shizuku Authorization", fontWeight = FontWeight.Bold)
                            }
                        }
                        ShizukuState.CONNECTING -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { },
                                enabled = false,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Connecting to Shizuku service...")
                            }
                        }
                        ShizukuState.PERMISSION_DENIED -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onOpenShizukuApp,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AlertCoral,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Shizuku", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onRequestAuth,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AlertCoral),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Request Again", color = AlertCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        ShizukuState.NOT_RUNNING -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onOpenShizukuApp,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HyperCyan,
                                        contentColor = CyberBackground
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Shizuku", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onRefresh,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, HyperCyan),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Retry Check", color = HyperCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        ShizukuState.CONNECTION_FAILED -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AlertCoral,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry Shizuku Connection", fontWeight = FontWeight.Bold)
                            }
                        }
                        ShizukuState.CONNECTED -> {
                            // Already connected!
                        }
                    }
                }
            }
        }

        // Hardware Baseline vs Current Override
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "HARDWARE DISPLAY BASELINE",
                        color = HyperCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Physical Dimensions", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                "${nativeConfig.width} × ${nativeConfig.height}",
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Column {
                            Text("Physical Density", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                "${nativeConfig.densityDpi} DPI",
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Column {
                            Text("Active Status", color = TextSecondary, fontSize = 11.sp)
                            val isAltered = currentConfig != nativeConfig
                            Text(
                                if (isAltered) "MODIFIED" else "NATIVE",
                                color = if (isAltered) AlertCoral else NeonLime,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Shell Ping & Diagnostic Test
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SHELL COMMAND RUNNER",
                            color = HyperCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isTestingPing = true
                                    val sizeRes = ShizukuManager.executeCommand("wm size")
                                    val densityRes = ShizukuManager.executeCommand("wm density")
                                    diagnosticLog = ">> wm size:\n${sizeRes.getOrNull() ?: sizeRes.exceptionOrNull()?.message}\n\n>> wm density:\n${densityRes.getOrNull() ?: densityRes.exceptionOrNull()?.message}"
                                    isTestingPing = false
                                }
                            },
                            enabled = status.state.isConnected,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (status.state.isConnected) HyperCyan else CyberCardBorder
                            ),
                            modifier = Modifier.testTag("run_ping_test_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = if (status.state.isConnected) HyperCyan else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Test wm Query",
                                color = if (status.state.isConnected) HyperCyan else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (diagnosticLog != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, CyberCardBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = diagnosticLog ?: "",
                                color = HyperCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Setup Instructions
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "SHIZUKU SETUP GUIDE (NO ROOT REQUIRED)",
                        color = HyperCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    InstructionStep("1", "Install Shizuku", "Download the official Shizuku app from Google Play or GitHub.")
                    InstructionStep("2", "Enable Wireless Debugging", "In Android Settings > Developer Options, enable Wireless Debugging and connect to Wi-Fi.")
                    InstructionStep("3", "Pair and Start Shizuku", "In Shizuku, tap 'Pairing' and enter the pairing code from developer options. Tap 'Start'.")
                    InstructionStep("4", "Authorize StretchX", "Return to StretchX and tap 'Authorize StretchX' to grant privileged window manager access.")
                }
            }
        }

        // Technical Boundaries Notice
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp)) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = HyperCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Technical Boundaries", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "• Shizuku grants ADB-level permissions (UID 2000), not full Linux root (UID 0).\n• 'wm overscan' was deprecated and removed by Google in Android 10+ AOSP. StretchX computes exact proportional viewport dimensions to achieve stretched ratios.\n• Original display resolution is permanently preserved and can be restored at any time via the Emergency Reset button or Quick Settings Tile.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
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

@Composable
private fun DiagnosticMetric(label: String, value: String) {
    Column {
        Text(text = label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InstructionStep(num: String, title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = HyperCyan.copy(alpha = 0.2f),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(num, color = HyperCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(description, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
