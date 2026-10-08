package yokai.presentation.theme.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import yokai.presentation.theme.CornerRadii

/**
 * Geometry and drag arithmetic for [SegmentedControl], as pure functions.
 *
 * Same reason as [LargeTitleBarMetrics] and [GroupedSectionMetrics]: the repo has no Compose UI
 * test infrastructure, so anything worth asserting has to be reachable without a render tree.
 */
object SegmentedControlMetrics {

    /**
     * iOS compact height. 32dp, not 44 — this is a *toolbar* control that normally sits inside a
     * row that is already at least 44dp tall, not a row in its own right. The 44dp floor belongs
     * to the row, not to every control in it.
     */
    val Height: Dp = 32.dp

    /** Track and thumb corner radius. `CornerRadii.small`, shared rather than invented. */
    val TrackRadius: Dp = CornerRadii.small

    /**
     * Gap between the track's edge and the thumb.
     *
     * iOS floats the thumb inside the track rather than filling it — without this inset the thumb
     * and the track are the same shape at the same radius and the thumb looks like it *is* the
     * track, so the control reads as a single pill with text on it.
     */
    val ThumbInset: Dp = 2.dp

    /**
     * Inset of the whole track from whatever contains it.
     *
     * Zero by design: a segmented control that shares a row's edges reads as a border on that
     * row. Callers place it; it does not defend its own margins.
     */
    val TrackInset: Dp = 0.dp

    /**
     * Segment count bounds, per `docs/DESIGN_CUPERTINO.md` §6.
     *
     * Below 2 there is nothing to select between; above 5 each segment is too narrow for its
     * label and iOS switches to a menu. Checked in [requireValidSegmentCount] rather than
     * clamped, so a caller passing 6 hears about it instead of silently getting 5.
     */
    const val MinSegments = 2
    const val MaxSegments = 5

    /**
     * Width of one segment's cell, in pixels.
     *
     * @param trackWidthPx the track's measured width.
     * @param thumbInsetPx [ThumbInset] in pixels.
     * @param segmentCount number of segments, already validated.
     *
     * Subtracting both insets from the width is what keeps the thumb from overhanging the track
     * at the last segment.
     */
    fun segmentWidthPx(trackWidthPx: Float, thumbInsetPx: Float, segmentCount: Int): Float {
        requireValidSegmentCount(segmentCount)
        val usable = trackWidthPx - 2f * thumbInsetPx
        return if (usable <= 0f) 0f else usable / segmentCount
    }

    /**
     * Where the thumb's left edge sits, as a float segment index.
     *
     * Whole numbers mean "snapped to segment N". A fractional value means the finger is between
     * two segments and the thumb should be following it there — which is the entire reason to
     * support dragging. Clamped to `-0.5 .. count - 0.5` so the thumb can travel half a segment
     * past either end before it would leave the track.
     */
    fun indicatorPosition(selectedIndex: Int, dragPx: Float, segmentWidthPx: Float, segmentCount: Int): Float {
        val offset = if (segmentWidthPx <= 0f) 0f else dragPx / segmentWidthPx
        return (selectedIndex + offset).coerceIn(-0.5f, maxIndicatorPosition(segmentCount))
    }

    /** Upper bound for [indicatorPosition]'s clamp, given a segment count. */
    fun maxIndicatorPosition(segmentCount: Int): Float {
        requireValidSegmentCount(segmentCount)
        return segmentCount - 0.5f
    }

    /**
     * Which segment the thumb belongs to once the finger lifts.
     *
     * Rounds half-up, so a thumb exactly halfway between segments 1 and 2 settles on 2 — the one
     * a finger dragging rightwards means. Deterministic on the tie, which matters because the
     * haptic fires on the crossing and two runs that disagree would tick twice.
     */
    fun nearestSegment(indicatorPosition: Float, segmentCount: Int): Int {
        requireValidSegmentCount(segmentCount)
        return indicatorPosition
            .roundToSegmentIndex()
            .coerceIn(0, segmentCount - 1)
    }

    /**
     * How many segment boundaries the thumb crosses between two positions.
     *
     * The haptic count. One tick per boundary, not one per frame — a drag across four segments
     * is four ticks, and a drag that wobbles inside one segment is silent. Returning the count
     * rather than a boolean is what lets the caller fire once per crossing instead of firing on
     * every frame it happens to straddle a boundary.
     */
    fun boundariesCrossed(from: Float, to: Float, segmentCount: Int): Int {
        requireValidSegmentCount(segmentCount)
        if (from == to) return 0
        val lower = minOf(from, to)
        val upper = maxOf(from, to)
        // Boundaries sit at the half-integers 0.5, 1.5, 2.5 ... Strict inequalities, so sitting
        // exactly on a boundary is not a crossing until the thumb is past it.
        var crossings = 0
        for (boundary in 0 until segmentCount - 1) {
            val at = boundary + 0.5f
            if (at > lower && at < upper) crossings++
        }
        return crossings
    }

    /** Throws unless [segmentCount] is within [MinSegments]..[MaxSegments]. */
    fun requireValidSegmentCount(segmentCount: Int) {
        require(segmentCount in MinSegments..MaxSegments) {
            "segmented control takes $MinSegments..$MaxSegments segments, got $segmentCount. " +
                "Past $MaxSegments each segment is too narrow for its label; iOS switches to a menu."
        }
    }

    /** Half-up rounding to a whole segment index, ties going the way a rightwards drag reads. */
    private fun Float.roundToSegmentIndex(): Int = kotlin.math.floor(this + 0.5f).toInt()
}
