package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.VoiceState
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderGlow
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LaserLime
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.QuantumCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveVoiceOverlay(
    voiceState: VoiceState,
    statusText: String,
    amplitude: Float,
    onMicClick: () -> Unit,
    onInterruptClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "quantum_reactor")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val orbitAngle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rot_1"
    )

    val orbitAngle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rot_2"
    )

    val effectiveScale = (pulseScale + (amplitude * 0.35f)).coerceIn(0.92f, 1.35f)

    QuantumMatrixBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020604).copy(alpha = 0.95f))
                .testTag("live_voice_overlay")
        ) {
            // Top HUD Bar & Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QuantumOrbitalIcon(sizeDp = 28.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ARUSHI DUPLEX VOICE",
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "DEVELOPED BY RAUF // 48kHz LOW-LATENCY",
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkBorder, CircleShape)
                        .testTag("close_voice_overlay_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Live Voice",
                        tint = NeonGreen
                    )
                }
            }

            // Center Quantum Core Reactor Orb
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Quantum Reactor Canvas
                Box(
                    modifier = Modifier.size(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Radial Glow behind
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .scale(effectiveScale * 1.2f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        when (voiceState) {
                                            VoiceState.SPEAKING -> NeonGreen.copy(alpha = 0.45f)
                                            VoiceState.LISTENING -> LaserLime.copy(alpha = 0.4f)
                                            VoiceState.EXECUTING_ACTION -> QuantumCyan.copy(alpha = 0.5f)
                                            else -> NeonGreen.copy(alpha = 0.25f)
                                        },
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Multi-orbital atomic reactor canvas
                    Canvas(
                        modifier = Modifier
                            .size(220.dp)
                            .scale(effectiveScale)
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f

                        // Outer radar ring
                        drawCircle(
                            color = Color(0xFF0A2B18),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 1.2f)
                        )

                        // Orbital 1
                        rotate(degrees = orbitAngle1, pivot = center) {
                            drawOval(
                                color = NeonGreen.copy(alpha = 0.85f),
                                topLeft = Offset(center.x - radius * 0.9f, center.y - radius * 0.35f),
                                size = Size(radius * 1.8f, radius * 0.7f),
                                style = Stroke(width = 2f)
                            )
                            val rad1 = Math.toRadians(orbitAngle1.toDouble() * 2)
                            val ex1 = center.x + (radius * 0.9f * cos(rad1)).toFloat()
                            val ey1 = center.y + (radius * 0.35f * sin(rad1)).toFloat()
                            drawCircle(color = LaserLime, radius = 4f, center = Offset(ex1, ey1))
                        }

                        // Orbital 2
                        rotate(degrees = 60f + orbitAngle2, pivot = center) {
                            drawOval(
                                color = QuantumCyan.copy(alpha = 0.85f),
                                topLeft = Offset(center.x - radius * 0.9f, center.y - radius * 0.35f),
                                size = Size(radius * 1.8f, radius * 0.7f),
                                style = Stroke(width = 2f)
                            )
                            val rad2 = Math.toRadians(orbitAngle2.toDouble() * 2)
                            val ex2 = center.x + (radius * 0.9f * cos(rad2)).toFloat()
                            val ey2 = center.y + (radius * 0.35f * sin(rad2)).toFloat()
                            drawCircle(color = QuantumCyan, radius = 4f, center = Offset(ex2, ey2))
                        }

                        // Orbital 3
                        rotate(degrees = 120f + (orbitAngle1 * 0.7f), pivot = center) {
                            drawOval(
                                color = LaserLime.copy(alpha = 0.75f),
                                topLeft = Offset(center.x - radius * 0.9f, center.y - radius * 0.35f),
                                size = Size(radius * 1.8f, radius * 0.7f),
                                style = Stroke(width = 1.6f)
                            )
                        }
                    }

                    // Inner Singularity Core
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF030C07))
                            .border(2.dp, NeonGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (voiceState) {
                                VoiceState.SPEAKING -> Icons.AutoMirrored.Filled.VolumeUp
                                VoiceState.LISTENING -> Icons.Default.Mic
                                VoiceState.EXECUTING_ACTION -> Icons.Default.Stop
                                else -> Icons.Default.Mic
                            },
                            contentDescription = "Voice State",
                            tint = when (voiceState) {
                                VoiceState.SPEAKING -> NeonGreen
                                VoiceState.LISTENING -> LaserLime
                                VoiceState.EXECUTING_ACTION -> QuantumCyan
                                else -> TextPrimary
                            },
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cyber Matrix Spectrum Equalizer
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(36.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 11) {
                        val barHeight = remember(amplitude, i) {
                            val factor = sin((i * 0.65) + (amplitude * 6)).toFloat()
                            val h = (8 + (amplitude * 28 * (0.5f + (factor * 0.5f)))).coerceIn(5f, 34f)
                            h.dp
                        }
                        Box(
                            modifier = Modifier
                                .width(4.5.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(NeonGreen, QuantumCyan)
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quantum State Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF05170B),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(NeonGreen)
                    )
                ) {
                    Text(
                        text = when (voiceState) {
                            VoiceState.SPEAKING -> "❖ ARUSHI TRANSMITTING"
                            VoiceState.LISTENING -> "❯ LISTENING TO SPEECH..."
                            VoiceState.THINKING -> "⚡ PROCESSING INTENT"
                            VoiceState.EXECUTING_ACTION -> "⚙ EXECUTING APP ACTION"
                            else -> "❖ ARUSHI LIVE READY"
                        },
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Message
                Text(
                    text = statusText,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Multilingual indicator
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = "Languages",
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hindi • English • Hinglish • Marathi • Gujarati • Bengali • Tamil • Telugu • Urdu",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }

            // Bottom Controls (Interrupt & Mic)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 36.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Interrupt Button
                    Button(
                        onClick = onInterruptClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F080C),
                            contentColor = CrimsonRed
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(CrimsonRed)
                        ),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("interrupt_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Interrupt",
                            tint = CrimsonRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "INTERRUPT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Toggle Mic Button
                    IconButton(
                        onClick = onMicClick,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                if (voiceState == VoiceState.LISTENING) LaserLime else NeonGreen
                            )
                            .border(2.dp, DarkBorderGlow, CircleShape)
                            .testTag("toggle_voice_mic_button")
                    ) {
                        Icon(
                            imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Toggle Mic",
                            tint = Color(0xFF030705),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}
