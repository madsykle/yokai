package eu.kanade.tachiyomi.ui.main

import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.navigation.NavigationBarView

/**
 * Phase 3 (DESIGN.md §17): the morphing tab indicator.
 *
 * A single pill ([R.id.nav_tab_indicator]) that springs between the nav's items with
 * [SpringAnimation] instead of Material's static per-item indicator (emptied out for this nav by
 * `Widget.Tachiyomi.EmptyIndicator`). While the spring is fast the pill squash-stretches along
 * its travel and squashes across it (§17: scaleX → 1.15, scaleY → 0.9 at peak velocity); at rest
 * it relaxes back to an exact capsule, because the deformation is a pure function of the
 * *current* spring velocity ([MorphingNavIndicator.stretchScalesFor]).
 *
 * Pure geometry only: it reads the menu order, the nav's laid-out width and the spring state.
 * Material (blur, tint, tiers) deliberately plays no part. Works for any [NavigationBarView];
 * the w720dp rail keeps its static Material indicator, which reads better on a vertical menu.
 *
 * The spring drives a [FloatValueHolder], not the view: `skipToEnd()` is a no-op on a
 * non-running spring and a running one only *requests* an end, so the holder is the single
 * source of truth for the animated value. Placing the pill without animation means writing the
 * holder AND the view - otherwise the next spring would take off from a stale position.
 */
internal class MorphingNavIndicatorController(
    private val nav: NavigationBarView,
    private val indicator: View,
) {

    private val valueHolder = FloatValueHolder(0f)

    private val damping = SpringForce().apply {
        // §17: visibly springy (an overshoot you can see), not wobbly. 0.6 sits in the middle of
        // the 0.55-0.65 band; STIFFNESS_MEDIUM settles in roughly 300-400ms.
        dampingRatio = SPRING_DAMPING_RATIO
        stiffness = SPRING_STIFFNESS
    }

    private val spring = SpringAnimation(valueHolder)
        .setSpring(damping)
        .addUpdateListener { _, value, velocity ->
            indicator.translationX = value
            val (scaleX, scaleY) = MorphingNavIndicator.stretchScalesFor(velocity)
            indicator.scaleX = scaleX
            indicator.scaleY = scaleY
        }

    /** Item index the spring is heading to (or resting on) - not the checked item mid-flight. */
    private var targetIndex = 0
    private var itemCount = 0

    /** False until the pill has been placed once, so a cold start snaps instead of animating. */
    private var hasBeenPlaced = false

    /**
     * Re-seats the pill whenever the nav re-lays-out (first layout, rotation, inset changes) -
     * otherwise a width change would leave it floating over the wrong item.
     */
    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        onNavLaidOut()
    }

    /**
     * Keeps the pill glued to the nav every frame: hide-on-scroll animates the nav's
     * translationY and the pushed-controller flow animates its alpha, and a sibling does not
     * follow either on its own. Same pattern the §16 blur pane uses for the same reason.
     */
    private val mirror = ViewTreeObserver.OnPreDrawListener {
        indicator.translationY = nav.translationY
        indicator.alpha = nav.alpha
        if (indicator.isVisible != nav.isVisible) indicator.isVisible = nav.isVisible
        true
    }

    init {
        nav.addOnLayoutChangeListener(layoutListener)
        nav.viewTreeObserver.addOnPreDrawListener(mirror)
    }

    /**
     * Springs the pill to the tapped item. Called from the nav's item-selected listener with the
     * clicked item's id - deliberately NOT from `selectedItemId`, whose checked state can lag or
     * lead the listener depending on why the selection changed.
     */
    fun onItemSelected(itemId: Int) {
        val index = indexOfItem(itemId)
        if (index < 0) return
        targetIndex = index
        itemCount = nav.menu.size()
        if (!MorphingNavIndicator.isLayoutUsable(nav.width) || itemCount == 0) return
        val target = placeTarget(index)
        if (hasBeenPlaced) {
            // Running or not, this sets the rest position and starts/integrates the spring
            // continuously - retargeting mid-flight keeps the current velocity.
            spring.animateToFinalPosition(target)
            hasBeenPlaced = true
        } else {
            placeWithoutAnimation(target)
        }
    }

    /** Adopts the nav's current selection without animating (activity recreate, theme change). */
    fun snapToSelection() {
        val id = nav.selectedItemId
        val index = indexOfItem(id)
        if (index < 0) return
        targetIndex = index
        itemCount = nav.menu.size()
        if (!MorphingNavIndicator.isLayoutUsable(nav.width) || itemCount == 0) return
        placeWithoutAnimation(placeTarget(index))
    }

    /** Re-seats the pill after a layout pass changed the nav's geometry. */
    private fun onNavLaidOut() {
        if (!MorphingNavIndicator.isLayoutUsable(nav.width) || itemCount == 0) return
        val target = placeTarget(targetIndex)
        if (spring.isRunning) {
            spring.animateToFinalPosition(target)
        } else {
            placeWithoutAnimation(target)
        }
    }

    private fun placeTarget(index: Int): Float =
        MorphingNavIndicator.translationXFor(index, itemCount, nav.width, indicator.width)

    private fun placeWithoutAnimation(target: Float) {
        spring.cancel()
        valueHolder.setValue(target)
        indicator.translationX = target
        indicator.scaleX = 1f
        indicator.scaleY = 1f
        hasBeenPlaced = true
    }

    private fun indexOfItem(itemId: Int): Int {
        val menu = nav.menu
        for (index in 0 until menu.size()) {
            if (menu.getItem(index).itemId == itemId) return index
        }
        return -1
    }

    /** Teardown: end the spring and drop both listeners. */
    fun detach() {
        spring.cancel()
        nav.removeOnLayoutChangeListener(layoutListener)
        nav.viewTreeObserver.removeOnPreDrawListener(mirror)
    }

    private companion object {
        const val SPRING_DAMPING_RATIO = 0.6f
        const val SPRING_STIFFNESS = SpringForce.STIFFNESS_MEDIUM
    }
}
