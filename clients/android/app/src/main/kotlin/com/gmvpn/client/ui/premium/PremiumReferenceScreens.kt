package com.gmvpn.client.ui.premium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmvpn.client.ui.components.GmIconKind
import com.gmvpn.client.ui.theme.GmvpnTheme

private object RefColors {
    val Background = Color(0xFF050B12)
    val BackgroundTop = Color(0xFF0B1725)
    val Card = Color(0xD30E1925)
    val CardTop = Color(0xE4142332)
    val CardSoft = Color(0x9C132235)
    val CardActive = Color(0xB80B231D)
    val Border = Color(0xFF26384A)
    val BorderSoft = Color(0x52364B62)
    val Primary = Color(0xFF69A1FF)
    val PrimarySoft = Color(0x33356FEA)
    val Success = Color(0xFF50D290)
    val Warning = Color(0xFFF1C84B)
    val Destructive = Color(0xFFFF6B5F)
    val TextPrimary = Color(0xFFF4F7FB)
    val TextSecondary = Color(0xFFB3C0D1)
    val TextMuted = Color(0xFF7F8EA3)
}

private object RefDimens {
    val ScreenPadding = 18.dp
    val CardRadius = 24.dp
    val ButtonRadius = 18.dp
    val RowGap = 10.dp
    val NavHeight = 78.dp
}

private data class RefProfile(
    val name: String,
    val protocol: String,
    val latency: String,
    val country: RefCountry,
    val active: Boolean = false,
    val selected: Boolean = false,
    val available: Boolean = true,
)

private enum class RefCountry {
    Netherlands,
    Germany,
    Poland,
    France,
}

private val referenceProfiles = listOf(
    RefProfile("Нидерланды", "VLESS", "18 мс", RefCountry.Netherlands, active = true),
    RefProfile("Германия", "VLESS", "24 мс", RefCountry.Germany),
    RefProfile("Франция", "VLESS", "46 мс", RefCountry.France, selected = true),
    RefProfile("Польша", "VLESS", "—", RefCountry.Poland, available = false),
)

@Preview(
    name = "Premium reference - Home",
    widthDp = 393,
    heightDp = 852,
    backgroundColor = 0xFF050B12,
    showBackground = true,
)
@Composable
fun PremiumHomeReferencePreview() {
    ReferencePreviewFrame {
        ReferenceScreen(selected = "Главная") {
            HomeReferenceContent(connected = false)
        }
    }
}

@Preview(
    name = "Premium reference - Home connected",
    widthDp = 393,
    heightDp = 852,
    backgroundColor = 0xFF050B12,
    showBackground = true,
)
@Composable
fun PremiumHomeConnectedReferencePreview() {
    ReferencePreviewFrame {
        ReferenceScreen(selected = "Главная") {
            HomeReferenceContent(connected = true)
        }
    }
}

