package yokai.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale (`docs/DESIGN_CUPERTINO.md` §3; `DESIGN.md` §4.4).
 *
 * Seven steps, base 4dp, every value a multiple of 4. Named by their dp value on purpose:
 * a numeric name makes the grid legible at the call site (`Spacing.space16`) where a semantic
 * name (`Spacing.medium`) hides the arithmetic, and where a bare `16.dp` is simply a number
 * that drifts off-scale the moment someone picks 15.
 *
 * [Size] in `Constants.kt` is the legacy predecessor. It is kept compiling — and **not**
 * annotated `@Deprecated`, because doing so would emit a deprecation warning at each of its
 * 40 call sites in 11 files that this change is not allowed to touch. The deprecation is
 * recorded in its KDoc instead. Migration ledger at `f00e530e7e`: 11 files / 40 call sites.
 *
 * Values here are duplicated as `val` rather than aliased to `Size` so that migrating a call
 * site cannot change its meaning. [Spacing.legacySizesAgree] exists so the duplication is
 * asserted rather than hoped for.
 */
object Spacing {

    /** 4dp — hairline offsets, icon-to-label gaps. The base unit. */
    val space4: Dp = 4.dp

    /** 8dp — padding inside a control, gaps within a group. */
    val space8: Dp = 8.dp

    /** 12dp — padding between related rows. */
    val space12: Dp = 12.dp

    /** 16dp — **the grouped-list horizontal inset**, and the default screen gutter. */
    val space16: Dp = 16.dp

    /**
     * 20dp — present in the new scale, absent from [Size].
     *
     * `Size` jumps 16 -> 24, so anything that wanted "a bit more than 16" had no token and
     * reached for a literal. This is the step that closes that hole.
     */
    val space20: Dp = 20.dp

    /** 24dp — separation between groups, sheet padding. */
    val space24: Dp = 24.dp

    /** 32dp — section separation on a roomy screen. */
    val space32: Dp = 32.dp

    /** Every step, ascending. Used by the token test to assert the scale in one pass. */
    val all: List<Dp> = listOf(space4, space8, space12, space16, space20, space24, space32)

    /**
     * Asserts that the legacy `Size` members which *do* have a scale equivalent still carry
     * exactly the value they always had.
     *
     * Guards the one real hazard of this refactor: an alias that silently rounds, e.g.
     * `Size.smedium` drifting onto a different step, would restyle the 11 onboarding and
     * settings files that still read it.
     */
    fun legacySizesAgree() {
        check(Size.tiny == space4) { "Size.tiny must stay 4dp" }
        check(Size.small == space8) { "Size.small must stay 8dp" }
        check(Size.smedium == space12) { "Size.smedium must stay 12dp" }
        check(Size.medium == space16) { "Size.medium must stay 16dp" }
        check(Size.large == space24) { "Size.large must stay 24dp" }
        check(Size.extraLarge == space32) { "Size.extraLarge must stay 32dp" }
    }
}