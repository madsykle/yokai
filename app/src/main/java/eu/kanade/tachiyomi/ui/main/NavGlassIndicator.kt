package eu.kanade.tachiyomi.ui.main

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import com.google.android.material.bottomnavigation.BottomNavigationView
import eu.kanade.tachiyomi.R

/**
 * Draws a glass capsule over the bottom navigation items and moves it to the selected tab.
 */
class NavGlassIndicator(private val nav: BottomNavigationView) {

    private val density = nav.resources.displayMetrics.density
    private val capsule = CapsuleDrawable(density).apply {
        itemCount = nav.menu.size()
    }
    private var animator: ValueAnimator? = null
    private var hasSynced = false

    init {
        nav.setBackgroundResource(R.drawable.nav_glass_bg)
        nav.overlay.add(capsule)
        nav.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            capsule.setBounds(0, 0, nav.width, nav.height)
            sync(animated = false)
        }
        nav.post {
            capsule.setBounds(0, 0, nav.width, nav.height)
            attachPressFeedback()
            sync(animated = false)
        }
    }

    /**
     * Moves the capsule to the currently selected item.
     */
    fun sync(animated: Boolean) {
        val itemCount = nav.menu.size()
        if (itemCount == 0) return

        val selectedId = nav.selectedItemId
        var index = 0
        for (i in 0 until itemCount) {
            if (nav.menu.getItem(i).itemId == selectedId) {
                index = i
                break
            }
        }

        val target = (index + 0.5f) * nav.width / itemCount
        Log.d("NavGlass", "sel=$index target=$target w=${nav.width}")

        capsule.itemCount = itemCount
        if (!animated || nav.width == 0 || !hasSynced) {
            animator?.cancel()
            capsule.centerX = target
            hasSynced = true
            return
        }

        animator?.cancel()
        animator = ValueAnimator.ofFloat(capsule.centerX, target).apply {
            duration = 380L
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener { capsule.centerX = it.animatedValue as Float }
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

    private class CapsuleDrawable(private val density: Float) : Drawable() {

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x1FFFFFFF
            style = Paint.Style.FILL
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x26FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = (0.75f * density).coerceAtLeast(1f)
        }

        private var alpha = 255

        var centerX: Float = 0f
            set(value) {
                if (field != value) {
                    field = value
                    invalidateSelf()
                }
            }

        var itemCount: Int = 0
            set(value) {
                if (field != value) {
                    field = value
                    invalidateSelf()
                }
            }

        private val capsuleInset = 8f * density
        private val verticalInset = 11f * density

        override fun onBoundsChange(bounds: Rect) {
            invalidateSelf()
        }

        override fun draw(canvas: Canvas) {
            if (itemCount <= 0 || bounds.width() <= 0 || bounds.height() <= 0) return

            val itemWidth = bounds.width() / itemCount.toFloat()
            val capsuleWidth = itemWidth - capsuleInset
            val top = bounds.top + verticalInset
            val bottom = bounds.bottom - verticalInset
            if (capsuleWidth <= 0f || bottom <= top) return

            val left = centerX - capsuleWidth / 2f
            val radius = (bottom - top) / 2f
            fillPaint.alpha = (0x1F * alpha) / 255
            strokePaint.alpha = (0x26 * alpha) / 255
            canvas.drawRoundRect(left, top, left + capsuleWidth, bottom, radius, radius, fillPaint)
            canvas.drawRoundRect(left, top, left + capsuleWidth, bottom, radius, radius, strokePaint)
        }

        override fun setAlpha(alpha: Int) {
            this.alpha = alpha
            invalidateSelf()
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            fillPaint.colorFilter = colorFilter
            strokePaint.colorFilter = colorFilter
            invalidateSelf()
        }

        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
