package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StretchXDarkColorScheme = darkColorScheme(
    primary = HyperCyan,
    onPrimary = Color(0xFF00201A),
    primaryContainer = HyperCyanContainer,
    onPrimaryContainer = HyperCyan,
    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = ElectricVioletContainer,
    onSecondaryContainer = Color(0xFFD8B4FE),
    tertiary = AlertCoral,
    onTertiary = Color.White,
    tertiaryContainer = AlertCoralContainer,
    onTertiaryContainer = Color(0xFFFFD9E2),
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardBorder,
    error = AlertCoral,
    onError = Color.White
)

@Composable
fun StretchXTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = StretchXDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = CyberBackground.toArgb()
                window.navigationBarColor = CyberBackground.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
