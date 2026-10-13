package yokai.presentation.theme.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Collapsing-bar invariants.
 *
 * JVM-only and pure, deliberately: this repo has no `androidTest` source set, no Robolectric
 * and no `ui-test-junit4`, and CI runs only `testStandardDebugUnitTest`. Anything asserted
 * through `createComposeRule()` would compile and never execute, so every decision the bar makes
 * lives in [LargeTitleBarMetrics] where this can reach it.
 *
 * Density is fixed at 3x throughout (`PX_PER_DP`), which is a real phone, so the thresholds
 * below are exercised at the scale they will actually run at.
 */
class LargeTitleBarTest {

    private val density = 3f

    private fun px(dp: Float) = dp * density

    // ---- Collapse threshold math -------------------------------------------

    @Test
    fun `collapse is complete exactly at the end threshold`() {
        val endPx = px(LargeTitleBarMetrics.CollapseEnd.value)
        LargeTitleBarMetrics.collapseFraction(endPx, density) shouldBe 1f
    }

    @Test
    fun `collapse is untouched at and below the start threshold`() {
        val startPx = px(LargeTitleBarMetrics.CollapseStart.value)
        LargeTitleBarMetrics.collapseFraction(startPx, density) shouldBe 0f
        LargeTitleBarMetrics.collapseFraction(startPx - 500f, density) shouldBe 0f
        LargeTitleBarMetrics.collapseFraction(0f, density) shouldBe 0f
    }

    @Test
    fun `collapse is clamped beyond both thresholds`() {
        val endPx = px(LargeTitleBarMetrics.CollapseEnd.value)
        LargeTitleBarMetrics.collapseFraction(endPx * 4f, density) shouldBe 1f
        LargeTitleBarMetrics.collapseFraction(-9_000f, density) shouldBe 0f
    }

    @Test
    fun `collapse is linear across the window`() {
        val startPx = px(LargeTitleBarMetrics.CollapseStart.value)
        val rangePx = px(LargeTitleBarMetrics.CollapseRange.value)
        // Halfway through the window is halfway collapsed, on every density.
        LargeTitleBarMetrics.collapseFraction(startPx + rangePx / 2f, density) shouldBe 0.5f
        LargeTitleBarMetrics.collapseFraction(startPx + rangePx / 4f, density) shouldBe 0.25f
        LargeTitleBarMetrics.collapseFraction(startPx + rangePx * 0.75f, density) shouldBe 0.75f
    }

    @Test
    fun `the window has width, so the title is never instantly condensed`() {
        // A zero-width window would make 34->17 a single-frame jump at the threshold, which is
        // the one thing a "spring-damped" title must not do.
        (LargeTitleBarMetrics.CollapseRange.value > 0f) shouldBe true
        (LargeTitleBarMetrics.CollapseEnd.value > LargeTitleBarMetrics.CollapseStart.value) shouldBe true
    }

    @Test
    fun `a degenerate density fails closed rather than dividing by zero`() {
        // pxPerDp <= 0 cannot come from a real Density, but a NaN or 0 slipping through would
        // poison every downstream interpolation if it divided.
        LargeTitleBarMetrics.collapseFraction(100f, 0f) shouldBe 1f
        LargeTitleBarMetrics.collapseFraction(100f, -3f) shouldBe 1f
        LargeTitleBarMetrics.collapseFraction(100f, Float.NaN) shouldBe 1f
    }

    // ---- Hairline visibility ------------------------------------------------

    @Test
    fun `hairline is absent at rest and present as soon as anything is under the bar`() {
        LargeTitleBarMetrics.showHairline(0f) shouldBe false
        LargeTitleBarMetrics.showHairline(0.001f) shouldBe true
        LargeTitleBarMetrics.showHairline(1f) shouldBe true
    }

    @Test
    fun `hairline is tied to scroll, not to a timer`() {
        // Exactly the offset the metrics call "nothing has scrolled under yet".
        val startPx = px(LargeTitleBarMetrics.CollapseStart.value)
        LargeTitleBarMetrics.collapseFraction(startPx, density) shouldBe 0f
        LargeTitleBarMetrics.showHairline(LargeTitleBarMetrics.collapseFraction(startPx, density)) shouldBe false

        val justUnder = startPx + 1f
        LargeTitleBarMetrics.showHairline(LargeTitleBarMetrics.collapseFraction(justUnder, density)) shouldBe true
    }

    // ---- Bar opacity ramp ---------------------------------------------------

    @Test
    fun `bar reaches full opacity before the title finishes condensing`() {
        // The bar has to be legible while the type is still shrinking, or the text washes out
        // mid-collapse. This is the assertion that pins that ordering.
        (LargeTitleBarMetrics.BarOpaqueBy < 1f) shouldBe true
        LargeTitleBarMetrics.barAlpha(LargeTitleBarMetrics.BarOpaqueBy) shouldBe 1f
        LargeTitleBarMetrics.barAlpha(LargeTitleBarMetrics.BarOpaqueBy * 0.5f) shouldBe 0.5f
    }

    @Test
    fun `bar opacity is transparent at rest and clamps past opaque`() {
        LargeTitleBarMetrics.barAlpha(0f) shouldBe 0f
        LargeTitleBarMetrics.barAlpha(1f) shouldBe 1f
        LargeTitleBarMetrics.barAlpha(5f) shouldBe 1f
        LargeTitleBarMetrics.barAlpha(-1f) shouldBe 0f
    }

