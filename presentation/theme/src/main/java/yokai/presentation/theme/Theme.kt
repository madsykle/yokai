package yokai.presentation.theme

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    // Tier 3 Scrim Fallback (DESIGN.md §3.3) — near-opaque, mirrors Reduce Transparency
    val ScrimLight = Color(0xF2F2F7)
    val ScrimDark = Color(0x1C1C1E)
    val ScrimFallbackAlpha = 0.90f

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
 * SharedPreferences key for the iOS 27 transparency slider (DESIGN.md §2.2).
 * Stored as a percent Int (30–95) in the app's default SharedPreferences file.
 * Default 70 ≈ DESIGN.md §2.2 "base tint alpha ≈ 0.72".
 */
const val GLASS_TRANSPARENCY_PREF_KEY = "glass_transparency_alpha"

/** Transparency slider bounds (DESIGN.md §2.2: ultra clear → fully tinted). */
const val GLASS_TRANSPARENCY_MIN = 30
const val GLASS_TRANSPARENCY_MAX = 95
const val GLASS_TRANSPARENCY_DEFAULT = 70

/**
 * The app's *default* SharedPreferences file - the one the preference UI writes to
 * (`PreferenceManager.getDefaultSharedPreferences` uses the same name).
 */
fun Context.glassPreferences(): SharedPreferences =
    getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)

/**
 * Persisted transparency percent, clamped to the slider's own bounds so a hand-edited
 * value can never produce an invisible or fully opaque material.
 */
fun Context.glassTransparencyPercent(): Int =
    glassPreferences()
        .getInt(GLASS_TRANSPARENCY_PREF_KEY, GLASS_TRANSPARENCY_DEFAULT)
        .coerceIn(GLASS_TRANSPARENCY_MIN, GLASS_TRANSPARENCY_MAX)

/**
 * Writes the transparency percent straight to the preference file.
 *
 * Written directly rather than through the preference screen's own persistence because the
 * slider has to take effect *while it is dragged* (DESIGN.md §1.5) - the change is published
 * through the SharedPreferences listeners that [glassTintAlphaState] and the View chrome
 * already observe, so no Activity recreation is needed.
 */
fun Context.setGlassTransparencyPercent(percent: Int) {
    glassPreferences()
        .edit()
        .putInt(
            GLASS_TRANSPARENCY_PREF_KEY,
            percent.coerceIn(GLASS_TRANSPARENCY_MIN, GLASS_TRANSPARENCY_MAX),
        )
        .apply()
}

/**
 * Non-recomposing read of the §2.2 transparency, for the View-based chrome
 * (`View.applyGlass`). Compose surfaces should use [glassTintAlphaState] instead so a drag in
 * Settings repaints them immediately.
 */
fun glassTintAlpha(context: Context): Float = context.glassTransparencyPercent() / 100f

/**
 * Transparency as Compose state: recomposes every glass surface when the slider moves.
 *
 * The SharedPreferences listener is registered for the lifetime of the composition and the
 * listener reference is held by this scope (the implementation stores listeners weakly).
 */
@Composable
fun glassTintAlphaState(): Float {
    val context = LocalContext.current
    val preferences = remember(context) { context.glassPreferences() }
    var percent by remember(preferences) {
        mutableIntStateOf(context.glassTransparencyPercent())
    }

    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == GLASS_TRANSPARENCY_PREF_KEY) {
                percent = context.glassTransparencyPercent()
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    return percent / 100f
}

/**
 * Glass tier info — consumed by glass components to pick Backdrop / Haze / scrim.
 */
sealed interface GlassTier {
    data object Full : GlassTier   // API 33+ — AGSL refraction via Backdrop
    data object Blur : GlassTier   // API 31–32 — Haze / RenderEffect
    data object Scrim : GlassTier  // API 29–30 — tinted scrim fallback
}

/**
 * Tier selection as a pure function of the API level, so the boundaries are unit-testable
 * without an emulator. The thresholds are fixed by the platform: AGSL (refraction) needs 33,
 * `RenderEffect` (blur) needs 31, and below that neither exists (DESIGN.md §3).
 */
fun glassTierFor(sdkInt: Int): GlassTier {
    return when {
        sdkInt >= 33 -> GlassTier.Full
        sdkInt >= 31 -> GlassTier.Blur
        else -> GlassTier.Scrim
    }
}

fun glassTier(): GlassTier = glassTierFor(Build.VERSION.SDK_INT)

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
const val GLASS_TRANSPARENCY_PREF = GLASS_TRANSPARENCY_PREF_KEY

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
        )

    MaterialTheme(
        colorScheme = colourScheme!!,
        typography = YokaiTypography.typography,
        shapes = glassShapes,
        content = content,
    )
}
