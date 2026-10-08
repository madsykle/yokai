package yokai.presentation.theme.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Switch geometry.
 *
 * The ruling said skip /tdd for this one, and that holds for the *behaviour* — a spring toggle
 * is two lines and the device pass will tell us if it feels wrong. These are the numbers that
 * would silently drift if someone changed a dp value, which is worth more than the behaviour
 * tests would be.
 */
class SwitchMetricsTest {

    @Test
    fun `the proportions are the iOS ones`() {
        SwitchMetrics.TrackWidth.value shouldBe 51f
        SwitchMetrics.TrackHeight.value shouldBe 31f
        SwitchMetrics.ThumbSize.value shouldBe 27f
    }

    @Test
    fun `the thumb fits inside the track with room for the inset`() {
        // 2dp top and bottom is exactly what lets 27dp live in 31dp. A larger inset would not
        // fit at all; a smaller one would leave the thumb flush with the track edge.
        SwitchMetrics.ThumbSize.value + SwitchMetrics.ThumbInset.value * 2 shouldBe
            SwitchMetrics.TrackHeight.value
    }

    @Test
    fun `travel is derived from the other numbers, not hard-coded`() {
        // 51 - 2*2 - 27 = 20.
        SwitchMetrics.ThumbTravel.value shouldBe 20f
        SwitchMetrics.ThumbTravel shouldBe
            SwitchMetrics.TrackWidth - SwitchMetrics.ThumbInset * 2 - SwitchMetrics.ThumbSize
    }

    @Test
    fun `the thumb is flush against its inset when off, and against the far inset when on`() {
        // Centre at inset + radius means the thumb's edge sits exactly `ThumbInset` from the
        // track edge in both resting states.
        SwitchMetrics.thumbCenterX(0f).value shouldBe SwitchMetrics.ThumbInset.value + 13.5f
        SwitchMetrics.thumbCenterX(1f).value shouldBe
            SwitchMetrics.TrackWidth.value - SwitchMetrics.ThumbInset.value - 13.5f
    }

    @Test
    fun `the thumb never leaves the track, whatever position it is given`() {
        // A spring overshoots past its target; clamping here is what stops the overshoot from
        // dragging the thumb outside the track on screen.
        val left = SwitchMetrics.thumbCenterX(-5f).value - SwitchMetrics.ThumbSize.value / 2f
        val right = SwitchMetrics.TrackWidth.value -
            (SwitchMetrics.thumbCenterX(9f).value + SwitchMetrics.ThumbSize.value / 2f)
        left shouldBe SwitchMetrics.ThumbInset.value
        right shouldBe SwitchMetrics.ThumbInset.value
    }

    @Test
    fun `the thumb does not move vertically`() {
        SwitchMetrics.thumbCenterY().value shouldBe 15.5f
        SwitchMetrics.thumbCenterY() shouldBe SwitchMetrics.TrackHeight / 2f
    }

    @Test
    fun `the thumb is a circle, not a squircle, while the track uses the shared superellipse`() {
        // The thumb takes no segments at all — it is drawn with drawCircle, not
        // Superellipse.path. The *track* takes Superellipse's default. If someone routes the
        // thumb through the squircle too, the second line stops matching the component.
        SwitchMetrics.TrackSegments shouldBe yokai.presentation.theme.Superellipse.DEFAULT_SEGMENTS
        // And the track's segment count is not a bespoke local constant.
        SwitchMetrics.TrackSegments shouldBe 64
    }
}
