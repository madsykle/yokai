package yokai.presentation.theme

import io.kotest.matchers.floats.shouldBeBetween
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * The glass recipe's numbers are invisible in CI and wrong in ways that read as "almost right" on
 * a screenshot: a ramp the wrong way round still looks like glass, a sweep that does not reach
 * zero leaves a permanent sheen, and a tier that picks the wrong fill is a legibility bug rather
 * than a crash. Same reasoning as [GlassTierTest].
 *
 * Everything asserted here is deliberately `android.*`-free, because the JVM unit tests have no
 * Robolectric and no `returnDefaultValues`.
 */
class GlassRecipeTest {

    private val tolerance = 0.0001f

    @Test
    fun `the scrim tier ignores the transparency slider`() {
        // Tier 3 is the legibility floor: the slider must not be able to thin it towards
        // unreadable over a busy cover.
        GlassRecipe.baseFillAlpha(GlassTier.Scrim, 0.30f, isDark = true) shouldBe
            GlassColors.ScrimFallbackAlpha
        GlassRecipe.baseFillAlpha(GlassTier.Scrim, 0.95f, isDark = false) shouldBe
            GlassColors.ScrimFallbackAlpha
    }

    @Test
    fun `the blur tiers follow the slider and dark mode is lifted at half strength`() {
        GlassRecipe.baseFillAlpha(GlassTier.Blur, 0.70f, isDark = false) shouldBe 0.70f
        GlassRecipe.baseFillAlpha(GlassTier.Blur, 0.70f, isDark = true) shouldBe
            0.70f * GlassColors.GLASS_DARK_BASE_ALPHA
    }

    @Test
    fun `the scrim tier fills with the lifted dark token, not the near-black one`() {
        GlassRecipe.baseFillColor(GlassTier.Scrim, isDark = true) shouldBe
            GlassRecipe.SCRIM_DARK_FILL
        // #2C2C2E, explicitly lighter than §4.1's #1C1C1E page - the §14 fix.
        GlassRecipe.baseFillColor(GlassTier.Scrim, isDark = true) shouldBe 0xFF2C2C2E.toInt()
        GlassRecipe.baseFillColor(GlassTier.Scrim, isDark = false) shouldBe
            GlassRecipe.SCRIM_LIGHT_FILL
    }

    @Test
    fun `the blur tiers fill white so dark glass lifts instead of cutting a hole`() {
        GlassRecipe.baseFillColor(GlassTier.Blur, isDark = true) shouldBe GlassRecipe.WHITE
        GlassRecipe.baseFillColor(GlassTier.Full, isDark = false) shouldBe GlassRecipe.WHITE
    }

    @Test
    fun `the ramp runs lighter at the top than the bottom`() {
        for (isDark in listOf(true, false)) {
            val colors = GlassRecipe.fillGradientColors(isDark)
            val topAlpha = colors[0] ushr 24
            val bottomAlpha = colors[1] ushr 24
            topAlpha shouldBeGreaterThan bottomAlpha
            // Both ends tint white: colouring the ramp anything but neutral makes the surface
            // read as a coloured panel rather than glass.
            (colors[0] and 0x00FFFFFF) shouldBe 0x00FFFFFF
            (colors[1] and 0x00FFFFFF) shouldBe 0x00FFFFFF
        }
    }

    @Test
    fun `the ramp stays within the fraction of white the recipe specifies`() {
        val darkTop = GlassRecipe.fillTopWhiteAlpha(true)
        val darkBottom = GlassRecipe.fillBottomWhiteAlpha(true)
        darkTop.shouldBeBetween(0.14f, 0.18f, tolerance)
        darkBottom.shouldBeBetween(0.06f, 0.10f, tolerance)
    }

    @Test
    fun `the rim is white and brighter in dark mode`() {
        val darkRim = GlassRecipe.rimColor(true)
        (darkRim and 0x00FFFFFF) shouldBe 0x00FFFFFF
        (darkRim ushr 24) shouldBeGreaterThan (GlassRecipe.rimColor(false) ushr 24)
    }

    @Test
    fun `the sweep is fully transparent before and after a pass`() {
        // This is what lets the animation stop and leave nothing behind - a non-zero end value
        // would be a permanent sheen on the surface.
        GlassRecipe.sweepBandAlpha(0f) shouldBe 0f
        GlassRecipe.sweepBandAlpha(1f) shouldBe 0f
    }

    @Test
    fun `the sweep peaks halfway and is symmetric about it`() {
        val peak = GlassRecipe.sweepBandAlpha(0.5f)
        peak.shouldBeBetween(
            GlassRecipe.SWEEP_MAX_WHITE_ALPHA - tolerance,
            GlassRecipe.SWEEP_MAX_WHITE_ALPHA + tolerance,
            tolerance,
        )
        GlassRecipe.sweepBandAlpha(0.25f).shouldBeBetween(
            GlassRecipe.sweepBandAlpha(0.75f) - tolerance,
            GlassRecipe.sweepBandAlpha(0.75f) + tolerance,
            tolerance,
        )
    }

    @Test
    fun `out of range sweep progress is clamped, not extrapolated`() {
        GlassRecipe.sweepBandAlpha(-5f) shouldBe 0f
        GlassRecipe.sweepBandAlpha(5f) shouldBe 0f
    }

    @Test
    fun `the noise tile is deterministic so surfaces do not shimmer against each other`() {
        val a = GlassRecipe.noisePixels(16)
        val b = GlassRecipe.noisePixels(16)
        a.size shouldBe 16 * 16
        a.toList() shouldBe b.toList()
        // A different seed is a genuinely different tile, which is what makes the fixed seed
        // meaningful rather than a constant zero field.
        GlassRecipe.noisePixels(16, seed = 1234L).toList().equals(a.toList()) shouldBe false
    }

    @Test
    fun `every noise pixel carries the faint recipe alpha and a near-neutral grey`() {
        val expectedAlpha = (GlassRecipe.NOISE_ALPHA * 255f).toInt()
        GlassRecipe.noisePixels(16).forEach { pixel ->
            (pixel ushr 24) shouldBe expectedAlpha
            val grey = pixel and 0xFF
            grey.shouldBeBetween(128 - GlassRecipe.NOISE_SPREAD, 128 + GlassRecipe.NOISE_SPREAD)
            // Grey, not tinted: a coloured noise layer would tint the whole surface.
            ((pixel shr 16) and 0xFF) shouldBe grey
            ((pixel shr 8) and 0xFF) shouldBe grey
        }
    }

    @Test
    fun `the rim covers a strict minority of the edge`() {
        GlassRecipe.rimSweepFraction().shouldBeBetween(0.5f, 0.7f, tolerance)
    }
}
