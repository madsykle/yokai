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

    /**
     * Circular sibling of [attach] for the standalone glass button that sits next to
     * the nav capsule (ref/Apple Books iOS 7.png: a separate round control, same
     * material, deliberately not part of the capsule).
     *
     * The pill's corner radius cannot describe a circle before the view is measured,
     * so the radius is derived from the declared or measured size. [View.applyGlass]
     * then clips with an oval, which keeps the shipped ripple round too.
     */
    fun attachCircle(view: View, tier: GlassTier) {
        val density = view.resources.displayMetrics.density
        val declaredSize = view.layoutParams?.width ?: 0
        val sizePx = view.width.takeIf { it > 0 }
            ?: declaredSize.takeIf { it > 0 }
            ?: (48 * density).toInt()

        view.applyGlass(cornerRadiusDp = sizePx / 2f / density, tier = tier, circle = true)
        view.applyGlassDecorators(tier)
    }
}
