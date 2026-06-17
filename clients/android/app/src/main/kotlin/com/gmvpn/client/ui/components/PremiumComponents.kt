package com.gmvpn.client.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gmvpn.client.ui.theme.GmColors
import com.gmvpn.client.ui.theme.GmElevation
import com.gmvpn.client.ui.theme.GmRadius
import com.gmvpn.client.ui.theme.GmSpacing

enum class GmCardTone {
    Neutral,
    Selected,
    Warning,
    Error,
}

enum class GmStatusTone {
    Connected,
    Disconnected,
    Preparing,
    Warning,
    Error,
    Privacy,
    Neutral,
}

enum class GmIconKind {
    Home,
    Profiles,
    Import,
    Settings,
    Shield,
    Routing,
    Diagnostics,
    Privacy,
    Lock,
}

fun Modifier.gmAppBackground(): Modifier = drawBehind {
    drawRect(GmColors.SurfaceBaseDark)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                GmColors.PrimaryBlue.copy(alpha = 0.24f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.82f, size.height * 0.08f),
            radius = size.maxDimension * 0.72f,
        ),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                GmColors.PrimaryBlue.copy(alpha = 0.16f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.12f, size.height * 0.86f),
            radius = size.maxDimension * 0.58f,
        ),
    )
}

@Composable
fun GmCard(
    modifier: Modifier = Modifier,
    tone: GmCardTone = GmCardTone.Neutral,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val borderColor = when (tone) {
        GmCardTone.Neutral -> colors.outline.copy(alpha = 0.42f)
        GmCardTone.Selected -> GmColors.Connected.copy(alpha = 0.48f)
        GmCardTone.Warning -> GmColors.Warning.copy(alpha = 0.48f)
        GmCardTone.Error -> GmColors.Error.copy(alpha = 0.54f)
    }
    val container = when (tone) {
        GmCardTone.Selected -> GmColors.SurfaceSelectedDark
        else -> colors.surface.copy(alpha = 0.94f)
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(GmRadius.card),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (tone == GmCardTone.Selected) {
                GmElevation.selected
            } else {
                GmElevation.card
            },
        ),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(GmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GmSpacing.sm),
            content = content,
        )
    }
}

