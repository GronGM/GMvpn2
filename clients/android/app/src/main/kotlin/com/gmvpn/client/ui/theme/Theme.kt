package com.gmvpn.client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object GmColors {
    val Connected = Color(0xFF3DDC97)
    val Disconnected = Color(0xFF8A95A8)
    val Preparing = Color(0xFFFFC857)
    val Warning = Color(0xFFFFB86B)
    val Error = Color(0xFFFF6B6B)
    val PrivacySafe = Color(0xFF7DD3FC)
    val Neutral = Color(0xFFA8B3C7)

    val SurfaceBaseDark = Color(0xFF080D12)
    val SurfaceCardDark = Color(0xFF101821)
    val SurfaceRaisedDark = Color(0xFF162331)
    val SurfaceSelectedDark = Color(0xFF132D31)
    val BorderDark = Color(0xFF253546)
    val GlowDark = Color(0x333DDC97)

    val SurfaceBaseLight = Color(0xFFF6F8FB)
    val SurfaceCardLight = Color(0xFFFFFFFF)
    val SurfaceRaisedLight = Color(0xFFEAF0F7)
    val BorderLight = Color(0xFFD6DEE8)
}

object GmSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

object GmRadius {
    val pill = 999.dp
    val card = 20.dp
    val control = 14.dp
    val dialog = 24.dp
}

object GmElevation {
    val card = 2.dp
    val selected = 6.dp
}

object GmMotion {
    const val FastMillis = 120
    const val NormalMillis = 220
    const val SlowMillis = 360
}

private val PremiumDark = darkColorScheme(
    primary = GmColors.PrivacySafe,
    onPrimary = Color(0xFF061018),
    secondary = GmColors.Connected,
    onSecondary = Color(0xFF05120C),
    tertiary = GmColors.Preparing,
    onTertiary = Color(0xFF151006),
    background = GmColors.SurfaceBaseDark,
    onBackground = Color(0xFFE8EEF6),
    surface = GmColors.SurfaceCardDark,
    onSurface = Color(0xFFE8EEF6),
    surfaceVariant = GmColors.SurfaceRaisedDark,
    onSurfaceVariant = Color(0xFFB7C2D3),
    outline = GmColors.BorderDark,
    error = GmColors.Error,
    onError = Color(0xFF240606),
)

private val PremiumLight = lightColorScheme(
    primary = Color(0xFF1F6FEB),
    onPrimary = Color.White,
    secondary = Color(0xFF087F5B),
    onSecondary = Color.White,
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color.White,
    background = GmColors.SurfaceBaseLight,
    onBackground = Color(0xFF111827),
    surface = GmColors.SurfaceCardLight,
    onSurface = Color(0xFF111827),
    surfaceVariant = GmColors.SurfaceRaisedLight,
    onSurfaceVariant = Color(0xFF475569),
    outline = GmColors.BorderLight,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val PremiumShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(GmRadius.control),
    large = androidx.compose.foundation.shape.RoundedCornerShape(GmRadius.card),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(GmRadius.dialog),
)

private val PremiumTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun GmvpnTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) PremiumDark else PremiumLight,
        typography = PremiumTypography,
        shapes = PremiumShapes,
        content = content,
    )
}
