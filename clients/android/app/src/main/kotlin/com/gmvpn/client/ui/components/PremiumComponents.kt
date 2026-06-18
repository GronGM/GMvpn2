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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    Connect,
    Shield,
    EyeOff,
    Routing,
    Diagnostics,
    TestAll,
    MakeActive,
    ActiveStatus,
    InactiveStatus,
    KillSwitch,
    Download,
    Upload,
    Delete,
    Edit,
    Copy,
    Latency,
    ServerLocation,
    ChevronLeft,
    ChevronRight,
    Warning,
    Star,
    MoreVertical,
    Privacy,
    Lock,
}

fun Modifier.gmAppBackground(): Modifier = drawBehind {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                GmColors.SurfaceBaseTopDark,
                GmColors.SurfaceBaseDark,
                Color(0xFF06101A),
            ),
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
        GmCardTone.Neutral -> GmColors.BorderSoftDark
        GmCardTone.Selected -> GmColors.Connected.copy(alpha = 0.40f)
        GmCardTone.Warning -> GmColors.Warning.copy(alpha = 0.40f)
        GmCardTone.Error -> GmColors.Error.copy(alpha = 0.46f)
    }
    val container = when (tone) {
        GmCardTone.Selected -> GmColors.SurfaceSelectedDark
        else -> colors.surface
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(GmRadius.card),
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = colors.onSurface,
        ),
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
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(GmSpacing.xs),
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
        val stroke = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
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
            GmIconKind.Settings -> {
                drawCircle(color, radius = w * 0.16f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                listOf(0.18f, 0.50f, 0.82f).forEach { x ->
                    drawLine(color, Offset(w * x, h * 0.18f), Offset(w * x, h * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * x, h * 0.72f), Offset(w * x, h * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
            }
            GmIconKind.Connect -> {
                drawArc(color, startAngle = -215f, sweepAngle = 250f, useCenter = false, topLeft = Offset(w * 0.20f, h * 0.20f), size = Size(w * 0.60f, h * 0.60f), style = stroke)
                drawLine(color, Offset(w * 0.50f, h * 0.12f), Offset(w * 0.50f, h * 0.42f), strokeWidth = stroke.width, cap = StrokeCap.Round)
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
                if (kind == GmIconKind.Privacy) {
                    drawLine(color, Offset(w * 0.38f, h * 0.54f), Offset(w * 0.48f, h * 0.64f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.48f, h * 0.64f), Offset(w * 0.66f, h * 0.42f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
            }
            GmIconKind.EyeOff -> {
                drawArc(color, startAngle = 205f, sweepAngle = 130f, useCenter = false, topLeft = Offset(w * 0.14f, h * 0.28f), size = Size(w * 0.72f, h * 0.48f), style = stroke)
                drawArc(color, startAngle = 25f, sweepAngle = 130f, useCenter = false, topLeft = Offset(w * 0.14f, h * 0.28f), size = Size(w * 0.72f, h * 0.48f), style = stroke)
                drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.52f), style = stroke)
                drawLine(color, Offset(w * 0.20f, h * 0.82f), Offset(w * 0.80f, h * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
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
            GmIconKind.TestAll -> {
                drawRoundRect(color, Offset(w * 0.20f, h * 0.20f), Size(w * 0.60f, h * 0.60f), CornerRadius(5.dp.toPx()), style = stroke)
                drawLine(color, Offset(w * 0.34f, h * 0.52f), Offset(w * 0.46f, h * 0.64f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.46f, h * 0.64f), Offset(w * 0.68f, h * 0.36f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GmIconKind.MakeActive, GmIconKind.Star -> {
                val path = Path().apply {
                    moveTo(w * 0.50f, h * 0.16f)
                    lineTo(w * 0.60f, h * 0.40f)
                    lineTo(w * 0.86f, h * 0.42f)
                    lineTo(w * 0.66f, h * 0.58f)
                    lineTo(w * 0.72f, h * 0.84f)
                    lineTo(w * 0.50f, h * 0.70f)
                    lineTo(w * 0.28f, h * 0.84f)
                    lineTo(w * 0.34f, h * 0.58f)
                    lineTo(w * 0.14f, h * 0.42f)
                    lineTo(w * 0.40f, h * 0.40f)
                    close()
                }
                drawPath(path, color, style = stroke)
            }
            GmIconKind.ActiveStatus -> {
                drawCircle(color, radius = w * 0.28f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                drawLine(color, Offset(w * 0.38f, h * 0.50f), Offset(w * 0.48f, h * 0.60f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.48f, h * 0.60f), Offset(w * 0.66f, h * 0.38f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GmIconKind.InactiveStatus -> {
                drawCircle(color, radius = w * 0.28f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
            }
            GmIconKind.KillSwitch, GmIconKind.Lock -> {
                drawRoundRect(color, Offset(w * 0.26f, h * 0.44f), Size(w * 0.48f, h * 0.34f), CornerRadius(4.dp.toPx()), style = stroke)
                drawLine(color, Offset(w * 0.34f, h * 0.44f), Offset(w * 0.34f, h * 0.34f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.66f, h * 0.44f), Offset(w * 0.66f, h * 0.34f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.34f, h * 0.34f), Offset(w * 0.66f, h * 0.34f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                if (kind == GmIconKind.KillSwitch) {
                    drawLine(color, Offset(w * 0.50f, h * 0.56f), Offset(w * 0.50f, h * 0.66f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
            }
            GmIconKind.Download, GmIconKind.Upload, GmIconKind.Import -> {
                drawRoundRect(color, Offset(w * 0.22f, h * 0.62f), Size(w * 0.56f, h * 0.18f), CornerRadius(4.dp.toPx()), style = stroke)
                val top = if (kind == GmIconKind.Upload) 0.70f else 0.18f
                val bottom = if (kind == GmIconKind.Upload) 0.28f else 0.58f
                drawLine(color, Offset(w * 0.50f, h * top), Offset(w * 0.50f, h * bottom), strokeWidth = stroke.width, cap = StrokeCap.Round)
                val arrowY = if (kind == GmIconKind.Upload) 0.28f else 0.58f
                val wingY = if (kind == GmIconKind.Upload) 0.42f else 0.44f
                drawLine(color, Offset(w * 0.36f, h * wingY), Offset(w * 0.50f, h * arrowY), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.64f, h * wingY), Offset(w * 0.50f, h * arrowY), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GmIconKind.Delete -> {
                drawRoundRect(color, Offset(w * 0.28f, h * 0.34f), Size(w * 0.44f, h * 0.46f), CornerRadius(4.dp.toPx()), style = stroke)
                drawLine(color, Offset(w * 0.22f, h * 0.28f), Offset(w * 0.78f, h * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.40f, h * 0.20f), Offset(w * 0.60f, h * 0.20f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.42f, h * 0.46f), Offset(w * 0.42f, h * 0.68f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.58f, h * 0.46f), Offset(w * 0.58f, h * 0.68f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GmIconKind.Edit -> {
                drawRoundRect(color, Offset(w * 0.18f, h * 0.26f), Size(w * 0.50f, h * 0.56f), CornerRadius(5.dp.toPx()), style = stroke)
                drawLine(color, Offset(w * 0.48f, h * 0.62f), Offset(w * 0.82f, h * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.72f, h * 0.18f), Offset(w * 0.82f, h * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GmIconKind.Copy -> {
                drawRoundRect(color, Offset(w * 0.18f, h * 0.30f), Size(w * 0.42f, h * 0.42f), CornerRadius(5.dp.toPx()), style = stroke)
                drawRoundRect(color, Offset(w * 0.38f, h * 0.18f), Size(w * 0.42f, h * 0.42f), CornerRadius(5.dp.toPx()), style = stroke)
            }
            GmIconKind.Latency -> {
                drawArc(color, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.18f, h * 0.28f), size = Size(w * 0.64f, h * 0.64f), style = stroke)
                drawLine(color, Offset(w * 0.50f, h * 0.62f), Offset(w * 0.68f, h * 0.42f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                listOf(0.28f, 0.50f, 0.72f).forEach { x ->
                    drawCircle(color, radius = w * 0.025f, center = Offset(w * x, h * 0.58f))
                }
            }
            GmIconKind.ServerLocation -> {
                drawCircle(color, radius = w * 0.30f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                drawLine(color, Offset(w * 0.20f, h * 0.50f), Offset(w * 0.80f, h * 0.50f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.50f, h * 0.20f), Offset(w * 0.50f, h * 0.80f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawArc(color, startAngle = 90f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.34f, h * 0.20f), size = Size(w * 0.32f, h * 0.60f), style = stroke)
                drawArc(color, startAngle = -90f, sweepAngle = 180f, useCenter = false, topLeft = Offset(w * 0.34f, h * 0.20f), size = Size(w * 0.32f, h * 0.60f), style = stroke)
            }
            GmIconKind.ChevronRight -> {
                drawLine(color, Offset(w * 0.38f, h * 0.22f), Offset(w * 0.62f, h * 0.50f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.62f, h * 0.50f), Offset(w * 0.38f, h * 0.78f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GmIconKind.ChevronLeft -> {
                drawLine(color, Offset(w * 0.62f, h * 0.22f), Offset(w * 0.38f, h * 0.50f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.38f, h * 0.50f), Offset(w * 0.62f, h * 0.78f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            GmIconKind.Warning -> {
                val path = Path().apply {
                    moveTo(w * 0.50f, h * 0.16f)
                    lineTo(w * 0.84f, h * 0.78f)
                    lineTo(w * 0.16f, h * 0.78f)
                    close()
                }
                drawPath(path, color, style = stroke)
                drawLine(color, Offset(w * 0.50f, h * 0.36f), Offset(w * 0.50f, h * 0.56f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawCircle(color, radius = w * 0.025f, center = Offset(w * 0.50f, h * 0.66f))
            }
            GmIconKind.MoreVertical -> {
                listOf(0.30f, 0.50f, 0.70f).forEach { y ->
                    drawCircle(color, radius = w * 0.045f, center = Offset(w * 0.50f, h * y))
                }
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
    GmCard(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title. $subtitle"
            },
    ) {
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
        modifier = if (onClick != null) {
            modifier
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = listOfNotNull(title, body, actionText)
                        .joinToString(". ")
                }
        } else {
            modifier
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GmSpacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            IconBadge(icon = icon, tone = GmStatusTone.Privacy, label = title)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (actionText != null) {
                    Spacer(Modifier.height(GmSpacing.xs))
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelMedium,
                        color = GmColors.PrimaryBlue,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
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
            modifier = Modifier.padding(horizontal = GmSpacing.xs, vertical = GmSpacing.xxs),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ProfileBadge(
    text: String,
    tone: GmStatusTone,
    modifier: Modifier = Modifier,
) {
    StatusPill(text = text, tone = tone, modifier = modifier)
}

@Composable
fun LatencyPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    StatusPill(text = text, tone = latencyTone(text), modifier = modifier)
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
    height: Dp = 54.dp,
) {
    val colors = if (destructive) {
        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    } else {
        ButtonDefaults.buttonColors(
            containerColor = GmColors.PrimaryBlue,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (destructive) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .height(height)
                .semantics { contentDescription = text },
            enabled = enabled,
            colors = colors,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.70f)),
            shape = RoundedCornerShape(GmRadius.control),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier
                .height(height)
                .semantics { contentDescription = text },
            enabled = enabled,
            colors = colors,
            shape = RoundedCornerShape(GmRadius.control),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
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
            .semantics(mergeDescendants = true) {
                contentDescription = listOf(
                    displayName,
                    protocol,
                    latency,
                    if (active) activeLabel else null,
                ).filterNotNull()
                    .filter { it.isNotBlank() }
                    .joinToString(". ")
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GmSpacing.sm),
    ) {
        StatusDot(
            tone = if (active) GmStatusTone.Connected else GmStatusTone.Neutral,
            label = displayName,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(GmSpacing.xxs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GmSpacing.xs),
            ) {
                Text(
                    text = protocol,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (latency.isNotBlank()) {
                    LatencyPill(text = latency)
                }
            }
        }
        if (active) {
            ProfileBadge(text = activeLabel, tone = GmStatusTone.Connected)
        } else {
            Spacer(Modifier.width(GmSpacing.xs))
        }
        trailingContent()
    }
}

@Composable
fun LocationCard(
    flagLabel: String,
    name: String,
    protocol: String,
    latency: String,
    favorite: Boolean,
    modifier: Modifier = Modifier,
) {
    GmCard(modifier = modifier, tone = GmCardTone.Neutral) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            FlagPlaceholder(label = flagLabel)
            GmLineIcon(
                kind = GmIconKind.Star,
                contentDescription = if (favorite) "favorite" else "not favorite",
                tone = if (favorite) GmStatusTone.Preparing else GmStatusTone.Neutral,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GmSpacing.xs),
        ) {
            StatusDot(tone = latencyTone(latency), label = latency)
            Text(
                text = protocol,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LatencyPill(text = latency)
        }
    }
}

@Composable
fun CompactServerCard(
    flagLabel: String,
    name: String,
    protocol: String,
    latency: String,
    favorite: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = listOf(name, protocol, latency)
                    .filter { it.isNotBlank() }
                    .joinToString(". ")
            },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(GmRadius.control),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.36f)),
    ) {
        Row(
            modifier = Modifier.padding(GmSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GmSpacing.sm),
        ) {
            FlagPlaceholder(label = flagLabel)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = protocol,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LatencyPill(text = latency)
            }
            GmLineIcon(
                kind = if (favorite) GmIconKind.Star else GmIconKind.ServerLocation,
                contentDescription = name,
                tone = if (favorite) GmStatusTone.Preparing else GmStatusTone.Neutral,
                modifier = Modifier.size(22.dp),
            )
        }
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
private fun FlagPlaceholder(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
    ) {
        Text(
            text = label.take(3).uppercase(),
            modifier = Modifier.padding(horizontal = GmSpacing.xs, vertical = GmSpacing.xs),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

private fun latencyTone(text: String): GmStatusTone {
    val ms = Regex("\\d+").find(text)?.value?.toIntOrNull()
    return when {
        ms == null -> GmStatusTone.Neutral
        ms <= 30 -> GmStatusTone.Connected
        ms <= 55 -> GmStatusTone.Preparing
        ms <= 90 -> GmStatusTone.Warning
        else -> GmStatusTone.Error
    }
}
