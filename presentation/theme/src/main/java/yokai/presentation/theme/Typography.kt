package yokai.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Cupertino type ramp — the primary text API of the design system
 * (`docs/DESIGN_CUPERTINO.md` §2).
 *
 * ## Why this exists
 *
 * `YokaiTypography` takes real iOS sizes and assigns them to Material3 slots
 * (`displayLarge` = LargeTitle, `labelMedium` = Caption). Any screen reaching for
 * `MaterialTheme.typography.bodyMedium` therefore reads a *Material vocabulary with
 * Cupertino numbers painted on* — the role name promises one thing and the shape delivers
 * another. That was the anti-pattern this file replaces.
 *
 * The fix is not to delete the Material mapping. 35 call sites across 22 files read it, and
 * `YokaiTheme` hands it to `MaterialTheme` (see `Theme.kt`). So the mapping survives as a
 * [YokaiTypography] bridge and the role-named tokens below become the primary API.
 *
 * **Kill condition (Ruling 5):** zero `MaterialTheme.typography.*` consumers before Phase 4
 * exit. Ledger at `f00e530e7e`: 22 files / 35 call sites. Re-count at each checkpoint.
 *
 * ## Rules this ramp encodes
 *
 * - **Inter only.** `inter_regular/medium/semibold/bold.ttf`, already shipped. `DESIGN.md`
 *   §7.1 forbids SF Pro for legal reasons. Single family — there is no second one.
 * - **Tracking inverts with size.** Positive above the body sizes, negative below. That
 *   inversion is the SF signature and Inter reproduces it; getting it backwards reads as a
 *   costume immediately.
 * - **Line height 1.2x on titles, 1.4x on body.** Preserved from the original file.
 * - **`sectionHeader` is a role, not a style.** It exists as a token so it cannot drift per
 *   call site. Colour is deliberately *not* baked in — see `CupertinoColors.labelSecondary`.
 *
 * ## Known divergences from the Material bridge
 *
 * Two line heights differ from what the bridge ships today, because §2.1 specifies them
 * differently from the current file:
 *
 * | Role | docs §2.1 | bridge (`YokaiTypography`) |
 * |---|---|---|
 * | `caption1` | 12 / 16 | `labelMedium` 12 / **17** |
 * | `caption2` | 11 / 14 | `labelSmall` 11 / **15** |
 *
 * The bridge keeps 17 and 15 **on purpose**: 35 call sites render those values today, and
 * changing them would be a visual edit to screens this commit must not touch. The delta lands
 * when each call site migrates, which is where it belongs.
 */
object CupertinoType {

    /** Inter, the only family in the system. `R.font.inter_*` ships in this module. */
    val interFontFamily: FontFamily = FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal, FontStyle.Normal),
        Font(R.font.inter_medium, FontWeight.Medium, FontStyle.Normal),
        Font(R.font.inter_semibold, FontWeight.SemiBold, FontStyle.Normal),
        Font(R.font.inter_bold, FontWeight.Bold, FontStyle.Normal),
    )

    // ---- Titles: 1.2x line height, positive tracking -------------------------

    /** Collapsing screen title. The signature of the design system. */
    val largeTitle: TextStyle = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.37.sp,
    )

    /** Manga detail title, rarely. */
    val title1: TextStyle = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.36.sp,
    )

    /** Sheet headers. */
    val title2: TextStyle = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.35.sp,
    )

    /**
     * Group titles in sheets — the step between [title2] and [headline].
     *
     * **Interpolated, not sourced.** `title3` is in neither `DESIGN.md` §4.2 nor Apple's
     * published ramp; it is our addition and the lowest-confidence token in the system. It is
     * the first to cut if it reads wrong on device.
     */
    val title3: TextStyle = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.34.sp,
    )

    // ---- Emphasis ----------------------------------------------------------

    /** Collapsed inline title, row emphasis. Semibold — the only emphatic body-ish role. */
    val headline: TextStyle = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.41).sp,
    )

    // ---- Body: 1.4x line height, negative tracking --------------------------

    /** Manga titles, chapter names. */
    val body: TextStyle = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.41).sp,
    )

    /** Dense row secondary. */
    val callout: TextStyle = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.32).sp,
    )

    /** Settings row labels at density. */
    val subhead: TextStyle = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.24).sp,
    )

    /** Metadata, timestamps, unread counts. */
    val footnote: TextStyle = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.08).sp,
    )

    /** Cover badges. */
    val caption1: TextStyle = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    )

    /** Timestamps, over-counts. */
    val caption2: TextStyle = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.06.sp,
    )

    /**
     * Uppercase grouped-list headers.
     *
     * Pair with `CupertinoColors.labelSecondary`. The token carries size, weight and tracking
     * only — colour is applied at the call site so it can be overridden in a dark chrome
     * context without forking the role.
     */
    val sectionHeader: TextStyle = TextStyle(
        fontFamily = interFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.06.sp,
    )

    /** Every role, in ramp order. Used by the token test to assert the whole table at once. */
    val all: List<TextStyle> = listOf(
        largeTitle, title1, title2, title3, headline, body,
        callout, subhead, footnote, caption1, caption2, sectionHeader,
    )
}

/**
 * Material3 typography bridge — **deprecated** (Ruling 5), retained only so the 35
 * `MaterialTheme.typography.*` call sites in 22 files keep compiling.
 *
 * Do not add new call sites to this. Reach for a role on [CupertinoType] instead.
 *
 * The values are frozen at what ships today, including the two line heights that differ from
 * [CupertinoType] (`caption1` 17 vs 16, `caption2` 15 vs 14) — moving them would restyle
 * screens that this change is not allowed to touch.
 */
@Deprecated(
    message = "Material3 typography slots carry Cupertino values under Material role names. " +
        "Use a role token on CupertinoType instead (docs/DESIGN_CUPERTINO.md §2, Ruling 5).",
    replaceWith = ReplaceWith("CupertinoType", "yokai.presentation.theme.CupertinoType"),
)
object YokaiTypography {
    val typography: Typography = Typography(
        // LargeTitle -> CupertinoType.largeTitle
        displayLarge = CupertinoType.largeTitle,
        // Title1 -> CupertinoType.title1
        displayMedium = CupertinoType.title1,
        // Title2 -> CupertinoType.title2
        displaySmall = CupertinoType.title2,
        // Headline -> CupertinoType.headline
        headlineLarge = CupertinoType.headline,
        headlineMedium = CupertinoType.headline,
        headlineSmall = CupertinoType.headline,
        // Body -> CupertinoType.body
        titleLarge = CupertinoType.body,
        bodyLarge = CupertinoType.body,
        // Callout -> CupertinoType.callout
        titleMedium = CupertinoType.callout,
        bodyMedium = CupertinoType.callout,
        // Subhead -> CupertinoType.subhead
        titleSmall = CupertinoType.subhead,
        bodySmall = CupertinoType.subhead,
        // Footnote -> CupertinoType.footnote (line height identical, safe to alias)
        labelLarge = CupertinoType.footnote,
        // Caption1 / caption2: line heights intentionally FROZEN at the shipped 17 / 15.
        // Aliasing CupertinoType here would silently restyle every existing call site.
        labelMedium = TextStyle(
            fontFamily = CupertinoType.interFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = CupertinoType.interFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.06.sp,
        ),
    )
}