package eu.kanade.tachiyomi.ui.main

import kotlin.math.abs
import kotlin.math.max

/**
 * Pure geometry for the §17 morphing nav indicator (Phase 3): where the pill sits, how big it
 * gets, and how the spring's velocity deforms it. No Views, no Android classes - the parts that
 * are visibly wrong when they are wrong (a pill that lands between two items, or refuses to
 * return to its own shape) are unit-testable only if they stay pure.
 */
internal object MorphingNavIndicator {

    /**
     * X translation that centres the indicator over the [index]-th nav item of [itemCount]
     * items spread evenly across [navWidth] px.
     *
     * Mirrors Material's own distribution: item i is centred at (i + 0.5) / N of the width.
     * The view's own layout position (fixed to the nav's start edge) is the zero point.
     */
    fun translationXFor(index: Int, itemCount: Int, navWidth: Int, indicatorWidth: Int): Float {
        if (itemCount <= 0) return 0f
        // An unmeasured nav has no geometry to distribute over: the only sane position is the
        // start edge (the caller's `isLayoutUsable` guard normally prevents getting here).
        val navWidthF = max(navWidth, 0).toFloat()
        if (navWidthF == 0f) return 0f
        val clampedIndex = index.coerceIn(0, itemCount - 1)
        val itemCenter = (clampedIndex + 0.5f) / itemCount * navWidthF
        return itemCenter - indicatorWidth / 2f
    }

    /**
     * Squash-and-stretch scales for the pill while the spring is moving fast.
     *
     * The pill stretches along its travel (X) and squashes across it (Y), with the area kept
     * roughly constant - the classic animation beat that makes motion read as mass instead of a
     * sliding rectangle. At [velocityMagnitude] = 0 both scales are exactly 1: rest is
     * undeformed, and the deformation is a pure function of the *current* spring velocity, so a
     * settling spring relaxes back to round on its own.
     *
     * [velocityMagnitude] is in px/s, the same unit SpringAnimation reports.
     */
    fun stretchScalesFor(velocityMagnitude: Float): Pair<Float, Float> {
        val v = abs(velocityMagnitude)
        if (v <= VELOCITY_DEADBAND_PX_PER_S) return 1f to 1f
        val t = ((v - VELOCITY_DEADBAND_PX_PER_S) / (VELOCITY_FULL_STRETCH_PX_PER_S -
            VELOCITY_DEADBAND_PX_PER_S)).coerceIn(0f, 1f)
        val scaleX = 1f + (MAX_STRETCH_X - 1f) * t
        val scaleY = 1f - (1f - MIN_STRETCH_Y) * t
        return scaleX to scaleY
    }

    /**
     * The index of the item the pill is currently over, from its centred X position. Inverse of
     * [translationXFor] - used to keep the pill and the checked item in sync after size changes.
     */
    fun indexFor(translationX: Float, itemCount: Int, navWidth: Int, indicatorWidth: Int): Int {
        if (itemCount <= 0) return 0
        val navWidthF = max(navWidth, 0).toFloat()
        if (navWidthF == 0f) return 0
        val center = translationX + indicatorWidth / 2f
        val index = (center / navWidthF * itemCount - 0.5f).toInt()
        return index.coerceIn(0, itemCount - 1)
    }

    /** True when the nav has been laid out wide enough to compute a pill position from. */
    fun isLayoutUsable(navWidth: Int): Boolean = navWidth > 0

    /** Biggest acceptable |velocity| fed to the squash: above this the stretch is capped. */
    const val VELOCITY_DEADBAND_PX_PER_S = 350f
    const val VELOCITY_FULL_STRETCH_PX_PER_S = 9000f

    /** Peak deformation. Matches DESIGN.md §17: scaleX 1.15 / scaleY 0.9 at full speed. */
    const val MAX_STRETCH_X = 1.15f
    const val MIN_STRETCH_Y = 0.9f
}
