package yokai.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * iOS 27 Dynamic Type Scale (DESIGN.md §4.2)
 * Font: Inter (do not ship SF Pro for legal reasons)
 * Scale: LargeTitle 34, Title1 28, Title2 22, Headline 17 semibold, Body 17, Callout 16, Subhead 15, Footnote 13, Caption 12
 * Line height: 1.2× for titles, 1.4× for body
 */
object YokaiTypography {
    private val interFontFamily = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal, FontStyle.Normal),
        Font(R.font.inter_medium, FontWeight.Medium, FontStyle.Normal),
        Font(R.font.inter_semibold, FontWeight.SemiBold, FontStyle.Normal),
        Font(R.font.inter_bold, FontWeight.Bold, FontStyle.Normal),
    )

    val typography = Typography(
        // LargeTitle: 34sp, line height 1.2x = 40.8sp
        displayLarge = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 34.sp,
            lineHeight = 41.sp,
            letterSpacing = 0.37.sp,
        ),
        // Title1: 28sp, line height 1.2x = 33.6sp
        displayMedium = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = 0.36.sp,
        ),
        // Title2: 22sp, line height 1.2x = 26.4sp
        displaySmall = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.35.sp,
        ),
        // Headline: 17sp semibold, line height 1.4x = 23.8sp
        headlineLarge = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            letterSpacing = -0.41.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            letterSpacing = -0.41.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            letterSpacing = -0.41.sp,
        ),
        // Body: 17sp, line height 1.4x = 23.8sp
        titleLarge = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            letterSpacing = -0.41.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = -0.32.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            letterSpacing = -0.24.sp,
        ),
        // Body: 17sp, line height 1.4x = 23.8sp
        bodyLarge = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            letterSpacing = -0.41.sp,
        ),
        // Callout: 16sp, line height 1.4x = 22.4sp
        bodyMedium = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = -0.32.sp,
        ),
        // Subhead: 15sp, line height 1.4x = 21sp
        bodySmall = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            letterSpacing = -0.24.sp,
        ),
        // Footnote: 13sp, line height 1.4x = 18.2sp
        labelLarge = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = -0.08.sp,
        ),
        // Caption: 12sp, line height 1.4x = 16.8sp
        labelMedium = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = interFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.06.sp,
        ),
    )
}