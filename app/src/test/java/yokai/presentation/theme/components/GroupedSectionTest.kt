package yokai.presentation.theme.components

import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Layout rules for the grouped inset list (`docs/DESIGN_CUPERTINO.md` §3.2).
 *
 * ## What these tests do and do not cover
 *
 * This repo has **no Compose UI test infrastructure** — no `androidTest` source set, no
 * Robolectric, no `ui-test-junit4` — and CI runs only `testStandardDebugUnitTest`. A
 * `createComposeRule()` assertion would compile and then never execute, which is worse than no
 * test because it looks like coverage.
 *
 * So the component was shaped to keep its decisions in [GroupedSectionMetrics], where they are
 * pure and testable here. What is pinned below is real. What is **not** covered, and needs a
 * render tree:
 *
 * - that the padding actually lands at 32dp on screen
 * - that the subtitle paints below the label rather than beside it
 * - that the chevron pixel-matches at the chosen weight
 * - that a click actually lands on the row
 *
 * Closing that gap needs `ui-test-junit4` plus Robolectric in `app/build.gradle.kts`, or an
 * instrumented source set and a device. Filed, not attempted.
 */
class GroupedSectionTest {

    // ------------------------------------------------------- Spec assertion 1: the inset

    @Test
    fun `section inset is 16dp from the screen edge`() {
        GroupedSectionMetrics.sectionInset shouldBe 16.dp
    }

    @Test
    fun `row content inset is 16dp from the card edge`() {
        GroupedSectionMetrics.rowContentInset shouldBe 16.dp
    }

    /**
     * The two insets **stack**; they do not collapse into one 16dp.
     *
     * This is the correction to the original spec, which said both that the insets "stack
     * visually" and that content "has 0 additional". A collapsed reading would put the label at
     * 16dp and lose the inset tell entirely.
     */
    @Test
    fun `the two insets stack so a label lands 32dp from the screen edge`() {
        GroupedSectionMetrics.labelOffsetFromScreenEdge shouldBe 32.dp
        (GroupedSectionMetrics.sectionInset + GroupedSectionMetrics.rowContentInset) shouldBe
            GroupedSectionMetrics.labelOffsetFromScreenEdge
    }

    @Test
    fun `the inset comes from the shared scale not a literal`() {
        GroupedSectionMetrics.sectionInset shouldBe yokai.presentation.theme.Spacing.space16
        GroupedSectionMetrics.rowContentInset shouldBe yokai.presentation.theme.Spacing.space16
    }

    // ------------------------------------------------------- Spec assertion 2: separator

    /**
     * A separator that starts anywhere but the row content edge is either full-bleed (wrong) or
     * misaligned with the text above it (worse). Equality is the invariant, not coincidence.
     */
    @Test
    fun `separator inset equals row content inset`() {
        GroupedSectionMetrics.separatorInset shouldBe GroupedSectionMetrics.rowContentInset
        GroupedSectionMetrics.separatorInset shouldBe 16.dp
    }

    // --------------------------------------------------- Spec assertion 3: last-row rule

    @Test
    fun `no separator follows the last row`() {
        GroupedSectionMetrics.shouldShowSeparator(index = 0, count = 1) shouldBe false
        GroupedSectionMetrics.shouldShowSeparator(index = 1, count = 2) shouldBe false
        GroupedSectionMetrics.shouldShowSeparator(index = 2, count = 3) shouldBe false
    }

    @Test
    fun `every row but the last gets a separator`() {
        GroupedSectionMetrics.shouldShowSeparator(index = 0, count = 3) shouldBe true
        GroupedSectionMetrics.shouldShowSeparator(index = 1, count = 3) shouldBe true
    }

    @Test
    fun `an empty section draws no separator at all`() {
        GroupedSectionMetrics.shouldShowSeparator(index = 0, count = 0) shouldBe false
    }

    /** Exactly `count - 1` dividers for any non-empty group — never one more, never one fewer. */
    @Test
    fun `divider count is always count minus one`() {
        for (count in 0..10) {
            val drawn = (0 until count).count { GroupedSectionMetrics.shouldShowSeparator(it, count) }
            drawn shouldBe maxOf(0, count - 1)
        }
    }

    // ----------------------------------------------------- Spec assertion 4: touch target

    @Test
    fun `row height meets the 44dp touch target floor`() {
        (GroupedSectionMetrics.rowMinHeight.value >= 44f) shouldBe true
        GroupedSectionMetrics.rowMinHeight shouldBe 44.dp
    }

