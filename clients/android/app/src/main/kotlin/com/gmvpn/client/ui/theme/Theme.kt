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
    val PrimaryBlue = Color(0xFF69A1FF)
    val PrimaryBluePressed = Color(0xFF467AE5)
    val PrimaryBlueSoft = Color(0x33356FEA)
    val Connected = Color(0xFF50D290)
    val Disconnected = Color(0xFF8FA0B6)
    val Preparing = Color(0xFFF1C84B)
    val Warning = Color(0xFFE0A45E)
    val Error = Color(0xFFFF6B5F)
    val PrivacySafe = Color(0xFF8DB7E8)
    val Neutral = Color(0xFFA6B0BF)

    val SurfaceBaseDark = Color(0xFF050B12)
    val SurfaceBaseTopDark = Color(0xFF0B1725)
    val SurfaceCardDark = Color(0xD30E1925)
    val SurfaceCardTopDark = Color(0xE4142332)
    val SurfaceRaisedDark = Color(0x9C132235)
    val SurfaceSelectedDark = Color(0xB80B231D)
    val BorderDark = Color(0xFF26384A)
    val BorderSoftDark = Color(0x52364B62)
    val TextMutedDark = Color(0xFF7F8EA3)

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
    val card = 22.dp
    val row = 16.dp
    val control = 18.dp
    val tile = 16.dp
    val dialog = 22.dp
}

object GmElevation {
    val card = 0.dp
    val selected = 1.dp
}

object GmMotion {
    const val FastMillis = 120
    const val NormalMillis = 220
    const val SlowMillis = 360
}

private val PremiumDark = darkColorScheme(
    primary = GmColors.PrimaryBlue,
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
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
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
