package yokai.presentation.theme.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Disclosure chevrons, drawn rather than imported.
 *
 * ## Why not a Material icon
 *
 * The Material default is genuinely available (`compose.bundles.compose` includes `icons`), so
 * this is a choice. `Icons.AutoMirrored.Filled.KeyboardArrowRight` is a **filled** path
 * authored for a 24dp viewport and Material's 48dp row rhythm, with miter joins. It is
 * optically too heavy and too square beside a 44dp row carrying a 15pt
 * (`CupertinoType.subhead`) label.
 *
 * ## Why not SF Symbols
 *
 * `chevron.right` is Apple-licensed — the same constraint that already forbids SF Pro
 * (`DESIGN.md` §7.1). Shipping the glyph would carry identical legal exposure.
 *
 * ## Tune-on-device — these metrics are design intent, not measurements
 *
 * **Stroke weight, cap style, aspect ratio and optical size below are stated intent, not
 * figures taken from a source.** SF Symbols was not available to inspect, and Apple's HIG is
 * JS-rendered — the same fetch that returned zero hex literals last session returns zero path
 * data. So nothing here should be read as citing Apple's actual vector.
 *
 * What *is* defensible: this is a **stroke**, not a fill, because the Material glyph's problem
 * is its weight; caps and joins are **round**, because a miter corner on a 7dp chevron spikes
 * visibly at this size; and the aspect is **tall** (7:12), because the chevron must read as a
 * direction indicator at the optical centre of a text row rather than as a small arrow.
 *
 * **Device-verify:** stroke weight, cap style and aspect ratio against a real settings list
 * before this ships to a screen.
 */
object CupertinoChevrons {

    /** Optical width of [Right]. Tall and narrow, so it reads as a direction, not an arrow. */
    val ChevronWidth: Dp = 7.dp

    /** Optical height of [Right]. */
    val ChevronHeight: Dp = 12.dp

    /** Stroke weight. See the tune-on-device note above. */
    val ChevronStrokeWidth: Dp = 1.5.dp

    /** Viewport padding so the round caps are not clipped. */
    private const val StrokeInset = 0.75f

    /**
     * Disclosure chevron, leading-to-trailing.
     *
     * Auto-mirrored, so it points the right way in RTL without the caller doing anything.
     */
    val Right: ImageVector by lazy {
        ImageVector.Builder(
            name = "Cupertino.ChevronRight",
            defaultWidth = ChevronWidth,
            defaultHeight = ChevronHeight,
            viewportWidth = ChevronWidth.value,
            viewportHeight = ChevronHeight.value,
            // autoMirror is a Builder constructor parameter, not a chainable method:
            // ImageVector.kt:119 declares `private val autoMirror: Boolean = false` as the
            // last ctor arg of the secondary constructor (which is the primary public one
            // now; the old signature is DeprecationLevel.HIDDEN). There is no
            // autoMirrored() function to call on the built vector.
            autoMirror = true,
        )
            .apply {
                path(
                    // Black is the conventional ImageVector placeholder, not the rendered
                    // colour: the call site applies a ColorFilter tint with the label colour.
                    stroke = SolidColor(Color(0xFF000000)),
                    fill = null,
                    strokeLineWidth = ChevronStrokeWidth.value,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 4f,
                ) {
                    // Two segments forming a ">". Inset from the viewport so the round caps
                    // are not clipped by the ImageVector bounds.
                    moveTo(StrokeInset, StrokeInset)
                    lineTo(ChevronWidth.value - StrokeInset, ChevronHeight.value / 2f)
                    lineTo(StrokeInset, ChevronHeight.value - StrokeInset)
                }
            }
            .build()
    }
}