    /**
     * Device-verify: Android's own guidance floor is 48dp and Material rows run 56-72dp. 44dp is
     * the smaller Cupertino figure, chosen against the density the design brief calls for. If
     * rows feel cramped on hardware this is the one constant to raise.
     */
    @Test
    fun `touch target floor is documented as the Cupertino figure, not the Android one`() {
        (GroupedSectionMetrics.rowMinHeight.value < 48f) shouldBe true
    }

    // ------------------------------------------------------ Spec assertion 5: chevron rule

    @Test
    fun `a clickable row with no trailing accessory shows a chevron`() {
        GroupedSectionMetrics.shouldShowChevron(null, navigable = true, hasTrailing = false) shouldBe true
    }

    @Test
    fun `a non-clickable row shows no chevron`() {
        GroupedSectionMetrics.shouldShowChevron(null, navigable = false, hasTrailing = false) shouldBe false
    }

    /** A chevron beside a switch is two endings on one row and reads as indecision. */
    @Test
    fun `a row carrying a trailing accessory shows no chevron`() {
        GroupedSectionMetrics.shouldShowChevron(null, navigable = true, hasTrailing = true) shouldBe false
    }

    @Test
    fun `an explicit chevron setting overrides the inference in both directions`() {
        GroupedSectionMetrics.shouldShowChevron(true, navigable = false, hasTrailing = true) shouldBe true
        GroupedSectionMetrics.shouldShowChevron(false, navigable = true, hasTrailing = false) shouldBe false
    }

    // ----------------------------------------------------- Spec assertion 6: header case

    @Test
    fun `header text is uppercased`() {
        GroupedSectionMetrics.headerLabel("Downloads") shouldBe "DOWNLOADS"
        GroupedSectionMetrics.headerLabel("Advanced") shouldBe "ADVANCED"
    }

    @Test
    fun `header uppercasing is locale-independent`() {
        // Turkish locale rules would turn this into a dotted capital I. Kotlin's no-arg
        // uppercase() is Locale.ROOT, which is what we want: a header must not lose a glyph
        // because the device is set to Turkish.
        GroupedSectionMetrics.headerLabel("i") shouldBe "I"
        GroupedSectionMetrics.headerLabel("title") shouldBe "TITLE"
    }

    @Test
    fun `header uppercasing leaves already-uppercase text alone`() {
        GroupedSectionMetrics.headerLabel("READING") shouldBe "READING"
    }

    // ----------------------------------------------------------------- radius and chevron

    @Test
    fun `group radius is the 12dp end of the 10 to 12dp range`() {
        GroupedSectionMetrics.groupRadius shouldBe 12.dp
        GroupedSectionMetrics.groupRadius shouldBe yokai.presentation.theme.CornerRadii.medium
        (GroupedSectionMetrics.groupRadius.value in 10f..12f) shouldBe true
    }

    /**
     * A chevron that is as wide as it is tall reads as a small arrow rather than a direction.
     * Building the vector here also guards malformed path data, which would otherwise only
     * surface the first time a screen rendered it.
     *
     * `ImageVector` exposes no `paths` list; the drawn nodes live under `root`, which is a
     * `VectorGroup` and iterable. One `path()` call means one child.
     */
    @Test
    fun `the chevron is a tall stroke and its path data is well formed`() {
        (CupertinoChevrons.ChevronHeight.value > CupertinoChevrons.ChevronWidth.value) shouldBe true
        CupertinoChevrons.ChevronWidth shouldBe 7.dp
        CupertinoChevrons.ChevronHeight shouldBe 12.dp

        val vector = CupertinoChevrons.Right
        vector.name shouldBe "Cupertino.ChevronRight"
        vector.defaultWidth shouldBe CupertinoChevrons.ChevronWidth
        vector.defaultHeight shouldBe CupertinoChevrons.ChevronHeight
        // Auto-mirrored so RTL needs no caller-side handling.
        vector.autoMirror shouldBe true
        vector.viewportWidth shouldBe CupertinoChevrons.ChevronWidth.value
        vector.viewportHeight shouldBe CupertinoChevrons.ChevronHeight.value
        // Exactly one stroked node: a stroke, not a fill.
        vector.root.count() shouldBe 1
    }

    @Test
    fun `chevron stroke is lighter than the row it sits in`() {
        (CupertinoChevrons.ChevronStrokeWidth.value <= 2f) shouldBe true
    }
}