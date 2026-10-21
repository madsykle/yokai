package eu.kanade.tachiyomi.ui.main

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.max

/**
 * Draws a glass capsule behind the bottom navigation items and springs it to the selected tab.
 */
class NavGlassIndicator(private val nav: BottomNavigationView) {

    private val density = nav.resources.displayMetrics.density
    private val itemWidthInset = dp(8f)
    private val minCapsuleWidth = dp(76f)

    private val background = NavGlassBackground(
        nav.background ?: ColorDrawable(Color.TRANSPARENT),
        density,
    )

    private val center = FloatValueHolder(0f)
    private var spring: SpringAnimation? = null

    init {
        nav.background = background
        nav.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                sync(animated = false)
            }
        }
        nav.post {
            attachPressFeedback()
            sync(animated = false)
        }
    }

    /**
     * Moves the capsule to the currently selected item. This is the only entry point for capsule
     * position changes so programmatic and user selections cannot drift apart.
     */
    fun sync(animated: Boolean) {
        if (nav.width <= 0 || nav.height <= 0) return

        val itemCount = nav.menu.size()
        if (itemCount == 0) return

        val selectedId = nav.selectedItemId
        val menuView = nav.getChildAt(0) as? ViewGroup
        val selectedIndex = (0 until itemCount).firstOrNull { nav.menu.getItem(it).itemId == selectedId }
            ?: 0
        val item = menuView?.findViewById<View>(selectedId)
            ?: menuView?.getChildAt(selectedIndex)

        val targetCenter: Float
        val targetWidth: Float
        if (item != null && item.width > 0) {
            val rect = Rect()
            nav.offsetDescendantRectToMyCoords(item, rect)
            targetCenter = rect.exactCenterX()
            targetWidth = max(minCapsuleWidth, rect.width() - itemWidthInset)
        } else {
            val itemWidth = nav.width.toFloat() / itemCount
            targetCenter = (selectedIndex + 0.5f) * itemWidth
            targetWidth = max(minCapsuleWidth, itemWidth - itemWidthInset)
        }

        background.capsuleWidth = targetWidth
        spring?.cancel()

        if (!animated || !nav.isLaidOut) {
            center.value = targetCenter
            background.centerX = targetCenter
            return
        }

        spring = SpringAnimation(center, FloatValueHolder.VALUE).apply {
            spring = SpringForce(targetCenter).apply {
                dampingRatio = 0.8f
                stiffness = 400f
            }
            addUpdateListener { _, value, _ ->
                background.centerX = value
            }
            start()
        }
    }

    private fun attachPressFeedback() {
        for (index in 0 until nav.menu.size()) {
            val itemId = nav.menu.getItem(index).itemId
            nav.findViewById<View>(itemId)?.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        view.animate()
                            .scaleX(0.92f)
                            .scaleY(0.92f)
                            .setDuration(120L)
                            .start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200L)
                            .setInterpolator(OvershootInterpolator(1.15f))
                            .start()
                    }
                }
                false
            }
        }
    }

    private fun dp(value: Float): Float = value * density
}
