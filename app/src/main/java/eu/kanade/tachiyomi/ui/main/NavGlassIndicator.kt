package eu.kanade.tachiyomi.ui.main

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Draws a glass capsule behind the bottom navigation items and springs it to the selected tab.
 */
class NavGlassIndicator(private val nav: BottomNavigationView) {

    private val density = nav.resources.displayMetrics.density
    private val pillInset = dp(8f)
    private val verticalClearance = dp(3f)
    private val pillSideInset = dp(16f)
    private val minCapsuleWidth = dp(76f)
    private val itemWidthInset = dp(8f)
    private val maxStretch = dp(14f)
    private val stretchDivisor = dp(80f)

    private var baseWidth = minCapsuleWidth

    private val capsule = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        orientation = GradientDrawable.Orientation.TOP_BOTTOM
        colors = intArrayOf(0x38FFFFFF, 0x14FFFFFF)
        setStroke(dp(1f).roundToInt(), 0x1FFFFFFF)
        alpha = 0
    }

    private val layers = LayerDrawable(
        arrayOf(
            nav.background ?: ColorDrawable(Color.TRANSPARENT),
            capsule,
        ),
    )

    private val centerX = FloatValueHolder(0f)
    private var spring: SpringAnimation? = null

    init {
        nav.background = layers
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
        val item = nav.findViewById<View>(nav.selectedItemId) ?: return
        if (nav.width <= 0 || nav.height <= 0 || item.width <= 0) return

        capsule.alpha = 255
        val parentLeft = (item.parent as? View)?.left ?: 0
        val rawCenter = parentLeft + item.left + item.width / 2f
        baseWidth = max(minCapsuleWidth, item.width - itemWidthInset)
        val half = baseWidth / 2f
        val minCenter = pillSideInset + half
        val maxCenter = nav.width - pillSideInset - half
        val target = if (minCenter <= maxCenter) {
            rawCenter.coerceIn(minCenter, maxCenter)
        } else {
            nav.width / 2f
        }

        spring?.cancel()
        if (!animated || !nav.isLaidOut) {
            centerX.value = target
            applyGeometry(target, baseWidth)
            return
        }

        spring = SpringAnimation(centerX).apply {
            spring = SpringForce(target).apply {
                dampingRatio = 0.8f
                stiffness = 400f
            }
            addUpdateListener { _, value, velocity ->
                val stretch = (abs(velocity) / stretchDivisor).coerceIn(0f, maxStretch)
                applyGeometry(value, baseWidth + stretch)
            }
            addEndListener { _, canceled, _, _ ->
                if (!canceled) {
                    applyGeometry(centerX.value, baseWidth)
                }
            }
            start()
        }
    }

    private fun applyGeometry(center: Float, width: Float) {
        if (nav.width <= 0 || nav.height <= 0) return
        val top = (pillInset + verticalClearance).roundToInt().coerceAtLeast(0)
        val bottom = (nav.height - pillInset - verticalClearance).roundToInt().coerceAtLeast(top)
        val half = width / 2f
        val left = (center - half).roundToInt().coerceAtLeast(0)
        val right = (center + half).roundToInt().coerceAtMost(nav.width)
        layers.setLayerInset(1, left, top, nav.width - right, nav.height - bottom)
        capsule.cornerRadius = (bottom - top) / 2f
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
