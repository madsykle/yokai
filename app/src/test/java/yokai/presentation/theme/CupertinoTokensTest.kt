package yokai.presentation.theme

import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * The token tables in `docs/DESIGN_CUPERTINO.md` §2-§5 are numbers that nothing in the build can
 * otherwise check. A transposed digit in a tracking value or a dropped spring does not fail
 * compilation — it renders a few percent wrong, everywhere, permanently. So they are pinned here.
 *
 * Everything asserted is copied from the doc, not from the implementation, so that changing the
 * implementation to match the implementation is impossible by construction.
 */
class CupertinoTokensTest {

    // ---------------------------------------------------------------- Typography

    @Test
    fun `the ramp matches the doc table row for row`() {
        val expected = listOf(
            "largeTitle" to Triple(34f, 41f, 0.37f),
            "title1" to Triple(28f, 34f, 0.36f),
            "title2" to Triple(22f, 26f, 0.35f),
            "title3" to Triple(20f, 25f, 0.34f),
            "headline" to Triple(17f, 24f, -0.41f),
            "body" to Triple(17f, 24f, -0.41f),
            "callout" to Triple(16f, 22f, -0.32f),
            "subhead" to Triple(15f, 21f, -0.24f),
            "footnote" to Triple(13f, 18f, -0.08f),
            "caption1" to Triple(12f, 16f, 0f),
            "caption2" to Triple(11f, 14f, 0.06f),
            "sectionHeader" to Triple(13f, 16f, 0.06f),
        )
        val actual = listOf(
            "largeTitle" to CupertinoType.largeTitle,
            "title1" to CupertinoType.title1,
            "title2" to CupertinoType.title2,
            "title3" to CupertinoType.title3,
            "headline" to CupertinoType.headline,
            "body" to CupertinoType.body,
            "callout" to CupertinoType.callout,
            "subhead" to CupertinoType.subhead,
            "footnote" to CupertinoType.footnote,
            "caption1" to CupertinoType.caption1,
            "caption2" to CupertinoType.caption2,
            "sectionHeader" to CupertinoType.sectionHeader,
        )

        actual.size shouldBe expected.size
        for ((i, row) in expected.withIndex()) {
            val (name, spec) = actual[i]
            name shouldBe row.first
            val (size, line, tracking) = row.second
            spec.fontSize.value shouldBeExactly size
            spec.lineHeight.value shouldBeExactly line
            spec.letterSpacing.value shouldBeExactly tracking
        }
    }

    /** §2.2: "Positive tracking on large sizes, negative on small." That inversion is the tell. */
    @Test
    fun `tracking is positive on titles and negative on small text`() {
        listOf(
            CupertinoType.largeTitle,
            CupertinoType.title1,
            CupertinoType.title2,
            CupertinoType.title3,
            CupertinoType.caption2,
            CupertinoType.sectionHeader,
        ).forEach { (it.letterSpacing.value >= 0f) shouldBe true }

        listOf(
            CupertinoType.headline,
            CupertinoType.body,
            CupertinoType.callout,
            CupertinoType.subhead,
            CupertinoType.footnote,
        ).forEach { (it.letterSpacing.value < 0f) shouldBe true }

        // caption1 is the one role that sits exactly on zero.
        CupertinoType.caption1.letterSpacing.value shouldBeExactly 0f
    }

    /**
     * §2.2: 1.2x line height on titles, 1.4x on body. Asserted as a band, because the doc rounds
     * 34x1.2 to 41 and 17x1.4 to 24 — the ratio never comes out exact.
     */
    @Test
    fun `line height is 1_2x on titles and 1_4x on body`() {
        fun ratio(size: androidx.compose.ui.text.TextStyle) =
            size.lineHeight.value / size.fontSize.value

        listOf(CupertinoType.largeTitle, CupertinoType.title1, CupertinoType.title2, CupertinoType.title3)
            .forEach { (ratio(it) in 1.15f..1.25f) shouldBe true }

        listOf(CupertinoType.body, CupertinoType.callout, CupertinoType.subhead, CupertinoType.footnote)
            .forEach { (ratio(it) in 1.35f..1.45f) shouldBe true }
    }

    /** §2: only two roles are emphatic, and `sectionHeader` is one of them. */
    @Test
    fun `only headline and sectionHeader are semibold`() {
        val semibold = CupertinoType.all.filter {
            it.fontWeight == androidx.compose.ui.text.font.FontWeight.SemiBold
        }
        semibold.size shouldBe 2
        semibold shouldBe listOf(CupertinoType.headline, CupertinoType.sectionHeader)
    }

