package yokai.presentation.theme

import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.accompanist.themeadapter.material3.createMdc3Theme

/**
 * iOS 27 Liquid Glass color scheme (DESIGN.md §4.1)
 */
object GlassColors {
    // Glass Base Colors
    val GlassLightBase = Color(0xFFF2F2F7)
    val GlassDarkBase = Color(0xFF1C1C1E)

    // Glass Tint Colors
    val GlassTintLight = Color(0x1FFFFFFF)   // #FFFFFF at 12%
    val GlassTintDark = Color(0x59000000)   // #000000 at 35%

    // Accent Colors (light/dark variants)
    val AccentBlue = Color(0xFF007AFF)
    val AccentBlueDark = Color(0xFF0A84FF)
    val AccentGreen = Color(0xFF34C759)
    val AccentGreenDark = Color(0xFF30D158)
    val AccentRed = Color(0xFFFF3B30)
    val AccentRedDark = Color(0xFFFF453A)

    // Label Colors
    val LabelPrimaryLight = Color(0xFF000000)
    val LabelPrimaryDark = Color(0xFFFFFFFF)
    val LabelSecondaryLight = Color(0x613C3C43)  // #3C3C43 at 60%
    val LabelSecondaryDark = Color(0x99EBEBF5)  // #EBEBF5 at 60%

    // Glass Border / Specular (DESIGN.md §2.2)
    val DarkenedEdge = Color(0x33000000)         // 1px border
    val SpecularHighlight = Color(0x1FFFFFFF)    // white at 12% on top edge

    // Tier 3 Scrim Fallback (DESIGN.md §3.3)
    val ScrimLight = Color(0xE6F2F2F7)
    val ScrimDark = Color(0xE61C1C1E)

    // Default opacity for glass tint (DESIGN.md §2.2: base tint alpha ≈ 0.72)
    val GlassBaseTintAlpha = 0.72f
}

/**
 * Spring animation spec matching iOS 27 (DESIGN.md §4.5)
 * spring(dampingRatio = 0.75f, stiffness = 300f)
 */
val iOSspring = spring<Float>(
    dampingRatio = 0.75f,
    stiffness = 300f,
)

/**
 * Glass tier info — consumed by glass components to pick Backdrop / Haze / scrim.
 */
sealed interface GlassTier {
    data object Full : GlassTier   // API 33+ — AGSL refraction via Backdrop
    data object Blur : GlassTier   // API 31–32 — Haze / RenderEffect
    data object Scrim : GlassTier  // API 29–30 — tinted scrim fallback
}

fun glassTier(): GlassTier {
    return when {
        Build.VERSION.SDK_INT >= 33 -> GlassTier.Full
        Build.VERSION.SDK_INT >= 31 -> GlassTier.Blur
        else -> GlassTier.Scrim
    }
}

@Composable
fun glassTierComposable(): GlassTier {
    return glassTier()
}

/**
 * Design tokens for corner radii — squircles with continuous curves (DESIGN.md §4.3).
 * RoundedCornerShape in Compose already uses continuous curves.
 */
val glassShapes = Shapes(
    extraLarge = RoundedCornerShape(28.dp),  // Bottom nav pill
    large = RoundedCornerShape(24.dp),       // Sheets
    medium = RoundedCornerShape(14.dp),      // Buttons
    small = RoundedCornerShape(12.dp),       // Cards
    extraSmall = RoundedCornerShape(8.dp),
)

/**
 * Glass transparency preference key
 */
const val GLASS_TRANSPARENCY_PREF = "pref_glass_transparency"

@Composable
fun YokaiTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current

    val (colourScheme) =
        @Suppress("DEPRECATION")
        createMdc3Theme(
            context = context,
            layoutDirection = LayoutDirection.Rtl,
            setTextColors = true,
            readTypography = false,
            typography = YokaiTypography.typography,
        )

    MaterialTheme(
        colorScheme = colourScheme!!,
        typography = YokaiTypography.typography,
        shapes = glassShapes,
        content = content,
    )
}
