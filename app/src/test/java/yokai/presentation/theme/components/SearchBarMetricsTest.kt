package yokai.presentation.theme.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Search-bar state invariants — the focus and cancel transitions.
 *
 * The ruling called for /tdd here rather than on the switch, and these are the facts worth
 * pinning: the bar is invisible at rest, gains Cancel on focus, swaps its magnifier for an X as
 * the query grows, and hides Cancel entirely when there is nothing for it to call.
 *
 * Pure JVM — no Compose UI test infrastructure in this repo.
 */
class SearchBarMetricsTest {

    // ---- Flush at rest ------------------------------------------------------

    @Test
    fun `the bar has no background until content is actually under it`() {
        // "Flush, no elevation" means exactly zero alpha at rest, not a faint tint. Anything
        // non-zero puts a visible grey rectangle on an empty screen.
        SearchBarMetrics.barAlpha(contentScrolledUnder = false) shouldBe 0f
        SearchBarMetrics.barAlpha(contentScrolledUnder = true) shouldBe 1f
    }

    // ---- Cancel visibility --------------------------------------------------

    @Test
    fun `cancel appears on focus`() {
        SearchBarMetrics.showCancel(focused = true, hasCancelHandler = true) shouldBe true
        SearchBarMetrics.showCancel(focused = false, hasCancelHandler = true) shouldBe false
    }

    @Test
    fun `cancel never appears when there is nothing to call`() {
        // A Cancel that does nothing is worse than no Cancel: it promises an exit and swallows
        // the tap. BrowseController is exactly this case.
        SearchBarMetrics.showCancel(focused = true, hasCancelHandler = false) shouldBe false
        SearchBarMetrics.showCancel(focused = false, hasCancelHandler = false) shouldBe false
    }

    @Test
    fun `cancel needs both focus and a handler, never either alone`() {
        val combos = listOf(
            false to false,
            false to true,
            true to false,
            true to true,
        )
        val shown = combos.map { (f, h) -> SearchBarMetrics.showCancel(f, h) }
        shown shouldBe listOf(false, false, false, true)
    }

    // ---- Clear button -------------------------------------------------------

    @Test
    fun `clear appears only once there is something to clear`() {
        SearchBarMetrics.showClear("") shouldBe false
        SearchBarMetrics.showClear("a") shouldBe true
        // Whitespace is invisible in the field and cannot be deleted by aiming at it, so the X
        // has to be there to remove it.
        SearchBarMetrics.showClear(" ") shouldBe true
        SearchBarMetrics.showClear("manga") shouldBe true
    }

    @Test
    fun `an empty query and a whitespace query are distinguishable by clear, not by emptiness`() {
        // Guards the reason the X keys off isNotEmpty rather than isNotBlank: a blank-but-present
        // query still needs a way out.
        SearchBarMetrics.showClear("   ").shouldBe true
        SearchBarMetrics.showClear("").shouldBe false
    }

    // ---- Geometry -----------------------------------------------------------

    @Test
    fun `the bar sits between a toolbar control and a list row`() {
        // 36dp: taller than the segmented control's 32 because it is a text field, shorter than
        // a 44dp row because it is chrome rather than content.
        SearchBarMetrics.Height.value shouldBe 36f
        (SearchBarMetrics.Height.value > 32f) shouldBe true
        (SearchBarMetrics.Height.value < 44f) shouldBe true
    }

    @Test
    fun `the bar shares the screen inset with the large title bar`() {
        SearchBarMetrics.HorizontalInset shouldBe LargeTitleBarMetrics.BarHorizontalInset
    }

    @Test
    fun `the field disc shares the bar's radius, and sits inside it`() {
        // iOS gives the focus disc and the bar the same corner radius; what separates them is
        // the fill, not the shape. A rounder disc would read as a nested capsule and is not what
        // this is.
        SearchBarMetrics.DiscRadius shouldBe SearchBarMetrics.Radius
        SearchBarMetrics.DiscRadius.value shouldBe 10f
        // ...and it still has to fit inside the bar vertically.
        (SearchBarMetrics.DiscRadius.value < SearchBarMetrics.Height.value / 2f) shouldBe true
    }

    @Test
    fun `the bar is flush, not a Material pill`() {
        // A Material search pill is a fully-rounded capsule: radius == height/2. That is
        // precisely the shape §6 rules out, so it is asserted against rather than left to taste.
        (SearchBarMetrics.Radius.value < SearchBarMetrics.Height.value / 2f) shouldBe true
    }
}
