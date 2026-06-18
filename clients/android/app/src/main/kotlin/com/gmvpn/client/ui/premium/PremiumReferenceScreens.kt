package com.gmvpn.client.ui.premium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmvpn.client.ui.components.GmIconKind
import com.gmvpn.client.ui.components.GmLineIcon
import com.gmvpn.client.ui.components.GmStatusTone
import com.gmvpn.client.ui.theme.GmvpnTheme

private object RefColors {
    val Background = Color(0xFF050B12)
    val BackgroundTop = Color(0xFF0B1725)
    val Card = Color(0xCC111B27)
    val CardSoft = Color(0xA8132131)
    val CardActive = Color(0xC90B261F)
    val Border = Color(0xFF26384A)
    val BorderSoft = Color(0x4D3A5067)
    val Primary = Color(0xFF5B8DEF)
    val PrimarySoft = Color(0x292D6FEA)
    val Success = Color(0xFF50D290)
    val Warning = Color(0xFFF1C84B)
    val Destructive = Color(0xFFFF6B5F)
    val TextPrimary = Color(0xFFF4F7FB)
    val TextSecondary = Color(0xFFB3C0D1)
    val TextMuted = Color(0xFF7F8EA3)
}

private object RefDimens {
    val ScreenPadding = 18.dp
    val CardRadius = 20.dp
    val ButtonRadius = 14.dp
    val RowGap = 10.dp
    val NavHeight = 68.dp
}

private data class RefProfile(
    val name: String,
    val protocol: String,
    val latency: String,
    val country: RefCountry,
    val active: Boolean = false,
)

private enum class RefCountry {
    Netherlands,
    Germany,
    Poland,
    France,
}

private val referenceProfiles = listOf(
    RefProfile("Нидерланды", "VLESS", "18 мс", RefCountry.Netherlands, active = true),
    RefProfile("Нидерланды 2", "VLESS", "24 мс", RefCountry.Netherlands),
    RefProfile("Германия", "VLESS", "46 мс", RefCountry.Germany),
    RefProfile("Польша", "VLESS", "24 мс", RefCountry.Poland),
    RefProfile("Франция", "VLESS", "46 мс", RefCountry.France),
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
            DestructiveOutlineButton(text = "Очистить", modifier = Modifier.fillMaxWidth())
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
                    text = "Ссылка скрывается. Preview покажет только безопасные имена и протоколы.",
                    color = RefColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(12.dp))
                ReferenceInputRow(label = "Ссылка скрыта •••••", icon = GmIconKind.Lock)
                Spacer(Modifier.height(8.dp))
                ReferenceSelectRow(text = "Формат: Список URI (Base64)")
                Spacer(Modifier.height(12.dp))
                PrimaryReferenceButton(
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
                    text = "Значение скрывается. В списке будет только безопасное имя профиля.",
                    color = RefColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(12.dp))
                ReferenceInputRow(label = "Ссылка на профиль", icon = GmIconKind.EyeOff)
                Spacer(Modifier.height(12.dp))
                PrimaryReferenceButton(
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
    HomeHeroCard(connected = connected)
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
        GmLineIcon(
            kind = GmIconKind.ChevronLeft,
            contentDescription = "Назад",
            tone = GmStatusTone.Neutral,
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
            ReferencePill(text = action, color = RefColors.Primary)
        }
    }
}

