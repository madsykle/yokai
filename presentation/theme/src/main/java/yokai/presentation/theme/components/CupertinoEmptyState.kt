package yokai.presentation.theme.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import yokai.presentation.theme.CupertinoColors
import yokai.presentation.theme.CupertinoType

/**
 * Geometry for [CupertinoEmptyState], as pure constants.
 *
 * Same reason as [LargeTitleBarMetrics] and [SegmentedControlMetrics]: there is no Compose UI
 * test infrastructure in this repo, so anything worth asserting has to be reachable without a
 * render tree.
 */
object CupertinoEmptyStateMetrics {

    /** Diameter of the glyph disc. */
    val GlyphSize: Dp = 64.dp

    /** Diameter of the glyph drawn inside the disc. */
    val IconSize: Dp = 32.dp

    /**
     * Gap between the disc and the title.
     *
     * A `Spacing` step would leave the two reading as one block; the empty state needs the disc to
     * read as a separate element.
     */
    val TitleGap: Dp = 16.dp

    /** Gap between title and body. */
    val BodyGap: Dp = 6.dp

    /**
     * Horizontal breathing room either side of the text.
     *
     * The body is centred under a screen-sized title, and a full-width line of body text on a
     * 360dp-wide phone breaks in the wrong places. Capping it rather than filling is what keeps
     * the sentence wrapping where a person would wrap it.
     */
    val HorizontalPadding: Dp = 40.dp

    /**
     * Type size for the title.
     *
     * [CupertinoType.title3] minus one step. iOS empty states sit below a screen title, and this
     * glyph is already the loudest thing in the composition — a full title3 on top of a 64dp disc
     * competes with the title that got the user here.
     */
    val TitleFontSize = 20.sp
}

/**
 * The "nothing here" state: a tinted glyph disc, a title and an optional body, centred.
 *
 * Replaces the `EmptyView` widget `recents_controller.xml` used to overlay. That widget drew a
 * Material icon and two `TextView`s, so keeping it would have meant a View floating over a Compose
 * list — two layout systems fighting over the same box, one of them always a frame behind.
 *
 * A sibling overlay rather than a `LazyColumn` item: an empty list and an empty state are
 * mutually exclusive, so rendering it as an item would mean the list has to know to emit exactly
 * one thing that is not a row.
 *
 * The caller owns both the glyph and the strings. This holds no resources and no state, so the
 * same component serves "no recents" and "no results for that search" without a flag telling it
 * which mood it is in.
 *
 * @param icon glyph to draw inside the disc.
 * @param title headline, already resolved by the caller.
 * @param body optional supporting line. Omit rather than pass an empty string.
 * @param modifier applied to the centring [Box].
 * @param background what to sit on, so the overlay is opaque rather than letting the list show
 *   through behind the text.
 */
@Composable
fun CupertinoEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    background: Color = CupertinoColors.current.surface,
) {
    Box(
        modifier = modifier.fillMaxSize().background(background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CupertinoEmptyStateMetrics.HorizontalPadding),
        ) {
            Box(
                modifier = Modifier.size(CupertinoEmptyStateMetrics.GlyphSize),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = rememberVectorPainter(icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(CupertinoColors.current.labelTertiary),
                    modifier = Modifier.size(CupertinoEmptyStateMetrics.IconSize),
                )
            }
            BasicText(
                text = title,
                style = CupertinoType.title3.copy(
                    fontSize = CupertinoEmptyStateMetrics.TitleFontSize,
                    color = CupertinoColors.current.labelSecondary,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.padding(top = CupertinoEmptyStateMetrics.TitleGap),
            )
            if (body != null) {
                BasicText(
                    text = body,
                    style = CupertinoType.subhead.copy(
                        color = CupertinoColors.current.labelTertiary,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.padding(top = CupertinoEmptyStateMetrics.BodyGap),
                )
            }
        }
    }
}