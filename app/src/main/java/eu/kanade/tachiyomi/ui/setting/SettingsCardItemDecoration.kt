package eu.kanade.tachiyomi.ui.setting

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroupAdapter
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.util.system.dpToPx

/**
 * Draws the iOS grouped card behind each contiguous run of settings rows.
 *
 * Category headers are not carded: a card starts at the first row after a header and ends at the
 * last row before the next header (or the end of the list), so screens without any category
 * (such as the main settings screen) end up as one single card.
 *
 * Cards are drawn in [onDraw], so they stay behind the rows, their ripples, and the separators
 * drawn by the preference library in `onDrawOver`.
 */
@SuppressLint("RestrictedApi")
class SettingsCardItemDecoration : RecyclerView.ItemDecoration() {

    // ios_grouped, as a literal: the ios_* colour resources only exist in values-night.
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF1C1C1E.toInt()
    }

    private val cornerRadius = 16.dpToPx.toFloat()
    private val cardBounds = RectF()

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        // Settings search and the debug screens use their own adapters; nothing to draw there.
        val adapter = parent.adapter as? PreferenceGroupAdapter ?: return
        val itemCount = adapter.itemCount
        if (itemCount == 0 || parent.childCount == 0) return

        val left = parent.paddingLeft.toFloat()
        val right = (parent.width - parent.paddingRight).toFloat()
        if (right <= left) return

        var drawnRunStart = -1
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION) continue
            if (isCategory(adapter, position)) continue

            val runStart = findRunStart(adapter, position)
            if (runStart == drawnRunStart) continue
            drawnRunStart = runStart
            val runEnd = findRunEnd(adapter, position, itemCount)

            var top = Float.MAX_VALUE
            var bottom = -Float.MAX_VALUE
            var firstAttached = false
            var lastAttached = false
            for (j in 0 until parent.childCount) {
                val sibling = parent.getChildAt(j)
                val siblingPosition = parent.getChildAdapterPosition(sibling)
                if (siblingPosition !in runStart..runEnd) continue
                top = minOf(top, sibling.top.toFloat())
                bottom = maxOf(bottom, sibling.bottom.toFloat())
                if (siblingPosition == runStart) firstAttached = true
                if (siblingPosition == runEnd) lastAttached = true
            }
            if (top == Float.MAX_VALUE) continue

            // A run that is partially scrolled out keeps drawing past the list edge instead of
            // growing rounded corners in the middle of the screen.
            if (!firstAttached) top = 0f
            if (!lastAttached) bottom = parent.height.toFloat()
            top = maxOf(top, 0f)
            bottom = minOf(bottom, parent.height.toFloat())
            if (bottom - top <= 0f) continue

            cardBounds.set(left, top, right, bottom)
            canvas.drawRoundRect(cardBounds, cornerRadius, cornerRadius, cardPaint)
        }
    }

    private fun isCategory(adapter: PreferenceGroupAdapter, position: Int): Boolean =
        adapter.getItem(position) is PreferenceCategory

    private fun findRunStart(adapter: PreferenceGroupAdapter, position: Int): Int {
        var start = position
        while (start > 0 && !isCategory(adapter, start - 1)) {
            start--
        }
        return start
    }

    private fun findRunEnd(adapter: PreferenceGroupAdapter, position: Int, itemCount: Int): Int {
        var end = position
        while (end + 1 < itemCount && !isCategory(adapter, end + 1)) {
            end++
        }
        return end
    }
}
