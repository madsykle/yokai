package eu.kanade.tachiyomi.util.system

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * iOS 27 Liquid Glass Motion Utilities (DESIGN.md §4.5)
 *
 * All animations use spring physics. No linear, no tween().
 * Compose: spring(dampingRatio = 0.75f, stiffness = 300f)
 * Views: SpringAnimation from androidx.dynamicanimation
 * Haptics: Trigger on tab switches, toggles, and sheet dismiss.
 *
 * All helpers are top-level [View] extensions so they can be imported and used
 * directly on any View (member extensions inside an object would not resolve).
 */

// iOS 27 spring params (DESIGN.md §4.5)
private const val GLASS_DAMPING_RATIO = 0.75f
private const val GLASS_STIFFNESS = 300f

private fun springAnimation(
    view: View,
    property: DynamicAnimation.ViewProperty,
    targetValue: Float,
    listener: ((Boolean) -> Unit)?,
): SpringAnimation {
    val springForce = SpringForce(targetValue).apply {
        dampingRatio = GLASS_DAMPING_RATIO
        stiffness = GLASS_STIFFNESS
    }
    return SpringAnimation(view, property).apply {
        spring = springForce
        listener?.let { callback ->
            // OnAnimationEndListener params: (animation, canceled, value, velocity)
            addEndListener { _, canceled, _, _ ->
                callback(!canceled)
            }
        }
        start()
    }
}

/**
 * Spring-animate a View property.
 * Usage: view.springAnimate(View.TRANSLATION_Y, targetValue)
 */
fun View.springAnimate(
    property: DynamicAnimation.ViewProperty,
    targetValue: Float,
    listener: ((Boolean) -> Unit)? = null,
) {
    springAnimation(this, property, targetValue, listener)
}

/**
 * Spring animate alpha (fade in, 0 → 1)
 */
fun View.springFadeIn(listener: ((Boolean) -> Unit)? = null) {
    alpha = 0f
    isVisible = true
    springAnimation(this, DynamicAnimation.ALPHA, 1f, listener)
}

/**
 * Spring animate alpha out (fade out, 1 → 0); hides the view when settled
 */
fun View.springFadeOut(listener: ((Boolean) -> Unit)? = null) {
    springAnimation(this, DynamicAnimation.ALPHA, 0f) { completed ->
        if (completed) {
            isVisible = false
        }
        listener?.invoke(completed)
    }
}

/**
 * Spring animate translation Y (slide up/down)
 */
fun View.springSlideY(targetY: Float, listener: ((Boolean) -> Unit)? = null) {
    springAnimate(DynamicAnimation.TRANSLATION_Y, targetY, listener)
}

/**
 * Spring animate scale X and Y
 */
fun View.springScale(targetScale: Float, listener: ((Boolean) -> Unit)? = null) {
    springAnimate(DynamicAnimation.SCALE_X, targetScale)
    springAnimate(DynamicAnimation.SCALE_Y, targetScale, listener)
}

/**
 * Light impact - for tab switches, toggle changes (DESIGN.md §4.5)
 */
fun View.lightImpact() {
    performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
}

/**
 * Medium impact - for sheet dismiss, confirm actions (DESIGN.md §4.5)
 * [HapticFeedbackConstants.CONFIRM] requires API 30; falls back to VIRTUAL_KEY.
 */
fun View.mediumImpact() {
    val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.VIRTUAL_KEY
    }
    performHapticFeedback(constant)
}

/**
 * Heavy impact - for destructive actions
 */
fun View.heavyImpact() {
    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

/**
 * Selection changed - for picker changes, slider moves
 */
fun View.selectionChanged() {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}
