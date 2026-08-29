package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.LaserLime
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.QuantumCyan
import com.example.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Matrix / Cyber Grid Canvas background
 */
@Composable
fun QuantumMatrixBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_grid")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_line"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Draw subtle matrix grid
            val gridSize = 48.dp.toPx()
            val gridColor = Color(0xFF071F11).copy(alpha = 0.45f)

            var x = 0f
            while (x <= width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 0.8f
                )
                x += gridSize
            }

            var y = 0f
            while (y <= height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 0.8f
                )
                y += gridSize
            }

            // Animated scanning beam
            val currentScanY = height * scanLineY
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        NeonGreen.copy(alpha = 0.35f),
                        QuantumCyan.copy(alpha = 0.5f),
                        NeonGreen.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, currentScanY),
                end = Offset(width, currentScanY),
                strokeWidth = 2.5f
            )
        }

        content()
    }
}

/**
 * Atomic Quantum Orbital Emblem (Inspired by Quantum Hacker core icon)
 */
@Composable
fun QuantumOrbitalIcon(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 36.dp,
    primaryColor: Color = NeonGreen,
    secondaryColor: Color = QuantumCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "quantum_spin")
    val angle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_1"
    )
    val angle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_2"
    )

    Canvas(modifier = modifier.size(sizeDp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2.2f

        // Central nucleus
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor, LaserLime, Color.Transparent),
                center = center,
                radius = radius * 0.45f
            ),
            radius = radius * 0.45f,
            center = center
        )
        drawCircle(
            color = Color.White,
            radius = radius * 0.18f,
            center = center
        )

        // Orbit 1: Ellipse at 0 deg
        rotate(degrees = angle1 * 0.3f, pivot = center) {
            drawOval(
                color = primaryColor.copy(alpha = 0.85f),
                topLeft = Offset(center.x - radius, center.y - radius * 0.4f),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 0.8f),
                style = Stroke(width = 1.6f)
            )

            // Electron particle on Orbit 1
            val rad = Math.toRadians(angle1.toDouble())
            val ex = center.x + (radius * cos(rad)).toFloat()
            val ey = center.y + (radius * 0.4f * sin(rad)).toFloat()
            drawCircle(color = primaryColor, radius = 2.5f, center = Offset(ex, ey))
        }

        // Orbit 2: Ellipse at 60 deg
        rotate(degrees = 60f + (angle2 * 0.25f), pivot = center) {
            drawOval(
                color = secondaryColor.copy(alpha = 0.85f),
                topLeft = Offset(center.x - radius, center.y - radius * 0.4f),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 0.8f),
                style = Stroke(width = 1.6f)
            )

            // Electron particle on Orbit 2
            val rad = Math.toRadians(angle2.toDouble())
            val ex = center.x + (radius * cos(rad)).toFloat()
            val ey = center.y + (radius * 0.4f * sin(rad)).toFloat()
            drawCircle(color = secondaryColor, radius = 2.5f, center = Offset(ex, ey))
        }

        // Orbit 3: Ellipse at 120 deg
        rotate(degrees = 120f + (angle1 * 0.2f), pivot = center) {
            drawOval(
                color = LaserLime.copy(alpha = 0.8f),
                topLeft = Offset(center.x - radius, center.y - radius * 0.4f),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 0.8f),
                style = Stroke(width = 1.6f)
            )
        }
    }
}

/**
 * Cyber Monospace HUD Badge with neon accents
 */
@Composable
fun CyberBadge(
    text: String,
    accentColor: Color = NeonGreen,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF05140A))
            .border(1.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = accentColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Container with sci-fi cyber brackets
 */
@Composable
fun CyberCornerBox(
    modifier: Modifier = Modifier,
    borderColor: Color = NeonGreen,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .background(Color(0xFF07120C), RoundedCornerShape(12.dp))
            .padding(1.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerLen = 14.dp.toPx()
            val w = size.width
            val h = size.height
            val stroke = 2.dp.toPx()

            // Top-left corner
            drawLine(borderColor, Offset(0f, 0f), Offset(cornerLen, 0f), stroke)
            drawLine(borderColor, Offset(0f, 0f), Offset(0f, cornerLen), stroke)

            // Top-right corner
            drawLine(borderColor, Offset(w, 0f), Offset(w - cornerLen, 0f), stroke)
            drawLine(borderColor, Offset(w, 0f), Offset(w, cornerLen), stroke)

            // Bottom-left corner
            drawLine(borderColor, Offset(0f, h), Offset(cornerLen, h), stroke)
            drawLine(borderColor, Offset(0f, h), Offset(0f, h - cornerLen), stroke)

            // Bottom-right corner
            drawLine(borderColor, Offset(w, h), Offset(w - cornerLen, h), stroke)
            drawLine(borderColor, Offset(w, h), Offset(w, h - cornerLen), stroke)
        }

        content()
    }
}