    /**
     * The bridge must not drift into the ramp. `caption1` and `caption2` are the two places §2.1
     * disagrees with what ships; if they are ever "helpfully" aliased, every existing
     * `MaterialTheme.typography` call site restyles without anyone touching a screen.
     */
    @Test
    fun `bridge keeps the shipped caption line heights`() {
        @Suppress("DEPRECATION")
        val t = YokaiTypography.typography
        t.labelMedium.fontSize.value shouldBeExactly 12f
        t.labelMedium.lineHeight.value shouldBeExactly 17f
        t.labelSmall.fontSize.value shouldBeExactly 11f
        t.labelSmall.lineHeight.value shouldBeExactly 15f
    }

    // ------------------------------------------------------------------ Spacing

    @Test
    fun `the spacing scale is exactly the seven documented steps`() {
        Spacing.all.map { it.value } shouldBe listOf(4f, 8f, 12f, 16f, 20f, 24f, 32f)
    }

    /** Base 4dp (§4.4): every step is a multiple of 4, and none is duplicated. */
    @Test
    fun `every spacing step is a distinct multiple of four`() {
        Spacing.all.forEach { (it.value % 4f) shouldBeExactly 0f }
        Spacing.all.map { it.value }.toSet().size shouldBe Spacing.all.size
    }

    /** 20dp is the step the old scale was missing. */
    @Test
    fun `the scale includes the 20dp step the legacy set lacked`() {
        Spacing.space20 shouldBe 20.dp
    }

    /** An alias that rounded would restyle 11 files that still read `Size`. */
    @Test
    fun `legacy Size members still hold their historical values`() {
        Spacing.legacySizesAgree()
        Size.tiny shouldBe 4.dp
        Size.small shouldBe 8.dp
        Size.smedium shouldBe 12.dp
        Size.medium shouldBe 16.dp
        Size.large shouldBe 24.dp
        Size.extraLarge shouldBe 32.dp
        // No scale equivalent; frozen literals.
        Size.extraExtraTiny shouldBe 1.dp
        Size.extraTiny shouldBe 2.dp
        Size.huge shouldBe 48.dp
        Size.extraHuge shouldBe 56.dp
        Size.navBarSize shouldBe 68.dp
        Size.none shouldBe 0.dp
    }

    // ------------------------------------------------------------ CornerRadii

    @Test
    fun `corner radii are the four documented steps and ascend`() {
        CornerRadii.all.map { it.value } shouldBe listOf(10f, 12f, 16f, 22f)
    }

    /** §3.2 specifies grouped-list corners as the 10-12dp *range*; both ends must exist. */
    @Test
    fun `grouped list radius range is covered by small and medium`() {
        (CornerRadii.small.value..CornerRadii.medium.value) shouldBe (10f..12f)
    }

    // ------------------------------------------------------------------ Motion

    @Test
    fun `the springs match the doc table`() {
        CupertinoMotion.all.size shouldBe 4
        CupertinoMotion.snappy.stiffness shouldBeExactly 700f
        CupertinoMotion.snappy.dampingRatio shouldBeExactly 0.85f
        CupertinoMotion.default.stiffness shouldBeExactly 300f
        CupertinoMotion.default.dampingRatio shouldBeExactly 0.75f
        CupertinoMotion.gentle.stiffness shouldBeExactly 180f
        CupertinoMotion.gentle.dampingRatio shouldBeExactly 0.90f
        CupertinoMotion.bouncy.stiffness shouldBeExactly 400f
        CupertinoMotion.bouncy.dampingRatio shouldBeExactly 0.60f
    }

    /** §4.2 rule 4: dismiss is gentler than present. The tokens must encode that asymmetry. */
    @Test
    fun `gentle is softer than default`() {
        (CupertinoMotion.gentle.stiffness < CupertinoMotion.default.stiffness) shouldBe true
        (CupertinoMotion.gentle.dampingRatio > CupertinoMotion.default.dampingRatio) shouldBe true
    }