@Composable
private fun HomeHeroCard(connected: Boolean) {
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
                    .height(68.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ShieldMark(connected = connected)
                Spacer(Modifier.height(9.dp))
                Text(
                    text = statusText,
                    color = RefColors.TextPrimary,
                    fontSize = 21.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitleText,
                    color = RefColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryReferenceButton(
                    text = buttonText,
                    icon = GmIconKind.Connect,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusDot(color = statusColor)
                    Text(
                        text = footerText,
                        color = RefColors.TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
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
                    ReferencePill(text = "Активный", color = RefColors.Success, compact = true)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FlagBadge(profile.country)
                    Column {
                        Text(
                            text = profile.name,
                            color = RefColors.TextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${profile.protocol} · ${profile.latency}",
                            color = RefColors.TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
            GmLineIcon(
                kind = GmIconKind.ChevronRight,
                contentDescription = "Открыть профиль",
                tone = GmStatusTone.Neutral,
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
    val border = if (profile.active) RefColors.Success.copy(alpha = 0.42f) else RefColors.BorderSoft
    val background = if (profile.active) RefColors.CardActive else RefColors.Card
    Surface(
        color = background,
        contentColor = RefColors.TextPrimary,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusDot(color = if (profile.active) RefColors.Success else RefColors.TextMuted)
            FlagBadge(profile.country, compact = true)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    color = RefColors.TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${profile.protocol} · ${profile.latency}",
                    color = RefColors.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (profile.active) {
                ReferencePill(text = "Активный", color = RefColors.Success)
            } else {
                ReferencePill(
                    text = "Сделать активным",
                    color = RefColors.Primary,
                    compact = true,
                )
            }
            GmLineIcon(
                kind = GmIconKind.MoreVertical,
                contentDescription = "Меню профиля",
                tone = GmStatusTone.Neutral,
                modifier = Modifier.size(18.dp),
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
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${profile.protocol} · ${profile.latency}",
                color = RefColors.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
        if (profile.active) {
            StatusDot(color = RefColors.Success)
            Text(text = "Активный", color = RefColors.Success, fontSize = 11.sp, lineHeight = 14.sp)
        }
        GmLineIcon(
            kind = GmIconKind.ChevronRight,
            contentDescription = "Открыть",
            tone = GmStatusTone.Neutral,
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            IconPill(icon = icon, tone = RefColors.Primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = RefColors.TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body,
                    color = RefColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
                if (action != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = action,
                        color = RefColors.Primary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            GmLineIcon(
                kind = GmIconKind.ChevronRight,
                contentDescription = "Открыть",
                tone = GmStatusTone.Neutral,
                modifier = Modifier.size(18.dp),
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = background,
        contentColor = RefColors.TextPrimary,
        shape = RoundedCornerShape(RefDimens.CardRadius),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            content = content,
        )
    }
}

@Composable
private fun PrimaryReferenceButton(
    text: String,
    icon: GmIconKind,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = {},
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(RefDimens.ButtonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = RefColors.Primary,
            contentColor = Color.White,
        ),
    ) {
        GmLineIcon(
            kind = icon,
            contentDescription = text,
            tone = GmStatusTone.Neutral,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DestructiveOutlineButton(
    text: String,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = {},
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(RefDimens.ButtonRadius),
        border = BorderStroke(1.dp, RefColors.Destructive.copy(alpha = 0.58f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = RefColors.Destructive),
    ) {
        GmLineIcon(
            kind = GmIconKind.Delete,
            contentDescription = text,
            tone = GmStatusTone.Error,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, RefColors.BorderSoft),
    ) {
        Column(
            modifier = Modifier.padding(9.dp),
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
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, RefColors.BorderSoft),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            GmLineIcon(
                kind = icon,
                contentDescription = label,
                tone = GmStatusTone.Neutral,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                color = RefColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
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
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, RefColors.BorderSoft),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = text, color = RefColors.TextSecondary, fontSize = 13.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f))
            GmLineIcon(
                kind = GmIconKind.ChevronRight,
                contentDescription = "Выбрать формат",
                tone = GmStatusTone.Neutral,
                modifier = Modifier.size(16.dp),
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
        Triple("Главная", GmIconKind.Home, GmStatusTone.Neutral),
        Triple("Профили", GmIconKind.Profiles, GmStatusTone.Neutral),
        Triple("Импорт", GmIconKind.Import, GmStatusTone.Neutral),
        Triple("Настройки", GmIconKind.Settings, GmStatusTone.Neutral),
    )
    Surface(
        modifier = modifier.height(RefDimens.NavHeight),
        color = Color(0xE60A141F),
        contentColor = RefColors.TextSecondary,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        border = BorderStroke(1.dp, RefColors.BorderSoft),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { (label, icon, tone) ->
                val isSelected = label == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    GmLineIcon(
                        kind = icon,
                        contentDescription = label,
                        selected = isSelected,
                        tone = tone,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = label,
                        color = if (isSelected) RefColors.Primary else RefColors.TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferencePill(text: String, color: Color, compact: Boolean = false) {
    Surface(
        color = color.copy(alpha = 0.13f),
        contentColor = color,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.34f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = if (compact) 3.dp else 5.dp,
            ),
            fontSize = if (compact) 9.sp else 10.sp,
            lineHeight = if (compact) 11.sp else 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun IconPill(icon: GmIconKind, tone: Color) {
    Surface(
        color = tone.copy(alpha = 0.11f),
        contentColor = tone,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, tone.copy(alpha = 0.20f)),
    ) {
        Box(modifier = Modifier.padding(7.dp), contentAlignment = Alignment.Center) {
            GmLineIcon(
                kind = icon,
                contentDescription = icon.name,
                tone = GmStatusTone.Neutral,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ShieldMark(connected: Boolean) {
    val tone = if (connected) RefColors.Success else RefColors.TextSecondary
    Canvas(modifier = Modifier.size(48.dp)) {
        val stroke = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
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
    Canvas(
        modifier = Modifier.size(
            width = if (compact) 28.dp else 36.dp,
            height = if (compact) 20.dp else 26.dp,
        ),
    ) {
        val radius = 5.dp.toPx()
        val rectWidth = size.width
        if (country == RefCountry.France) {
            val stripeWidth = size.width / colors.size
            colors.forEachIndexed { index, color ->
                drawRoundRect(
                    color = color,
                    topLeft = Offset(stripeWidth * index, 0f),
                    size = Size(stripeWidth + 1f, size.height),
                    cornerRadius = CornerRadius(radius, radius),
                )
            }
        } else {
            val stripeHeight = size.height / colors.size
            colors.forEachIndexed { index, color ->
                drawRoundRect(
                    color = color,
                    topLeft = Offset(0f, stripeHeight * index),
                    size = Size(rectWidth, stripeHeight + 1f),
                    cornerRadius = CornerRadius(radius, radius),
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