@Preview(
    name = "Premium reference - Profiles",
    widthDp = 393,
    heightDp = 852,
    backgroundColor = 0xFF050B12,
    showBackground = true,
)
@Composable
fun PremiumProfilesReferencePreview() {
    ReferencePreviewFrame {
        ReferenceScreen(selected = "Профили") {
            ReferenceTitleBar(title = "Профили", action = "Тестировать все")
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Сохранено: 4",
                color = RefColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(12.dp))
            referenceProfiles.take(4).forEach { profile ->
                ProfileReferenceRow(profile = profile)
                Spacer(Modifier.height(RefDimens.RowGap))
            }
            Spacer(Modifier.height(30.dp))
            ReferenceDestructiveButton(text = "Очистить", modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(
    name = "Premium reference - Import",
    widthDp = 393,
    heightDp = 852,
    backgroundColor = 0xFF050B12,
    showBackground = true,
)
@Composable
fun PremiumImportReferencePreview() {
    ReferencePreviewFrame {
        ReferenceScreen(selected = "Импорт") {
            ReferenceTitleBar(title = "Импорт подписки")
            Spacer(Modifier.height(16.dp))
            ReferenceGlassCard {
                Text(
                    text = "Ссылка подписки",
                    color = RefColors.TextPrimary,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Ссылка скрыта. Будут показаны безопасные имена и протоколы.",
                    color = RefColors.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                )
                Spacer(Modifier.height(12.dp))
                ReferenceInputRow(label = "Ссылка скрыта •••••", icon = GmIconKind.Lock)
                Spacer(Modifier.height(8.dp))
                ReferenceSelectRow(text = "Формат: Список профилей")
                Spacer(Modifier.height(12.dp))
                ReferencePrimaryButton(
                    text = "Загрузить и импортировать",
                    icon = GmIconKind.Download,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
            ReferenceGlassCard {
                Text(
                    text = "Профиль вручную",
                    color = RefColors.TextPrimary,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Значение скрывается. Сохраняется только безопасное имя.",
                    color = RefColors.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                )
                Spacer(Modifier.height(12.dp))
                ReferenceInputRow(label = "Ссылка на профиль", icon = GmIconKind.EyeOff)
                Spacer(Modifier.height(12.dp))
                ReferencePrimaryButton(
                    text = "Сохранить профиль",
                    icon = GmIconKind.Import,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(
    name = "Premium reference - Privacy",
    widthDp = 393,
    heightDp = 852,
    backgroundColor = 0xFF050B12,
    showBackground = true,
)
@Composable
fun PremiumPrivacyReferencePreview() {
    ReferencePreviewFrame {
        ReferenceScreen(selected = "Настройки") {
            ReferenceTitleBar(title = "Настройки приватности")
            Spacer(Modifier.height(16.dp))
            PrivacyReferenceCard(
                icon = GmIconKind.Routing,
                title = "Маршрутизация приложений",
                body = "Выберите приложения, которые будут использовать VPN-туннель.",
                action = "Открыть настройки",
            )
            Spacer(Modifier.height(12.dp))
            PrivacyReferenceCard(
                icon = GmIconKind.Privacy,
                title = "Privacy-first интерфейс",
                body = "Профили хранятся локально. Приватные данные не показываются в обычном UI.",
            )
            Spacer(Modifier.height(12.dp))
            PrivacyReferenceCard(
                icon = GmIconKind.KillSwitch,
                title = "Системный kill switch",
                body = "Включите Always-on VPN и блокировку без VPN в настройках Android.",
                action = "Открыть Always-on VPN",
            )
        }
    }
}

@Composable
private fun ReferencePreviewFrame(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale = 1f),
    ) {
        GmvpnTheme {
            content()
        }
    }
}

@Composable
private fun ReferenceScreen(
    selected: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        RefColors.BackgroundTop,
                        RefColors.Background,
                        Color(0xFF06101A),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RefDimens.ScreenPadding)
                .padding(top = 18.dp, bottom = RefDimens.NavHeight + 30.dp),
        ) {
            content()
        }
        ReferenceBottomNav(
            selected = selected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun HomeReferenceContent(connected: Boolean) {
    CompactHomeBar()
    Spacer(Modifier.height(8.dp))
    ReferenceConnectionHeroCard(connected = connected)
    Spacer(Modifier.height(12.dp))
    ActiveProfileCard(referenceProfiles.first())
    Spacer(Modifier.height(12.dp))
    ToolsCard()
    Spacer(Modifier.height(12.dp))
    SavedProfilesPreview()
}

@Composable
private fun CompactHomeBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusDot(color = RefColors.Primary)
        Text(
            text = "GMvpn",
            color = RefColors.TextPrimary,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ReferenceTitleBar(title: String, action: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReferenceLineIcons.Icon(
            kind = GmIconKind.ChevronLeft,
            contentDescription = "Назад",
            color = RefColors.TextSecondary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = title,
            color = RefColors.TextPrimary,
            fontSize = 19.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            ReferenceStatusBadge(text = action, color = RefColors.Primary)
        }
    }
}

@Composable
private fun ReferenceConnectionHeroCard(connected: Boolean) {
    val statusText = if (connected) "Подключено и защищено" else "Не подключено"
    val subtitleText = if (connected) "Ваш трафик в безопасности" else "Ваш трафик не защищён"
    val buttonText = if (connected) "Отключить" else "Подключить"
    val footerText = if (connected) "Защита активна" else "Готово к подключению"
    val statusColor = if (connected) RefColors.Success else RefColors.TextMuted

    ReferenceGlassCard {
        Box(modifier = Modifier.fillMaxWidth()) {
            WorldGrid(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(78.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ShieldMark(connected = connected)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = statusText,
                    color = RefColors.TextPrimary,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitleText,
                    color = RefColors.TextSecondary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                ReferencePrimaryButton(
                    text = buttonText,
                    icon = GmIconKind.Connect,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusDot(color = statusColor)
                    Text(
                        text = footerText,
                        color = RefColors.TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveProfileCard(profile: RefProfile) {
    ReferenceGlassCard(borderColor = RefColors.Success.copy(alpha = 0.34f), background = RefColors.CardActive) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        text = "Активный профиль",
                        color = RefColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                    )
                    ReferenceActiveBadge(compact = true)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FlagBadge(profile.country)
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = profile.name,
                            color = RefColors.TextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = profile.protocol,
                            color = RefColors.TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    }
                    ReferenceLatencyPill(text = profile.latency, compact = true)
                }
            }
            ReferenceLineIcons.Icon(
                kind = GmIconKind.ChevronRight,
                contentDescription = "Открыть профиль",
                color = RefColors.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ToolsCard() {
    ReferenceGlassCard {
        Text(
            text = "Инструменты",
            color = RefColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolReferenceTile(
                icon = GmIconKind.Routing,
                title = "Маршрутизация",
                subtitle = "Выбрать приложения",
                modifier = Modifier.weight(1f),
            )
            ToolReferenceTile(
                icon = GmIconKind.Diagnostics,
                title = "Диагностика",
                subtitle = "Проверить подключение",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SavedProfilesPreview() {
    ReferenceGlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Сохранённые профили",
                color = RefColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "4 профиля",
                color = RefColors.TextMuted,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        referenceProfiles.take(2).forEach { profile ->
            CompactProfileReferenceRow(profile)
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
private fun ProfileReferenceRow(profile: RefProfile) {
    val border = when {
        profile.active -> RefColors.Success.copy(alpha = 0.50f)
        profile.selected -> RefColors.Primary.copy(alpha = 0.74f)
        !profile.available -> RefColors.BorderSoft.copy(alpha = 0.55f)
        else -> RefColors.BorderSoft
    }
    val background = when {
        profile.active -> RefColors.CardActive
        profile.selected -> RefColors.PrimarySoft.copy(alpha = 0.20f)
        !profile.available -> RefColors.Card.copy(alpha = 0.56f)
        else -> RefColors.Card
    }
    val textColor = if (profile.available) RefColors.TextPrimary else RefColors.TextMuted
    val secondaryColor = if (profile.available) RefColors.TextSecondary else RefColors.TextMuted.copy(alpha = 0.72f)
    Surface(
        color = background,
        contentColor = RefColors.TextPrimary,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatusDot(
                color = when {
                    profile.active -> RefColors.Success
                    profile.selected -> RefColors.Primary
                    profile.available -> RefColors.TextMuted
                    else -> RefColors.TextMuted.copy(alpha = 0.45f)
                },
            )
            FlagBadge(profile.country, compact = true)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    color = textColor,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (profile.available) {
                        "${profile.protocol} · ${profile.latency}"
                    } else {
                        "${profile.protocol} · недоступен"
                    },
                    color = secondaryColor,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                profile.active -> ReferenceActiveBadge()
                profile.available -> ReferenceOutlineButton(text = "Выбрать")
                else -> ReferenceStatusBadge(text = "Недоступно", color = RefColors.TextMuted, compact = true)
            }
            ReferenceLineIcons.Icon(
                kind = GmIconKind.MoreVertical,
                contentDescription = "Меню профиля",
                color = RefColors.TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CompactProfileReferenceRow(profile: RefProfile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RefColors.CardSoft)
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlagBadge(profile.country, compact = true)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.name,
                color = RefColors.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${profile.protocol} · ${profile.latency}",
                color = RefColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 14.sp,
            )
        }
        if (profile.active) {
            StatusDot(color = RefColors.Success)
            Text(text = "Активный", color = RefColors.Success, fontSize = 12.sp, lineHeight = 15.sp)
        }
        ReferenceLineIcons.Icon(
            kind = GmIconKind.ChevronRight,
            contentDescription = "Открыть",
            color = RefColors.TextSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun PrivacyReferenceCard(
    icon: GmIconKind,
    title: String,
    body: String,
    action: String? = null,
) {
    ReferenceGlassCard {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            IconPill(icon = icon, tone = RefColors.Primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = RefColors.TextPrimary,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body,
                    color = RefColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                if (action != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = action,
                        color = RefColors.Primary,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            ReferenceLineIcons.Icon(
                kind = GmIconKind.ChevronRight,
                contentDescription = "Открыть",
                color = RefColors.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ReferenceGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = RefColors.BorderSoft,
    background: Color = RefColors.Card,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(RefDimens.CardRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        RefColors.CardTop.copy(alpha = 0.90f),
                        background,
                        RefColors.Background.copy(alpha = 0.34f),
                    ),
                ),
            )
            .border(1.dp, borderColor, shape),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

@Composable
private fun ReferencePrimaryButton(
    text: String,
    icon: GmIconKind,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(RefDimens.ButtonRadius)
    val top = if (enabled) Color(0xFF73A9FF) else RefColors.Primary.copy(alpha = 0.24f)
    val bottom = if (enabled) Color(0xFF467AE5) else RefColors.Primary.copy(alpha = 0.14f)
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.14f else 0.05f), shape)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            ReferenceLineIcons.Icon(
                kind = if (loading) GmIconKind.Diagnostics else icon,
                contentDescription = text,
                color = Color.White.copy(alpha = if (enabled) 0.92f else 0.50f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                color = Color.White.copy(alpha = if (enabled) 0.96f else 0.56f),
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ReferenceOutlineButton(
    text: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(17.dp)
    Box(
        modifier = modifier
            .width(92.dp)
            .height(33.dp)
            .clip(shape)
            .background(RefColors.Primary.copy(alpha = 0.12f))
            .border(1.dp, RefColors.Primary.copy(alpha = 0.58f), shape)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = RefColors.Primary,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ReferenceDestructiveButton(
    text: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(RefDimens.ButtonRadius)
    Box(
        modifier = modifier
            .height(49.dp)
            .clip(shape)
            .background(RefColors.Destructive.copy(alpha = 0.055f))
            .border(1.dp, RefColors.Destructive.copy(alpha = 0.48f), shape)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            ReferenceLineIcons.Icon(
                kind = GmIconKind.Delete,
                contentDescription = text,
                color = RefColors.Destructive,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = RefColors.Destructive,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ToolReferenceTile(
    icon: GmIconKind,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = RefColors.CardSoft,
        contentColor = RefColors.TextPrimary,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, RefColors.BorderSoft),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconPill(icon = icon, tone = RefColors.Primary)
            Text(text = title, color = RefColors.TextPrimary, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = RefColors.TextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun ReferenceInputRow(label: String, icon: GmIconKind) {
    Surface(
        color = Color(0x66101B28),
        contentColor = RefColors.TextSecondary,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, RefColors.BorderSoft),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReferenceLineIcons.Icon(
                kind = icon,
                contentDescription = label,
                color = RefColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                color = RefColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ReferenceSelectRow(text: String) {
    Surface(
        color = Color(0x66101B28),
        contentColor = RefColors.TextSecondary,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, RefColors.BorderSoft),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = text, color = RefColors.TextSecondary, fontSize = 14.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
            ReferenceLineIcons.Icon(
                kind = GmIconKind.ChevronRight,
                contentDescription = "Выбрать формат",
                color = RefColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ReferenceBottomNav(
    selected: String,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        "Главная" to GmIconKind.Home,
        "Профили" to GmIconKind.Profiles,
        "Импорт" to GmIconKind.Import,
        "Настройки" to GmIconKind.Settings,
    )
    Surface(
        modifier = modifier.height(RefDimens.NavHeight),
        color = Color(0xD90A141F),
        contentColor = RefColors.TextSecondary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, RefColors.BorderSoft),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { (label, icon) ->
                val isSelected = label == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    ReferenceLineIcons.Icon(
                        kind = icon,
                        contentDescription = label,
                        color = if (isSelected) RefColors.Primary else RefColors.TextMuted,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = label,
                        color = if (isSelected) RefColors.Primary else RefColors.TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceStatusBadge(text: String, color: Color, compact: Boolean = false) {
    Surface(
        color = color.copy(alpha = 0.13f),
        contentColor = color,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.34f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = if (compact) 4.dp else 6.dp,
            ),
            fontSize = if (compact) 11.sp else 12.sp,
            lineHeight = if (compact) 13.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ReferenceActiveBadge(compact: Boolean = false) {
    ReferenceStatusBadge(text = "Активный", color = RefColors.Success, compact = compact)
}

@Composable
private fun ReferenceLatencyPill(text: String, compact: Boolean = false) {
    ReferenceStatusBadge(text = text, color = latencyColor(text), compact = compact)
}

private fun latencyColor(text: String): Color {
    val ms = Regex("\\d+").find(text)?.value?.toIntOrNull()
    return when {
        ms == null -> RefColors.TextMuted
        ms <= 30 -> RefColors.Success
        ms <= 55 -> RefColors.Warning
        else -> RefColors.Destructive
    }
}

private object ReferenceLineIcons {
    @Composable
    fun Icon(
        kind: GmIconKind,
        contentDescription: String,
        color: Color,
        modifier: Modifier = Modifier,
    ) {
        Canvas(
            modifier = modifier
                .size(24.dp)
                .semantics { this.contentDescription = contentDescription },
        ) {
            val stroke = Stroke(
                width = 1.85.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            val w = size.width
            val h = size.height
            when (kind) {
                GmIconKind.Home -> {
                    drawLine(color, Offset(w * 0.17f, h * 0.52f), Offset(w * 0.50f, h * 0.22f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.50f, h * 0.22f), Offset(w * 0.83f, h * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawRoundRect(color, Offset(w * 0.29f, h * 0.47f), Size(w * 0.42f, h * 0.36f), CornerRadius(4.dp.toPx()), style = stroke)
                }
                GmIconKind.Profiles -> {
                    drawRoundRect(color, Offset(w * 0.22f, h * 0.23f), Size(w * 0.56f, h * 0.16f), CornerRadius(5.dp.toPx()), style = stroke)
                    drawRoundRect(color, Offset(w * 0.22f, h * 0.61f), Size(w * 0.56f, h * 0.16f), CornerRadius(5.dp.toPx()), style = stroke)
                    drawCircle(color, radius = w * 0.045f, center = Offset(w * 0.33f, h * 0.31f))
                    drawCircle(color, radius = w * 0.045f, center = Offset(w * 0.33f, h * 0.69f))
                }
                GmIconKind.Import, GmIconKind.Download -> {
                    drawLine(color, Offset(w * 0.50f, h * 0.18f), Offset(w * 0.50f, h * 0.56f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.36f, h * 0.42f), Offset(w * 0.50f, h * 0.56f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.64f, h * 0.42f), Offset(w * 0.50f, h * 0.56f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawRoundRect(color, Offset(w * 0.24f, h * 0.68f), Size(w * 0.52f, h * 0.13f), CornerRadius(4.dp.toPx()), style = stroke)
                }
                GmIconKind.Upload -> {
                    drawLine(color, Offset(w * 0.50f, h * 0.58f), Offset(w * 0.50f, h * 0.20f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.36f, h * 0.34f), Offset(w * 0.50f, h * 0.20f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.64f, h * 0.34f), Offset(w * 0.50f, h * 0.20f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawRoundRect(color, Offset(w * 0.24f, h * 0.68f), Size(w * 0.52f, h * 0.13f), CornerRadius(4.dp.toPx()), style = stroke)
                }
                GmIconKind.Settings -> {
                    listOf(0.30f, 0.50f, 0.70f).forEachIndexed { index, x ->
                        val knobY = listOf(0.36f, 0.56f, 0.42f)[index]
                        drawLine(color, Offset(w * x, h * 0.18f), Offset(w * x, h * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                        drawCircle(color, radius = w * 0.07f, center = Offset(w * x, h * knobY), style = stroke)
                    }
                }
                GmIconKind.Connect -> {
                    drawArc(color, startAngle = -220f, sweepAngle = 260f, useCenter = false, topLeft = Offset(w * 0.20f, h * 0.20f), size = Size(w * 0.60f, h * 0.60f), style = stroke)
                    drawLine(color, Offset(w * 0.50f, h * 0.12f), Offset(w * 0.50f, h * 0.42f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
                GmIconKind.Shield, GmIconKind.Privacy -> {
                    val path = Path().apply {
                        moveTo(w * 0.50f, h * 0.12f)
                        lineTo(w * 0.76f, h * 0.25f)
                        lineTo(w * 0.70f, h * 0.62f)
                        quadraticTo(w * 0.50f, h * 0.86f, w * 0.30f, h * 0.62f)
                        lineTo(w * 0.24f, h * 0.25f)
                        close()
                    }
                    drawPath(path, color, style = stroke)
                    if (kind == GmIconKind.Privacy) {
                        drawLine(color, Offset(w * 0.38f, h * 0.52f), Offset(w * 0.48f, h * 0.62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                        drawLine(color, Offset(w * 0.48f, h * 0.62f), Offset(w * 0.64f, h * 0.42f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    }
                }
                GmIconKind.Routing -> {
                    val nodes = listOf(Offset(w * 0.28f, h * 0.68f), Offset(w * 0.50f, h * 0.30f), Offset(w * 0.74f, h * 0.68f))
                    drawLine(color, nodes[0], nodes[1], strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, nodes[1], nodes[2], strokeWidth = stroke.width, cap = StrokeCap.Round)
                    nodes.forEach { drawCircle(color, radius = w * 0.065f, center = it, style = stroke) }
                }
                GmIconKind.Diagnostics -> {
                    drawCircle(color, radius = w * 0.32f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                    drawLine(color, Offset(w * 0.32f, h * 0.52f), Offset(w * 0.44f, h * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.44f, h * 0.52f), Offset(w * 0.50f, h * 0.38f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.50f, h * 0.38f), Offset(w * 0.60f, h * 0.64f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.60f, h * 0.64f), Offset(w * 0.70f, h * 0.50f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
                GmIconKind.Delete -> {
                    drawLine(color, Offset(w * 0.30f, h * 0.32f), Offset(w * 0.70f, h * 0.32f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawRoundRect(color, Offset(w * 0.34f, h * 0.38f), Size(w * 0.32f, h * 0.40f), CornerRadius(4.dp.toPx()), style = stroke)
                    drawLine(color, Offset(w * 0.42f, h * 0.48f), Offset(w * 0.42f, h * 0.68f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.58f, h * 0.48f), Offset(w * 0.58f, h * 0.68f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.42f, h * 0.24f), Offset(w * 0.58f, h * 0.24f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
                GmIconKind.Warning -> {
                    val path = Path().apply {
                        moveTo(w * 0.50f, h * 0.16f)
                        lineTo(w * 0.82f, h * 0.78f)
                        lineTo(w * 0.18f, h * 0.78f)
                        close()
                    }
                    drawPath(path, color, style = stroke)
                    drawLine(color, Offset(w * 0.50f, h * 0.38f), Offset(w * 0.50f, h * 0.58f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawCircle(color, radius = w * 0.025f, center = Offset(w * 0.50f, h * 0.68f))
                }
                GmIconKind.ChevronRight -> {
                    drawLine(color, Offset(w * 0.40f, h * 0.24f), Offset(w * 0.64f, h * 0.50f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.64f, h * 0.50f), Offset(w * 0.40f, h * 0.76f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
                GmIconKind.ChevronLeft -> {
                    drawLine(color, Offset(w * 0.60f, h * 0.24f), Offset(w * 0.36f, h * 0.50f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.36f, h * 0.50f), Offset(w * 0.60f, h * 0.76f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
                GmIconKind.Lock, GmIconKind.KillSwitch -> {
                    drawRoundRect(color, Offset(w * 0.28f, h * 0.42f), Size(w * 0.44f, h * 0.34f), CornerRadius(5.dp.toPx()), style = stroke)
                    drawArc(color, startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = Offset(w * 0.34f, h * 0.18f), size = Size(w * 0.32f, h * 0.38f), style = stroke)
                    drawCircle(color, radius = w * 0.035f, center = Offset(w * 0.50f, h * 0.58f))
                }
                GmIconKind.EyeOff -> {
                    drawArc(color, startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = Offset(w * 0.18f, h * 0.28f), size = Size(w * 0.64f, h * 0.46f), style = stroke)
                    drawArc(color, startAngle = 20f, sweepAngle = 140f, useCenter = false, topLeft = Offset(w * 0.18f, h * 0.28f), size = Size(w * 0.64f, h * 0.46f), style = stroke)
                    drawLine(color, Offset(w * 0.22f, h * 0.78f), Offset(w * 0.78f, h * 0.22f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
                GmIconKind.MoreVertical -> {
                    listOf(0.30f, 0.50f, 0.70f).forEach { y ->
                        drawCircle(color, radius = w * 0.035f, center = Offset(w * 0.50f, h * y))
                    }
                }
                else -> {
                    drawCircle(color, radius = w * 0.30f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                }
            }
        }
    }
}

@Composable
private fun IconPill(icon: GmIconKind, tone: Color) {
    Surface(
        color = tone.copy(alpha = 0.11f),
        contentColor = tone,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, tone.copy(alpha = 0.20f)),
    ) {
        Box(modifier = Modifier.padding(11.dp), contentAlignment = Alignment.Center) {
            ReferenceLineIcons.Icon(
                kind = icon,
                contentDescription = icon.name,
                color = tone,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ShieldMark(connected: Boolean) {
    val tone = if (connected) RefColors.Success else RefColors.TextSecondary
    Canvas(modifier = Modifier.size(58.dp)) {
        val stroke = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.10f)
            lineTo(w * 0.78f, h * 0.24f)
            lineTo(w * 0.72f, h * 0.62f)
            quadraticTo(w * 0.50f, h * 0.90f, w * 0.28f, h * 0.62f)
            lineTo(w * 0.22f, h * 0.24f)
            close()
        }
        drawPath(path, tone.copy(alpha = 0.16f))
        drawPath(path, tone, style = stroke)
        if (connected) {
            drawLine(tone, Offset(w * 0.36f, h * 0.52f), Offset(w * 0.48f, h * 0.64f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(tone, Offset(w * 0.48f, h * 0.64f), Offset(w * 0.68f, h * 0.40f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        } else {
            drawCircle(tone.copy(alpha = 0.88f), radius = w * 0.16f, center = Offset(w * 0.70f, h * 0.62f))
            drawLine(Color(0xFF07111A), Offset(w * 0.70f, h * 0.54f), Offset(w * 0.70f, h * 0.64f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(Color(0xFF07111A), radius = 1.4.dp.toPx(), center = Offset(w * 0.70f, h * 0.70f))
        }
    }
}

@Composable
private fun FlagBadge(country: RefCountry, compact: Boolean = false) {
    val colors = when (country) {
        RefCountry.Netherlands -> listOf(Color(0xFFCA243C), Color.White, Color(0xFF214F9A))
        RefCountry.Germany -> listOf(Color(0xFF0B0B0D), Color(0xFFD52B1E), Color(0xFFFFD84D))
        RefCountry.Poland -> listOf(Color.White, Color(0xFFD4213D))
        RefCountry.France -> listOf(Color(0xFF244AA5), Color.White, Color(0xFFE23A4C))
    }
    val shape = RoundedCornerShape(if (compact) 6.dp else 7.dp)
    Canvas(
        modifier = Modifier
            .size(
                width = if (compact) 36.dp else 40.dp,
                height = if (compact) 24.dp else 28.dp,
            )
            .clip(shape)
            .border(0.7.dp, Color.White.copy(alpha = 0.18f), shape),
    ) {
        val rectWidth = size.width
        if (country == RefCountry.France) {
            val stripeWidth = size.width / colors.size
            colors.forEachIndexed { index, color ->
                drawRect(
                    color = color,
                    topLeft = Offset(stripeWidth * index, 0f),
                    size = Size(stripeWidth + 1f, size.height),
                )
            }
        } else {
            val stripeHeight = size.height / colors.size
            colors.forEachIndexed { index, color ->
                drawRect(
                    color = color,
                    topLeft = Offset(0f, stripeHeight * index),
                    size = Size(rectWidth, stripeHeight + 1f),
                )
            }
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Canvas(modifier = Modifier.size(8.dp)) {
        drawCircle(color = color, radius = size.minDimension / 2f)
    }
}

@Composable
private fun WorldGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val dotColor = RefColors.Primary.copy(alpha = 0.055f)
        val step = 14.dp.toPx()
        var y = step
        while (y < size.height) {
            var x = step
            while (x < size.width - step) {
                if ((x + y).toInt() % 5 != 0) {
                    drawCircle(dotColor, radius = 0.8.dp.toPx(), center = Offset(x, y))
                }
                x += step
            }
            y += step
        }
    }
}
