package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted profile defining per-game projection and display adjustments.
 */
@Entity(tableName = "game_profiles")
data class GameProfile(
    @PrimaryKey val packageName: String,
    val appName: String,
    val projectionMultiplier: Float = 1.11f,
    val scalingModeName: String = ScalingMode.STRETCH.name,
    val customWidth: Int = 1080,
    val customHeight: Int = 2597,
    val customDpi: Int = 420,
    val autoApplyOnLaunch: Boolean = true,
    val autoRestoreOnExit: Boolean = true,
    val showFloatingBubble: Boolean = true,
    val isFavorite: Boolean = false,
    val lastUsedTimestamp: Long = 0L
) {
    val scalingMode: ScalingMode
        get() = try {
            ScalingMode.valueOf(scalingModeName)
        } catch (_: Exception) {
            ScalingMode.STRETCH
        }

    val displayConfig: DisplayConfig
        get() = DisplayConfig(customWidth, customHeight, customDpi)
}

/**
 * Runtime game item from package manager scanner.
 */
data class InstalledGame(
    val packageName: String,
    val appName: String,
    val isSystemGameCategory: Boolean,
    val hasCustomProfile: Boolean = false,
    val configuredProfile: GameProfile? = null
)

/**
 * Game session lifecycle states.
 */
enum class SessionStatus(val label: String) {
    IDLE("No Active Game Session"),
    GAME_ACTIVE("Game Active — Stretched Projection Active"),
    LAUNCHER_RESTORED("Launcher / Background Clean — Native Display Restored"),
    TERMINATED("Session Ended — Display Restored")
}
