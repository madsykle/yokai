package yokai.presentation.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Motion vocabulary (`docs/DESIGN_CUPERTINO.md` §4). Spring physics only — no authored tweens.
 *
 * ## Why springs and not durations
 *
 * A spring carries its own continuity: it knows how fast it was moving when it was handed over,
 * so an interruption blends instead of restarting. A tween cannot, which is why tweens read as
 * a seam when motion is reversed or re-triggered mid-flight. In a reader app where a list is
 * flung and caught a dozen times a minute, that seam is the difference between "responsive" and
 * "laggy".
 *
 * The installed APK already moves on springs — `GlassRecipe.SWEEP_DURATION_MS = 650L` and the
 * morphing tab indicator (`DESIGN.md` §18) — so the house default is chosen for continuity with
 * motion users have already learned, not for preference. That is reason 1 for `default` being
 * 300 / 0.75.
 *
 * ## Damping values are literals, not Compose constants
 *
 * Compose 1.10.3 ships these (verified from `androidx.compose.animation:animation-core-android`
 * `Spring.class`, `javap -constants`):
 *
 * | constant | value |
 * |---|---|
 * | `StiffnessHigh` | 10000f |
 * | `StiffnessMedium` | 1500f |
 * | `StiffnessMediumLow` | 400f |
 * | `StiffnessLow` | 200f |
 * | `StiffnessVeryLow` | 50f |
 * | `DampingRatioHighBouncy` | 0.2f |
 * | `DampingRatioMediumBouncy` | **0.5f** |
 * | `DampingRatioLowBouncy` | 0.75f |
 * | `DampingRatioNoBouncy` | 1.0f |
 *
 * Only one of our four damping values (0.75) coincides with a named constant, and the nearest
 * name for `bouncy` is 0.5 — well away from the 0.60 §4.1 specifies. Reaching for the named
 * constant anyway would silently restyle the motion, so all four are written as literals that
 * can be grepped against the doc table.
 */
object CupertinoMotion {

    /**
     * Press-in, chip.
     *
     * Fast and near-critically damped: the fastest thing in the system, and it must still land
     * without wobbling or it reads as a glitch rather than a response.
     */
    val snappy: SpringSpec<Float> = spring(stiffness = 700f, dampingRatio = 0.85f)

    /**
     * **House default.** Sheets, large-title collapse, list push.
     *
     * 300 / 0.75 is what already ships (see the class KDoc). Depart from it deliberately.
     */
    val default: SpringSpec<Float> = spring(stiffness = 300f, dampingRatio = 0.75f)

    /**
     * Dismiss, fade-out, layout settle.
     *
     * Always gentler than the present that precedes it. That asymmetry — present 300/0.75,
     * dismiss 180/0.90 — is what makes a dismissal feel acknowledged rather than reversed.
     */
    val gentle: SpringSpec<Float> = spring(stiffness = 180f, dampingRatio = 0.90f)

    /**
     * Segmented-control indicator **only**.
     *
     * The one place in the app permitted to overshoot. Bounce is a one-verb vocabulary; spent
     * broadly it reads as cheap, so this is the only token that gets used.
     */
    val bouncy: SpringSpec<Float> = spring(stiffness = 400f, dampingRatio = 0.60f)

    /**
     * Machine-greppable statement of Ruling 1.
     *
     * There is no Lint check behind this — wiring one up means a custom `LintCheck` in
     * `buildSrc`, which is out of scope for a token commit. So it is a marker constant instead:
     * `grep -rn AUTHORED_ANIMATIONS_MUST_NOT_TWEEN` finds it, and that is what a future CI grep
     * should key on.
     *
     * The carve-out, stated so a grep does not have to guess:
     *
     * - **Forbidden:** any `tween(`, `infiniteRepeatable` or `keyframes` written by us.
     * - **Grandfathered:** framework-provided defaults — `AnimatedVisibility`, `AnimatedContent`,
     *   `Crossfade`, Nav3 transitions, and `fadeIn()` / `fadeOut()` as Phase 3 already shipped
     *   them. These are tween-based in the framework and stay.
     *
     * The line is authorship, not API: if we wrote it, it springs; if the framework chose it, we
     * accept it.
     */
    const val AUTHORED_ANIMATIONS_MUST_NOT_TWEEN: String =
        "Ruling 1: authored animations use spring physics. Framework-provided tween defaults " +
            "(AnimatedVisibility, AnimatedContent, Crossfade, Nav3) are grandfathered."

    /**
     * Tab switch. A crossfade on **framework defaults** — deliberately not a token, because
     * §4.2 rule 6 grandfathers it out of the no-tween rule (Ruling 1).
     *
     * The tab indicator morph is the only authored motion on that screen and it already exists.
     */
    const val CROSSFADE_RULE: String =
        "Tab switch uses a crossfade on framework defaults (Crossfade/AnimatedContent). " +
            "No authored spring, no authored tween. See docs/DESIGN_CUPERTINO.md §4.2 rule 6."

    /** Every authored spring in the system, so a test can assert the whole table at once. */
    val all: List<SpringSpec<Float>> = listOf(snappy, default, gentle, bouncy)

    /**
     * Nearest named Compose damping constant for [dampingRatio], for readability at call sites
     * that want to explain themselves. 0.75 is the only value §4.1 shares with Compose; the rest
     * return `null` rather than a near-miss, because a near-miss constant name is worse than no
     * name at all.
     */
    fun namedDamping(dampingRatio: Float): Float? = when (dampingRatio) {
        Spring.DampingRatioNoBouncy -> Spring.DampingRatioNoBouncy
        Spring.DampingRatioLowBouncy -> Spring.DampingRatioLowBouncy
        Spring.DampingRatioMediumBouncy -> Spring.DampingRatioMediumBouncy
        Spring.DampingRatioHighBouncy -> Spring.DampingRatioHighBouncy
        else -> null
    }
}