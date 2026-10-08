package yokai.presentation.theme.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.lerp
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import yokai.presentation.theme.CupertinoColors
import yokai.presentation.theme.CupertinoMotion
import yokai.presentation.theme.CupertinoType

/**
 * Collapse state for [LargeTitleBar], fed a scroll offset.
 *
 * ## Why the caller feeds an offset instead of handing over a scroll state
 *
 * Two things rule out taking a `ScrollState` directly:
 *
 * 1. `ScrollState` exposes no velocity. It is `value`, `maxValue`, `viewportSize`,
 *    `interactionSource` (`foundation/Scroll.kt:89-120`) — fling velocity is only available from
 *    a `NestedScrollConnection.onPostFling`, which is the app's job to own, not the bar's.
 * 2. Screen content here will be a `LazyColumn`, whose `LazyListState` reports
 *    `firstVisibleItemIndex` + `firstVisibleItemScrollOffset` — a different coordinate space
 *    entirely.
 *
 * So the bar takes the number it needs and the screen maps its own scroll state into it. That
 * is the shape Material3's `TopAppBarState` already uses, for the same reason.
 *
 * ## The one invariant worth stating out loud
 *
 * **While the finger is down this must not animate.** [onScroll] is a synchronous field write —
 * no coroutine, no clock, no animation — so the title tracks the finger exactly and cannot
 * disagree with the content under it. Only [onSettle] starts a spring, and it cancels any
 * in-flight one first. `docs/DESIGN_CUPERTINO.md` §4.1 rule 5.
 *
 * A caller that wants the old behaviour — the title animating itself while the list sits still
 * — cannot express it through this API, which is the point.
 */
@Stable
class LargeTitleBarState internal constructor(
    private val scope: CoroutineScope,
    private val pxPerDp: Float,
) {

    private var fractionState by mutableFloatStateOf(0f)

    /** Last offset handed to [onScroll]. Where [onSettle] springs to. */
    private var lastOffsetPx: Float = 0f

    private var settleJob: Job? = null

    /**
     * Fling velocity reported by [velocityCapture], consumed by the next [onSettle].
     *
     * Kept as pending state rather than a parameter because velocity's *source* is a nested
     * scroll callback that fires at a different moment from the caller's scroll observation, so
     * threading it through by hand would be a three-way race at every call site.
     */
    private var pendingFlingVelocity: Float = 0f

    /** `0f` fully expanded (34pt, transparent bar), `1f` fully condensed (17pt, opaque bar). */
    val fraction: Float get() = fractionState

    /** Bar background alpha for the current [fraction]. */
    val barAlpha: Float get() = LargeTitleBarMetrics.barAlpha(fraction)

    /** Whether content is under the bar and the hairline belongs. */
    val showHairline: Boolean get() = LargeTitleBarMetrics.showHairline(fraction)

    /** Title size at the current [fraction], interpolated 34sp -> 17sp. */
    val titleFontSize get() = LargeTitleBarMetrics.titleFontSize(fraction)

    /**
     * Record a fling's y velocity, in px/s. Called by [velocityCapture]; there is no reason to
     * call it directly.
     */
    internal fun onFling(velocityPxPerSec: Float) {
        pendingFlingVelocity = velocityPxPerSec
    }

    /** Record the current scroll offset. Called every frame while the list is scrolling.
     *
     * Synchronous by design — see the class KDoc. Cancels any settle spring first, so grabbing
     * the list mid-flight grabs the title too instead of fighting a running animation.
     */
    fun onScroll(scrollOffsetPx: Float) {
        settleJob?.cancel()
        settleJob = null
        lastOffsetPx = scrollOffsetPx
        fractionState = LargeTitleBarMetrics.collapseFraction(scrollOffsetPx, pxPerDp)
    }

    /**
     * Report that scrolling has stopped, and let the spring carry the title home.
     *
     * @param scrollVelocityPxPerSec fling velocity in px/s, positive when scrolling down.
     *   Defaults to whatever [velocityCapture] last recorded, which is the normal case. Converted
     *   through [LargeTitleBarMetrics.collapseVelocity] into the spring's initial velocity, so a
     *   hard flick arrives faster than a slow drag instead of both taking the same fixed
     *   duration. A fling away from the threshold will overshoot and come back — that is a
     *   spring doing its job, not a bug.
     *
     * A no-op when the title is already at the fraction the final offset implies, which is the
     * common case: the last [onScroll] already put it there. The spring earns its place when
     * scrolling stops somewhere the raw offset cannot express — overscroll settling, or a list
     * too short to scroll through the collapse window at all.
     */
    fun onSettle(scrollVelocityPxPerSec: Float = pendingFlingVelocity) {
        settleJob?.cancel()
        pendingFlingVelocity = 0f
        val target = LargeTitleBarMetrics.collapseFraction(lastOffsetPx, pxPerDp)
        if (target == fractionState) {
            settleJob = null
            return
        }
        val initialVelocity = LargeTitleBarMetrics.collapseVelocity(scrollVelocityPxPerSec, pxPerDp)
        settleJob = scope.launch {
            // A throwaway Animatable rather than a long-lived one: it exists only to own this
            // one spring's velocity, and it is discarded on completion so no state survives
            // between settles.
            Animatable(fractionState).animateTo(
                targetValue = target,
                animationSpec = CupertinoMotion.default,
                initialVelocity = initialVelocity,
            ) {
                // Writing through the block, not a separate collector, so the bar repaints on
                // the same frame the spring updates.
                fractionState = value
            }
        }
    }

    /**
     * A [NestedScrollConnection] that records fling velocity for the next [onSettle].
     *
     * This is the missing half of why the bar takes an offset rather than a scroll state:
     * neither `ScrollState` nor `LazyListState` exposes a velocity, so the only way to learn how
     * hard the user flicked is to be in the nested scroll chain.
     *
     * Apply it to the scrollable itself:
     *
     * ```kotlin
     * LazyColumn(state = listState, modifier = Modifier.nestedScroll(state.velocityCapture()))
     * ```
     *
     * Reporting [Velocity.Zero] back means this bar never wants to consume any of the fling
     * itself — the scrollable is already doing the work, and eating its tail would leave the
     * list feeling short.
     */
    fun velocityCapture(): NestedScrollConnection = capture

    // Lazy, not remembered: the connection is stateless between callbacks, so one instance per
    // state is enough and a @Composable factory would have to be a top-level function instead.
    private val capture: NestedScrollConnection by lazy { FlingVelocityCapture(this) }
}

