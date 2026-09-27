package yokai.presentation.theme

import kotlin.math.PI
import kotlin.math.sin

/**
 * The single glass recipe (DESIGN.md §18).
 *
 * Every glass surface in the app - the View-side [GlassPane] and the Compose-side
 * `GlassSurface` - draws from these numbers, so the material cannot drift between an
 * XML-inflated screen and a Compose one.
 *
 * The recipe is *drawn*, never captured: a vertical white gradient (lighter top, darker
 * bottom), a tiled noise layer to kill the gradient banding, a 1px near-white stroke limited
 * to the top ~60% of the edge, a soft [android.graphics.BlurMaskFilter] glow on the shape's own
 * edge plus a drop shadow below it, and a one-shot specular sweep on state change.
 *
 * **This deliberately does not sample the backdrop.** `RenderEffect` needs API 31 and AGSL
 * needs API 33, so neither exists on the target device, and `RenderScript` is banned outright
 * by DESIGN.md §7.1. A real backdrop blur on API 30 is therefore impossible without breaking
 * the tier rules, and the material has to carry its legibility itself - which is why the base
 * fill stays as strong as §3's scrim tier requires (see [baseFillAlpha]).
 *
 * Nothing here touches `android.graphics.Bitmap` or any other type a JVM unit test cannot
 * instantiate, so the parts that are wrong-invisibly are unit tested
 * (`GlassRecipeTest`, `SuperellipseTest`).
 */
object GlassRecipe {

    /** Top of the vertical fill: how much white sits over the base tint. */
    const val FILL_TOP_WHITE_ALPHA_DARK = 0.18f
    const val FILL_TOP_WHITE_ALPHA_LIGHT = 0.14f

    /** Bottom of the vertical fill: the darker end of the ramp. */
    const val FILL_BOTTOM_WHITE_ALPHA_DARK = 0.08f
    const val FILL_BOTTOM_WHITE_ALPHA_LIGHT = 0.06f

    /**
     * Tiled noise. Enough to break the banding a wide, low-contrast vertical gradient shows on
     * an 8-bit panel; far too faint to read as texture.
     */
    const val NOISE_ALPHA = 0.04f
    const val NOISE_TILE_PX = 64

    /** The lit rim: a 1px near-white stroke on the *top* part of the edge only. */
    const val RIM_WHITE_ALPHA_DARK = 0.55f
    const val RIM_WHITE_ALPHA_LIGHT = 0.40f
    const val RIM_SWEEP_FRACTION = 0.60f

    /** Soft glow along the shape's own edge, and the shadow the surface casts below it. */
    const val EDGE_GLOW_RADIUS_DP = 6f
    const val EDGE_GLOW_ALPHA_DARK = 0.18f
    const val EDGE_GLOW_ALPHA_LIGHT = 0.22f
    const val SHADOW_RADIUS_DP = 6f
    const val SHADOW_DY_DP = 2f
    const val SHADOW_ALPHA_DARK = 0.45f
    const val SHADOW_ALPHA_LIGHT = 0.30f

    /** The one-shot specular sweep. It fires once per interaction and settles. */
    const val SWEEP_DURATION_MS = 650L
    const val SWEEP_BAND_FRACTION = 0.22f
    const val SWEEP_MAX_WHITE_ALPHA = 0.28f

    /**
     * Base fill alpha under the gradient - the tier's own legibility floor.
     *
     * On tier `Scrim` (the target device) there is no blur behind the surface, so the base has
     * to be near-opaque or labels become unreadable over a busy manga cover; that is §3's
     * scrim fallback, and the gradient/noise/rim are painted on top of it. On the blur tiers
     * the §2.2 slider sets it, and the dark-mode lift ([GlassColors.GLASS_DARK_BASE_ALPHA])
     * keeps the surface *lighter* than its page rather than darker.
     */
    fun baseFillAlpha(tier: GlassTier, tintAlpha: Float, isDark: Boolean): Float = when (tier) {
        is GlassTier.Scrim -> GlassColors.ScrimFallbackAlpha
        else -> tintAlpha * if (isDark) GlassColors.GLASS_DARK_BASE_ALPHA else 1f
    }

    /**
     * Base fill colour, before the ramp. Light mode frosts white; dark mode uses the *lifted*
     * scrim on tier 3 (§14: a `#1C1C1E` fill on a `#1C1C1C` page is an invisible bar) and a
     * white veil on the higher tiers.
     *
     * These are plain ARGB ints rather than `GlassColors` values on purpose: this whole object
     * has to stay free of `android.*` types so it can be unit tested on the JVM (see the class
     * KDoc). [SCRIM_DARK_FILL] mirrors §14's lift and [SCRIM_LIGHT_FILL] mirrors
     * `GlassColors.ScrimLight`.
     */
    fun baseFillColor(tier: GlassTier, isDark: Boolean): Int = when {
        tier is GlassTier.Scrim && isDark -> SCRIM_DARK_FILL
        tier is GlassTier.Scrim -> SCRIM_LIGHT_FILL
        else -> WHITE
    }

