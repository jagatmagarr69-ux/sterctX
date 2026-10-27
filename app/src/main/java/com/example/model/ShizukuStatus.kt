package com.example.model

/**
 * Shizuku service status on the host device.
 */
enum class ShizukuState(val displayName: String) {
    NOT_RUNNING("Not Running"),
    AUTHORIZATION_REQUIRED("Authorization Required"),
    CONNECTING("Connecting"),
    CONNECTED("Connected"),
    PERMISSION_DENIED("Permission Denied"),
    CONNECTION_FAILED("Connection Failed");

    val isConnected: Boolean
        get() = this == CONNECTED
}

data class ShizukuStatusInfo(
    val state: ShizukuState,
    val version: Int = 0,
    val isRoot: Boolean = false,
    val uid: Int = -1,
    val detailMessage: String = ""
)

/**
 * Runtime state of active display configuration and game session.
 */
data class SessionState(
    val activePackage: String? = null,
    val activeGameName: String? = null,
    val sessionStatus: SessionStatus = SessionStatus.IDLE,
    val isAltered: Boolean = false,
    val currentConfig: DisplayConfig,
    val nativeConfig: DisplayConfig,
    val projectionMultiplier: Float = 1.11f,
    val scalingMode: ScalingMode = ScalingMode.STRETCH,
    val isCountdownActive: Boolean = false,
    val countdownSecondsRemaining: Int = 0,
    val lastAppliedMessage: String? = null
)