    /**
     * §4.2 rule 3: bouncy is the only token *permitted* to overshoot.
     *
     * Note this is a usage rule, not a physics claim — all four springs are underdamped
     * (`dampingRatio < 1`), so "only bouncy overshoots" cannot be tested as `dampingRatio < 1`.
     * What is testable is that bouncy is the *most* oscillatory of the four, i.e. the lowest
     * damping ratio, and that the other three are all strictly calmer.
     */
    @Test
    fun `bouncy is the most oscillatory spring and the only one allowed to be`() {
        val ratios = CupertinoMotion.all.map { it.dampingRatio }
        CupertinoMotion.bouncy.dampingRatio shouldBeExactly ratios.min()
        ratios.filter { it == ratios.min() }.size shouldBe 1

        // The three non-bouncy tokens are measurably calmer than it.
        listOf(CupertinoMotion.snappy, CupertinoMotion.default, CupertinoMotion.gentle).forEach {
            (it.dampingRatio > CupertinoMotion.bouncy.dampingRatio) shouldBe true
        }
    }

    /** Guards the reason the values are literals: Compose's names would be near-misses. */
    @Test
    fun `namedDamping only claims an exact match`() {
        CupertinoMotion.namedDamping(0.75f) shouldBe Spring.DampingRatioLowBouncy
        CupertinoMotion.namedDamping(0.85f) shouldBe null
        CupertinoMotion.namedDamping(0.90f) shouldBe null
        CupertinoMotion.namedDamping(0.60f) shouldBe null
    }

    @Test
    fun `the no-tween marker states its own carve-out`() {
        CupertinoMotion.AUTHORED_ANIMATIONS_MUST_NOT_TWEEN.contains("spring") shouldBe true
        CupertinoMotion.AUTHORED_ANIMATIONS_MUST_NOT_TWEEN.contains("Crossfade") shouldBe true
        CupertinoMotion.CROSSFADE_RULE.contains("no authored tween") shouldBe true
    }

    // ------------------------------------------------------------------ Colours

    @Test
    fun `the surface ladder is strictly ordered in both schemes`() {
        for (s in listOf(LightCupertinoColors, DarkCupertinoColors)) {
            // all three steps must be distinct, or the ladder collapses
            (s.surfaceRaised != s.surface) shouldBe true
            (s.surfaceChrome != s.surface) shouldBe true
            (s.surfaceRaised != s.surfaceChrome) shouldBe true
            // §3.2: chrome is a translucent variant of surface, not an opaque one
            (s.surfaceChrome.alpha < 1f) shouldBe true
            // raised must actually separate from surface in perceived lightness.
            // Parens required: infix `shouldBe` binds tighter than `>` in Kotlin.
            (kotlin.math.abs(s.surfaceRaised.luminance() - s.surface.luminance()) > 0.01f) shouldBe true
        }
    }

    /** §3.3: four label levels, each fainter than the last. */
    @Test
    fun `labels descend in emphasis`() {
        for (s in listOf(LightCupertinoColors, DarkCupertinoColors)) {
            (s.labelPrimary.alpha) shouldBeExactly 1f
            (s.labelSecondary.alpha < 1f) shouldBe true
            (s.labelTertiary.alpha < s.labelSecondary.alpha) shouldBe true
        }
    }

    /** §3.4: the row separator is fainter than the opaque hairline. */
    @Test
    fun `separator is fainter than separatorOpaque`() {
        for (s in listOf(LightCupertinoColors, DarkCupertinoColors)) {
            (s.separator.alpha < s.separatorOpaque.alpha) shouldBe true
        }
    }

    /** Every accent must be legible as text on [labelOnAccent] and vice versa. */
    @Test
    fun `accent colours are dark enough to carry white label text`() {
        for (s in listOf(LightCupertinoColors, DarkCupertinoColors)) {
            listOf(s.accent, s.success, s.warning, s.destructive).forEach {
                (it.luminance() < 0.6f) shouldBe true
            }
        }
    }

    /** §3.2: `surface` is the same page colour `GlassColors` already uses. */
    @Test
    fun `surface matches the existing glass base colours`() {
        LightCupertinoColors.surface shouldBe GlassColors.GlassLightBase
        DarkCupertinoColors.surface shouldBe GlassColors.GlassDarkBase
    }

    /** §3.6: dark is not an inversion. */
    @Test
    fun `the two schemes are genuinely different`() {
        LightCupertinoColors.labelPrimary shouldBe Color(0xFF000000)
        DarkCupertinoColors.labelPrimary shouldBe Color(0xFFFFFFFF)
        LightCupertinoColors.accent shouldBe Color(0xFF007AFF)
        DarkCupertinoColors.accent shouldBe Color(0xFF0A84FF)
    }

    /** The four tokens §3 introduces without a hex must stay enumerable until they are sourced. */
    @Test
    fun `provisional tokens are enumerated`() {
        PROVISIONAL_TOKENS shouldBe listOf("labelTertiary", "labelOnAccent", "separatorOpaque", "warning")
    }
}