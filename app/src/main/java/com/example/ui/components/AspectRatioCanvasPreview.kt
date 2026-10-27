package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DisplayConfig
import com.example.model.ScalingMode
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.HyperCyan
import com.example.ui.theme.NeonLime
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AspectRatioCanvasPreview(
    nativeConfig: DisplayConfig,
    targetConfig: DisplayConfig,
    multiplier: Float,
    scalingMode: ScalingMode,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberSurface, RoundedCornerShape(16.dp))
            .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PROJECTION VIEWPORT PREVIEW",
                color = HyperCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = "${String.format("%.2f", multiplier)}× Multiplier",
                color = NeonLime,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(CyberBackground, RoundedCornerShape(12.dp))
                .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp).padding(16.dp)) {
                val canvasW = size.width
                val canvasH = size.height

                // Native landscape ratio (e.g. 2340 / 1080 = ~2.16)
                val nativeAspect = if (nativeConfig.shortEdge > 0) {
                    nativeConfig.longEdge.toFloat() / nativeConfig.shortEdge.toFloat()
                } else 2.16f

                // Draw physical screen boundary
                val phoneW = canvasW * 0.85f
                val phoneH = (phoneW / nativeAspect).coerceAtMost(canvasH * 0.85f)
                val phoneLeft = (canvasW - phoneW) / 2f
                val phoneTop = (canvasH - phoneH) / 2f

                // Physical panel outline
                drawRoundRect(
                    color = Color(0xFF28354D),
                    topLeft = Offset(phoneLeft, phoneTop),
                    size = Size(phoneW, phoneH),
                    cornerRadius = CornerRadius(12f, 12f),
                    style = Stroke(width = 2f)
                )

                // Projected horizontal stretch rendering
                // Multiplier stretches the horizontal field of view
                val stretchFactor = multiplier.coerceIn(1.0f, 1.4f)
                val fillRatio = 1.0f / stretchFactor
                val activeW = phoneW
                val activeH = phoneH // Exactly fills vertical panel! No top/bottom black bars!

                // Shaded viewport
                drawRoundRect(
                    color = Color(0x2200F5D4),
                    topLeft = Offset(phoneLeft, phoneTop),
                    size = Size(activeW, activeH),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Projected boundary outline
                drawRoundRect(
                    color = HyperCyan,
                    topLeft = Offset(phoneLeft, phoneTop),
                    size = Size(activeW, activeH),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(
                        width = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                    )
                )

                // Horizontal stretch arrows indicating outward FOV expansion
                val cy = phoneTop + phoneH / 2f
                val arrowOffset = phoneW * 0.15f
                // Left arrow
                drawLine(
                    color = HyperCyan,
                    start = Offset(phoneLeft + arrowOffset, cy),
                    end = Offset(phoneLeft + 15f, cy),
                    strokeWidth = 3f
                )
                // Right arrow
                drawLine(
                    color = HyperCyan,
                    start = Offset(phoneLeft + phoneW - arrowOffset, cy),
                    end = Offset(phoneLeft + phoneW - 15f, cy),
                    strokeWidth = 3f
                )

                // Center crosshair
                val cx = phoneLeft + phoneW / 2f
                drawLine(
                    color = Color(0x6600F5D4),
                    start = Offset(cx - 12f, cy),
                    end = Offset(cx + 12f, cy),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color(0x6600F5D4),
                    start = Offset(cx, cy - 12f),
                    end = Offset(cx, cy + 12f),
                    strokeWidth = 2f
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Metrics pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricPill("PHYSICAL", "${nativeConfig.longEdge}×${nativeConfig.shortEdge}")
            MetricPill("PROJECTED", "${targetConfig.longEdge}×${targetConfig.shortEdge}")
            MetricPill("DENSITY", "${targetConfig.densityDpi} DPI")
            MetricPill("BLACK BARS", "0 px (None)")
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
