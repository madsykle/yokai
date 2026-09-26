package eu.kanade.tachiyomi.ui.main

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import eightbitlab.com.blurview.BlurView
import eu.kanade.tachiyomi.databinding.MainActivityBinding
import yokai.presentation.theme.applyGlassBackdropPane
import yokai.presentation.theme.applyGlassDecorators
import yokai.presentation.theme.glassBlurTintColor

/**
 * Phase 2 (DESIGN.md §16) - real backdrop blur for the floating chrome.
 *
 * Only the *floating* surfaces get a [BlurView]: the top search card, the bottom nav pill and its
 * circular companion. List items, cards, sheets and the reader page are deliberately left flat -
 * they are content, not material, and blurring them is both wrong and expensive.
 *
 * The blur is set up through `setupWith(root)`, the library's convenience overload that picks
 * `RenderEffectBlur` on API 31+ and `RenderScriptBlur` below, so the material upgrades itself on
 * newer devices instead of one algorithm being hardcoded. The downsample factor is left at the
 * library default (6): the brief is explicit that a Snapdragon 720G buys frames by raising that
 * factor, never by pretending a lower one is "more blur".
 *
 * The BlurView samples [androidx.constraintlayout.widget.ConstraintLayout]-hosted
 * `controller_container`, not the whole window, for two reasons: the pill's own icons and the
 * top card's text must never be blurred into their own backdrop, and the smaller snapshot is
 * cheaper.
 */
internal class GlassBlurChrome(private val binding: MainActivityBinding) {

    private data class Pane(val blur: BlurView, val cornerRadiusDp: Float, val circle: Boolean)

    private val panes: List<Pane> = listOfNotNull(
        binding.cardBlur?.let { Pane(it, TOP_BAR_CORNER_RADIUS_DP, circle = false) },
        // Portrait pill and its w720dp rail counterpart - never both, the layouts are exclusive.
        binding.bottomNavBlur?.let { Pane(it, NAV_CORNER_RADIUS_DP, circle = false) },
        binding.sideNavBlur?.let { Pane(it, NAV_CORNER_RADIUS_DP, circle = false) },
        binding.bottomNavSearchBlur?.let { Pane(it, SEARCH_BUTTON_CORNER_RADIUS_DP, circle = true) },
    )

    /**
     * The pill is a *sibling* of its BlurView (it has to be, or the pill's own icons would be
     * sampled and appear smeared behind the sharp ones), so translating or fading the pill does
     * not move the material. Mirroring it every frame is what keeps them glued together while
     * the nav slides away on scroll.
     */
    private val mirror = ViewTreeObserver.OnPreDrawListener {
        binding.bottomNav?.let { pill ->
            binding.bottomNavBlur?.let { blur ->
                blur.translationY = pill.translationY
                blur.alpha = pill.alpha
                if (blur.isVisible != pill.isVisible) blur.isVisible = pill.isVisible
            }
        }
        binding.bottomNavSearch?.let { button ->
            binding.bottomNavSearchBlur?.let { blur ->
                blur.alpha = button.alpha
                if (blur.isVisible != button.isVisible) blur.isVisible = button.isVisible
            }
        }
        true
    }

    /**
     * Builds the material. Safe to call again with a new [blurRadiusPx] (a live preview, a
     * configuration change) - `setupWith` replaces the previous controller, which is also what
     * releases the RenderScript context.
     */
    fun attach(root: ViewGroup, windowBackground: Drawable?, blurRadiusPx: Float) {
        if (panes.isEmpty()) return
        panes.forEach { pane ->
            pane.blur.setupWith(root)
                // The controller container is mostly transparent, and a transparent snapshot
                // would blur to a washed-out veil; clearing each frame with the window
                // background (the `?background` the app actually draws) keeps it opaque.
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(blurRadiusPx)
            pane.blur.setOverlayColor(glassBlurTintColor(binding.root.context))
            // The BlurView IS the surface now: a transparent rounded pane (so the blur is what
            // shows), clipped to the shape, with only the §2.2 rim and sheen painted over it.
            pane.blur.applyGlassBackdropPane(pane.cornerRadiusDp, circle = pane.circle)
            pane.blur.applyGlassDecorators(pane.cornerRadiusDp, circle = pane.circle)
        }
        binding.root.viewTreeObserver.removeOnPreDrawListener(mirror)
        binding.root.viewTreeObserver.addOnPreDrawListener(mirror)
    }

    /**
     * Re-reads the §2.2 transparency for the veil over the blur. This is the live-preview path:
     * the slider writes the preference and the Activity calls this without recreating anything.
     */
    fun updateTint() {
        val tint = glassBlurTintColor(binding.root.context)
        panes.forEach { it.blur.setOverlayColor(tint) }
    }

    fun detach() {
        binding.root.viewTreeObserver.removeOnPreDrawListener(mirror)
    }

    private companion object {
        const val TOP_BAR_CORNER_RADIUS_DP = 24f
        const val NAV_CORNER_RADIUS_DP = 28f
        const val SEARCH_BUTTON_CORNER_RADIUS_DP = 24f
    }
}
