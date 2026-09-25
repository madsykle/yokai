package yokai.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * iOS 27 Liquid Glass - tier 1 and tier 2 material (DESIGN.md §3).
 *
 * The two lower tiers both need the same two-part contract, so they are wrapped in one
 * platform-agnostic type:
 *
 * 1. the content that should be sampled applies [glassBackdropSource];
 * 2. the chrome floating above it applies [glassBackdrop].
 *
 * Tier 1 (API 33+) uses Kyant0 Backdrop, which refracts the recorded content through an
 * AGSL shader. Tier 2 (API 31-32) falls back to Haze, which can only blur. Tier 3
 * (API 29-30) gets no backdrop at all - the caller keeps the §3 scrim, because blur is
 * neither reliable nor cheap there and Haze would silently draw its own scrim over ours.
 *
 * RULE (DESIGN.md §1.2): never stack glass on glass. A source must only be attached where
 * chrome genuinely floats over scrolling content, or the effect samples itself.
 */
class GlassBackdropState internal constructor(
    internal val haze: HazeState,
    internal val layer: LayerBackdrop,
)

/**
 * Creates the source state for one screen's glass chrome. Remember this in the screen, not
 * per glass surface: one source must feed every glass surface above it.
 */
@Composable
fun rememberGlassBackdropState(): GlassBackdropState {
    val haze = rememberHazeState()
    val layer = rememberLayerBackdrop()
    return remember(haze, layer) { GlassBackdropState(haze, layer) }
}

/**
 * Marks the content that the glass chrome floats above. Returns `this` unchanged on tier 3,
 * so screens keep working on API 29-30 without any extra graphics layer.
 */
fun Modifier.glassBackdropSource(state: GlassBackdropState?): Modifier {
    if (state == null) return this
    return when (glassTier()) {
        is GlassTier.Full -> layerBackdrop(state.layer)
        is GlassTier.Blur -> hazeSource(state.haze)
        is GlassTier.Scrim -> this
    }
}

/**
 * Applies the tier 1 / tier 2 material to a glass surface.
 *
 * Only the blur/refraction is drawn here - the material tint itself stays with the caller so
 * the §2.2 colours and the user's transparency preference keep applying on every tier.
 * Returns `this` unchanged on tier 3 / when no source was provided.
 */
fun Modifier.glassBackdrop(
    state: GlassBackdropState?,
    shape: Shape,
    blurRadius: Dp = DEFAULT_GLASS_BLUR_RADIUS,
): Modifier {
    if (state == null) return this
    return when (glassTier()) {
        is GlassTier.Full -> drawBackdrop(
            backdrop = state.layer,
            shape = { shape },
            effects = {
                // Blur, then lens: the lensing is what separates tier 1 from tier 2, and it
                // is already a no-op below API 33 inside the library.
                blur(blurRadius.toPx())
                lens(
                    refractionHeight = DEFAULT_GLASS_REFRACTION_HEIGHT.toPx(),
                    refractionAmount = DEFAULT_GLASS_REFRACTION_AMOUNT.toPx(),
                )
            },
        )
        is GlassTier.Blur -> hazeEffect(
            state = state.haze,
            // `tints` has to be spelled out: HazeStyle declares both a `tints` and a `tint`
            // constructor and every other parameter has a default, so a looser call is an
            // overload-resolution ambiguity. It stays empty on purpose - the material tint is
            // drawn by the caller on top of the blur, exactly as on the tier 1 path.
            style = HazeStyle(tints = emptyList(), blurRadius = blurRadius),
        )
        is GlassTier.Scrim -> this
    }
}

/** DESIGN.md §3: enough to read as frosted without smearing what is behind. */
val DEFAULT_GLASS_BLUR_RADIUS: Dp = 24.dp

/**
 * How far into the surface the tier 1 lens distortion reaches, and how far it bends.
 * Deliberately restrained: the point is the bend you can see at the rim, not a fisheye.
 */
val DEFAULT_GLASS_REFRACTION_HEIGHT: Dp = 24.dp
val DEFAULT_GLASS_REFRACTION_AMOUNT: Dp = 16.dp