@Composable
fun GmLineIcon(
    kind: GmIconKind,
    contentDescription: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    tone: GmStatusTone = GmStatusTone.Neutral,
) {
    val color = if (selected) GmColors.PrimaryBlue else tone.color()
    Canvas(
        modifier = modifier
            .size(28.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        when (kind) {
            GmIconKind.Home -> {
                drawLine(color, Offset(w * 0.18f, h * 0.52f), Offset(w * 0.50f, h * 0.22f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.50f, h * 0.22f), Offset(w * 0.82f, h * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawRoundRect(color, Offset(w * 0.28f, h * 0.48f), Size(w * 0.44f, h * 0.36f), CornerRadius(4.dp.toPx()), style = stroke)
            }
            GmIconKind.Profiles -> {
                drawRoundRect(color, Offset(w * 0.22f, h * 0.20f), Size(w * 0.56f, h * 0.20f), CornerRadius(4.dp.toPx()), style = stroke)
                drawRoundRect(color, Offset(w * 0.22f, h * 0.58f), Size(w * 0.56f, h * 0.20f), CornerRadius(4.dp.toPx()), style = stroke)
                drawLine(color, Offset(w * 0.34f, h * 0.30f), Offset(w * 0.64f, h * 0.30f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.34f, h * 0.68f), Offset(w * 0.64f, h * 0.68f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GmIconKind.Import -> {
                drawRoundRect(color, Offset(w * 0.22f, h * 0.58f), Size(w * 0.56f, h * 0.22f), CornerRadius(4.dp.toPx()), style = stroke)
                drawLine(color, Offset(w * 0.50f, h * 0.18f), Offset(w * 0.50f, h * 0.58f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.36f, h * 0.44f), Offset(w * 0.50f, h * 0.58f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.64f, h * 0.44f), Offset(w * 0.50f, h * 0.58f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GmIconKind.Settings -> {
                drawCircle(color, radius = w * 0.16f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                listOf(0.18f, 0.50f, 0.82f).forEach { x ->
                    drawLine(color, Offset(w * x, h * 0.18f), Offset(w * x, h * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * x, h * 0.72f), Offset(w * x, h * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
            }
            GmIconKind.Shield, GmIconKind.Privacy -> {
                val path = Path().apply {
                    moveTo(w * 0.50f, h * 0.14f)
                    lineTo(w * 0.76f, h * 0.26f)
                    lineTo(w * 0.70f, h * 0.62f)
                    quadraticTo(w * 0.50f, h * 0.86f, w * 0.30f, h * 0.62f)
                    lineTo(w * 0.24f, h * 0.26f)
                    close()
                }
                drawPath(path, color, style = stroke)
            }
            GmIconKind.Routing -> {
                val a = Offset(w * 0.24f, h * 0.50f)
                val b = Offset(w * 0.50f, h * 0.24f)
                val c = Offset(w * 0.76f, h * 0.64f)
                drawLine(color, a, b, strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, b, c, strokeWidth = stroke.width, cap = StrokeCap.Round)
                listOf(a, b, c).forEach { drawCircle(color, radius = w * 0.08f, center = it, style = stroke) }
            }
            GmIconKind.Diagnostics -> {
                drawCircle(color, radius = w * 0.34f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                drawLine(color, Offset(w * 0.28f, h * 0.52f), Offset(w * 0.40f, h * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.40f, h * 0.52f), Offset(w * 0.48f, h * 0.34f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.48f, h * 0.34f), Offset(w * 0.60f, h * 0.66f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.60f, h * 0.66f), Offset(w * 0.72f, h * 0.44f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GmIconKind.Lock -> {
                drawRoundRect(color, Offset(w * 0.26f, h * 0.44f), Size(w * 0.48f, h * 0.34f), CornerRadius(4.dp.toPx()), style = stroke)
                drawLine(color, Offset(w * 0.34f, h * 0.44f), Offset(w * 0.34f, h * 0.34f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.66f, h * 0.44f), Offset(w * 0.66f, h * 0.34f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.34f, h * 0.34f), Offset(w * 0.66f, h * 0.34f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
fun ToolActionCard(
    title: String,
    subtitle: String,
    icon: GmIconKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GmCard(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(GmSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon = icon, tone = GmStatusTone.Privacy, label = title)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun PrivacySettingsCard(
    title: String,
    body: String,
    icon: GmIconKind,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    GmCard(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GmSpacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            IconBadge(icon = icon, tone = GmStatusTone.Privacy, label = title)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (actionText != null) {
                    Spacer(Modifier.height(GmSpacing.xs))
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge,
                        color = GmColors.PrimaryBlue,
                    )
                }
            }
            if (onClick != null) {
                Text(text = "›", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun IconBadge(icon: GmIconKind, tone: GmStatusTone, label: String) {
    val color = tone.color()
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.20f)),
    ) {
        Box(
            modifier = Modifier.padding(GmSpacing.xs),
            contentAlignment = Alignment.Center,
        ) {
            GmLineIcon(kind = icon, contentDescription = label, tone = tone)
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    tone: GmStatusTone,
    modifier: Modifier = Modifier,
) {
    val color = tone.color()
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.10f),
        contentColor = color,
        shape = RoundedCornerShape(GmRadius.pill),
        border = BorderStroke(1.dp, color.copy(alpha = 0.26f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = GmSpacing.sm, vertical = GmSpacing.xs),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun ConnectionStatusMark(
    tone: GmStatusTone,
    text: String,
    modifier: Modifier = Modifier,
) {
    val color = tone.color()
    Row(
        modifier = modifier
            .semantics { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GmSpacing.xs),
    ) {
        StatusDot(tone = tone, label = text)
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun PremiumConnectButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val colors = if (destructive) {
        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    } else {
        ButtonDefaults.buttonColors(
            containerColor = GmColors.PrivacySafe,
            contentColor = Color(0xFF061018),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (destructive) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            enabled = enabled,
            colors = colors,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.70f)),
            shape = RoundedCornerShape(GmRadius.control),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(52.dp),
            enabled = enabled,
            colors = colors,
            shape = RoundedCornerShape(GmRadius.control),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ProfileListItem(
    displayName: String,
    protocol: String,
    active: Boolean,
    activeLabel: String,
    latency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = GmSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GmSpacing.sm),
    ) {
        StatusDot(
            tone = if (active) GmStatusTone.Connected else GmStatusTone.Neutral,
            label = displayName,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = displayName, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(GmSpacing.xxs))
            Text(
                text = listOf(protocol, latency).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (active) {
            StatusPill(text = activeLabel, tone = GmStatusTone.Connected)
        } else {
            Spacer(Modifier.width(GmSpacing.xs))
        }
        trailingContent()
    }
}

@Composable
fun PrivacyNotice(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    GmCard(modifier = modifier, tone = GmCardTone.Neutral) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GmSpacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            StatusDot(tone = GmStatusTone.Privacy, label = title)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusDot(tone: GmStatusTone, label: String) {
    val color = tone.color()
    Canvas(
        modifier = Modifier
            .size(10.dp)
            .semantics { contentDescription = label },
    ) {
        drawCircle(color = color, radius = size.minDimension / 2f)
    }
}

@Composable
private fun GmStatusTone.color(): Color = when (this) {
    GmStatusTone.Connected -> GmColors.Connected
    GmStatusTone.Disconnected -> GmColors.Disconnected
    GmStatusTone.Preparing -> GmColors.Preparing
    GmStatusTone.Warning -> GmColors.Warning
    GmStatusTone.Error -> GmColors.Error
    GmStatusTone.Privacy -> GmColors.PrivacySafe
    GmStatusTone.Neutral -> GmColors.Neutral
}
