package yokai.presentation.theme.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import yokai.presentation.theme.CupertinoColors
import yokai.presentation.theme.CupertinoType

/**
 * Grouped section list — the iOS grouped inset list primitive
 * (`docs/DESIGN_CUPERTINO.md` §3.2).
 *
 * ## Zero Material
 *
 * This file imports nothing from `androidx.compose.material3`. Text is `BasicText`, the chevron
 * is a hand-drawn `ImageVector` through `Image`. That is checkable —
 * `grep -c "^import androidx.compose.material3" GroupedSection.kt` must return 0, and it is
 * the point: a Cupertino component that reaches for Material defaults the moment nobody is
 * looking is how a design system quietly stops being one.
 *
 * ## What this is deliberately not
 *
 * No animation, no haptic, no swipe actions, no drag reorder, no context menu. Each of those is
 * the consuming screen's business, and each is a separate component a screen can adopt without
 * this one knowing about it. Nothing here has an `AnimatedVisibility`, calls a haptic, or
 * installs a gesture detector.
 *
 * ## Layout
 *
 * ```
 * screen edge
 *   |<- 16dp ->|<--------- surfaceRaised, 12dp radius -------->|
 *                |<- 16dp ->|  label             value  chevron |
 *                |          |----------------------------------|  separator, inset 16dp
 *                |          |  next row                         |
 * ```
 *
 * The two 16dp insets **stack**; see [GroupedSectionMetrics] for why.
 *
 * ## Two entry points
 *
 * The slot form ([content]) is the flexible one. The list form ([rows]) is the migration path:
 * thirteen `SettingsLegacyController` subclasses already hold a `List` of preferences, and the
 * list form draws their dividers correctly by construction instead of asking each to remember.
 */
object GroupedSectionDefaults {
    /** Vertical gap between header, card and footer. */
    val blockSpacing: Dp = 8.dp

    /** Inner gap between a row's two text lines. */
    val lineSpacing: Dp = 2.dp

    /** Gap between a leading accessory and the row's text. */
    val accessorySpacing: Dp = 8.dp

    /** Square box reserved for a leading accessory, so rows align with or without one. */
    val accessorySize: Dp = 28.dp

    /** Vertical breathing room inside a row, around its text. */
    val rowVerticalPadding: Dp = 8.dp

    /** Horizontal inset applied to header and footer, relative to the card. */
    val blockTextInset: Dp = 4.dp
}

// ------------------------------------------------------------------------------- Section

/**
 * A grouped section: optional uppercase header, a raised card of rows, optional footer.
 *
 * Use the [rows] overload when you have a list — it draws dividers correctly for you.
 */
@Composable
fun GroupedSection(
    header: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = sectionColumn(modifier),
        verticalArrangement = Arrangement.spacedBy(GroupedSectionDefaults.blockSpacing),
    ) {
        if (header != null) GroupedHeader(header)
        GroupedCard(content = content)
        if (footer != null) GroupedFooter(footer)
    }
}

/**
 * The list form. Dividers are drawn for you — including their absence after the last row.
 */
@Composable
fun GroupedSection(
    header: String? = null,
    footer: String? = null,
    rows: List<GroupedRowSpec>,
    modifier: Modifier = Modifier,
    leading: @Composable ((GroupedRowSpec) -> Unit)? = null,
    trailing: @Composable ((GroupedRowSpec) -> Unit)? = null,
) {
    Column(
        modifier = sectionColumn(modifier),
        verticalArrangement = Arrangement.spacedBy(GroupedSectionDefaults.blockSpacing),
    ) {
        if (header != null) GroupedHeader(header)
        GroupedCard {
            rows.forEachIndexed { index, row ->
                GroupedRow(
                    label = row.label,
                    subtitle = row.subtitle,
                    secondaryLabel = row.secondaryLabel,
                    onClick = row.onClick,
                    showChevron = row.showChevron,
                    leading = leading?.let { slot -> @Composable { slot(row) } },
                    trailing = trailing?.let { slot -> @Composable { slot(row) } },
                )
                if (GroupedSectionMetrics.shouldShowSeparator(index, rows.size)) {
                    GroupedSeparator()
                }
            }
        }
        if (footer != null) GroupedFooter(footer)
    }
}

/** The outer column: full width, inset from the screen edge, stacking header/card/footer. */
@Composable
private fun sectionColumn(modifier: Modifier): Modifier = modifier
    .fillMaxWidth()
    .padding(horizontal = GroupedSectionMetrics.sectionInset)

/** The raised, rounded card the rows sit in. */
@Composable
private fun GroupedCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = CupertinoColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GroupedSectionMetrics.groupRadius))
            .background(colors.surfaceRaised),
        content = content,
    )
}

// ------------------------------------------------------------------------------------- Row

