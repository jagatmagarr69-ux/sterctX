package com.example.shizuku

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.example.model.DisplayConfig

object DisplayCommandRunner {
    private const val TAG = "StretchX_Display"

    /**
     * Resolves the device's native hardware display metrics using WindowManager as baseline fallback.
     */
    fun getHardwareBaseline(context: Context): DisplayConfig {
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = wm.currentWindowMetrics.bounds
                val densityDpi = context.resources.configuration.densityDpi
                DisplayConfig(bounds.width(), bounds.height(), densityDpi)
            } else {
                @Suppress("DEPRECATION")
                val display = wm.defaultDisplay
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                display.getRealMetrics(metrics)
                DisplayConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get hardware baseline", e)
            DisplayConfig(1080, 2400, 420)
        }
    }

    /**
     * Queries physical and override size from `wm size`.
     */
    suspend fun queryDisplaySize(): Pair<DisplayConfigSize?, DisplayConfigSize?> {
        val result = ShizukuManager.executeCommand("wm size")
        if (result.isFailure) return Pair(null, null)
        val text = result.getOrNull() ?: ""

        var physical: DisplayConfigSize? = null
        var override: DisplayConfigSize? = null

        text.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("Physical size:", ignoreCase = true)) {
                parseDimensions(trimmed.substringAfter("Physical size:").trim())?.let {
                    physical = it
                }
            } else if (trimmed.startsWith("Override size:", ignoreCase = true)) {
                parseDimensions(trimmed.substringAfter("Override size:").trim())?.let {
                    override = it
                }
            }
        }
        return Pair(physical, override)
    }

    /**
     * Queries physical and override density from `wm density`.
     */
    suspend fun queryDisplayDensity(): Pair<Int?, Int?> {
        val result = ShizukuManager.executeCommand("wm density")
        if (result.isFailure) return Pair(null, null)
        val text = result.getOrNull() ?: ""

        var physical: Int? = null
        var override: Int? = null

        text.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("Physical density:", ignoreCase = true)) {
                physical = trimmed.substringAfter("Physical density:").trim().toIntOrNull()
            } else if (trimmed.startsWith("Override density:", ignoreCase = true)) {
                override = trimmed.substringAfter("Override density:").trim().toIntOrNull()
            }
        }
        return Pair(physical, override)
    }

    /**
     * Queries the currently focused or resumed application package via ADB shell dumpsys.
     * Used by the Game Session Manager to detect when user leaves the game or returns.
     */
    suspend fun queryTopPackage(): String? {
        val focusResult = ShizukuManager.executeCommand("dumpsys window | grep -E 'mCurrentFocus'")
        if (focusResult.isSuccess) {
            val text = focusResult.getOrNull() ?: ""
            extractPackageFromDumpsys(text)?.let { return it }
        }

        val activityResult = ShizukuManager.executeCommand("dumpsys activity activities | grep -E 'mResumedActivity'")
        if (activityResult.isSuccess) {
            val text = activityResult.getOrNull() ?: ""
            extractPackageFromDumpsys(text)?.let { return it }
        }

        return null
    }

    private fun extractPackageFromDumpsys(text: String): String? {
        val regex = Regex("([a-zA-Z0-9_]+\\.[a-zA-Z0-9_.]+)/")
        val match = regex.find(text)
        return match?.groupValues?.get(1)
    }

    /**
     * Applies resolution and density in one synchronized operation.
     */
    suspend fun applyConfiguration(config: DisplayConfig): Result<Unit> {
        val sizeCmd = "wm size ${config.width}x${config.height}"
        val densityCmd = if (config.densityDpi > 0) "wm density ${config.densityDpi}" else null

        val sizeResult = ShizukuManager.executeCommand(sizeCmd)
        if (sizeResult.isFailure) {
            return Result.failure(sizeResult.exceptionOrNull() ?: RuntimeException("Failed to set size"))
        }

        if (densityCmd != null) {
            val densityResult = ShizukuManager.executeCommand(densityCmd)
            if (densityResult.isFailure) {
                Log.w(TAG, "Size succeeded but density failed: ${densityResult.exceptionOrNull()?.message}")
            }
        }

        return Result.success(Unit)
    }

    /**
     * Resets both resolution and density back to device physical hardware defaults.
     */
    suspend fun resetDisplay(): Result<Unit> {
        val sizeReset = ShizukuManager.executeCommand("wm size reset")
        val densityReset = ShizukuManager.executeCommand("wm density reset")

        if (sizeReset.isFailure && densityReset.isFailure) {
            return Result.failure(
                sizeReset.exceptionOrNull() ?: RuntimeException("Display reset failed")
            )
        }
        return Result.success(Unit)
    }

    private fun parseDimensions(str: String): DisplayConfigSize? {
        val parts = str.split("x")
        if (parts.size == 2) {
            val w = parts[0].trim().toIntOrNull()
            val h = parts[1].trim().toIntOrNull()
            if (w != null && h != null && w > 0 && h > 0) {
                return DisplayConfigSize(w, h)
            }
        }
        return null
    }

    data class DisplayConfigSize(val width: Int, val height: Int)
}
