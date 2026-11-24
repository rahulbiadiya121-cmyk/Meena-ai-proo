package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.domain.MeenaCharacter
import com.example.domain.OrbState
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.NeonCyan
import kotlin.math.roundToInt

@Composable
fun MeenaCharacterAvatar(
    character: MeenaCharacter,
    orbState: OrbState = OrbState.IDLE,
    audioLevel: Float = 0f,
    reduceMotion: Boolean = false,
    size: Dp = 210.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val glow = LocalMeenaGlow.current
    val infinite = rememberInfiniteTransition(label = "character_anim")
    val floatY by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (reduceMotion) 6000 else 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "char_float"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (reduceMotion) 4000 else 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "char_pulse"
    )
    val dynamicScale by animateFloatAsState(
        targetValue = when (orbState) {
            OrbState.SPEAKING -> if (reduceMotion) 1f else pulse
            OrbState.LISTENING -> 1f + audioLevel.coerceIn(0f, 1f) * 0.05f
            else -> 1f
        },
        animationSpec = spring(),
        label = "char_scale"
    )

    val ringColor = when (orbState) {
        OrbState.LISTENING -> NeonCyan
        OrbState.SPEAKING -> glow.primaryGlow
        OrbState.PROCESSING -> glow.secondaryGlow
        OrbState.IDLE -> glow.primaryGlow.copy(alpha = 0.65f)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .offset {
                IntOffset(0, if (reduceMotion) 0 else floatY.roundToInt())
            }
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
    ) {
        // Glowing Aura Halo behind character
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ringColor.copy(alpha = 0.42f),
                        glow.secondaryGlow.copy(alpha = 0.16f),
                        Color.Transparent
                    )
                ),
                radius = radius
            )
            if (orbState == OrbState.LISTENING || orbState == OrbState.SPEAKING) {
                drawCircle(
                    color = ringColor.copy(alpha = 0.5f),
                    radius = radius * 0.88f * pulse,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .scale(dynamicScale)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(ringColor, glow.secondaryGlow, ringColor.copy(alpha = 0.4f))
                    ),
                    shape = CircleShape
                )
        ) {
            Image(
                painter = painterResource(id = character.imageRes),
                contentDescription = "${character.displayName} avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
