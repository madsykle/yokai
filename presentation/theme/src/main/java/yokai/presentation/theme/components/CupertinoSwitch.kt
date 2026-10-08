package yokai.presentation.theme.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import yokai.presentation.theme.CupertinoColors
import yokai.presentation.theme.CupertinoMotion
import yokai.presentation.theme.Superellipse

/** Geometry for [CupertinoSwitch]. */
object SwitchMetrics {

    /** iOS `UISwitch` track, in dp. */
    val TrackWidth: Dp = 51.dp
    val TrackHeight: Dp = 31.dp

    /** iOS thumb, in dp. */
    val ThumbSize: Dp = 27.dp

    /**
     * Gap between the thumb and the track edge, in dp.
     *
     * 2dp on all four sides — which is what lets 27dp fit inside 31dp vertically. At zero inset
     * the thumb sits edge-to-edge with the track and stops reading as a separate object that
     * slides.
     */
    val ThumbInset: Dp = 2.dp

    /**
     * How far the thumb travels between off and on, in dp.
     *
     * `51 - 2 * 2 - 27 = 20dp`. Derived, not hard-coded: changing the track or thumb in one
     * place cannot leave the thumb sliding off the end of the track.
     */
    val ThumbTravel: Dp = TrackWidth - ThumbInset * 2 - ThumbSize

    /**
     * Superellipse segment count for the track.
     *
     * [Superellipse.DEFAULT_SEGMENTS] = 64 is kept. At 51dp that is roughly 2.4dp per segment,
     * well under anything visible on a shape this small; lowering it would trade a faceting risk
     * for a handful of float multiplies. Named so a future larger switch can raise it without
     * touching the component.
     */
    const val TrackSegments: Int = Superellipse.DEFAULT_SEGMENTS

    /**
     * Thumb centre x for a toggle position, in dp from the track's left edge.
     *
     * @param position `0f` off, `1f` on. A fractional value puts the thumb mid-travel, which is
     *   what lets `spring.snappy`'s overshoot actually be seen instead of being clamped away.
     */
    fun thumbCenterX(position: Float): Dp =
        ThumbInset + ThumbSize / 2f + ThumbTravel * position.coerceIn(0f, 1f)

    /** Thumb centre y. Fixed — the thumb slides, it does not move vertically. */
    fun thumbCenterY(): Dp = TrackHeight / 2f
}

/**
 * A Cupertino switch.
 *
 * iOS proportions (51x31dp track, 27dp thumb), `spring.snappy` between states, **no haptic**.
 *
 * ## No haptic, on purpose
 *
 * `docs/DESIGN_CUPERTINO.md` §5 gives the switch toggle no entry, and the reason is already
 * written down there: the animation *is* the feedback. A snap that visibly moves is
 * acknowledgement; adding a buzz makes it twice, and the buzz is the half the user notices less.
 *
 * ## Stateless
 *
 * `checked` + `onCheckedChange`. `ThemeStep.kt:99` and `SwitchPreferenceWidget.kt:24` both read
 * it that way and both own the value in a preference, so an owning switch would immediately need
 * a two-way sync. Tap only, not drag — iOS `UISwitch` is tap-only too.
 *
 * @param enabled greys the control and blocks interaction, for a disabled preference row.
 */
@Composable
fun CupertinoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = CupertinoColors.current
    val scope = rememberCoroutineScope()

    // Thumb position, spring-solved. Held here rather than hoisted so the caller keeps owning
    // the boolean and only the animation is internal.
    var position by remember { mutableFloatStateOf(if (checked) 1f else 0f) }

    LaunchedEffect(checked) {
        val target = if (checked) 1f else 0f
        // Reversing mid-flight starts from wherever the thumb currently is. Restarting from the
        // previous target instead would make a fast double-tap snap the wrong way.
        if (position == target) return@LaunchedEffect
        scope.launch {
            Animatable(position).animateTo(target, CupertinoMotion.snappy) {
                position = value
            }
        }
    }

    val trackColor = when {
        !enabled -> colors.labelTertiary
        checked -> colors.accent
        else -> colors.surfaceChrome
    }
    val thumbColor = if (enabled) colors.surfaceRaised else colors.labelTertiary
    val outlineColor = if (checked) Color.Transparent else colors.separatorOpaque

    // Read the animated value once, here, so the draw lambdas take a plain Float rather than
    // reaching back into composition state from inside a DrawScope.
    val thumbPosition = position

    Canvas(
        modifier = modifier
            .size(SwitchMetrics.TrackWidth, SwitchMetrics.TrackHeight)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                onValueChange = onCheckedChange,
            ),
    ) {
        drawTrack(trackColor, outlineColor)
        drawThumb(thumbColor, thumbPosition)
    }
}

/**
 * The track: a superellipse, not a rounded rectangle and not a capsule.
 *
 * [Superellipse] already exists and is built — do not rebuild it. It is parameterised by the
 * half-extents and segment count, so 51x31 needs no variant and no new token; only `segments`
 * would ever want tuning for a different surface, and that is already a parameter.
 */
private fun DrawScope.drawTrack(fill: Color, outline: Color) {
    // Built from Superellipse.points(), not Superellipse.path(). path() returns and takes an
    // *android.graphics.Path* — GlassPane.kt:13 imports the Android Path for that reason — and
    // DrawScope.drawPath needs the Compose one. points() is the same primitive path() is
    // assembled from (Superellipse.kt:41), so this is that code with a Compose Path, and it
    // avoids depending on an asComposePath() bridge.
    val halfWidth = size.width / 2f
    val halfHeight = size.height / 2f
    val pts = Superellipse.points(
        halfWidth = halfWidth,
        halfHeight = halfHeight,
        segments = SwitchMetrics.TrackSegments,
    )
    val path = Path().apply {
        moveTo(halfWidth + pts[0], halfHeight + pts[1])
        for (i in 1 until SwitchMetrics.TrackSegments) {
            lineTo(halfWidth + pts[i * 2], halfHeight + pts[i * 2 + 1])
        }
        close()
    }
    drawPath(path, fill)
    if (outline != Color.Transparent) {
        drawPath(path, outline, style = Stroke(width = 1f))
    }
}

/**
 * The thumb: a circle.
 *
 * Deliberately *not* a superellipse. At 27dp inside a 31dp track the corner exponent's
 * contribution is under a pixel, and a circle is the shape iOS actually draws — a squircle here
 * would be theatre.
 */
private fun DrawScope.drawThumb(color: Color, position: Float) {
    drawCircle(
        color = color,
        radius = SwitchMetrics.ThumbSize.toPx() / 2f,
        center = Offset(
            x = SwitchMetrics.thumbCenterX(position).toPx(),
            y = SwitchMetrics.thumbCenterY().toPx(),
        ),
    )
}
