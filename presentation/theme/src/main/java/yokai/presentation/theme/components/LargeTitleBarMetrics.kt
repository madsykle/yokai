package yokai.presentation.theme.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp

/**
 * Every number the collapsing bar needs, and the arithmetic that turns a scroll offset into
 * them — as pure functions, with no Compose state.
 *
 * ## Why this file exists twice over
 *
 * This repo has no Compose UI test infrastructure (no `androidTest` source set, no Robolectric,
 * no `ui-test-junit4`), and CI runs only `testStandardDebugUnitTest`. A `createComposeRule()`
 * assertion would compile and then never execute. So the bar's *decisions* live here where a
 * JVM test can reach them, and [LargeTitleBar] is left holding only the drawing.
 *
 * Same split as [GroupedSectionMetrics]; see `docs/DESIGN_CUPERTINO.md` §4.1 rule 5.
 */
object LargeTitleBarMetrics {

    // ---- Size range ---------------------------------------------------------

    /** Scroll offset at which the title starts condensing. Content is still fully transparent here. */
    val CollapseStart: Dp = 12.dp

    /**
     * Scroll offset at which 34pt has fully condensed to 17pt.
     *
     * The window is [CollapseRange] = 32dp wide, so the title is never *instantly* 17pt.
     * Per §4.1 rule 5 the spring owns the settling, not this number: [CollapseRange] sets how
     * much scroll buys how much size, and nothing here decides *when* the spring runs.
     */
    val CollapseEnd: Dp = CollapseStart + 32.dp

    /** Width of the 34 -> 17pt window. [CollapseEnd] - [CollapseStart]. */
    val CollapseRange: Dp = CollapseEnd - CollapseStart

    // ---- Type ---------------------------------------------------------------

    /** Expanded size, from `CupertinoType.largeTitle` (34sp). */
    val LargeTitleSize: TextUnit = 34.sp

    /** Condensed size, from `CupertinoType.headline` (17sp). */
    val HeadlineSize: TextUnit = 17.sp

    // ---- Bar geometry -------------------------------------------------------

    /**
     * Height of the *condensed* bar — the pinned strip the 17pt title and any trailing
     * accessory live in.
     *
     * 44dp, matching [GroupedSectionMetrics.rowMinHeight] and the iOS compact bar, so a
     * tap anywhere in the strip hits something.
     */
    val BarHeight: Dp = 44.dp

    /** Standard bar inset, same value the grouped list uses. */
    val BarHorizontalInset: Dp = 16.dp

    /** Hairline thickness, one physical pixel at the current density — see [LargeTitleBar]. */
    val HairlineHeight: Dp = 1.dp

    // ---- Opacity ramp -------------------------------------------------------

    /**
     * How far through the collapse the bar reaches full opacity.
     *
     * The bar must be legible before the title has finished shrinking, or the title appears to
     * shrink through a translucent background and the text washes out mid-animation. Full
     * opacity is reached at [BarOpaqueBy] = 0.6 of the way through — before the type settles.
     */
    const val BarOpaqueBy = 0.6f

    // ---- Pure arithmetic ----------------------------------------------------

    /**
     * Collapse fraction for a scroll offset, clamped to `0f..1f`.
     *
     * `0f` = fully expanded (34pt, transparent bar). `1f` = fully condensed (17pt, opaque bar).
     *
     * Offsets at or below [CollapseStart] are `0f` and offsets at or above [CollapseEnd] are
     * `1f`; the linear ramp between them is what makes the size change track the finger instead
     * of animating on a timer (§4.1 rule 5).
     *
     * @param scrollOffsetPx scroll offset in pixels, as fed by the caller.
     * @param pxPerDp density, needed because the thresholds are [Dp] and scroll is in pixels.
     */
    fun collapseFraction(scrollOffsetPx: Float, pxPerDp: Float): Float {
        // `!(x > 0f)` rather than `x <= 0f`: NaN fails both of those comparisons' intent, and a
        // NaN density would otherwise propagate into every interpolation downstream.
        if (!(pxPerDp > 0f)) return 1f
        val startPx = CollapseStart.toPx(pxPerDp)
        val endPx = CollapseEnd.toPx(pxPerDp)
        if (scrollOffsetPx <= startPx) return 0f
        if (scrollOffsetPx >= endPx) return 1f
        return (scrollOffsetPx - startPx) / (endPx - startPx)
    }

    /**
     * Bar background alpha for a collapse fraction.
     *
     * Reaches `1f` at [BarOpaqueBy] and holds. Linear, because the bar has no motion of its
     * own to justify anything else — it is a readout of the same fraction the title uses.
     */
    fun barAlpha(fraction: Float): Float = (fraction / BarOpaqueBy).coerceIn(0f, 1f)

    /**
     * Whether the hairline under the bar is drawn.
     *
     * Only once content is actually under the bar, i.e. once the bar has started to become
     * opaque. Drawing it at fraction 0 puts a rule across a transparent bar at rest, which is
     * the thing this component exists to avoid.
     */
    fun showHairline(fraction: Float): Boolean = fraction > 0f

    /**
     * Title size for a collapse fraction, interpolated [LargeTitleSize] -> [HeadlineSize].
     *
     * Interpolating the *size* rather than cross-fading two text runs keeps the title a single
     * layout node, so it cannot reflow the bar's height mid-collapse.
     *
     * Uses `lerp(TextUnit, TextUnit, Float)` rather than arithmetic: `TextUnit` deliberately has
     * no `plus`/`minus` operators, only scalar multiply, because adding two type sizes has no
     * meaning. `TextUnit.kt:367`.
     */
    fun titleFontSize(fraction: Float): TextUnit =
        lerp(LargeTitleSize, HeadlineSize, fraction.coerceIn(0f, 1f))

    /**
     * Title leading weight for a collapse fraction.
     *
     * `largeTitle` is Normal and `headline` is Semibold. A jump in weight at the end of the
     * collapse reads as a flicker, so it rides the same fraction — reaching Semibold only as
     * the title is nearly settled.
     */
    fun titleWeightFraction(fraction: Float): Float = fraction.coerceIn(0f, 1f)

    /**
     * Collapse fraction a fling should start from being handed to the spring, given the scroll
     * velocity in px/s.
     *
     * The spring's job is to carry the flick past the threshold the finger was heading for, so
     * the title settles where momentum was taking it instead of creeping. Returns the fraction
     * of the collapse window per second that [scrollVelocityPxPerSec] buys.
     *
     * Negative (scrolling back toward the top) falls through as a negative value on purpose —
     * that is a fling that should un-collapse the title, and `animateTo` handles a negative
     * `initialVelocity` correctly. Only zero is normalised away, so a stopped scroll does not
     * inject a fake push.
     */
    fun collapseVelocity(scrollVelocityPxPerSec: Float, pxPerDp: Float): Float {
        if (!(pxPerDp > 0f)) return 0f
        if (scrollVelocityPxPerSec == 0f) return 0f
        val rangePx = CollapseRange.toPx(pxPerDp)
        if (rangePx <= 0f) return 0f
        return scrollVelocityPxPerSec / rangePx
    }

    /** [Dp] to pixels, given a density. Local so the arithmetic above has one conversion point. */
    private fun Dp.toPx(pxPerDp: Float): Float = value * pxPerDp
}
