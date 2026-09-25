package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import eu.kanade.tachiyomi.R
import kotlin.math.abs
import yokai.presentation.theme.GLASS_TRANSPARENCY_DEFAULT
import yokai.presentation.theme.GLASS_TRANSPARENCY_MAX
import yokai.presentation.theme.GLASS_TRANSPARENCY_MIN
import yokai.presentation.theme.glassTransparencyPercent
import yokai.presentation.theme.setGlassTransparencyPercent

/**
 * iOS 27 transparency slider (DESIGN.md §1.5, §2.2) with a live preview.
 *
 * A plain [SeekBar] row rather than the list-dialog preference it replaces: the value has to
 * be applied *while it is dragged*. Every progress change writes the preference immediately,
 * which is what makes the floating pill, the round search button and the nav rail re-tint
 * under the finger (`MainActivity` listens to the preference; Compose reads it through
 * `glassTintAlphaState`). Nothing recreates the Activity, so the preview is continuous.
 *
 * Persistence is done by hand ([setGlassTransparencyPercent]) instead of by the preference
 * framework so the value lands in the same file the glass surfaces read, and so the value is
 * clamped in one place. `isPersistent = false` keeps the framework from shadowing it with an
 * in-memory copy.
 *
 * The slider is quantised to the six DESIGN.md §2.2 stops at the end of the drag
 * ([STOPS]); during the drag it stays continuous, because a snapping slider fights the
 * preview it is demonstrating.
 */
class GlassTransparencyPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : Preference(context, attrs) {

    init {
        layoutResource = R.layout.preference_glass_transparency
        isPersistent = false
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val titleView = holder.findViewById(R.id.glass_transparency_title) as TextView
        val summaryView = holder.findViewById(R.id.glass_transparency_summary) as TextView
        val seekBar = holder.findViewById(R.id.glass_transparency_seekbar) as SeekBar

        titleView.text = title

        val current = context.glassTransparencyPercent()
        summaryView.text = summaryFor(current)

        // The SeekBar is 0-based; the preference is a percent, so the offset is carried here
        // rather than being baked into the layout.
        seekBar.max = GLASS_TRANSPARENCY_MAX - GLASS_TRANSPARENCY_MIN
        seekBar.progress = current - GLASS_TRANSPARENCY_MIN
        seekBar.contentDescription = title
        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val percent = progress + GLASS_TRANSPARENCY_MIN
                    context.setGlassTransparencyPercent(percent)
                    // Updated in place: notifyChanged() would re-bind the holder and fight the
                    // user's finger.
                    summaryView.text = summaryFor(percent)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            },
        )
    }

    /**
     * The design stops from DESIGN.md §2.2, named so the slider still communicates the
     * ultra-clear → fully-tinted range the spec describes.
     */
    private fun summaryFor(percent: Int): String {
        val stop = STOPS.minByOrNull { abs(it.first - percent) } ?: STOPS.first()
        return "${stop.second} · $percent%"
    }

    private companion object {
        val STOPS = listOf(
            30 to "Ultra clear",
            45 to "Clear",
            60 to "Frosted",
            GLASS_TRANSPARENCY_DEFAULT to "Default",
            85 to "Milky",
            95 to "Fully tinted",
        )
    }
}
