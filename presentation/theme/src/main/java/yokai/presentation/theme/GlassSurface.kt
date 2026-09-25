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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    tintAlpha: Float = glassTintAlpha(LocalContext.current),
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
            GlassColors.ScrimDark.copy(alpha = GlassColors.ScrimFallbackAlpha)
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
    if (darkenedEdge) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(GlassColors.DarkenedEdge, Color.Transparent),
                    ),
                    shape = shape,
                ),
        )
    }
    if (specularHighlight) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(GlassColors.SpecularHighlight, Color.Transparent),
                    ),
                    shape = shape,
                ),
        )
    }
}

/**
 * Glass tint color with given alpha.
 */
@Composable
fun glassTintColor(tintAlpha: Float, isDark: Boolean): Color {
    return if (isDark) {
        Color(0x59000000).copy(alpha = tintAlpha)
    } else {
        Color(0x1AFFFFFF).copy(alpha = tintAlpha)
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
            // Transparency) in the exact §3 colors (#F2F2F7 / #1C1C1E @ 90%).
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radiusPx
                setColor((if (isDark) GlassColors.ScrimDark else GlassColors.ScrimLight).toArgbCompat())
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
 * Light mode tints white (§4.1 GlassTintLight), dark mode tints black (GlassTintDark).
 */
private fun glassTintDrawable(radiusPx: Float, alpha: Float, isDark: Boolean): GradientDrawable {
    val baseColor = if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radiusPx
        setColor(baseColor)
        this.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
    }
}

/**
 * Applies iOS 27 Liquid Glass decorators (darkened edge + specular highlight)
 * to a View by setting its `foreground` to a layered gradient drawable.
 * Idempotent: re-assignment replaces the previous decorators (§2.2).
 * Mirrors [GlassSurfaceDecorators] for the Compose side.
 */
fun View.applyGlassDecorators(tier: GlassTier? = null) {
    val actualTier = tier ?: glassTier()

    // Only apply decorators for blur/glass tiers (not scrim)
    if (actualTier is GlassTier.Scrim) {
        foreground = null
        return
    }

    val d = resources.displayMetrics.density
    val specularShader = LinearGradient(
        0f, 0f, 0f, 24 * d,
        intArrayOf(GlassColors.SpecularHighlight.toArgbCompat(), android.graphics.Color.TRANSPARENT),
        null,
        Shader.TileMode.CLAMP,
    )
    val specular = object : Drawable() {
        private val paint = Paint().apply { isAntiAlias = true }
        override fun draw(canvas: Canvas) {
            paint.shader = specularShader
            canvas.drawRect(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.top + 24 * d, paint)
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    // Darkened edge: 1px border per DESIGN.md §2.2 (rim on all sides)
    val border = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setStroke(1.coerceAtLeast((0.5 * d).toInt()), GlassColors.DarkenedEdge.toArgbCompat())
    }

    foreground = LayerDrawable(arrayOf<Drawable>(specular, border))
}