/** Records the fling's y velocity; see [LargeTitleBarState.velocityCapture]. */
private class FlingVelocityCapture(private val state: LargeTitleBarState) : NestedScrollConnection {
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        state.onFling(consumed.y)
        return Velocity.Zero
    }
}

/** Remember a [LargeTitleBarState] bound to the current density. */
@Composable
fun rememberLargeTitleBarState(): LargeTitleBarState {
    val scope = rememberCoroutineScope()
    val pxPerDp = LocalDensity.current.density
    return remember(scope, pxPerDp) { LargeTitleBarState(scope, pxPerDp) }
}

/**
 * A collapsing screen title — the signature component.
 *
 * 34pt `largeTitle` at rest, condensing to 17pt `headline` as content scrolls under, with the
 * bar behind it going translucent-to-opaque and a hairline appearing once anything is actually
 * underneath. Driven by a [LargeTitleBarState] the caller feeds; see that class for why.
 *
 * ```kotlin
 * val state = rememberLargeTitleBarState()
 * val listState = rememberLazyListState()
 *
 * // One line of glue per state change: report the offset while scrolling, report the stop.
 * LaunchedEffect(listState) {
 *     snapshotFlow { Triple(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, listState.isScrollInProgress) }
 *         .collect { (index, offset, scrolling) ->
 *             state.onScroll(offset.toFloat())
 *             if (!scrolling) state.onSettle()
 *         }
 * }
 *
 * Box {
 *     LazyColumn(
 *         state = listState,
 *         modifier = Modifier.nestedScroll(state.velocityCapture()),
 *         // Leave room for the pinned bar, or the first item starts underneath it.
 *         contentPadding = PaddingValues(top = LargeTitleBarMetrics.BarHeight),
 *     ) { /* items */ }
 *     LargeTitleBar("Library", state)
 * }
 * ```
 *
 * Place the bar **after** the content in the parent's `Box` so it draws on top; the content
 * scrolls beneath it. There is no animation, gesture or haptic here beyond what
 * [LargeTitleBarState] owns — that is [LargeTitleBarState]'s job and this file's drawing.
 */
@Composable
fun LargeTitleBar(
    title: String,
    state: LargeTitleBarState,
    modifier: Modifier = Modifier,
    actions: @Composable BoxScope.() -> Unit = {},
) {
    val colors = CupertinoColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LargeTitleBarMetrics.BarHeight)
            .background(colors.surface.copy(alpha = state.barAlpha)),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(
            text = title,
            style = CupertinoType.largeTitle.copy(
                color = colors.labelPrimary,
                fontSize = state.titleFontSize,
                // largeTitle is Normal and headline is Semibold; stepping between them on the
                // same fraction keeps the swap from reading as a flicker at the end of the run.
                fontWeight = lerp(FontWeight.Normal, FontWeight.SemiBold, state.fraction),
            ),
            modifier = Modifier.padding(horizontal = LargeTitleBarMetrics.BarHorizontalInset),
        )

        actions()

        if (state.showHairline) {
            Hairline(
                color = colors.separatorOpaque,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * One physical-pixel rule, drawn in a 1dp box.
 *
 * `drawBehind` with a height of `1f` — one *pixel*, not one dp. A 1dp rule at 3x is a 3px band
 * and reads as a gap between sections rather than a hairline. Same treatment as
 * [GroupedSeparator].
 */
@Composable
private fun Hairline(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LargeTitleBarMetrics.HairlineHeight)
            .drawBehind { drawRect(color = color, size = Size(size.width, 1f)) },
    )
}
