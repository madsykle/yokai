package eu.kanade.tachiyomi.ui.recents

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.isLTR

class RecentMangaDivider(context: Context) : RecyclerView.ItemDecoration() {

    private val divider: Drawable = ColorDrawable(context.getResourceColor(R.attr.colorOutline))
    private val dividerHeight = 1.dpToPx
    private val dividerInset = 84.dpToPx

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        for (i in 0 until childCount - 1) {
            val child = parent.getChildAt(i)
            val holder = parent.getChildViewHolder(child)
            if (holder is RecentMangaHolder &&
                parent.getChildViewHolder(parent.getChildAt(i + 1)) is RecentMangaHolder
            ) {
                val params = child.layoutParams as RecyclerView.LayoutParams
                val top = child.bottom + params.bottomMargin
                val bottom = top + dividerHeight
                val left = parent.paddingStart + if (parent.context.resources.isLTR) dividerInset else 0
                val right =
                    parent.width - parent.paddingEnd - if (!parent.context.resources.isLTR) dividerInset else 0

                divider.setBounds(left, top, right, bottom)
                divider.draw(c)
            }
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        outRect.set(0, 0, 0, dividerHeight)
    }
}
