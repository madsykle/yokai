package yokai.presentation.theme.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import yokai.presentation.theme.CupertinoColors
import yokai.presentation.theme.CupertinoMotion
import yokai.presentation.theme.CupertinoType
import yokai.presentation.theme.hapticSelection

/**
 * One segment of a [SegmentedControl].
 *
 * @param value the selection this segment stands for. Generic so a screen can pass its own enum
 *   — `RecentsViewType`, say — and get it back typed rather than re-resolving an index.
 * @param label already-resolved, already-localised text. The component does not hold string
 *   resources, so it cannot be the thing that decides what a segment says.
 */
@Immutable
data class SegmentedControlSegment<T>(
    val value: T,
    val label: String,
)

/**
 * A Cupertino segmented control.
 *
 * `surfaceChrome` track, a thumb that morphs between segments on `spring.bouncy` — the only
 * spring in the system allowed to overshoot (`docs/DESIGN_CUPERTINO.md` §4.2 rule 3) — and a
 * `CLOCK_TICK` per segment boundary crossed while dragging (§5).
 *
 * ## Generic over an index
 *
 * `RecentsController.kt:925` builds its tabs from `RecentsViewType.entries` and hands the result
 * to `setViewType(RecentsViewType)` at `:743`. An `Int`-indexed control would make that call site
 * `entries[index]` in, `entries[indexOf]` out, with a silent wrong answer if the two ever
 * disagree. The enum lives in `:app` and this lives in `:presentation:theme`, so referencing it
 * directly is not on the table — but a type parameter carries it across for free.
 *
 * ## One callback, fired on drags too
 *
 * `onSelectionChange` fires on tap *and* on every boundary crossed mid-drag. Not two callbacks:
 * a control that draws the highlight on segment 2 while reporting segment 1 is lying, and a
 * screen that only honours one of two callbacks has to guess which. A screen that must not
 * persist mid-drag reads [state].isDragging and commits on release — the component reports
 * truth, the screen decides what truth costs.
 *
 * @param segments 2..5 of them; more and the labels stop fitting and iOS switches to a menu.
 * @param selected the currently selected segment's value.
 * @param state optional holder, for reading [SegmentedControlState.isDragging] from outside.
 */
@Stable
class SegmentedControlState internal constructor() {
    internal var selectedIndex by mutableIntStateOf(0)
    internal var indicatorPosition by mutableFloatStateOf(0f)
    internal var dragging by mutableFloatStateOf(false)

    /**
     * Whether a drag is in progress right now.
     *
     * `true` from the first drag pixel to the release. Read this to defer expensive work — a
     * prefs write, a list reload — until the finger is up, while still letting the highlight and
     * the haptic update live.
     */
    val isDragging: Boolean get() = dragging
}

/**
 * Drag position, in float segment indices, for the thumb.
 *
 * Held in a plain float state rather than an `Animatable` because the finger owns it directly:
 * while dragging it is set from the gesture, not animated. [snapTo] is the only thing that
 * animates, and it runs on release.
 */
@Stable
private class ThumbPosition(private val scope: CoroutineScope) {
    var value by mutableFloatStateOf(0f)
        private set

    private var snapJob: Job? = null

    /** Follow the finger. No animation, no coroutine — see the class KDoc. */
    fun follow(target: Float) {
        snapJob?.cancel()
        snapJob = null
        value = target
    }

    /** Release: spring to [target]. [CupertinoMotion.bouncy] is the only overshooting spring. */
    fun snapTo(target: Float, initialVelocity: Float = 0f) {
        snapJob?.cancel()
        if (value == target) {
            snapJob = null
            return
        }
        snapJob = scope.launch {
            Animatable(value).animateTo(target, CupertinoMotion.bouncy, initialVelocity) {
                value = this.value
            }
        }
    }
}

/**
 * A Cupertino segmented control — 2 to 5 mutually exclusive options.
 *
 * ```kotlin
 * SegmentedControl(
 *     segments = RecentsViewType.entries.map { SegmentedControlSegment(it, stringResource(it.stringRes)) },
 *     selected = presenter.viewType,
 *     onSelectionChange = ::setViewType,
 * )
 * ```
 *
 * Track is `surfaceChrome`, thumb is `surfaceRaised` at full opacity — lighter than the track, so
 * the selection reads without needing a colour change, a shadow, or a border.
 */
