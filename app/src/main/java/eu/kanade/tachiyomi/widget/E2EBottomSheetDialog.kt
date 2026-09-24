package eu.kanade.tachiyomi.widget

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.doOnNextLayout
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.util.system.mediumImpact
import eu.kanade.tachiyomi.util.system.springFadeIn
import eu.kanade.tachiyomi.util.system.springFadeOut

/**
 * Edge to Edge BottomSheetDialog that uses a custom theme and settings to extend pass the nav bar
 */
@Suppress("LeakingThis")
abstract class E2EBottomSheetDialog<VB : ViewBinding>(activity: Activity) :
    BottomSheetDialog(activity) {
    protected val binding: VB

    protected val sheetBehavior: BottomSheetBehavior<*>
    protected open var recyclerView: RecyclerView? = null

    private val isLight: Boolean
    init {
        binding = createBinding(activity.layoutInflater)
        setContentView(binding.root)

        sheetBehavior = BottomSheetBehavior.from(binding.root.parent as ViewGroup)

        val contentView = binding.root

        val aWic = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        isLight = aWic.isAppearanceLightStatusBars
        window?.let { window ->
            val wic = WindowInsetsControllerCompat(window, binding.root)
            window.navigationBarColor = activity.window.navigationBarColor
            wic.isAppearanceLightNavigationBars = isLight
            // iOS 27 Liquid Glass (DESIGN.md §5.3): the sheet surface itself is OPAQUE
            // (rule 1.2 — no glass on glass); the dimming scrim stays. The previous
            // decorView-wide glass painted a full-screen tint over the whole app.
            //
            // iOS 27 Liquid Glass: Spring animation for sheet enter/exit (Phase 5)
            val decorView = window.decorView
            decorView.alpha = 0f
            decorView.doOnNextLayout {
                decorView.springFadeIn()
            }

            setOnDismissListener { _ ->
                decorView.springFadeOut {
                    if (it) {
                        decorView.mediumImpact() // Haptic on sheet dismiss (§4.5)
                    }
                }
            }
        }
        contentView.requestLayout()
    }

    override fun onStart() {
        super.onStart()
        recyclerView?.let { recyclerView ->
            recyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE ||
                            newState == RecyclerView.SCROLL_STATE_SETTLING
                        ) {
                            sheetBehavior.isDraggable = true
                        } else {
                            sheetBehavior.isDraggable = !recyclerView.canScrollVertically(-1)
                        }
                    }
                },
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        window?.let { window ->
            val wic = WindowInsetsControllerCompat(window, binding.root)
            wic.isAppearanceLightNavigationBars = isLight
        }
    }

    abstract fun createBinding(inflater: LayoutInflater): VB
}