/**
 * One row.
 *
 * [subtitle] renders **below** [label], never beside it: a row is a two-line stack, and
 * side-by-side collapses both strings into unreadable truncation on a 44dp row.
 */
@Composable
fun GroupedRow(
    label: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    secondaryLabel: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean? = null,
) {
    val colors = CupertinoColors.current
    val chevron = GroupedSectionMetrics.shouldShowChevron(
        showChevron = showChevron,
        navigable = onClick != null,
        hasTrailing = trailing != null,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = GroupedSectionMetrics.rowMinHeight)
            .padding(horizontal = GroupedSectionMetrics.rowContentInset)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(
                modifier = Modifier.size(GroupedSectionDefaults.accessorySize),
                contentAlignment = Alignment.Center,
            ) { leading() }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = GroupedSectionDefaults.rowVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(GroupedSectionDefaults.lineSpacing),
        ) {
            // BasicText has no `color: Color` parameter — it takes a `ColorProducer?` SAM
            // (BasicText.kt:101). Colour therefore rides in on the TextStyle, which is the
            // same thing `ColorProducer { it }` would have produced and avoids wrapping five
            // call sites in CompositionLocalProvider.
            BasicText(text = label, style = CupertinoType.subhead.copy(color = colors.labelPrimary))
            if (subtitle != null) {
                BasicText(
                    text = subtitle,
                    style = CupertinoType.caption1.copy(color = colors.labelSecondary),
                )
            }
        }

        if (secondaryLabel != null) {
            BasicText(
                text = secondaryLabel,
                style = CupertinoType.subhead.copy(color = colors.labelSecondary),
                modifier = Modifier.padding(start = GroupedSectionDefaults.accessorySpacing),
            )
        }

        if (trailing != null) {
            Box(
                modifier = Modifier.padding(start = GroupedSectionDefaults.accessorySpacing),
                contentAlignment = Alignment.Center,
            ) { trailing() }
        }

        if (chevron) {
            GroupedChevron()
        }
    }
}

/** The disclosure chevron, tinted to the tertiary label colour so it recedes. */
@Composable
fun GroupedChevron(modifier: Modifier = Modifier) {
    val colors = CupertinoColors.current
    Image(
        painter = rememberVectorPainter(CupertinoChevrons.Right),
        contentDescription = null,
        colorFilter = ColorFilter.tint(colors.labelTertiary),
        modifier = modifier
            .padding(start = GroupedSectionDefaults.accessorySpacing)
            .size(
                width = CupertinoChevrons.ChevronWidth,
                height = CupertinoChevrons.ChevronHeight,
            ),
    )
}

/**
 * A row described as data — for the [GroupedSection] list overload and for the migration off
 * the legacy preference controllers.
 *
 * Note what is deliberately absent: no enabled/disabled flag, no selected state, no destructive
 * flag. Those are affordances a row acquires when a screen needs one, and baking them in now
 * would put opinions into a primitive.
 */
data class GroupedRowSpec(
    val label: String,
    /** Second line, below [label]. `caption1`, secondary label colour. */
    val subtitle: String? = null,
    /** Trailing value text, before any accessory. `subhead`, secondary label colour. */
    val secondaryLabel: String? = null,
    val onClick: (() -> Unit)? = null,
    /** Override the inferred chevron. `null` infers from navigability. */
    val showChevron: Boolean? = null,
)

// ----------------------------------------------------------------- Header, footer, divider

/** Uppercase group header: [CupertinoType.sectionHeader] on the secondary label colour. */
@Composable
fun GroupedHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = CupertinoColors.current
    BasicText(
        text = GroupedSectionMetrics.headerLabel(text),
        style = CupertinoType.sectionHeader.copy(color = colors.labelSecondary),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GroupedSectionDefaults.blockTextInset),
    )
}

/** Section footer: `caption1` on the secondary label colour — smaller than the header. */
@Composable
fun GroupedFooter(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = CupertinoColors.current
    BasicText(
        text = text,
        style = CupertinoType.caption1.copy(color = colors.labelSecondary),
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = GroupedSectionDefaults.blockTextInset,
                vertical = GroupedSectionDefaults.rowVerticalPadding / 2,
            ),
    )
}

/**
 * The hairline between rows.
 *
 * Inset to align with row content, never full-bleed (§3.4).
 *
 * Drawn **one physical pixel** tall inside a 1dp box, not 1dp tall: at 3x density a 1dp rule is
 * a 3px band and reads as a gap rather than a hairline. The `drawBehind` is what makes the
 * `height` above it a layout slot rather than the line itself.
 */
@Composable
fun GroupedSeparator(modifier: Modifier = Modifier) {
    val colors = CupertinoColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GroupedSectionMetrics.separatorInset)
            .height(1.dp)
            .drawBehind { drawRect(color = colors.separator, size = Size(size.width, 1f)) },
    )
}