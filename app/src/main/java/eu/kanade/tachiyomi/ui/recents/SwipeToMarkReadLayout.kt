package eu.kanade.tachiyomi.ui.recents

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
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
 * width. Taps and vertical drags are never intercepted, so children keep their clicks and the
 * LazyColumn keeps the scroll.
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

    private var downX = 0f
    private var downY = 0f
    private var dragging = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Never intercept the down: the row's own listeners (which flip the read icon)
                // and every child button need it, and a tap must never be stolen.
                downX = ev.x
                downY = ev.y
                dragging = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging) {
                    val dx = ev.x - downX
                    val dy = ev.y - downY
                    // Horizontal intent only, and only past slop. Everything else is left to the
                    // children and to the LazyColumn's vertical scroll. Returning true here hands
                    // the rest of the gesture to onTouchEvent and auto-cancels the child.
                    if (abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                        dragging = true
                        endView?.isVisible = true
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> dragging = false
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val front = frontView
        if (front == null) return super.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                front.translationX = ev.x - downX
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val offset = front.translationX
                if (abs(offset) >= swipeThreshold(front)) {
                    onSwipePastThreshold?.invoke()
                }
                // Snap home either way: on a successful swipe the row re-binds its read state, and
                // on a failed one nothing changed.
                front.animate()
                    .translationX(0f)
                    .setDuration(SETTLE_DURATION_MS)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
                endView?.isInvisible = true
                dragging = false
                return true
            }
        }
        return super.onTouchEvent(ev)
    }

    private fun swipeThreshold(front: View): Float =
        if (front.width > 0) front.width * SWIPE_THRESHOLD_FRACTION else touchSlop * 4f

    private companion object {
        /** Fraction of the row width a drag must pass, matching FlexibleAdapter's default. */
        const val SWIPE_THRESHOLD_FRACTION = 0.5f

        const val SETTLE_DURATION_MS = 150L
    }
}
