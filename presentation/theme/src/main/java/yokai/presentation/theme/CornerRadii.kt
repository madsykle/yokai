package yokai.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Corner radii for the Cupertino surface ladder (`docs/DESIGN_CUPERTINO.md` §3.2).
 *
 * ## These are radii, not the shapes
 *
 * Every entry here is a [RoundedCornerShape]. **None of them is a squircle.** That is
 * deliberate and it is the honest limit of the current module: producing a continuous-curvature
 * outline needs [Superellipse], whose `points()` returns a raw point array to be consumed by a
 * `GenericShape`, and wiring that up belongs to the first component that needs it — not to a
 * token file. `Superellipse` is built (`CORNER_EXPONENT = 4.0`, `DEFAULT_SEGMENTS = 64`) and
 * is **not** rebuilt here.
 *
 * So the rule this file enforces is the one that is cheap to enforce today: radii come from
 * this ladder and nowhere else, so that when the squircle wiring lands it is a one-file change
 * rather than a sweep over hardcoded corners.
 *
 * ## The ladder
 *
 * | token | dp | used by |
 * |---|---|---|
 * | [small] | 10 | Grouped-list container, the lower bound of §3.2's 10-12dp range |
 * | [medium] | 12 | Grouped-list container, the upper bound; cards |
 * | [large] | 16 | Sheets' content blocks, the segmented track at rest |
 * | [extraLarge] | 22 | The floating pill bottom nav (`DESIGN.md` §5.1) |
 *
 * §3.2 specifies grouped-list corners as a *range*, 10-12dp, because iOS tightens the radius as
 * the list grows taller. [small] and [medium] are the two ends of that range.
 */
object CornerRadii {

    /** 10dp — grouped-list container, tighter end. */
    val small: Dp = 10.dp

    /** 12dp — grouped-list container, looser end; cards. */
    val medium: Dp = 12.dp

    /** 16dp — sheet content blocks, segmented track. */
    val large: Dp = 16.dp

    /** 22dp — floating pill bottom nav. */
    val extraLarge: Dp = 22.dp

    /** Every step, ascending. Used by the token test to assert monotonicity in one pass. */
    val all: List<Dp> = listOf(small, medium, large, extraLarge)

    /** 10dp as a shape. */
    val smallShape: RoundedCornerShape = RoundedCornerShape(small)

    /** 12dp as a shape. */
    val mediumShape: RoundedCornerShape = RoundedCornerShape(medium)

    /** 16dp as a shape. */
    val largeShape: RoundedCornerShape = RoundedCornerShape(large)

    /** 22dp as a shape. */
    val extraLargeShape: RoundedCornerShape = RoundedCornerShape(extraLarge)

    /**
     * Uniform shape at [dp], for the case where the ladder does not apply — a circle, a pill
     * whose radius is half its width, a matched pair.
     */
    fun shape(dp: Dp): RoundedCornerShape = RoundedCornerShape(dp)
}