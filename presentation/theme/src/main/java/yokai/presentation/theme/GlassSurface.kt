package yokai.presentation.theme

import android.graphics.Outline
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kyant0.backdrop.Backdrop
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTheme
import dev.chrisbanes.haze.haze

/**
 * Tier-aware GlassSurface composable.
 *
 * Implements iOS 27 Liquid Glass with three tiers (DESIGN.md §3):
 * - Tier 1 (API 33+): Backdrop AGSL shader (refraction + lensing)
 * - Tier 2 (API 31–32): Haze blur
 * - Tier 3 (API 29–30): Scrim fallback
 *
 * RULE: Never stack glass on glass (DESIGN.md §1.2).
 * RULE: Cap at 2–3 glass surfaces per screen (DESIGN.md §1.3).
 * RULE: Content must shine through — sample backdrop luminance (DESIGN.md §1.4).
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    tintAlpha: Float = GlassColors.GlassBaseTintAlpha,
    darkenedEdge: Boolean = true,
    specularHighlight: Boolean = true,
    content: @Composable () -> Unit,
) {
    val tier = glassTier()
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(cornerRadius)

    when (tier) {
        is GlassTier.Full -> {
            // Tier 1: Backdrop AGSL shader with refraction
            Backdrop(
                modifier = modifier.clip(shape),
                tint = glassTintColor(tintAlpha, isDark),
            ) {
                GlassSurfaceDecorators(
                    shape = shape,
                    darkenedEdge = darkenedEdge,
                    specularHighlight = specularHighlight,
                )
                content()
            }
        }

        is GlassTier.Blur -> {
            // Tier 2: Haze blur
            Box(
                modifier = modifier
                    .clip(shape)
                    .haze(
                        hazeStyle = HazeStyle.Translucent,
                        theme = HazeTheme(
                            blur = 20f,
                            tint = glassTintColor(tintAlpha, isDark),
                        ),
                    )
                    .background(glassTintColor(tintAlpha, isDark)),
            ) {
                GlassSurfaceDecorators(
                    shape = shape,
                    darkenedEdge = darkenedEdge,
                    specularHighlight = specularHighlight,
                )
                content()
            }
        }

        is GlassTier.Scrim -> {
            // Tier 3: Scrim fallback
            val scrimColor = if (isDark) GlassColors.ScrimDark else GlassColors.ScrimLight

            Box(
                modifier = modifier
                    .clip(shape)
                    .background(scrimColor),
            ) {
                GlassSurfaceDecorators(
                    shape = shape,
                    darkenedEdge = darkenedEdge,
                    specularHighlight = specularHighlight,
                )
                content()
            }
        }
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
                .matchParentSize()
                .clip(shape)
                .background(
                    brush = Brush.verticalGradient(
                        0f to 96f,
                        colors = listOf(GlassColors.DarkenedEdge, Color.Transparent),
                    ),
                ),
        )
    }
    if (specularHighlight) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(
                    brush = Brush.verticalGradient(
                        0f to 24f,
                        colors = listOf(GlassColors.SpecularHighlight, Color.Transparent),
                    ),
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
 * View-based glass implementation for XML-inflated components.
 * Used by MainActivity's bottom nav and top bar.
 * Call: `view.applyGlass(28f, glassTier())`
 */
fun View.applyGlass(cornerRadiusDp: Float, tier: GlassTier? = null) {
    val actualTier = tier ?: glassTier()
    val radiusPx = cornerRadiusDp * resources.displayMetrics.density

    when (actualTier) {
        is GlassTier.Full, is GlassTier.Blur -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val blurEffect = RenderEffect.createGaussianBlurEffect(
                    20f, 20f, Shader.TileMode.CLAMP,
                )
                setRenderEffect(blurEffect)
            }
        }
        is GlassTier.Scrim -> {
            val isDark = (resources.configuration.uiMode
                and android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES
            setBackgroundColor(
                if (isDark) GlassColors.ScrimDark.toArgb()
                else GlassColors.ScrimLight.toArgb(),
            )
        }
    }

    // Apply squircle clip via outline
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
        }
    }
    clipToOutline = true
}

/**
 * Spring animation helper for Views (DESIGN.md §4.5)
 * Matches iOS 27 spring: dampingRatio=0.75, stiffness=300
 */
fun springAnimate(view: View, property: String, to: Float) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val anim = androidx.dynamicanimation.animation.SpringAnimation(view, property, to)
        anim.spring = androidx.dynamicanimation.animation.SpringForce(to)
            .setDampingRatio(0.75f)
            .setStiffness(300f)
        anim.start()
    }
}

/**
 * Applies iOS 27 Liquid Glass decorators (darkened edge + specular highlight)
 * to a View by setting a custom foreground drawable with gradient layers.
 * Mirrors [GlassSurfaceDecorators] for the Compose side.
 */
fun View.applyGlassDecorators(tier: GlassTier? = null) {
    val actualTier = tier ?: glassTier()

    // Only apply decorators for blur/glass tiers (not scrim)
    if (actualTier is GlassTier.Scrim) return

    // Build a foreground drawable with vertical gradients for darkened edge + specular highlight
    // GradientDrawable draws gradient across full bounds; we use a custom drawable to limit height
    val decoratorDrawable = object : android.graphics.drawable.Drawable() {
        private val darkenedEdgeShader = android.graphics.LinearGradient(
            0f, 0f, 0f, 96 * resources.displayMetrics.density,
            intArrayOf(GlassColors.DarkenedEdge.toArgb(), Color.Transparent.toArgb()),
            null,
            android.graphics.Shader.TileMode.CLAMP,
        )
        private val specularShader = android.graphics.LinearGradient(
            0f, 0f, 0f, 24 * resources.displayMetrics.density,
            intArrayOf(GlassColors.SpecularHighlight.toArgb(), Color.Transparent.toArgb()),
            null,
            android.graphics.Shader.TileMode.CLAMP,
        )
        private val paint = android.graphics.Paint().apply { isAntiAlias = true }

        override fun draw(canvas: android.graphics.Canvas) {
            val bounds = this.bounds
            // Darkened edge (96dp from top)
            paint.shader = darkenedEdgeShader
            canvas.drawRect(
                0f, 0f, bounds.width().toFloat(), 96 * resources.displayMetrics.density,
                paint,
            )
            // Specular highlight (24dp from top)
            paint.shader = specularShader
            canvas.drawRect(
                0f, 0f, bounds.width().toFloat(), 24 * resources.displayMetrics.density,
                paint,
            )
        }

        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { paint.colorFilter = colorFilter }
        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
    }

    // Add to View overlay (non-interfering with background/clicks)
    overlay.add(decoratorDrawable)
}
