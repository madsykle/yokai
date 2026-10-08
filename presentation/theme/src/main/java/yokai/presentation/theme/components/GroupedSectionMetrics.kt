package yokai.presentation.theme.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import yokai.presentation.theme.CornerRadii
import yokai.presentation.theme.Spacing

/**
 * Geometry and decision rules for the grouped inset list
 * (`docs/DESIGN_CUPERTINO.md` §3.2).
 *
 * ## Why these are separate from the composables
 *
 * Everything here is pure: constants, arithmetic, three boolean rules. That is deliberate. This
 * repo has **no Compose UI test infrastructure** — no `androidTest` source set, no Robolectric,
 * no `ui-test-junit4`, and CI runs only `testStandardDebugUnitTest`, so a `createComposeRule()`
 * test would compile and then never execute. Isolating the decisions means they can be pinned by
 * JVM tests that actually run, leaving only true pixel measurement for whenever that gap closes.
 *
 * ## The inset arithmetic
 *
 * iOS grouped lists **stack** their two insets. They do not collapse:
 *
 * ```
 *   screen edge
 *     |<- 16dp ->|<-------- card, surfaceRaised, 12dp radius -------->|
 *                  |<- 16dp ->|<----------- row content --------->|
 *                               "Downloads"                    chevron
 * ```
 *
 * So a row label sits **32dp** from the screen edge, and the separator starts at **16dp** —
 * aligned to row content, never full-bleed. This is the strongest Cupertino tell in a settings
 * screen, stronger than colour and stronger than radius, which is why the arithmetic is
 * asserted rather than left to the eye.
 *
 * `sectionInset` and `rowContentInset` are separate constants precisely so that the other
 * reading — collapsed to a single 16dp — remains a one-line change if this turns out wrong.
 */
object GroupedSectionMetrics {

    /** Screen edge to the card's edge. §3.2. */
    val sectionInset: Dp = Spacing.space16

    /** Card edge to row content. Stacks with [sectionInset]. §3.2. */
    val rowContentInset: Dp = Spacing.space16

    /**
     * Separator leading inset.
     *
     * Equal to [rowContentInset] by construction, not by coincidence: a separator that starts
     * anywhere else is either full-bleed (wrong) or misaligned with the text above it (worse).
     */
    val separatorInset: Dp = rowContentInset

    /** Where a row label actually lands: 32dp from the screen edge. */
    val labelOffsetFromScreenEdge: Dp = sectionInset + rowContentInset

    /**
     * Minimum row height — the touch target floor.
     *
     * Android's own guidance floor is 48dp, and Material rows are typically 56–72dp. 44dp is
     * the *smaller* Cupertino figure (pt, on iOS), chosen here because the component is
     * specified against an iOS-derived density (see `docs/DESIGN_CUPERTINO.md` §1, density as a
     * feature). **Device-verify:** if the rows feel cramped against Android's 48dp convention,
     * raise this one constant — it is the only place the number appears.
     */
    val rowMinHeight: Dp = 44.dp

    /**
     * Card corner radius — the looser end of §3.2's 10-12dp range.
     *
     * [CornerRadii.small] (10dp) is the tighter end; iOS tightens as a list grows taller. The
     * single radius here is the practical simplification, and it is why both ends of the range
     * exist as tokens.
     */
    val groupRadius: Dp = CornerRadii.medium

    /** Section header — uppercase [yokai.presentation.theme.CupertinoType.sectionHeader]. */
    val headerHeight: Dp = 16.dp

    /** Section footer — `caption1`, secondary label colour. */
    val footerHeight: Dp = 16.dp

    /**
     * Whether the divider after row [index] should be drawn.
     *
     * Never after the last row: a hairline between the final row and the card's bottom edge
     * reads as a rendering bug, and it is the single most common mistake in a grouped list.
     * Empty and single-row groups therefore draw no divider at all.
     */
    fun shouldShowSeparator(index: Int, count: Int): Boolean =
        count > 1 && index in 0 until (count - 1)

    /**
     * Whether a row shows its disclosure chevron.
     *
     * A chevron means "this goes somewhere", so it appears on navigable rows and is suppressed
     * when the row already carries a trailing accessory — a switch, a value, a checkbox. A
     * chevron next to a switch is two endings on one row and reads as indecision.
     *
     * [showChevron] overrides the inference in either direction for the rare row that needs it.
     */
    fun shouldShowChevron(showChevron: Boolean?, navigable: Boolean, hasTrailing: Boolean): Boolean =
        showChevron ?: (navigable && !hasTrailing)

    /**
     * Section header text, uppercased at render time.
     *
     * Case is applied here rather than at the call site so that a header cannot be passed in
     * already-lowercased and quietly render wrong — the token carries size, weight and tracking,
     * but never case, because case depends on locale and a literal in the source does not.
     *
     * [String.uppercase] is locale-independent by default, which is what is wanted: Turkish
     * dotless-i rules must not turn "downloads" into "DOWNLOADS" with a missing glyph.
     */
    fun headerLabel(text: String): String = text.uppercase()
}