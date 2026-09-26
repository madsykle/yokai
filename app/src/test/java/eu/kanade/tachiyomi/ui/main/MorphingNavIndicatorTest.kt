package eu.kanade.tachiyomi.ui.main

import io.kotest.matchers.floats.shouldBeBetween
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * The morphing indicator's spring is invisible in CI and its geometry is wrong in ways that look
 * almost right on a screenshot (a pill centred one item off, a capsule that never relaxes), so
 * the arithmetic is pinned here - same reasoning as [FloatingNavInsetsTest].
 */
class MorphingNavIndicatorTest {

    // A 1080px nav with 4 items and a 168px pill (56dp @3x) - Realme 6 dimensions.
    private val navWidth = 1080
    private val itemCount = 4
    private val indicatorWidth = 168

    @Test
    fun `item centres are evenly distributed across the nav`() {
        MorphingNavIndicator.translationXFor(0, itemCount, navWidth, indicatorWidth) shouldBe 135f - 84f
        MorphingNavIndicator.translationXFor(1, itemCount, navWidth, indicatorWidth) shouldBe 405f - 84f
        MorphingNavIndicator.translationXFor(2, itemCount, navWidth, indicatorWidth) shouldBe 675f - 84f
        MorphingNavIndicator.translationXFor(3, itemCount, navWidth, indicatorWidth) shouldBe 945f - 84f
    }

    @Test
    fun `indicator rests half a width after the nav start on the first item`() {
        // Centre of item 0 is at navWidth * 0.125; the pill is placed by its centre there.
        MorphingNavIndicator.translationXFor(0, itemCount, navWidth, indicatorWidth) shouldBe
            navWidth * 0.125f - indicatorWidth / 2f
    }

    @Test
    fun `index is recovered from a placed position`() {
        for (index in 0 until itemCount) {
            val x = MorphingNavIndicator.translationXFor(index, itemCount, navWidth, indicatorWidth)
            MorphingNavIndicator.indexFor(x, itemCount, navWidth, indicatorWidth) shouldBe index
        }
    }

    @Test
    fun `out of range indices are clamped`() {
        val first = MorphingNavIndicator.translationXFor(-3, itemCount, navWidth, indicatorWidth)
        MorphingNavIndicator.indexFor(first, itemCount, navWidth, indicatorWidth) shouldBe 0

        val last = MorphingNavIndicator.translationXFor(9, itemCount, navWidth, indicatorWidth)
        MorphingNavIndicator.indexFor(last, itemCount, navWidth, indicatorWidth) shouldBe itemCount - 1
    }

    @Test
    fun `degenerate widths and counts fall back to the start edge`() {
        MorphingNavIndicator.translationXFor(1, 0, navWidth, indicatorWidth) shouldBe 0f
        MorphingNavIndicator.translationXFor(1, itemCount, 0, indicatorWidth) shouldBe 0f
        MorphingNavIndicator.indexFor(500f, 0, navWidth, indicatorWidth) shouldBe 0
    }

    @Test
    fun `a resting spring leaves the pill an exact capsule`() {
        val (scaleX, scaleY) = MorphingNavIndicator.stretchScalesFor(0f)
        scaleX shouldBe 1f
        scaleY shouldBe 1f
    }

    @Test
    fun `slow drift below the deadband does not deform the pill`() {
        val (scaleX, scaleY) = MorphingNavIndicator.stretchScalesFor(100f)
        scaleX shouldBe 1f
        scaleY shouldBe 1f
    }

    @Test
    fun `peak velocity stretch matches the section 17 spec`() {
        val (scaleX, scaleY) = MorphingNavIndicator.stretchScalesFor(
            MorphingNavIndicator.VELOCITY_FULL_STRETCH_PX_PER_S,
        )
        scaleX shouldBe MorphingNavIndicator.MAX_STRETCH_X
        scaleY shouldBe MorphingNavIndicator.MIN_STRETCH_Y
    }

    @Test
    fun `mid-speed stretch sits between rest and peak`() {
        val (scaleX, scaleY) = MorphingNavIndicator.stretchScalesFor(4500f)
        scaleX.shouldBeBetween(1f, MorphingNavIndicator.MAX_STRETCH_X, TOLERANCE)
        scaleY.shouldBeBetween(MorphingNavIndicator.MIN_STRETCH_Y, 1f, TOLERANCE)
    }

    @Test
    fun `velocity is directionless - flying either way stretches the same`() {
        val (a, b) = MorphingNavIndicator.stretchScalesFor(7000f)
        val (c, d) = MorphingNavIndicator.stretchScalesFor(-7000f)
        a shouldBe c
        b shouldBe d
    }

    @Test
    fun `beyond full stretch the deformation is capped`() {
        val (scaleX, _) = MorphingNavIndicator.stretchScalesFor(
            MorphingNavIndicator.VELOCITY_FULL_STRETCH_PX_PER_S * 10f,
        )
        scaleX shouldBe MorphingNavIndicator.MAX_STRETCH_X
    }

    @Test
    fun `zero-width nav is not usable for placement`() {
        MorphingNavIndicator.isLayoutUsable(0) shouldBe false
        MorphingNavIndicator.isLayoutUsable(1080) shouldBe true
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
