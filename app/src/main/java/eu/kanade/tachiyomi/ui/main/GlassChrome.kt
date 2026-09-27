package eu.kanade.tachiyomi.ui.main

import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.databinding.MainActivityBinding
import yokai.presentation.theme.GlassPane

/**
 * Phase 2, rebuilt (DESIGN.md §18) - the custom-drawn glass material for the floating chrome.
 *
 * Supersedes `GlassBlurChrome`. That version wrapped a Dimezis `BlurView` behind each surface,
 * which meant a backdrop capture, a RenderScript context on this device (banned by §7.1), a
 * sibling view per surface that had to be kept in sync by hand, and a legibility veil painted
 * over the result. The material is now drawn by the surface's own [GlassPane]: no capture, no
 * extra dependency, no sync problem, and the recipe is the same one the Compose surfaces use.
 *
 * Only the *floating* surfaces are glass: the top card, the bottom nav pill and its circular
 * companion, and the w720dp rail. List items, cards, sheets and the reader page are content, not
 * material, and are deliberately left flat (§1.1).
 *
 * What remains here is only what XML cannot express - the per-surface radius and shape, the
 * transparency-slider repaint, the specular sweep trigger, and mirroring the pill's position.
 */
internal class GlassChrome(private val binding: MainActivityBinding) {

    private data class Pane(val glass: GlassPane, val cornerRadiusDp: Float, val circle: Boolean)

    private val panes: List<Pane> = listOfNotNull(
        binding.cardGlass?.let { Pane(it, TOP_BAR_CORNER_RADIUS_DP, circle = false) },
        // Portrait pill and its w720dp rail counterpart - never both, the layouts are exclusive.
        binding.bottomNavGlass?.let { Pane(it, NAV_CORNER_RADIUS_DP, circle = false) },
        binding.sideNavGlass?.let { Pane(it, NAV_CORNER_RADIUS_DP, circle = false) },
        binding.bottomNavSearchGlass?.let { Pane(it, SEARCH_BUTTON_CORNER_RADIUS_DP, circle = true) },
    )

    /**
     * The pill is a *sibling* of its glass pane (it has to be, or the icons would be clipped by
     * the pane's own outline), so translating or fading the pill does not move the material.
     * Mirroring it every frame is what keeps them glued together while the nav slides away on
     * scroll.
     */
    private val mirror = ViewTreeObserver.OnPreDrawListener {
        binding.bottomNav?.let { pill ->
            binding.bottomNavGlass?.let { glass ->
                glass.translationY = pill.translationY
                glass.alpha = pill.alpha
                if (glass.isVisible != pill.isVisible) glass.isVisible = pill.isVisible
            }
        }
        binding.bottomNavSearch?.let { button ->
            binding.bottomNavSearchGlass?.let { glass ->
                glass.alpha = button.alpha
                if (glass.isVisible != button.isVisible) glass.isVisible = button.isVisible
            }
        }
        true
    }

    init {
        binding.bottomNavGlass?.translationY = binding.bottomNav?.translationY ?: 0f
    }

    /**
     * Applies the shapes and starts mirroring. Safe to call again - the panes rebuild their own
     * material, so a radius change is just a re-assignment.
     */
    fun attach() {
        panes.forEach { pane ->
            pane.glass.circle = pane.circle
            pane.glass.cornerRadiusDp = pane.cornerRadiusDp
            pane.glass.refreshMaterial()
        }
        binding.root.viewTreeObserver.removeOnPreDrawListener(mirror)
        binding.root.viewTreeObserver.addOnPreDrawListener(mirror)
    }

    /**
     * Re-reads the §2.2 transparency for every pane. This is the live-preview path: the slider
     * writes the preference and the Activity calls this without recreating anything.
     */
    fun updateTint() {
        panes.forEach { it.glass.refreshMaterial() }
    }

    /**
     * Fires the one-shot specular sweep across the chrome (DESIGN.md §18): the beat that marks a
     * tab switch or a screen transition. A sweep is a single pass that settles, not a persistent
     * shine.
     */
    fun playSpecularSweep() {
        panes.forEach { it.glass.playSpecularSweep() }
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
