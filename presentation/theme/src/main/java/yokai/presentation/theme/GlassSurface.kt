package yokai.presentation.theme

import android.content.res.Configuration
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewOutlineProvider
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tier-aware GlassSurface composable.
 *
 * Implements iOS 27 Liquid Glass with three tiers (DESIGN.md §3):
 * - Tier 1 (API 33+): Backdrop refraction over the sampled content ([backdrop])
 * - Tier 2 (API 31–32): Haze blur over the sampled content ([backdrop])
 * - Tier 3 (API 29–30): scrim fallback
 *
 * [backdrop] must come from [rememberGlassBackdropState] and be attached to the content
 * underneath with [glassBackdropSource]; without it this is a plain translucent material and
 * nothing is blurred or refracted.
 *
 * RULE: Never stack glass on glass (DESIGN.md §1.2).
 * RULE: Cap at 2–3 glass surfaces per screen (DESIGN.md §1.3).
 * RULE: Content must shine through — sample backdrop luminance (DESIGN.md §1.4).
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    tintAlpha: Float = glassTintAlphaState(),
    backdrop: GlassBackdropState? = null,
    darkenedEdge: Boolean = true,
    specularHighlight: Boolean = true,
    content: @Composable () -> Unit,
) {
    val tier = glassTier()
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(cornerRadius)
    val tintColor = glassTintColor(tintAlpha, isDark)

    // All tiers use the same simple implementation for compatibility
    val backgroundColor = when (tier) {
        is GlassTier.Scrim -> if (isDark) {
            // §3 near-opaque scrim, but LIFTED: a #1C1C1E fill on a #1C1C1C page is a
            // invisible bar. The scrim stays deliberate by being plainly lighter.
            Color(0xFF2C2C2E).copy(alpha = GlassColors.ScrimFallbackAlpha)
        } else {
            GlassColors.ScrimLight.copy(alpha = GlassColors.ScrimFallbackAlpha)
        }
        else -> tintColor
    }

    Box(
        modifier = modifier
            // Blur/refraction first, then the tint on top of it: that ordering is what makes
            // the material read as glass instead of just a translucent panel.
            .glassBackdrop(state = backdrop, shape = shape)
            .background(backgroundColor, shape),
    ) {
        GlassSurfaceDecorators(
            shape = shape,
            darkenedEdge = darkenedEdge,
            specularHighlight = specularHighlight,
        )
        content()
    }
}

/**
 * Internal: draws the darkened-edge border and specular highlight gradients.
 */
@Composable
private fun GlassSurfaceDecorators(
    shape: RoundedCornerShape,
    darkenedEdge: Boolean,
    specularHighlight: Boolean,
) {
    val isDark = isSystemInDarkTheme()
    if (darkenedEdge) {
        // Mode-aware rim (see View.applyGlassDecorators): dark mode rims light, because a
        // black rim cannot separate a dark surface from a dark page.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = SolidColor(if (isDark) GlassColors.DarkenedEdgeDark else GlassColors.DarkenedEdge),
                    shape = shape,
                ),
        )
    }
    if (specularHighlight) {
        // §2.1 brighter specular: highlight ramp over the full height, stronger in dark mode.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isDark) 0.25f else 0.12f),
                            Color.Transparent,
                        ),
                    ),
                    shape = shape,
                ),
        )
    }
}

/**
 * Glass tint color at the given alpha.
 *
 * Both modes tint WHITE — the §1.4 lift. Dark glass lifts from the dark page (white veil);
 * light glass sits on the light page (white frost). Tinting black in dark mode produced a
 * surface *darker* than the `#1C1C1C` page — a hole, not a material — which is exactly what
 * the device screenshots showed.
 */
@Composable
fun glassTintColor(tintAlpha: Float, isDark: Boolean): Color {
    return if (isDark) {
        Color.White.copy(alpha = tintAlpha * GlassColors.GLASS_DARK_BASE_ALPHA)
    } else {
        Color.White.copy(alpha = tintAlpha)
    }
}

/**
 * Converts a Compose Color to an Android ARGB Int.
 */
fun Color.toArgbCompat(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
    )
}

/**
 * View-based glass implementation for XML-inflated components.
 * Used by MainActivity's bottom nav and top bar.
 * Call: `view.applyGlass(28f, glassTier())`
 *
 * DESIGN.md §3: the material layer is a translucent tint over the backdrop.
 * A View must NEVER blur its own content via [android.view.View.setRenderEffect] —
 * that blurs the icons/labels INSIDE the view, not what is behind it (HIG: the
 * material layer blurs the backdrop, not the foreground content).
 *
 * The tint alpha is read from the §2.2 preference on every call, so re-invoking this after
 * the user moves the transparency slider is what re-paints the chrome. See
 * [glassTintAlphaState] for the Compose side of the same preference.
 */
