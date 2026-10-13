package eu.kanade.tachiyomi.ui.recents

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import kotlin.math.abs

/**
 * Swipe-to-mark-read driver for a single Recents row.
 *
 * A View-side driver, not a Compose `pointerInput`. The row is still the inflated
 * [RecentMangaHolder] inside an `AndroidView`, and a Compose gesture here would fight both the
 * LazyColumn's vertical scroll and the row's own buttons.
 *
 * This is the contract FlexibleAdapter's `ItemTouchHelperCallback` used to implement, minus the
 * RecyclerView: claim the stream only once a horizontal drag passes touch slop, slide the row
 * content, and on release report the swipe if it passed [SWIPE_THRESHOLD_FRACTION] of the row's
 * width — or if it was a fast flick past [escapeVelocityPx], which is how a short quick swipe
 * registered before B1c and what a distance-only threshold silently dropped. Taps and vertical
 * drags are never intercepted, so children keep their clicks and the LazyColumn keeps the scroll.
 */
class SwipeToMarkReadLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    /** Invoked on release once the drag passed the threshold. Wired by the row factory. */
    var onSwipePastThreshold: (() -> Unit)? = null

    /** The row content that slides (`R.id.front_view`). Wired by the row factory. */
    var frontView: View? = null

    /** The reveal behind the row (`R.id.end_view`). Wired by the row factory. */
    var endView: View? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    /**
     * The fling velocity, in px/s, past which a swipe is a decision even though the finger never
     * travelled [SWIPE_THRESHOLD_FRACTION] of the row.
     *
     * 120dp is `dimen/item_touch_helper_swipe_escape_velocity` from recyclerview — the number
     * `ItemTouchHelper`, and therefore FlexibleAdapter's old callback, swiped away on. Read from
     * the recyclerview 1.2.1 values.xml in the local Gradle cache, not from memory.
     */
    private val escapeVelocityPx = 120f * context.resources.displayMetrics.density

    private var downX = 0f
    private var downY = 0f
    private var dragging = false

    private var velocityTracker: VelocityTracker? = null

    /**
     * The row's translation for the frame that has not drawn yet.
     *
     * A fast swipe delivers several `ACTION_MOVE`s per frame, and writing `translationX` once per
     * event makes the row outrun the compositor — the stutter that read as "glitchy". The latest
     * value is applied at most once per frame through the view's own animation callback queue
     * instead, so the row lands on the frame boundary with the newest position.
     */
    private var pendingTranslationX = 0f
    private var frameScheduled = false
    private val applyTranslation = Runnable {
        frameScheduled = false
        frontView?.translationX = pendingTranslationX
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Never intercept the down: the row's own listeners (which flip the read icon)
                // and every child button need it, and a tap must never be stolen.
                downX = ev.x
                downY = ev.y
                dragging = false
                pendingTranslationX = 0f
                // A snap-home animation left over from the previous gesture keeps writing
                // `translationX` under this one, so the row would not follow the finger. Dock it to
                // the animation's own end state — a tap that never moves must not leave the row
                // offset either, which would also hand the shared-element transition a shifted
                // snapshot.
                frontView?.let {
                    it.animate().cancel()
                    it.translationX = 0f
                }
                endView?.isInvisible = true
                recycleVelocityTracker()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(ev) }
            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(ev)
                if (!dragging) {
                    // Cumulative since the down, not per-event: a fast flick can open with a
                    // diagonal first frame, and a per-event delta would never re-measure it.
                    val dx = ev.x - downX
                    val dy = ev.y - downY
                    if (abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                        dragging = true
                        endView?.isVisible = true
                        // Hold the gesture against any ancestor that would rather scroll: the
                        // child has just been cancelled, and without this a parent that starts
                        // its own drag mid-swipe takes the rest of the stream.
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                dragging = false
                recycleVelocityTracker()
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val front = frontView
        if (front == null) return super.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(ev)
                pendingTranslationX = ev.x - downX
                if (!frameScheduled) {
                    frameScheduled = true
                    postOnAnimation(applyTranslation)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                settle(front, ev)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                // A cancel is not a decision by the user: settle back without marking anything.
                settle(front, null)
                return true
            }
        }
        return super.onTouchEvent(ev)
    }

    /** Docks the row at its translated position and animates it home, optionally reporting a swipe. */
    private fun settle(front: View, up: MotionEvent?) {
        removeCallbacks(applyTranslation)
        frameScheduled = false
        // The pending value is the newest one, and may be a frame ahead of `front.translationX`.
        val travelled = pendingTranslationX
        if (up != null) {
            velocityTracker?.addMovement(up)
            velocityTracker?.computeCurrentVelocity(VELOCITY_UNITS)
            val velocityX = velocityTracker?.xVelocity ?: 0f
            val flicked = abs(velocityX) >= escapeVelocityPx
            if (travelled != 0f && (abs(travelled) >= swipeThreshold(front) || flicked)) {
                onSwipePastThreshold?.invoke()
            }
        }
        recycleVelocityTracker()
        parent?.requestDisallowInterceptTouchEvent(false)
        front.translationX = travelled
        front.animate()
            .translationX(0f)
            .setDuration(SETTLE_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
        pendingTranslationX = 0f
        endView?.isInvisible = true
        dragging = false
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun swipeThreshold(front: View): Float =
        if (front.width > 0) front.width * SWIPE_THRESHOLD_FRACTION else touchSlop * 4f

    override fun onDetachedFromWindow() {
        removeCallbacks(applyTranslation)
        frameScheduled = false
        recycleVelocityTracker()
        super.onDetachedFromWindow()
    }

    private companion object {
        /** Fraction of the row width a drag must pass, matching FlexibleAdapter's default. */
        const val SWIPE_THRESHOLD_FRACTION = 0.5f

        const val SETTLE_DURATION_MS = 150L

        /** Units for [VelocityTracker.computeCurrentVelocity]: pixels per second. */
        const val VELOCITY_UNITS = 1000
    }
}
