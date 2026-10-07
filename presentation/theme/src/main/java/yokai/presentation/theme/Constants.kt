package yokai.presentation.theme

import androidx.compose.ui.unit.dp

const val SecondaryItemAlpha = .78f
const val HalfAlpha = .5f

/**
 * **Deprecated** legacy spacing set — superseded by [Spacing]
 * (`docs/DESIGN_CUPERTINO.md` §3; Ruling 4). Kept compiling; do not add call sites.
 *
 * ### Why this has no `@Deprecated` annotation
 *
 * Annotating it would emit a deprecation warning at all 40 call sites across the 11 files
 * listed below — files that the change adding [Spacing] is explicitly not allowed to touch,
 * and that would turn "no new warnings" into 40 new warnings. The deprecation is therefore
 * recorded here, and the migration ledger tracks it:
 *
 * | file | call sites |
 * |---|---|
 * | `yokai/presentation/component/ThemeItem.kt` | 10 |
 * | `yokai/presentation/onboarding/InfoScreen.kt` | 8 |
 * | `yokai/presentation/onboarding/steps/ThemeStep.kt` | 4 |
 * | `yokai/presentation/onboarding/steps/StorageStep.kt` | 3 |
 * | `yokai/presentation/onboarding/steps/PermissionStep.kt` | 3 |
 * | `yokai/presentation/settings/screen/data/StorageInfo.kt` | 2 |
 * | `yokai/presentation/component/preference/widget/InfoWidget.kt` | 2 |
 * | `eu/kanade/tachiyomi/ui/crash/CrashActivity.kt` | 2 |
 * | `yokai/presentation/onboarding/OnboardingScreen.kt` | 1 |
 * | `yokai/presentation/extension/repo/component/ExtensionRepoItem.kt` | 1 |
 * | `yokai/presentation/component/LabeledCheckbox.kt` | 1 |
 *
 * Baseline measured at `f00e530e7e`. Re-count at each Phase 4 checkpoint.
 *
 * ### Why it was replaced
 *
 * Three defects, all of which are silently load-bearing:
 *
 * 1. **It has no 20dp step.** The scale jumps 16 -> 24, so "a bit more than 16" had no token
 *    and callers reached for a literal instead. [Spacing.space20] fills that hole.
 * 2. **`smedium` is a typo** — six letters, missing the `e` of `small`. It is the 12dp step and
 *    the third most-used member in the app.
 * 3. **Six of twelve members are dead**: `none`, `smedium`, `large`, `extraLarge`, `extraHuge`,
 *    `navBarSize`. (Four of those have no equivalent on the new scale and keep literal values
 *    below; `smedium` has one and is retained only until its 6 call sites migrate.)
 *
 * ### Values are frozen
 *
 * Every member below carries exactly the value it has always carried. Aliasing onto [Spacing]
 * would be tidier but would couple 40 untouched call sites to a file that is still being
 * edited; `Spacing.legacySizesAgree()` asserts the overlapping members agree instead.
 */
object Size {
    val none = 0.dp
    val extraExtraTiny = 1.dp
    val extraTiny = 2.dp

    /** == `Spacing.space4` */
    val tiny = 4.dp

    /** == `Spacing.space8` */
    val small = 8.dp

    /** Typo'd name for the 12dp step. == `Spacing.space12` */
    val smedium = 12.dp

    /** == `Spacing.space16` */
    val medium = 16.dp

    /** == `Spacing.space24` */
    val large = 24.dp

    /** == `Spacing.space32` */
    val extraLarge = 32.dp

    val huge = 48.dp
    val extraHuge = 56.dp
    val navBarSize = 68.dp
}