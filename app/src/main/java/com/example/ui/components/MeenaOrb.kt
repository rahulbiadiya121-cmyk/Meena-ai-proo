package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.OrbState
import com.example.domain.OrbStyle
import com.example.ui.theme.BrightViolet
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.DeepPurple
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SoftBlue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MeenaOrb(
    orbStyle: OrbStyle,
    orbState: OrbState,
    audioLevel: Float = 0f,
    reduceMotion: Boolean = false,
    size: Dp = 210.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    val infinite = rememberInfiniteTransition(label = "meena_orb")
    val phaseDuration = when (orbState) {
        OrbState.IDLE -> if (reduceMotion) 18000 else 7000
        OrbState.LISTENING -> if (reduceMotion) 12000 else 3600
        OrbState.PROCESSING -> if (reduceMotion) 8000 else 1800
        OrbState.SPEAKING -> if (reduceMotion) 10000 else 2600
    }
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = phaseDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (reduceMotion) 3200 else 1300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )
    val smoothAudio by animateFloatAsState(
        targetValue = when (orbState) {
            OrbState.LISTENING -> audioLevel.coerceIn(0.15f, 1f)
            OrbState.SPEAKING -> 0.55f + (pulse - 1f) * 2.5f
            OrbState.PROCESSING -> 0.45f
            OrbState.IDLE -> 0.1f
        },
        animationSpec = spring(),
        label = "orb_audio"
    )

    val stateDesc = "Meena ${orbStyle.displayName} orb in ${orbState.name.lowercase()} state"

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .semantics { contentDescription = stateDesc }
            .let { m ->
                if (onClick != null) {
                    m.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else m
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val center = Offset(cx, cy)
            val baseRadius = this.size.minDimension * 0.34f *
                (1f + if (reduceMotion) 0f else smoothAudio * 0.14f)

            val primary = glow.primaryGlow
            val secondary = glow.secondaryGlow
            val activeAccent = when (orbState) {
                OrbState.LISTENING -> NeonCyan
                OrbState.PROCESSING -> BrightViolet
                OrbState.SPEAKING -> NeonViolet
                OrbState.IDLE -> primary
            }

            // Ambient outer halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeAccent.copy(alpha = 0.38f + smoothAudio * 0.25f),
                        secondary.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.9f
                ),
                radius = baseRadius * 1.9f,
                center = center
            )

            when (orbStyle) {
                OrbStyle.CLASSIC_VIOLET_NEBULA -> {
                    rotate(rotation, pivot = center) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(DeepPurple, NeonViolet, SoftBlue, BrightViolet, DeepPurple),
                                center = center
                            ),
                            radius = baseRadius,
                            center = center
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.55f), primary.copy(alpha = 0.7f), Color.Transparent),
                                center = Offset(cx - baseRadius * 0.24f, cy - baseRadius * 0.24f),
                                radius = baseRadius * 0.9f
                            ),
                            radius = baseRadius,
                            center = center
                        )
                    }
                    // Orbiting stardust particles
                    for (i in 0 until 8) {
                        val angle = ((rotation + i * 45f) * PI / 180.0).toFloat()
                        val r = baseRadius * (1.18f + (i % 3) * 0.11f)
                        drawCircle(
                            color = if (i % 2 == 0) NeonCyan else BrightViolet,
                            radius = (2.5f + (i % 3)).dp.toPx() * 0.5f,
                            center = Offset(cx + cos(angle) * r, cy + sin(angle) * r)
                        )
                    }
                }

                OrbStyle.HOLOGRAM -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(NeonCyan.copy(alpha = 0.35f), DeepPurple.copy(alpha = 0.2f), Color.Transparent),
                            center = center,
                            radius = baseRadius * 1.1f
                        ),
                        radius = baseRadius,
                        center = center
                    )
                    for (ring in 1..4) {
                        val ringRadius = baseRadius * (ring / 4f)
                        drawCircle(
                            color = NeonCyan.copy(alpha = 0.7f - ring * 0.1f),
                            radius = ringRadius,
                            center = center,
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                    }
                    rotate(rotation, pivot = center) {
                        for (i in 0 until 3) {
                            rotate(i * 60f, pivot = center) {
                                drawOval(
                                    color = BrightViolet.copy(alpha = 0.75f),
                                    topLeft = Offset(cx - baseRadius, cy - baseRadius * 0.38f),
                                    size = Size(baseRadius * 2f, baseRadius * 0.76f),
                                    style = Stroke(width = 1.8.dp.toPx())
                                )
                            }
                        }
                    }
                }

                OrbStyle.BLACK_HOLE -> {
                    rotate(rotation, pivot = center) {
                        drawOval(
                            brush = Brush.sweepGradient(
                                listOf(NeonViolet, SoftBlue, Color.White, BrightViolet, NeonViolet),
                                center = center
                            ),
                            topLeft = Offset(cx - baseRadius * 1.45f, cy - baseRadius * 0.48f),
                            size = Size(baseRadius * 2.9f, baseRadius * 0.96f),
                            style = Stroke(width = (6f + smoothAudio * 4f).dp.toPx())
                        )
                    }
                    drawCircle(
                        color = CosmicBlack,
                        radius = baseRadius * 0.78f,
                        center = center
                    )
                    drawCircle(
                        color = activeAccent.copy(alpha = 0.9f),
                        radius = baseRadius * 0.80f,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                OrbStyle.ECLIPSE -> {
                    val coronaScale = 1.12f + smoothAudio * 0.16f
                    rotate(rotation, pivot = center) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(SoftBlue, NeonViolet, Color.White, SoftBlue),
                                center = center
                            ),
                            radius = baseRadius * coronaScale,
                            center = center,
                            style = Stroke(width = 8.dp.toPx())
                        )
                    }
                    drawCircle(
                        color = Color(0xFF05030A),
                        radius = baseRadius * 0.92f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = baseRadius * 0.93f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                OrbStyle.SATURN -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(BrightViolet, DeepPurple, CosmicBlack),
                            center = Offset(cx - baseRadius * 0.2f, cy - baseRadius * 0.2f),
                            radius = baseRadius * 1.1f
                        ),
                        radius = baseRadius * 0.78f,
                        center = center
                    )
                    rotate(-22f + sin(rotation * PI / 180.0).toFloat() * 4f, pivot = center) {
                        drawOval(
                            brush = Brush.linearGradient(
                                listOf(NeonCyan, NeonViolet, BrightViolet)
                            ),
                            topLeft = Offset(cx - baseRadius * 1.45f, cy - baseRadius * 0.34f),
                            size = Size(baseRadius * 2.9f, baseRadius * 0.68f),
                            style = Stroke(width = 5.dp.toPx())
                        )
                        drawOval(
                            color = Color.White.copy(alpha = 0.45f),
                            topLeft = Offset(cx - baseRadius * 1.2f, cy - baseRadius * 0.24f),
                            size = Size(baseRadius * 2.4f, baseRadius * 0.48f),
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                    }
                }

                OrbStyle.BUBBLE -> {
                    val wobble = if (reduceMotion) 1f else 1f + sin(rotation * 2f * PI / 180.0).toFloat() * 0.05f
                    drawOval(
                        brush = Brush.sweepGradient(
                            listOf(
                                NeonCyan.copy(alpha = 0.55f),
                                NeonViolet.copy(alpha = 0.65f),
                                SoftBlue.copy(alpha = 0.55f),
                                BrightViolet.copy(alpha = 0.65f),
                                NeonCyan.copy(alpha = 0.55f)
                            ),
                            center = center
                        ),
                        topLeft = Offset(cx - baseRadius * wobble, cy - baseRadius / wobble),
                        size = Size(baseRadius * 2f * wobble, baseRadius * 2f / wobble),
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.32f),
                                primary.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(cx - baseRadius * 0.32f, cy - baseRadius * 0.32f),
                            radius = baseRadius
                        ),
                        radius = baseRadius * 0.95f,
                        center = center
                    )
                }

                OrbStyle.TESLA_BALL -> {
                    drawCircle(
                        color = DeepPurple.copy(alpha = 0.35f),
                        radius = baseRadius,
                        center = center
                    )
                    drawCircle(
                        color = SoftBlue.copy(alpha = 0.7f),
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White,
                        radius = baseRadius * 0.18f,
                        center = center
                    )
                    val bolts = 7
                    for (b in 0 until bolts) {
                        val baseAngle = (rotation + b * (360f / bolts)) * PI / 180.0
                        val boltPath = Path().apply {
                            moveTo(cx, cy)
                            for (step in 1..4) {
                                val frac = step / 4f
                                val jitter = sin(rotation * 0.12 + step * 2.4 + b).toFloat() * 0.22f
                                val a = baseAngle + jitter
                                val rad = baseRadius * frac
                                lineTo(cx + cos(a).toFloat() * rad, cy + sin(a).toFloat() * rad)
                            }
                        }
                        drawPath(
                            path = boltPath,
                            color = if (b % 2 == 0) NeonCyan else BrightViolet,
                            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                OrbStyle.PLASMA_ENERGY -> {
                    for (layer in 0 until 3) {
                        rotate(rotation * (if (layer % 2 == 0) 1f else -1.3f) + layer * 40f, pivot = center) {
                            val ribbon = Path().apply {
                                val steps = 36
                                for (s in 0..steps) {
                                    val theta = (s.toFloat() / steps) * 2f * PI.toFloat()
                                    val mod = 0.85f + 0.18f * sin(3f * theta + layer)
                                    val rx = cx + cos(theta) * baseRadius * mod
                                    val ry = cy + sin(theta) * baseRadius * (1.05f - mod * 0.12f)
                                    if (s == 0) moveTo(rx, ry) else lineTo(rx, ry)
                                }
                                close()
                            }
                            drawPath(
                                path = ribbon,
                                brush = Brush.linearGradient(listOf(NeonViolet, NeonCyan, BrightViolet)),
                                style = Stroke(width = (3.5f - layer * 0.6f).dp.toPx())
                            )
                        }
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.8f), activeAccent.copy(alpha = 0.6f), Color.Transparent),
                            center = center,
                            radius = baseRadius * 0.55f
                        ),
                        radius = baseRadius * 0.55f,
                        center = center
                    )
                }
            }
        }
    }
}