fun View.applyGlass(cornerRadiusDp: Float, tier: GlassTier? = null, circle: Boolean = false) {
    val actualTier = tier ?: glassTier()
    val radiusPx = cornerRadiusDp * resources.displayMetrics.density
    val isDark = isNightMode()

    background = when (actualTier) {
        is GlassTier.Full, is GlassTier.Blur -> {
            // Translucent material tint; user-adjustable via the transparency pref (§2.2).
            glassTintDrawable(radiusPx, glassTintAlpha(context), isDark)
        }
        is GlassTier.Scrim -> {
            // DESIGN.md §3 Tier 3: deliberate near-opaque scrim (mirrors Reduce
            // Transparency). Real blur is impossible here - AGSL needs API 33, RenderEffect
            // needs 31, and RenderScript is banned outright by §7.1 - so the tier is
            // differentiated by its edge treatment instead (see applyGlassDecorators).
            // The dark scrim is LIFTED (#2C2C2E, not §4.1's #1C1C1E): a #1C1C1E fill on a
            // #1C1C1C page is an invisible bar, which the device screenshots showed.
            GradientDrawable().apply {
                shape = if (circle) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
                cornerRadius = radiusPx
                setColor(if (isDark) 0xFF2C2C2E.toInt() else GlassColors.ScrimLight.toArgbCompat())
                alpha = (GlassColors.ScrimFallbackAlpha * 255).toInt()
            }
        }
    }

    // Apply squircle clip via outline. [circle] is used by the standalone round glass
    // button beside the nav pill, where the shared corner radius cannot express the
    // shape (the view is only measured after inflation).
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            if (circle) {
                outline.setOval(0, 0, view.width, view.height)
            } else {
                outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
            }
        }
    }
    clipToOutline = true
}

private fun View.isNightMode(): Boolean =
    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

/**
 * Rounded translucent material-tint drawable used as the glass background.
 *
 * Both modes tint WHITE (§1.4 lift). Tinting black in dark mode landed *darker* than the
 * `#1C1C1C` page — a hole, not a material — so dark glass instead lifts with a white veil at
 * half the user's transparency value ([GlassColors.GLASS_DARK_BASE_ALPHA]).
 */
private fun glassTintDrawable(radiusPx: Float, alpha: Float, isDark: Boolean, circle: Boolean = false): GradientDrawable {
    return GradientDrawable().apply {
        shape = if (circle) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
        cornerRadius = radiusPx
        setColor(android.graphics.Color.WHITE)
        this.alpha = ((alpha * (if (isDark) GlassColors.GLASS_DARK_BASE_ALPHA else 1f)).coerceIn(0f, 1f) * 255).toInt()
    }
}

/**
 * Applies iOS 27 Liquid Glass decorators (darkened edge + specular highlight) to a View by
 * setting its `foreground` to a layered gradient drawable. Idempotent: re-assignment replaces
 * the previous decorators (§2.2). Mirrors [GlassSurfaceDecorators] for the Compose side.
 *
 * Deliberately tier-independent: the rim and the sheen are the same treatment on every tier,
 * including [GlassTier.Scrim], where §3 asks the fallback for a "1px light top edge" so it
 * still reads as a deliberate material rather than a plain opaque bar.
 *
 * [cornerRadiusDp] must match the radius passed to [applyGlass] - the stroke is drawn as a
 * GradientDrawable, and without the radius the rim would render as a square outline over a
 * rounded surface. [circle] switches it to an oval, for the round sibling of the nav pill.
 */
fun View.applyGlassDecorators(cornerRadiusDp: Float = 24f, circle: Boolean = false) {
    val d = resources.displayMetrics.density
    val radiusPx = cornerRadiusDp * d
    val isDark = isNightMode()

    // §2.1 darkened edge — but mode-aware. On a dark page a BLACK rim cannot separate a
    // black surface (it measured invisible on-device), so dark mode rims LIGHT. The stroke
    // follows the shape of the surface it outlines, not its bounding box.
    val rimColor = if (isDark) GlassColors.DarkenedEdgeDark else GlassColors.DarkenedEdge
    val border = GradientDrawable().apply {
        shape = if (circle) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
        cornerRadius = radiusPx
        setStroke(1.coerceAtLeast((0.5 * d).toInt()), rimColor.toArgbCompat())
    }

    // §2.1 brighter specular: a top-edge highlight ramp over the full height so the surface
    // reads as a lit material instead of a flat wash. The ramp is subtler in light mode, where
    // white-on-white needs less help.
    val highlightTop = if (isDark) 0x40 else GlassColors.SpecularHighlight.alpha
    val sheenShader = LinearGradient(
        0f, 0f, 0f, bounds.height().coerceAtLeast(1).toFloat(),
        intArrayOf(
            (highlightTop shl 24) or 0x00FFFFFF,
            android.graphics.Color.TRANSPARENT,
        ),
        null,
        Shader.TileMode.CLAMP,
    )
    val sheen = object : Drawable() {
        private val paint = Paint().apply { isAntiAlias = true }
        override fun draw(canvas: Canvas) {
            paint.shader = sheenShader
            canvas.drawRect(
                bounds.left.toFloat(), bounds.top.toFloat(),
                bounds.right.toFloat(), bounds.bottom.toFloat(),
                paint,
            )
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    foreground = LayerDrawable(arrayOf<Drawable>(sheen, border))
}
