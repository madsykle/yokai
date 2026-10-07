package yokai.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colour tokens (`docs/DESIGN_CUPERTINO.md` §3).
 *
 * ## Why this exists when `GlassColors` already has a palette
 *
 * `GlassColors` (`Theme.kt:27-59`) carries the iOS system palette and is correct, but it is
 * **glass vocabulary** — it describes materials. What is missing is the layer underneath: what
 * colour a *surface* is, what colour a *row* is, what colour a *separator* is, independent of
 * whether glass happens to sit on top. That is what these tokens are, and they exist whether or
 * not any glass is present.
 *
 * ## Naming rule
 *
 * **Semantic names. No Material roles.** No `primary`, `onPrimary`, `surfaceVariant`,
 * `surfaceTint`, `outlineVariant`. Those names encode Material's elevation model, which is
 * precisely what this design system discards. Ours encode *what the thing is*.
 *
 * ## Provenance of the values below
 *
 * | tokens | source |
 * |---|---|
 * | surfaces, `labelPrimary`, `labelSecondary`, accents, `separator` | `DESIGN_CUPERTINO.md` §3, from `DESIGN.md` §4.1 / iOS system palette |
 * | `surface`, `surfaceRaised` | §3.2; `surface` matches `GlassColors.GlassLightBase` / `GlassDarkBase` |
 * | **`labelTertiary`, `labelOnAccent`, `separatorOpaque`, `warning`** | **Apple system colour — INTERIM, see below** |
 *
 * The four interim tokens were gaps in the brief: §3.3 introduces `labelTertiary` and
 * `labelOnAccent` as "NEW" without giving hexes, §3.5 introduces `warning` without one, and
 * `separatorOpaque` appears in §3.4 as a name only.
 *
 * They are filled from Apple's standard system palette (the values UIKit uses), on the ruling
 * that they are **interim and must be verified against a primary source before Phase 4 exit.**
 * Apple's own HIG was not usable as that source here: the page is JS-rendered and a direct
 * fetch of `developer.apple.com/design/human-interface-guidelines/color` returned 17,807 bytes
 * containing zero hex literals.
 *
 * The marker is the audit trail and it stays. These remain in [PROVISIONAL_TOKENS] — not
 * promoted — until that verification happens, so a grep finds all four at once.
 */
data class CupertinoColorScheme(
    // ---- Surfaces: a strict three-step ladder (§3.2) -------------------------
    /** Screen background. */
    val surface: Color,
    /** Grouped-list cards, rows, opaque sheets. */
    val surfaceRaised: Color,
    /** Nav bar, top bar when opaque, segmented track. At 92% over [surface]. */
    val surfaceChrome: Color,

    // ---- Labels: four levels, not two (§3.3) --------------------------------
    /** The thing you read in order to act. */
    val labelPrimary: Color,
    /** Supporting text. */
    val labelSecondary: Color,
    /** PROVISIONAL — timestamps, counts, disabled. */
    val labelTertiary: Color,

    /**
     * PROVISIONAL — text on a filled accent.
     *
     * **Known limitation:** one token, not per-accent. It is `#FFFFFF` in both schemes because
     * the current accent family (`#007AFF` / `#0A84FF`) is always dark enough to carry white
     * text. A lighter accent — `systemYellow`, say — would need an override here, and nothing
     * in the type system will catch the mistake: it would simply render white-on-yellow.
     */
    val labelOnAccent: Color,

    // ---- Separators (§3.4) ---------------------------------------------------
    /** Row separators. Hairlines start inset 16dp; never full-bleed. */
    val separator: Color,
    /** PROVISIONAL — the opaque "shelf edge" hairline. */
    val separatorOpaque: Color,

    // ---- Accents (§3.5) ------------------------------------------------------
    /** Primary accent. Interactive, selected. */
    val accent: Color,
    val success: Color,
    /** PROVISIONAL — download queue, migration. */
    val warning: Color,
    val destructive: Color,
)

/** Light scheme. */
val LightCupertinoColors: CupertinoColorScheme = CupertinoColorScheme(
    surface = Color(0xFFF2F2F7),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceChrome = Color(0xFFF2F2F7).copy(alpha = 0.92f),

    labelPrimary = Color(0xFF000000),
    labelSecondary = Color(0x613C3C43),
    labelTertiary = Color(0x4D3C3C43),
    labelOnAccent = Color(0xFFFFFFFF),

    separator = Color(0x4A3C3C43),
    separatorOpaque = Color(0xFFC6C6C8),

    accent = Color(0xFF007AFF),
    success = Color(0xFF34C759),
    warning = Color(0xFFFF9500),
    destructive = Color(0xFFFF3B30),
)

/**
 * Dark scheme. First-class, not an inversion (§3.6).
 *
 * This is a **design decision, not a claim about our users** — there is no analytics or research
 * in the tree to support it. It is justified by the product's nature (long reading sessions, low
 * ambient light) and is revisitable on device.
 */
val DarkCupertinoColors: CupertinoColorScheme = CupertinoColorScheme(
    surface = Color(0xFF1C1C1E),
    surfaceRaised = Color(0xFF2C2C2E),
    surfaceChrome = Color(0xFF1C1C1E).copy(alpha = 0.92f),

    labelPrimary = Color(0xFFFFFFFF),
    labelSecondary = Color(0x99EBEBF5),
    labelTertiary = Color(0x4DEBEBF5),
    labelOnAccent = Color(0xFFFFFFFF),

    separator = Color(0x4AEBEBF5),
    separatorOpaque = Color(0xFF38383A),

    accent = Color(0xFF0A84FF),
    success = Color(0xFF30D158),
    warning = Color(0xFFFF9F0A),
    destructive = Color(0xFFFF453A),
)

/**
 * The scheme in scope.
 *
 * **Defaults to light and is currently provided by nothing.** Wiring it into `YokaiTheme` is a
 * one-line change, deliberately deferred: `GlassColors` lives in `Theme.kt` and is explicitly
 * out of scope for this commit, and adding a `MaterialTheme` colour override now would restyle
 * screens. The design system lands inert.
 */
val LocalCupertinoColors = staticCompositionLocalOf { LightCupertinoColors }

/** Names of the four tokens whose values are interim. See [CupertinoColorScheme]. */
val PROVISIONAL_TOKENS: List<String> = listOf(
    "labelTertiary",
    "labelOnAccent",
    "separatorOpaque",
    "warning",
)

/**
 * Standing note on [PROVISIONAL_TOKENS].
 *
 * Apple system colour (interim). **Primary-source verification required before Phase 4 exit.**
 */
const val PROVISIONAL_TOKENS_NOTE: String =
    "PROVISIONAL TOKENS: 4 (labelTertiary, labelOnAccent, separatorOpaque, warning). " +
        "Interim values from Apple system palette. " +
        "Primary source verification required before Phase 4 exit."