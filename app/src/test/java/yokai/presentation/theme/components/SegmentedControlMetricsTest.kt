package yokai.presentation.theme.components

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Segmented-control invariants — mostly the drag arithmetic, which is the part that is easy to
 * get subtly wrong and impossible to eyeball.
 *
 * Pure JVM: no Compose UI test infrastructure exists in this repo, so [SegmentedControlMetrics]
 * holds anything worth asserting.
 */
class SegmentedControlMetricsTest {

    private val thumbInsetPx = 6f // 2dp at 3x
    private val trackWidthPx = 306f // ~340dp screen minus the list inset
    private val segmentWidth = SegmentedControlMetrics.segmentWidthPx(trackWidthPx, thumbInsetPx, 4)

    // ---- Segment count is validated, not clamped -----------------------------

    @Test
    fun `segment counts inside the range are accepted`() {
        SegmentedControlMetrics.requireValidSegmentCount(2)
        SegmentedControlMetrics.requireValidSegmentCount(5)
        SegmentedControlMetrics.MinSegments shouldBe 2
        SegmentedControlMetrics.MaxSegments shouldBe 5
    }

    @Test
    fun `segment counts outside the range throw with the reason`() {
        // A caller passing 6 should hear about it. Clamping to 5 would silently hide segments
        // and the screen would lose an option with no indication why.
        shouldThrow<IllegalArgumentException> { SegmentedControlMetrics.requireValidSegmentCount(1) }
        shouldThrow<IllegalArgumentException> { SegmentedControlMetrics.requireValidSegmentCount(6) }
        shouldThrow<IllegalArgumentException> { SegmentedControlMetrics.requireValidSegmentCount(0) }
    }

    // ---- Cell geometry -------------------------------------------------------

    @Test
    fun `cells divide the track after both insets`() {
        // (306 - 2*6) / 4 = 73.5. The thumb must not overhang the track at the last segment.
        segmentWidth shouldBe 73.5f
        (4 * segmentWidth + 2 * thumbInsetPx) shouldBe trackWidthPx
    }

    @Test
    fun `a track too small for its insets yields zero rather than a negative cell`() {
        SegmentedControlMetrics.segmentWidthPx(10f, 6f, 4) shouldBe 0f
        SegmentedControlMetrics.segmentWidthPx(12f, 6f, 4) shouldBe 0f
        SegmentedControlMetrics.segmentWidthPx(11f, 6f, 4) shouldBe 0f
    }

    @Test
    fun `more segments means narrower cells, at the same track width`() {
        val two = SegmentedControlMetrics.segmentWidthPx(trackWidthPx, thumbInsetPx, 2)
        val five = SegmentedControlMetrics.segmentWidthPx(trackWidthPx, thumbInsetPx, 5)
        (five < two) shouldBe true
        two shouldBe (trackWidthPx - 2f * thumbInsetPx) / 2f
    }

    // ---- Drag follows the finger --------------------------------------------

    @Test
    fun `a resting thumb sits exactly on its selected index`() {
        SegmentedControlMetrics.indicatorPosition(0, 0f, segmentWidth, 4) shouldBe 0f
        SegmentedControlMetrics.indicatorPosition(3, 0f, segmentWidth, 4) shouldBe 3f
    }

    @Test
    fun `dragging a quarter cell moves the thumb a quarter of a cell, not a whole cell`() {
        // This is the whole point of supporting drag: the thumb is between cells while the finger
        // is between cells. Snapping here would make dragging identical to tapping.
        SegmentedControlMetrics.indicatorPosition(1, segmentWidth * 0.25f, segmentWidth, 4) shouldBe 1.25f
        SegmentedControlMetrics.indicatorPosition(1, segmentWidth * 0.5f, segmentWidth, 4) shouldBe 1.5f
        SegmentedControlMetrics.indicatorPosition(1, -segmentWidth * 0.5f, segmentWidth, 4) shouldBe 0.5f
    }

    @Test
    fun `the thumb can travel half a cell past each end, and no further`() {
        SegmentedControlMetrics.indicatorPosition(0, -segmentWidth, segmentWidth, 4) shouldBe -0.5f
        SegmentedControlMetrics.indicatorPosition(3, segmentWidth, segmentWidth, 4) shouldBe 3.5f
        SegmentedControlMetrics.maxIndicatorPosition(4) shouldBe 3.5f
    }

    @Test
    fun `a zero-width segment cannot produce a divide by zero`() {
        SegmentedControlMetrics.indicatorPosition(2, 50f, 0f, 4) shouldBe 2f
    }

    // ---- Release snaps to the nearest segment -------------------------------

