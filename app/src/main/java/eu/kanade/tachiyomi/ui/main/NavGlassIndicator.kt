package eu.kanade.tachiyomi.ui.main

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.roundToInt

/**
 * Draws a glass capsule behind the bottom navigation items and springs it to the selected tab.
 */
class NavGlassIndicator(private val nav: BottomNavigationView) {

    private val density = nav.resources.displayMetrics.density
    private val capsuleWidth = dp(76f)
    private val pillInset = dp(8f)
    private val capsuleInset = dp(4f)
    private val pillSideInset = dp(16f)

    private val capsule = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(0x24FFFFFF)
        setStroke(dp(1f).roundToInt(), 0x26FFFFFF)
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
    private var pendingItemId = View.NO_ID

    init {
        nav.background = layers
        nav.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                snapTo(pendingItemId.takeIf { it != View.NO_ID } ?: nav.selectedItemId)
            }
        }
        nav.post {
            snapTo(pendingItemId.takeIf { it != View.NO_ID } ?: nav.selectedItemId)
        }
    }

    fun moveTo(itemId: Int) {
        pendingItemId = itemId
        val target = targetCenter(itemId) ?: return
        capsule.alpha = 255
        spring?.cancel()
        spring = SpringAnimation(centerX).apply {
            spring = SpringForce(target).apply {
                dampingRatio = 0.8f
                stiffness = 400f
            }
            addUpdateListener { _, value, _ -> applyCenter(value) }
            start()
        }
    }

    fun snapTo(itemId: Int) {
        pendingItemId = itemId
        val target = targetCenter(itemId) ?: return
        capsule.alpha = 255
        spring?.cancel()
        centerX.value = target
        applyCenter(target)
    }

    private fun targetCenter(itemId: Int): Float? {
        if (nav.width <= 0 || nav.height <= 0) return null
        val item = nav.findViewById<View>(itemId) ?: return null
        if (item.width <= 0) return null
        val parentLeft = (item.parent as? View)?.left ?: 0
        val rawCenter = parentLeft + item.left + item.width / 2f
        val half = capsuleWidth / 2f
        val min = pillSideInset + capsuleInset + half
        val max = nav.width - pillSideInset - capsuleInset - half
        return if (min <= max) rawCenter.coerceIn(min, max) else nav.width / 2f
    }

    private fun applyCenter(value: Float) {
        if (nav.width <= 0 || nav.height <= 0) return
        val top = pillInset + capsuleInset
        val bottom = nav.height - pillInset - capsuleInset
        val half = capsuleWidth / 2f
        val left = (value - half).roundToInt().coerceAtLeast(0)
        val right = (value + half).roundToInt().coerceAtMost(nav.width)
        val safeTop = top.roundToInt().coerceAtLeast(0)
        val safeBottom = bottom.roundToInt().coerceAtLeast(safeTop)
        layers.setLayerInset(1, left, safeTop, nav.width - right, nav.height - safeBottom)
        capsule.cornerRadius = (safeBottom - safeTop) / 2f
    }

    private fun dp(value: Float): Float = value * density
}