@Composable
fun <T> SegmentedControl(
    segments: List<SegmentedControlSegment<T>>,
    selected: T,
    onSelectionChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    state: SegmentedControlState = remember { SegmentedControlState() },
) {
    SegmentedControlMetrics.requireValidSegmentCount(segments.size)

    val colors = CupertinoColors.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val thumb = remember { ThumbPosition(scope) }
    val thumbInsetPx = remember(density.density) {
        SegmentedControlMetrics.ThumbInset.value * density.density
    }

    // Index of `selected`, recomputed when the caller changes it — the control is fully
    // controlled, so external changes must move the thumb too, not just taps.
    val selectedIndex = segments.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    if (!state.dragging && state.selectedIndex != selectedIndex) {
        state.selectedIndex = selectedIndex
        thumb.snapTo(selectedIndex.toFloat())
    }

    BoxWithConstraints(
        modifier = modifier
            .height(SegmentedControlMetrics.Height)
            .background(
                color = colors.surfaceChrome,
                shape = RoundedCornerShape(SegmentedControlMetrics.TrackRadius),
            )
            .pointerInput(segments, selectedIndex) {
                val segmentWidthPx = SegmentedControlMetrics.segmentWidthPx(
                    trackWidthPx = size.width.toFloat(),
                    thumbInsetPx = thumbInsetPx,
                    segmentCount = segments.size,
                )
                var dragPx = 0f

                detectHorizontalDragGestures(
                    onDragStart = {
                        state.dragging = true
                        dragPx = 0f
                        hapticSelection()
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragPx += dragAmount
                        val from = thumb.value
                        val to = SegmentedControlMetrics.indicatorPosition(
                            selectedIndex = selectedIndex,
                            dragPx = dragPx,
                            segmentWidthPx = segmentWidthPx,
                            segmentCount = segments.size,
                        )
                        thumb.follow(to)

                        // One tick per boundary crossed, not one per frame. A drag that wobbles
                        // inside one segment stays silent.
                        val crossings = SegmentedControlMetrics.boundariesCrossed(from, to, segments.size)
                        if (crossings > 0) {
                            repeat(crossings) { hapticSelection() }
                            // Report the landing segment immediately so the highlight and the
                            // caller's data agree with what the finger is over.
                            val landed = SegmentedControlMetrics.nearestSegment(to, segments.size)
                            if (landed != selectedIndex) {
                                state.selectedIndex = landed
                                onSelectionChange(segments[landed].value)
                            }
                        }
                    },
                    onDragEnd = {
                        state.dragging = false
                        val landed = SegmentedControlMetrics.nearestSegment(thumb.value, segments.size)
                        thumb.snapTo(landed.toFloat())
                        state.selectedIndex = landed
                        if (landed != selectedIndex) onSelectionChange(segments[landed].value)
                    },
                    onDragCancel = {
                        state.dragging = false
                        dragPx = 0f
                        thumb.snapTo(selectedIndex.toFloat())
                        state.selectedIndex = selectedIndex
                    },
                )
            },
    ) {
        val trackWidthPx = constraints.maxWidth.toFloat()
        val segmentWidthPx = SegmentedControlMetrics.segmentWidthPx(
            trackWidthPx = trackWidthPx,
            thumbInsetPx = thumbInsetPx,
            segmentCount = segments.size,
        )

        // Thumb. Positioned by float segment index, so during a drag it sits wherever the finger
        // is rather than jumping between cells.
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (thumb.value * segmentWidthPx).roundToInt() + thumbInsetPx.roundToInt(),
                        y = 0,
                    )
                }
                .width(segmentWidthPx)
                .fillMaxHeight()
                .padding(vertical = SegmentedControlMetrics.ThumbInset)
                .background(
                    color = colors.surfaceRaised,
                    shape = RoundedCornerShape(
                        SegmentedControlMetrics.TrackRadius - SegmentedControlMetrics.ThumbInset,
                    ),
                ),
        )

        // Labels, above the thumb so the selected one is legible against it.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            segments.forEach { segment ->
                BasicText(
                    text = segment.label,
                    style = CupertinoType.subhead.copy(
                        color = colors.labelPrimary,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier
                        .width(segmentWidthPx)
                        .fillMaxHeight()
                        .wrapContentHeight(Alignment.CenterVertically)
                        .clickable {
                            hapticSelection()
                            thumb.snapTo(segments.indexOf(segment).toFloat())
                            onSelectionChange(segment.value)
                        },
                )
            }
        }
    }
}