    @Test
    fun `release snaps to whichever segment the thumb is nearer`() {
        SegmentedControlMetrics.nearestSegment(0.4f, 4) shouldBe 0
        SegmentedControlMetrics.nearestSegment(0.6f, 4) shouldBe 1
        SegmentedControlMetrics.nearestSegment(2.5f, 4) shouldBe 3
        SegmentedControlMetrics.nearestSegment(2.4f, 4) shouldBe 2
    }

    @Test
    fun `a thumb exactly halfway settles the same way every time`() {
        // The haptic fires on the crossing. A tie that resolved differently run to run would
        // tick twice for one drag.
        repeat(4) {
            SegmentedControlMetrics.nearestSegment(1.5f, 4) shouldBe 2
        }
        // Half-up, matching a rightwards drag: at a tie you were heading right.
        SegmentedControlMetrics.nearestSegment(1.5f, 4) shouldBe 2
        SegmentedControlMetrics.nearestSegment(0.5f, 4) shouldBe 1
    }

    @Test
    fun `release can never land outside the segment list`() {
        SegmentedControlMetrics.nearestSegment(-4f, 4) shouldBe 0
        SegmentedControlMetrics.nearestSegment(99f, 4) shouldBe 3
    }

    // ---- Haptic crossings ---------------------------------------------------

    @Test
    fun `one tick per boundary crossed, not one per frame`() {
        // A drag from segment 0 to segment 3 is three boundaries.
        SegmentedControlMetrics.boundariesCrossed(0f, 3f, 4) shouldBe 3
        SegmentedControlMetrics.boundariesCrossed(0f, 1f, 4) shouldBe 1
        SegmentedControlMetrics.boundariesCrossed(2f, 3f, 4) shouldBe 1
    }

    @Test
    fun `a drag that stays inside one segment is silent`() {
        SegmentedControlMetrics.boundariesCrossed(1.1f, 1.4f, 4) shouldBe 0
        SegmentedControlMetrics.boundariesCrossed(1.4f, 1.1f, 4) shouldBe 0
        SegmentedControlMetrics.boundariesCrossed(0f, 0.49f, 4) shouldBe 0
        SegmentedControlMetrics.boundariesCrossed(1f, 1f, 4) shouldBe 0
    }

    @Test
    fun `crossing the middle of a segment is one crossing, not a silent pass`() {
        // 1.1 -> 1.9 passes over the 1.5 boundary. Sitting still must not tick, but crossing must.
        SegmentedControlMetrics.boundariesCrossed(1.1f, 1.9f, 4) shouldBe 1
    }

    @Test
    fun `sitting exactly on a boundary is not a crossing until the thumb passes it`() {
        // Frame N lands the thumb precisely on 0.5. Strict inequalities, so this must not tick
        // until the next frame takes it past.
        SegmentedControlMetrics.boundariesCrossed(0.4f, 0.5f, 4) shouldBe 0
        SegmentedControlMetrics.boundariesCrossed(0.4f, 0.51f, 4) shouldBe 1
    }

    @Test
    fun `crossings count the same going back as going forward`() {
        // Dragging leftwards ticks too; otherwise the control is silent when reversing.
        SegmentedControlMetrics.boundariesCrossed(3f, 0f, 4) shouldBe 3
    }

    @Test
    fun `crossings never exceed the number of boundaries that exist`() {
        SegmentedControlMetrics.boundariesCrossed(0f, 3f, 2) shouldBe 1
        SegmentedControlMetrics.boundariesCrossed(-99f, 99f, 5) shouldBe 4
    }

    // ---- Geometry tokens ----------------------------------------------------

    @Test
    fun `the control is toolbar height, not a full row`() {
        // 32dp because it lives inside a row that is already 44dp. If it were 44 it would be
        // claiming a tap target it does not need.
        SegmentedControlMetrics.Height.value shouldBe 32f
        (SegmentedControlMetrics.Height.value < GroupedSectionMetrics.rowMinHeight.value) shouldBe true
    }

    @Test
    fun `the thumb is inset from the track, or it is indistinguishable from it`() {
        // Zero inset means thumb and track are the same shape at the same radius and the
        // control reads as one pill with text on it.
        (SegmentedControlMetrics.ThumbInset.value > 0f) shouldBe true
        SegmentedControlMetrics.ThumbInset.value shouldBe 2f
        // And the thumb's own radius has to give that inset back, or the corners disagree.
        (SegmentedControlMetrics.TrackRadius.value - SegmentedControlMetrics.ThumbInset.value) shouldBe 8f
    }

    @Test
    fun `the track radius is a shared token, not a local guess`() {
        SegmentedControlMetrics.TrackRadius shouldBe yokai.presentation.theme.CornerRadii.small
    }
}