    // ---- Title size and weight ---------------------------------------------

    @Test
    fun `title runs 34 to 17 across the collapse and clamps`() {
        LargeTitleBarMetrics.titleFontSize(0f).value shouldBe 34f
        LargeTitleBarMetrics.titleFontSize(1f).value shouldBe 17f
        LargeTitleBarMetrics.titleFontSize(0.5f).value shouldBe 25.5f
        LargeTitleBarMetrics.titleFontSize(9f).value shouldBe 17f
        LargeTitleBarMetrics.titleFontSize(-9f).value shouldBe 34f
    }

    @Test
    fun `title size endpoints match the type ramp they interpolate between`() {
        // CupertinoType.largeTitle is 34sp and .headline is 17sp. If either token moves, this
        // fails rather than the bar quietly interpolating to a size no token uses.
        LargeTitleBarMetrics.LargeTitleSize.value shouldBe 34f
        LargeTitleBarMetrics.HeadlineSize.value shouldBe 17f
    }

    @Test
    fun `title weight rides the same fraction as the size`() {
        LargeTitleBarMetrics.titleWeightFraction(0f) shouldBe 0f
        LargeTitleBarMetrics.titleWeightFraction(0.5f) shouldBe 0.5f
        LargeTitleBarMetrics.titleWeightFraction(1f) shouldBe 1f
        LargeTitleBarMetrics.titleWeightFraction(-3f) shouldBe 0f
        LargeTitleBarMetrics.titleWeightFraction(7f) shouldBe 1f
    }

    // ---- Spring responsiveness to drag velocity -----------------------------

    @Test
    fun `a faster flick buys more collapse per second than a slow drag`() {
        val rangePx = px(LargeTitleBarMetrics.CollapseRange.value)
        // 32dp window at 3x = 96px, so these are exact: a 2400px/s flick crosses the whole
        // collapse window in a quarter second, a 200px/s drag takes five times as long.
        rangePx shouldBe 96f
        LargeTitleBarMetrics.collapseVelocity(2_400f, density) shouldBe 25f
        LargeTitleBarMetrics.collapseVelocity(200f, density) shouldBe 2.0833333f
        (LargeTitleBarMetrics.collapseVelocity(2_400f, density) >
            LargeTitleBarMetrics.collapseVelocity(200f, density)) shouldBe true
    }

    @Test
    fun `velocity scales with the width of the collapse window`() {
        // Same flick, denser screen: the window is more pixels wide, so the same px/s covers
        // fewer fractions of it. Without this the title would collapse faster on a tablet.
        val at2x = LargeTitleBarMetrics.collapseVelocity(2_400f, 2f)
        val at4x = LargeTitleBarMetrics.collapseVelocity(2_400f, 4f)
        at2x shouldBe 37.5f
        at4x shouldBe 18.75f
        (at4x < at2x) shouldBe true
    }

    @Test
    fun `velocity sign follows scroll direction`() {
        val down = LargeTitleBarMetrics.collapseVelocity(1_500f, density)
        val up = LargeTitleBarMetrics.collapseVelocity(-1_500f, density)
        (down > 0f) shouldBe true
        (up < 0f) shouldBe true
        // Symmetric: a flick away from the threshold must un-collapse exactly as fast as one
        // towards it collapses, or the bar feels sticky in one direction.
        (down == -up) shouldBe true
    }

    @Test
    fun `a stopped scroll injects no velocity`() {
        // Zero is the one value normalised away: passing a fabricated push to the spring for a
        // scroll that simply ended would make the title creep.
        LargeTitleBarMetrics.collapseVelocity(0f, density) shouldBe 0f
        LargeTitleBarMetrics.collapseVelocity(0f, 0f) shouldBe 0f
    }

    @Test
    fun `velocity is zero rather than infinite on a degenerate density`() {
        LargeTitleBarMetrics.collapseVelocity(1_000f, 0f) shouldBe 0f
        LargeTitleBarMetrics.collapseVelocity(1_000f, Float.NaN) shouldBe 0f
    }

    // ---- Geometry -----------------------------------------------------------

    @Test
    fun `bar height is the minimum touch target`() {
        // The condensed bar is a tap target, not decoration.
        (LargeTitleBarMetrics.BarHeight.value >= GroupedSectionMetrics.rowMinHeight.value) shouldBe true
    }

    @Test
    fun `bar shares the standard horizontal inset with the grouped list`() {
        // Two components, one screen edge. If these drift, the title and the first row stop
        // lining up and the screen reads as two designs.
        LargeTitleBarMetrics.BarHorizontalInset shouldBe GroupedSectionMetrics.sectionInset
    }

    @Test
    fun `the bar is exactly the condensed title's own height, not a guess`() {
        // headline is 17sp with a 24sp line height; 44dp is the bar that hosts it, shared with
        // GroupedSectionMetrics.rowMinHeight so the two agree on the minimum touch target.
        LargeTitleBarMetrics.BarHeight.value shouldBe 44f
        LargeTitleBarMetrics.BarHeight shouldBe GroupedSectionMetrics.rowMinHeight
        LargeTitleBarMetrics.HeadlineSize.value shouldBe 17f
    }

    @Test
    fun `hairline thickness stays a single dp box`() {
        // The rule is drawn 1f px inside a 1dp box. If the box grows, a 1px line floats in the
        // middle of dead space instead of sitting on the bar's edge.
        LargeTitleBarMetrics.HairlineHeight.value shouldBe 1f
    }
}
