package fr.augustine.androgustinecopilote.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fr.augustine.androgustinecopilote.R

val OxaniumFontFamily = FontFamily(
    Font(R.font.oxanium_light, FontWeight.Light),
    Font(R.font.oxanium_regular, FontWeight.Normal),
    Font(R.font.oxanium_medium, FontWeight.Medium),
    Font(R.font.oxanium_semibold, FontWeight.SemiBold),
    Font(R.font.oxanium_bold, FontWeight.Bold),
    Font(R.font.oxanium_extrabold, FontWeight.ExtraBold),
)

private val DefaultTypography = Typography()

private fun TextStyle.withOxanium(): TextStyle {
    return copy(fontFamily = OxaniumFontFamily)
}

val Typography = Typography(
    displayLarge = DefaultTypography.displayLarge.withOxanium(),
    displayMedium = DefaultTypography.displayMedium.withOxanium(),
    displaySmall = DefaultTypography.displaySmall.withOxanium(),
    headlineLarge = DefaultTypography.headlineLarge.withOxanium(),
    headlineMedium = DefaultTypography.headlineMedium.withOxanium(),
    headlineSmall = DefaultTypography.headlineSmall.withOxanium(),
    titleLarge = DefaultTypography.titleLarge.withOxanium(),
    titleMedium = DefaultTypography.titleMedium.withOxanium(),
    titleSmall = DefaultTypography.titleSmall.withOxanium(),
    bodyLarge = TextStyle(
        fontFamily = OxaniumFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = DefaultTypography.bodyMedium.withOxanium(),
    bodySmall = DefaultTypography.bodySmall.withOxanium(),
    labelLarge = DefaultTypography.labelLarge.withOxanium(),
    labelMedium = DefaultTypography.labelMedium.withOxanium(),
    labelSmall = DefaultTypography.labelSmall.withOxanium(),
)
