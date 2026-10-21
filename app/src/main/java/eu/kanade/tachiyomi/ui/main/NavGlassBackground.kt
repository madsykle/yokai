package eu.kanade.tachiyomi.ui.main

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import kotlin.math.roundToInt

/**
 * Draws the untouched nav background followed by a capsule centered at [centerX].
 */
class NavGlassBackground(
    private val base: Drawable,
    density: Float,
) : Drawable() {

    private val verticalInset = (11f * density).roundToInt()
    private val strokeWidth = (0.75f * density).roundToInt().coerceAtLeast(1)

    private val capsule = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(0x2EFFFFFF)
        setStroke(strokeWidth, 0x26FFFFFF)
    }

    var centerX: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidateSelf()
            }
        }

    var capsuleWidth: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidateSelf()
            }
        }

    override fun draw(canvas: Canvas) {
        base.bounds = bounds
        base.draw(canvas)

        if (capsuleWidth <= 0f || bounds.height() <= verticalInset * 2) return

        val top = bounds.top + verticalInset
        val bottom = bounds.bottom - verticalInset
        val half = capsuleWidth / 2f
        capsule.cornerRadius = (bottom - top) / 2f
        capsule.setBounds(
            (centerX - half).roundToInt(),
            top,
            (centerX + half).roundToInt(),
            bottom,
        )
        capsule.draw(canvas)
    }

    override fun onBoundsChange(bounds: Rect) {
        base.bounds = bounds
    }

    override fun setAlpha(alpha: Int) {
        base.alpha = alpha
        capsule.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        base.colorFilter = colorFilter
        capsule.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
