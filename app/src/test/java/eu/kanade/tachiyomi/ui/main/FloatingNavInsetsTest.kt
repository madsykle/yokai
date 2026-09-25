package eu.kanade.tachiyomi.ui.main

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * The floating nav inset is the difference between "the last row of the list is reachable" and
 * "the pill permanently covers it", and it is invisible in CI, so the arithmetic is pinned here.
 */
class FloatingNavInsetsTest {

    @Test
    fun `no floating nav reserves nothing`() {
        FloatingNavInsets.insetFor(
            hasFloatingNav = false,
            navTotalHeightPx = 200,
            systemBottomInsetPx = 144,
        ) shouldBe 0
    }

    @Test
    fun `floating nav reserves its height plus the system inset`() {
        FloatingNavInsets.insetFor(
            hasFloatingNav = true,
            navTotalHeightPx = 200,
            systemBottomInsetPx = 144,
        ) shouldBe 344
    }

    @Test
    fun `floating nav without a system inset still reserves its height`() {
        FloatingNavInsets.insetFor(
            hasFloatingNav = true,
            navTotalHeightPx = 200,
            systemBottomInsetPx = 0,
        ) shouldBe 200
    }

    @Test
    fun `padding is added where the screen reserves nothing`() {
        FloatingNavInsets.needsBottomPadding(currentPaddingPx = 0, insetPx = 344) shouldBe true
    }

    @Test
    fun `padding is added where the screen reserves too little`() {
        FloatingNavInsets.needsBottomPadding(currentPaddingPx = 100, insetPx = 344) shouldBe true
    }

    // Screens that already reserve more than the pill needs keep their own value: a download bar
    // or FAB pushes content up further than the nav does, and shrinking the padding to the nav
    // inset would tuck that content back under the pill.
    @Test
    fun `padding is left alone where the screen already reserves more`() {
        FloatingNavInsets.needsBottomPadding(currentPaddingPx = 400, insetPx = 344) shouldBe false
    }

    @Test
    fun `padding is left alone at the exact inset`() {
        FloatingNavInsets.needsBottomPadding(currentPaddingPx = 344, insetPx = 344) shouldBe false
    }
}
