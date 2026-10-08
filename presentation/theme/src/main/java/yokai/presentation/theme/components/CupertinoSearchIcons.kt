package yokai.presentation.theme.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Stroked glyphs for [CupertinoSearchBar].
 *
 * Hand-drawn for the same reason as [CupertinoChevron]: SF Symbols is Apple-licensed and blocked
 * by `DESIGN.md` §7.1, and Material's `Icons.Filled.Close` is a *filled* 24dp path whose weight
 * is tuned for a full-screen app bar, not a 17pt field 36dp tall. A filled X beside 17pt text is
 * visibly heavier than the text it sits next to.
 *
 * **NEEDS DEVICE VERIFY** — stroke weight and cap style are stated intent, not measurements, for
 * the same reason the chevron's are. File it; do not block on it.
 */
object CupertinoSearchIcons {

    /** Magnifier, shown while the field is empty. 14x14dp. */
    val MagnifierWidth = 14.dp
    val MagnifierHeight = 14.dp
    private const val MagnifierStroke = 1.5f
    private const val LensRadius = 4.2f

    /** Clear (X). 12x12dp, deliberately smaller than the magnifier it replaces. */
    val ClearWidth = 12.dp
    val ClearHeight = 12.dp
    private const val ClearStroke = 1.5f

    /** Half the stroke width, so round caps are not clipped by the viewport edge. */
    private const val Inset = 0.75f

    /**
     * Lens plus handle.
     *
     * The handle is a separate segment rather than part of a filled outline, so the whole glyph
     * is one stroke weight.
     */
    val Magnifier: ImageVector by lazy {
        ImageVector.Builder(
            name = "Cupertino.Magnifier",
            defaultWidth = MagnifierWidth,
            defaultHeight = MagnifierHeight,
            viewportWidth = MagnifierWidth.value,
            viewportHeight = MagnifierHeight.value,
        )
            .apply {
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    fill = null,
                    strokeLineWidth = MagnifierStroke,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 4f,
                ) {
                    // Circle: 16 short segments is plenty at 14dp; more is wasted, not smoother.
                    val cx = Inset + LensRadius + 0.6f
                    val cy = Inset + LensRadius + 0.6f
                    val steps = 16
                    for (i in 0..steps) {
                        val t = 2.0 * Math.PI * i / steps
                        val x = cx + LensRadius * kotlin.math.cos(t)
                        val y = cy + LensRadius * kotlin.math.sin(t)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    // Handle, leaving the lens edge at 45 degrees.
                    val hx = MagnifierWidth.value - Inset
                    val hy = MagnifierHeight.value - Inset
                    moveTo(cx + LensRadius * 0.72f, cy + LensRadius * 0.72f)
                    lineTo(hx, hy)
                }
            }
            .build()
    }

    /** Clear glyph: an X, not a filled box with a cross knocked out. */
    val Clear: ImageVector by lazy {
        ImageVector.Builder(
            name = "Cupertino.Clear",
            defaultWidth = ClearWidth,
            defaultHeight = ClearHeight,
            viewportWidth = ClearWidth.value,
            viewportHeight = ClearHeight.value,
        )
            .apply {
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    fill = null,
                    strokeLineWidth = ClearStroke,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 4f,
                ) {
                    moveTo(Inset, Inset)
                    lineTo(ClearWidth.value - Inset, ClearHeight.value - Inset)
                    moveTo(ClearWidth.value - Inset, Inset)
                    lineTo(Inset, ClearHeight.value - Inset)
                }
            }
            .build()
    }
}
