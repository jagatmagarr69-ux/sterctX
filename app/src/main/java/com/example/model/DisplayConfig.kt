package com.example.model

import kotlin.math.roundToInt

/**
 * Representation of display resolution and density metrics.
 */
data class DisplayConfig(
    val width: Int,
    val height: Int,
    val densityDpi: Int
) {
    val shortEdge: Int
        get() = minOf(width, height)

    val longEdge: Int
        get() = maxOf(width, height)

    val landscapeWidth: Int
        get() = longEdge

    val landscapeHeight: Int
        get() = shortEdge

    val aspectRatio: Float
        get() = if (shortEdge > 0) longEdge.toFloat() / shortEdge.toFloat() else 1.0f

    val formattedRatio: String
        get() {
            if (width <= 0 || height <= 0) return "Unknown"
            val gcdVal = gcd(longEdge, shortEdge)
            val rw = longEdge / gcdVal
            val rh = shortEdge / gcdVal
            return "$rw:$rh"
        }

    val displayLabel: String
        get() = "${longEdge} × ${shortEdge} @ ${densityDpi} DPI"

    val portraitLabel: String
        get() = "${shortEdge} × ${longEdge} @ ${densityDpi} DPI"

    private fun gcd(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val t = y
            y = x % y
            x = t
        }
        return x
    }
}

/**
 * Scaling modes for per-game display adjustments.
 */
enum class ScalingMode(
    val label: String,
    val description: String,
    val isGameSpecific: Boolean
) {
    STRETCH(
        label = "Stretch (Projection Multiplier)",
        description = "Scales horizontal projection across the display. Widens character hitboxes and player models without top/bottom black bars.",
        isGameSpecific = true
    ),
    CROP(
        label = "Crop (Zoom & Fill)",
        description = "Expands viewport dimensions to fill screen while cropping peripheral overscan margins.",
        isGameSpecific = true
    ),
    FIT(
        label = "Fit (Preserve Ratio)",
        description = "Maintains fixed geometric aspect ratio. Note: Requires letterboxing on wide displays to avoid distortion.",
        isGameSpecific = false
    ),
    NATIVE(
        label = "Native (1.00×)",
        description = "Default physical device display configuration with no scaling or projection applied.",
        isGameSpecific = true
    )
}

/**
 * Projection Engine Calculator matching the Cat Mouse Pro reference model.
 */
object ProjectionEngine {
    /**
     * Calculates the projection dimensions using the projection multiplier:
     * projectedLandscapeWidth = round(nativeLongEdge * multiplier)
     * projectedLandscapeHeight = nativeShortEdge
     *
     * In Android WindowManager, portrait dimensions are used for `wm size <shortEdge>x<longEdge>`
     * so that the physical short edge (1080) is preserved 1:1, preventing top and bottom black bars!
     */
    fun calculateProjection(
        native: DisplayConfig,
        multiplier: Float,
        mode: ScalingMode
    ): DisplayConfig {
        return when (mode) {
            ScalingMode.STRETCH -> {
                val projectedLong = (native.longEdge * multiplier).roundToInt()
                val projectedShort = native.shortEdge
                val projectedDpi = (native.densityDpi * (projectedLong.toFloat() / native.longEdge)).roundToInt()

                // Android wm size on portrait phones expects <shortEdge>x<longEdge>
                DisplayConfig(
                    width = projectedShort,
                    height = projectedLong,
                    densityDpi = projectedDpi
                )
            }
            ScalingMode.CROP -> {
                val cropScale = multiplier.coerceAtLeast(1.0f)
                val projectedLong = (native.longEdge * cropScale).roundToInt()
                val projectedShort = (native.shortEdge * cropScale).roundToInt()
                DisplayConfig(
                    width = projectedShort,
                    height = projectedLong,
                    densityDpi = native.densityDpi
                )
            }
            ScalingMode.FIT -> {
                // Fixed 4:3 fit inside native landscape bounds
                val targetLong = native.longEdge
                val targetShort = (native.longEdge * 3f / 4f).roundToInt()
                DisplayConfig(
                    width = targetShort,
                    height = targetLong,
                    densityDpi = native.densityDpi
                )
            }
            ScalingMode.NATIVE -> {
                DisplayConfig(
                    width = native.shortEdge,
                    height = native.longEdge,
                    densityDpi = native.densityDpi
                )
            }
        }
    }
}