    /** `#2C2C2E`, the *lifted* dark scrim (DESIGN.md §14). */
    const val SCRIM_DARK_FILL = 0xFF2C2C2E.toInt()

    /** `GlassColors.ScrimLight`, `#F2F2F7`. */
    const val SCRIM_LIGHT_FILL = 0xFFF2F2F7.toInt()

    /** Opaque white, the material base above the scrim tier. */
    const val WHITE = 0xFFFFFFFF.toInt()

    fun fillTopWhiteAlpha(isDark: Boolean): Float =
        if (isDark) FILL_TOP_WHITE_ALPHA_DARK else FILL_TOP_WHITE_ALPHA_LIGHT

    fun fillBottomWhiteAlpha(isDark: Boolean): Float =
        if (isDark) FILL_BOTTOM_WHITE_ALPHA_DARK else FILL_BOTTOM_WHITE_ALPHA_LIGHT

    fun rimWhiteAlpha(isDark: Boolean): Float =
        if (isDark) RIM_WHITE_ALPHA_DARK else RIM_WHITE_ALPHA_LIGHT

    fun edgeGlowAlpha(isDark: Boolean): Float =
        if (isDark) EDGE_GLOW_ALPHA_DARK else EDGE_GLOW_ALPHA_LIGHT

    fun shadowAlpha(isDark: Boolean): Float =
        if (isDark) SHADOW_ALPHA_DARK else SHADOW_ALPHA_LIGHT

    /**
     * The white ramp painted over the base fill: lighter at the top, darker at the bottom.
     * Returned top-first, so it can be handed straight to a vertical `LinearGradient`.
     */
    fun fillGradientColors(isDark: Boolean): IntArray = intArrayOf(
        whiteAt(fillTopWhiteAlpha(isDark)),
        whiteAt(fillBottomWhiteAlpha(isDark)),
    )

    /** Near-white rim colour for the lit edge. */
    fun rimColor(isDark: Boolean): Int = whiteAt(rimWhiteAlpha(isDark))

    /**
     * Opacity of the specular band at [progress] (0 = before it enters, 1 = after it leaves).
     *
     * A single `sin` pass: the band fades in, crosses, and fades out, so the sweep is a beat
     * rather than a repetitive shine. The ends are exactly transparent, which is what lets the
     * animation stop and leave nothing behind.
     */
    fun sweepBandAlpha(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return (sin(PI * p).toFloat()) * SWEEP_MAX_WHITE_ALPHA
    }

    /**
     * Fraction of the surface height the rim covers, measured from the top. The stroke is
     * painted on a path that runs along the top edge and stops [RIM_SWEEP_FRACTION] of the way
     * down; below that the edge is left to the darkened rim token alone.
     */
    fun rimSweepFraction(): Float = RIM_SWEEP_FRACTION

    /**
     * ARGB pixels for one tile of the noise layer.
     *
     * Deterministic and parameterless in its randomness (a fixed seed), because the tile is
     * repeated across every glass surface: two instances that generated different noise would
     * shimmer against each other, and a tile that changed on every repaint would crawl.
     */
    fun noisePixels(sizePx: Int = NOISE_TILE_PX, seed: Long = NOISE_SEED): IntArray {
        val random = java.util.Random(seed)
        val alpha = (NOISE_ALPHA * 255f).toInt().coerceIn(0, 255)
        return IntArray(sizePx * sizePx) {
            // Mid-grey plus/minus a small spread: the layer is a banding-breaker, not a texture,
            // so it has to stay close to neutral luminance even before it is blended.
            val grey = (128 + random.nextInt(NOISE_SPREAD * 2) - NOISE_SPREAD).coerceIn(0, 255)
            argb(alpha, grey, grey, grey)
        }
    }

    /** Half-range of the noise tile's grey. */
    const val NOISE_SPREAD = 48

    /** Fixed seed: identical noise on every surface and every rebuild (see [noisePixels]). */
    const val NOISE_SEED = 0x9E3779B9L

    private fun whiteAt(alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255f).toInt()
        return argb(a, 255, 255, 255)
    }

    /**
     * ARGB packed by hand instead of through `android.graphics.Color.argb`.
     *
     * `Color` is a platform class and the JVM unit tests have no Robolectric and no
     * `returnDefaultValues`, so any call into it from here would throw rather than be asserted on.
     */
    private fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a shl 24) or (r shl 16) or (g shl 8) or b
}
