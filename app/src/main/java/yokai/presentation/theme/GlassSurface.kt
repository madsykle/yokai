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
import androidx.core.view.isGone
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
            setBackgroundResource(
                if (isDark) 0 else 0,
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
