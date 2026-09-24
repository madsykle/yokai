package yokai.presentation.theme

import android.view.View

/**
 * Keeps the floating glass nav pill (DESIGN.md §5.1) styled as one unit.
 *
 * Applies the tier-aware material tint + the §2.2 decorators (darkened rim,
 * specular highlight) to the [BottomNavigationView]-style pill. Re-invocation is
 * safe (idempotent); the Activity recreates on night-mode changes, which re-runs
 * [attach] from onCreate, so no polling is needed.
 */
object FloatingGlassNavController {

    fun attach(view: View, cornerRadiusDp: Float, tier: GlassTier) {
        view.applyGlass(cornerRadiusDp, tier)
        view.applyGlassDecorators(tier)
    }
}
