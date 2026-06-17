package com.gmvpn.client.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun GmCard(
    modifier: Modifier = Modifier,
    tone: GmCardTone = GmCardTone.Neutral,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val borderColor = when (tone) {
        GmCardTone.Neutral -> colors.outline.copy(alpha = 0.72f)
        GmCardTone.Selected -> GmColors.Connected.copy(alpha = 0.82f)
        GmCardTone.Warning -> GmColors.Warning.copy(alpha = 0.78f)
        GmCardTone.Error -> GmColors.Error.copy(alpha = 0.82f)
    }
    val container = when (tone) {
        GmCardTone.Selected -> GmColors.SurfaceSelectedDark
        else -> colors.surface
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
fun StatusPill(
    text: String,
    tone: GmStatusTone,
    modifier: Modifier = Modifier,
) {
    val color = tone.color()
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(GmRadius.pill),
        border = BorderStroke(1.dp, color.copy(alpha = 0.38f)),
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
fun ConnectionStatusOrb(
    tone: GmStatusTone,
    label: String,
    modifier: Modifier = Modifier,
) {
    val color = tone.color()
    Canvas(
        modifier = modifier
            .size(132.dp)
            .semantics { contentDescription = label },
    ) {
        val radius = size.minDimension / 2f
        drawCircle(color = color.copy(alpha = 0.12f), radius = radius)
        drawCircle(color = color.copy(alpha = 0.18f), radius = radius * 0.78f)
        drawCircle(
            color = color.copy(alpha = 0.42f),
            radius = radius * 0.58f,
            style = Stroke(width = 4.dp.toPx()),
        )
        drawCircle(color = color, radius = radius * 0.24f)
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
        ButtonDefaults.outlinedButtonColors(contentColor = GmColors.Warning)
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
            border = BorderStroke(1.dp, GmColors.Warning.copy(alpha = 0.72f)),
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
    GmCard(modifier = modifier, tone = GmCardTone.Selected) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GmSpacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            StatusDot(tone = GmStatusTone.Privacy, label = title)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(GmSpacing.xxs))
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
            .size(12.dp)
            .semantics { contentDescription = label },
    ) {
        drawCircle(color = color.copy(alpha = 0.22f), radius = size.minDimension / 2f)
        drawCircle(color = color, radius = size.minDimension / 3f)
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
