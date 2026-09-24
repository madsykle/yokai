package eu.kanade.tachiyomi.ui.reader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import yokai.util.lang.getString
import eu.kanade.tachiyomi.util.system.springFadeIn
import eu.kanade.tachiyomi.util.system.springFadeOut
import kotlin.math.abs

private object ViewPropertyAnimatorHolderMarker

class ReaderNavigationOverlayView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    // Guards against double-triggering fade animations; the underlying
    // animations are springs from GlassMotion (§4.5).
    private var viewPropertyAnimator: Any? = null

    private var navigation: ViewerNavigation? = null

    var isLTR = true

    fun setNavigation(navigation: ViewerNavigation, showOnStart: Boolean) {
        if (!showOnStart && (this.navigation == null || this.navigation === navigation)) {
            if (this.navigation == null) {
                this.navigation = navigation
                isVisible = false
            }
            return
        }

        this.navigation = navigation
        showNavigationAgain()
    }

    fun showNavigationAgain() {
        invalidate()

        if (isVisible || navigation is DisabledNavigation) return

        // DESIGN.md §5.4: overlay fades with opacity (never slide);
        // §4.5: springs only — no tween().
        // NOTE: no glass background here — this view is FULL-SCREEN over the manga
        // page, and §1.1 forbids glass on content. The tint painted the page over.
        viewPropertyAnimator = ViewPropertyAnimatorHolderMarker
        springFadeIn {
            viewPropertyAnimator = null
        }
    }

    private val regionPaint = Paint()

    private val textPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
        textSize = 64f
    }

    private val textBorderPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        color = Color.BLACK
        textSize = 64f
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    override fun onDraw(canvas: Canvas) {
        if (navigation == null) return

        navigation?.regions?.forEach {
            val region = it.invert(navigation!!.invertMode)
            val rect = region.rectF

            canvas.save()

            // Scale rect from 1f,1f to screen width and height
            canvas.scale(width.toFloat(), height.toFloat())
            val directionalRegion = region.type.directionalRegion(isLTR)
            regionPaint.color = ContextCompat.getColor(context, directionalRegion.colorRes)
            canvas.drawRect(rect, regionPaint)

            canvas.restore()
            // Don't want scale anymore because it messes with drawText
            canvas.save()

            // Translate origin to rect start (left, top)
            canvas.translate((width * rect.left), (height * rect.top))

            // Calculate center of rect width on screen
            val x = width * (abs(rect.left - rect.right) / 2)

            // Calculate center of rect height on screen
            val y = height * (abs(rect.top - rect.bottom) / 2)

            canvas.drawText(context.getString(directionalRegion.nameRes), x, y, textBorderPaint)
            canvas.drawText(context.getString(directionalRegion.nameRes), x, y, textPaint)

            canvas.restore()
        }
    }

    override fun performClick(): Boolean {
        super.performClick()

        if (viewPropertyAnimator == null && isVisible) {
            springFadeOut { completed ->
                if (completed) {
                    isVisible = false
                }
                viewPropertyAnimator = null
            }
            viewPropertyAnimator = ViewPropertyAnimatorHolderMarker
        }

        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Hide overlay if user start tapping or swiping
        performClick()
        return super.onTouchEvent(event)
    }
}
