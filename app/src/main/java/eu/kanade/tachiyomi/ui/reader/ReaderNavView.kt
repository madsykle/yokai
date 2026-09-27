package eu.kanade.tachiyomi.ui.reader

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import eu.kanade.tachiyomi.R
import yokai.presentation.theme.GlassPane

class ReaderNavView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ConstraintLayout(context, attrs) {

    /**
     * DESIGN.md §18: the page-slider control is a functional layer over the page, so its material
     * is the shared [GlassPane] rather than the flat fill the layout used to carry.
     *
     * The capsule radius is half the bar's height, which is not known until it has been measured -
     * hence the layout pass rather than a constant in the constructor.
     */
    private val glassPane: GlassPane?
        get() = findViewById(R.id.reader_nav_glass)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val pane = glassPane ?: return
        val radiusDp = height / 2f / resources.displayMetrics.density
        if (pane.cornerRadiusDp != radiusDp) pane.cornerRadiusDp = radiusDp
    }

    override fun canScrollVertically(direction: Int): Boolean {
        return true
    }

    override fun shouldDelayChildPressedState(): Boolean {
        return true
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun getScrollCaptureHint(): Int {
        return SCROLL_CAPTURE_HINT_EXCLUDE
    }
}
