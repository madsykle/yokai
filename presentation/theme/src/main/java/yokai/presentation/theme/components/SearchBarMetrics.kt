package yokai.presentation.theme.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import yokai.presentation.theme.CornerRadii

/**
 * Geometry and state transitions for [CupertinoSearchBar], as pure functions.
 *
 * Same reason as the other three components' metrics objects: the repo has no Compose UI test
 * infrastructure, and search is the component where the *transitions* matter most — the bar is
 * invisible at rest, translucent when content slides under, and gains a Cancel button on focus.
 * Those are testable facts about a boolean, so they live here.
 */
object SearchBarMetrics {

    /**
     * Bar height.
     *
     * 36dp, not 32dp like the segmented control and not 44dp like a row. iOS search sits between
     * the two: taller than a toolbar control because it is a text field that has to be legible
     * and tappable, shorter than a list row because it is chrome, not content.
     */
    val Height: Dp = 36.dp

    /** Bar corner radius. Flush-ish, not the Material pill. */
    val Radius: Dp = CornerRadii.small

    /**
     * Gap between the glass disc, the field text and the trailing buttons.
     *
     * iOS draws a translucent "disc" behind the text on focus; the radius is deliberately large
     * relative to the bar so it reads as a separate object rather than a highlight.
     */
    val DiscRadius: Dp = 10.dp

    /** Horizontal inset from the screen edge. Matches the large title bar's. */
    val HorizontalInset: Dp = 16.dp

    /** Gap between the trailing X and the Cancel button. */
    val TrailingSpacing: Dp = 8.dp

    // ---- Translucency -------------------------------------------------------

    /**
     * Bar background alpha.
     *
     * **Flush at rest, translucent only when content is actually under it.** At rest the bar has
     * no background at all: the screen's own surface shows through, which is what "flush, no
     * elevation" means. Anything else puts a visible grey rectangle on an empty screen.
     *
     * @param contentScrolledUnder fed by the caller, who already knows it from their scroll
     *   state. Deriving it here would mean the bar owning a nested-scroll connection it has no
     *   business owning.
     */
    fun barAlpha(contentScrolledUnder: Boolean): Float = if (contentScrolledUnder) 1f else 0f

    // ---- Cancel visibility --------------------------------------------------

    /**
     * Whether the Cancel button is shown.
     *
     * Shown on focus, and only if the screen supplied an `onCancel` to call — a Cancel that does
     * nothing is worse than no Cancel.
     *
     * @param focused current field focus, owned by the bar.
     * @param hasCancelHandler whether an `onCancel` callback was supplied.
     */
    fun showCancel(focused: Boolean, hasCancelHandler: Boolean): Boolean = focused && hasCancelHandler

    /**
     * Whether the clear (X) button is shown.
     *
     * Non-empty query only. An X on an empty field is a target that does nothing, and it
     * appears in the one state where the user is least likely to want to dismiss it.
     */
    fun showClear(query: String): Boolean = query.isNotEmpty()
}
