package com.vergipro.mobile.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.vergipro.mobile.R

object VPColor {
    val Brand: Color @Composable get() = MaterialTheme.colorScheme.primary
    val BrandInverse: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
    val Success: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF059669)
    val Warning: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFD97706)
    val Danger: Color @Composable get() = MaterialTheme.colorScheme.error
    val AccentBlue: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val PorcelainCanvas = Color(0xFFF8FAFC)
    val PorcelainSurface = Color(0xFFFFFFFF)
    val CardBorder: Color @Composable get() = MaterialTheme.colorScheme.outline
    val TextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onBackground
    val TextSecondary: Color @Composable get() = MaterialTheme.colorScheme.secondary
    val ObsidianCanvas = Color(0xFF000000)
    val ObsidianSurface = Color(0xFF121214)

    val Canvas: Color @Composable get() = MaterialTheme.colorScheme.background
    val Surface: Color @Composable get() = MaterialTheme.colorScheme.surface
    val SecondaryText: Color @Composable get() = MaterialTheme.colorScheme.secondary
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF18181B),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF71717A),
    onSecondary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E4E7),
    onPrimaryContainer = Color(0xFF18181B),
    secondaryContainer = Color(0xFFEFF1F5),
    onSecondaryContainer = Color(0xFF27272A),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF18181B),
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFFA1A1AA),
    onSecondary = Color(0xFF18181B),
    primaryContainer = Color(0xFF27272A),
    onPrimaryContainer = Color(0xFFFAFAFA),
    secondaryContainer = Color(0xFF202024),
    onSecondaryContainer = Color(0xFFE4E4E7),
    background = Color(0xFF000000),
    surface = Color(0xFF121214),
    onBackground = Color(0xFFFAFAFA),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF1C1C1F),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF27272A),
    error = Color(0xFFEF4444),
)

@Composable
fun VergiProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = VergiProTypography,
        content = content,
    )
}

// MARK: - Marka Bileşenleri

/**
 * VP monogram ikonu — kare blok içinde, dark/light'a göre renk terslenmiş.
 * Light: siyah VP (şeffaf zemin). Dark: beyaz VP (şeffaf zemin).
 */
@Composable
fun VPMark(
    modifier: Modifier = Modifier,
    size: Dp = 58.dp,
    darkTheme: Boolean? = null,
    contained: Boolean = true,
) {
    val resolvedDarkTheme = darkTheme ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f)
    val res = if (resolvedDarkTheme) R.drawable.vp_mark_white else R.drawable.vp_mark_black
    if (contained) {
        Surface(
            modifier = modifier.size(size),
            shape = RoundedCornerShape(size * 0.26f),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Image(
                painter = painterResource(res),
                contentDescription = null,
                modifier = Modifier.padding(size * 0.10f),
            )
        }
    } else {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            modifier = modifier.size(size),
        )
    }
}

/**
 * "VergiPro" kelime markası — ikon solda, metin sağda.
 * Nav bar, splash ve karşılama ekranlarında kullanılır.
 */
@Composable
fun VPWordmark(
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp,
    fontSize: TextUnit = 20.sp,
    darkTheme: Boolean? = null,
) {
    Row(
        modifier = modifier.semantics { contentDescription = "VergiPro" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VPMark(size = iconSize, darkTheme = darkTheme, contained = false)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "VergiPro",
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// MARK: - TRY Currency Formatting

private val tryDecimalSymbols = java.text.DecimalFormatSymbols(java.util.Locale("tr", "TR")).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

private val tryFormatter = java.text.DecimalFormat("#,##0.00", tryDecimalSymbols)
private val tryCompactFormatter = java.text.DecimalFormat("#,##0", tryDecimalSymbols)

fun Double.formattedTRY(): String = "₺${tryFormatter.format(this)}"
fun Double.formattedTRYCompact(): String = "₺${tryCompactFormatter.format(this)}"
fun Int.formattedTRY(): String = "₺${tryFormatter.format(this)}"
fun Int.formattedTRYCompact(): String = "₺${tryCompactFormatter.format(this)}"
fun Float.formattedTRYCompact(): String = toDouble().formattedTRYCompact()
