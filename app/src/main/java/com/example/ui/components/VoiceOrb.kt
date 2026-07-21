package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.voice.AssistantState
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun VoiceOrb(
    state: AssistantState,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "orb_transition")

    // Rotation angle
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AssistantState.THINKING) 1500 else 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse scale
    val pulseDuration = when (state) {
        AssistantState.LISTENING -> 800
        AssistantState.SPEAKING -> 1200
        AssistantState.THINKING -> 2000
        AssistantState.IDLE -> 3000
    }
    val pulseMin = if (state == AssistantState.LISTENING) 0.95f else 0.9f
    val pulseMax = if (state == AssistantState.LISTENING) 1.15f else 1.05f
    
    val pulseScale by transition.animateFloat(
        initialValue = pulseMin,
        targetValue = pulseMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Wave/sine phase
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Colors according to state (Pastel Wellness Aesthetic)
    val centerColor = when (state) {
        AssistantState.IDLE -> Color(0xFFDCD6F7) // Soft Lavender
        AssistantState.LISTENING -> Color(0xFFA6E3E9) // Pastel Turquoise
        AssistantState.THINKING -> Color(0xFFFFB6B9) // Soft Peach/Pink
        AssistantState.SPEAKING -> Color(0xFFFFD3B6) // Warm Pastel Peach
    }

    val outerColor1 = when (state) {
        AssistantState.IDLE -> Color(0xFFA6B1E1) // Soft Purple Blue
        AssistantState.LISTENING -> Color(0xFF71C9CE) // Deep Mint
        AssistantState.THINKING -> Color(0xFFFFAAA6) // Pastel Rose
        AssistantState.SPEAKING -> Color(0xFFFF8B94) // Pastel Coral
    }

    val outerColor2 = when (state) {
        AssistantState.IDLE -> Color(0xFFE8F1F5) // Soft Light Blue
        AssistantState.LISTENING -> Color(0xFFE3FDFD) // Pale Teal
        AssistantState.THINKING -> Color(0xFFFFE3E3) // Creamy Pink
        AssistantState.SPEAKING -> Color(0xFFFFF5EB) // Light Warm Glow
    }

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.width / 3.5f
            val currentRadius = baseRadius * pulseScale

            // Draw outer glowing concentric rings
            val ringCount = when (state) {
                AssistantState.LISTENING -> 3
                AssistantState.SPEAKING -> 2
                else -> 1
            }

            for (i in 1..ringCount) {
                val offsetScale = 1.0f + (i * 0.25f)
                val alpha = 0.4f / i
                val radiusWithOffset = currentRadius * offsetScale
                
                // Listening state has wavy rings
                if (state == AssistantState.LISTENING) {
                    val strokeWidth = 2.dp.toPx()
                    val points = 72
                    val path = androidx.compose.ui.graphics.Path()
                    
                    for (angleIndex in 0..points) {
                        val angle = (angleIndex.toFloat() / points) * 2 * PI.toFloat()
                        val waveOffset = kotlin.math.sin(angle * 5f + phase) * 12.dp.toPx()
                        val r = radiusWithOffset + waveOffset
                        val x = center.x + r * kotlin.math.cos(angle)
                        val y = center.y + r * kotlin.math.sin(angle)
                        
                        if (angleIndex == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    path.close()
                    drawPath(
                        path = path,
                        color = outerColor1.copy(alpha = alpha),
                        style = Stroke(width = strokeWidth)
                    )
                } else if (state == AssistantState.SPEAKING) {
                    // Speaking rings pulse like sound waves
                    val waveOffset = sin(phase) * 8.dp.toPx() * i
                    drawCircle(
                        color = outerColor1.copy(alpha = alpha),
                        radius = radiusWithOffset + waveOffset,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                } else {
                    // Idle standard glowing rings
                    drawCircle(
                        color = outerColor1.copy(alpha = alpha),
                        radius = radiusWithOffset,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // Draw primary main orb with rotating radial gradient
            val gradientBrush = Brush.radialGradient(
                colors = listOf(centerColor, outerColor1, outerColor2),
                center = center,
                radius = currentRadius
            )

            drawCircle(
                brush = gradientBrush,
                radius = currentRadius,
                center = center
            )

            // Dynamic core glow line
            drawCircle(
                color = Color.White.copy(alpha = if (state == AssistantState.LISTENING) 0.6f else 0.4f),
                radius = currentRadius * 0.9f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
