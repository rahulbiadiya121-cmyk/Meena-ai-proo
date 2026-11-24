package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.LocalMeenaGlow
import com.example.ui.theme.MidnightVoid
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CosmicBackground(
    reduceMotion: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glow = LocalMeenaGlow.current
    val infinite = rememberInfiniteTransition(label = "cosmic_bg")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (reduceMotion) 30000 else 12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nebula_drift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CosmicBlack, MidnightVoid, CosmicBlack)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val offsetFactor = if (reduceMotion) 0f else (drift - 0.5f) * 0.08f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glow.primaryGlow.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(w * (0.25f + offsetFactor), h * 0.18f),
                    radius = w * 0.85f
                ),
                radius = w * 0.85f,
                center = Offset(w * (0.25f + offsetFactor), h * 0.18f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glow.secondaryGlow.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(w * (0.82f - offsetFactor), h * 0.62f),
                    radius = w * 0.78f
                ),
                radius = w * 0.78f,
                center = Offset(w * (0.82f - offsetFactor), h * 0.62f)
            )
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val glow = LocalMeenaGlow.current
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            glow.primaryGlow.copy(alpha = 0.45f),
            glow.secondaryGlow.copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.08f)
        )
    )
    val baseModifier = modifier
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    glow.cardGlass.copy(alpha = 0.78f),
                    glow.cardGlass.copy(alpha = 0.52f)
                )
            ),
            shape = shape
        )
        .border(width = 1.dp, brush = borderBrush, shape = shape)
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
        .padding(contentPadding)

    Box(modifier = baseModifier, content = content)
}

@Composable
fun FirebaseNoticeBanner(
    modifier: Modifier = Modifier,
    onOpenConnections: (() -> Unit)? = null
) {
    val glow = LocalMeenaGlow.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = glow.primaryGlow.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, glow.primaryGlow.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { m -> if (onOpenConnections != null) m.clickable(onClick = onOpenConnections) else m }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = glow.secondaryGlow,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Preview AI Mode: Live Gemini requires Firebase AI Logic. Tap to add a Groq or ElevenLabs key in Connections.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MarkdownMessageText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val glow = LocalMeenaGlow.current
    val segments = parseMarkdownSegments(markdown)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        segments.forEach { seg ->
            when (seg) {
                is MarkdownSegment.TextBlock -> {
                    Text(
                        text = buildInlineAnnotatedString(seg.content, glow.secondaryGlow),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }
                is MarkdownSegment.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF090714),
                        border = BorderStroke(1.dp, glow.primaryGlow.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = seg.language.ifBlank { "code" }.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glow.secondaryGlow
                                )
                                IconButton(
                                    onClick = { clipboard.setText(AnnotatedString(seg.code)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy code",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = seg.code,
                                fontFamily = MonoFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        }
    }
}

private sealed interface MarkdownSegment {
    data class TextBlock(val content: String) : MarkdownSegment
    data class CodeBlock(val language: String, val code: String) : MarkdownSegment
}

private fun parseMarkdownSegments(input: String): List<MarkdownSegment> {
    val result = mutableListOf<MarkdownSegment>()
    val regex = Regex("```([a-zA-Z0-9_-]*)\\n([\\s\\S]*?)```")
    var lastIndex = 0
    for (match in regex.findAll(input)) {
        if (match.range.first > lastIndex) {
            val text = input.substring(lastIndex, match.range.first).trim()
            if (text.isNotEmpty()) result.add(MarkdownSegment.TextBlock(text))
        }
        val lang = match.groupValues[1]
        val code = match.groupValues[2].trimEnd()
        result.add(MarkdownSegment.CodeBlock(lang, code))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < input.length) {
        val tail = input.substring(lastIndex).trim()
        if (tail.isNotEmpty()) result.add(MarkdownSegment.TextBlock(tail))
    }
    if (result.isEmpty() && input.isNotBlank()) {
        result.add(MarkdownSegment.TextBlock(input))
    }
    return result
}

private fun buildInlineAnnotatedString(text: String, codeAccent: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = MonoFontFamily,
                                color = codeAccent,
                                background = Color(0x331E293B)
                            )
                        ) {
                            append(" ${text.substring(i + 1, end)} ")
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text[i] == '*' && (i + 1 < text.length && text[i + 1] != ' ') -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = TextMuted)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
