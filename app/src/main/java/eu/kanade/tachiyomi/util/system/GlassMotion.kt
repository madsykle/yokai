package eu.kanade.tachiyomi.util.system

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.view.HapticFeedbackConstants
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.core.view.performHapticFeedback

/**
 * iOS 27 Liquid Glass Motion Utilities
 *
 * All animations use spring physics. No linear, no tween().
 * Compose: spring(dampingRatio = 0.75f, stiffness = 300f)
 * Views: SpringAnimation from androidx.dynamicanimation
 * Haptics: Trigger on tab switches, toggles, and sheet dismiss.
 */
object GlassMotion {

    // iOS 27 spring params (from DESIGN.md §4.5)
    private const val DAMPING_RATIO = 0.75f
    private const val STIFFNESS = 300f

    /**
     * Apply spring animation to a View property.
     * Usage: view.springAnimate(View.TRANSLATION_Y, targetValue)
     */
    @SuppressLint("RestrictedApi")
    fun View.springAnimate(property: DynamicAnimation.ViewProperty, targetValue: Float, listener: ((Boolean) -> Unit)? = null) {
        val springForce = SpringForce(targetValue).apply {
            dampingRatio = DAMPING_RATIO
            stiffness = STIFFNESS
        }
        val springAnimation = SpringAnimation(this, property).apply {
            spring = springForce
        }

        listener?.let { callback ->
            springAnimation.addEndListener { _, _, _, isCanceled ->
                callback(!isCanceled)
            }
        }
        springAnimation.start()
    }

    /**
     * Spring animate alpha (fade in/out)
     */
    @SuppressLint("RestrictedApi")
    fun View.springFadeIn(duration: Long = 250, listener: ((Boolean) -> Unit)? = null) {
        alpha = 0f
        isVisible = true
        val springForce = SpringForce(1f).apply {
            dampingRatio = DAMPING_RATIO
            stiffness = STIFFNESS
        }
        val springAnimation = SpringAnimation(this, DynamicAnimation.ALPHA).apply {
            spring = springForce
        }
        listener?.let { callback ->
            springAnimation.addEndListener { _, _, _, isCanceled ->
                callback(!isCanceled)
            }
        }
        springAnimation.start()
    }

    /**
     * Spring animate alpha out
     */
    @SuppressLint("RestrictedApi")
    fun View.springFadeOut(duration: Long = 250, listener: ((Boolean) -> Unit)? = null) {
        val springForce = SpringForce(0f).apply {
            dampingRatio = DAMPING_RATIO
            stiffness = STIFFNESS
        }
        val springAnimation = SpringAnimation(this, DynamicAnimation.ALPHA).apply {
            spring = springForce
        }
        listener?.let { callback ->
            springAnimation.addEndListener { _, _, _, isCanceled ->
                if (!isCanceled) {
                    isVisible = false
                }
                callback(!isCanceled)
            }
        }
        springAnimation.start()
    }

    /**
     * Spring animate translation Y (slide up/down)
     */
    @SuppressLint("RestrictedApi")
    fun View.springSlideY(targetY: Float, listener: ((Boolean) -> Unit)? = null) {
        springAnimate(DynamicAnimation.TRANSLATION_Y, targetY, listener)
    }

    /**
     * Spring animate scale X and Y
     */
    @SuppressLint("RestrictedApi")
    fun View.springScale(targetScale: Float, listener: ((Boolean) -> Unit)? = null) {
        springAnimate(DynamicAnimation.SCALE_X, targetScale)
        springAnimate(DynamicAnimation.SCALE_Y, targetScale, listener)
    }
}

/**
 * Haptic feedback extensions for iOS 27 Liquid Glass
 * DESIGN.md §4.5: Haptics on tab switches, toggles, and sheet dismiss
 */
object GlassHaptics {

    /**
     * Light impact - for tab switches, toggle changes
     */
    fun View.lightImpact() {
        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    /**
     * Medium impact - for sheet dismiss, confirm actions
     */
    fun View.mediumImpact() {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
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
}

/**
 * Extension functions for easier access
 */
fun View.springAnimate(property: DynamicAnimation.ViewProperty, targetValue: Float, listener: ((Boolean) -> Unit)? = null): Unit =
    GlassMotion.springAnimate(this, property, targetValue, listener)

fun View.springFadeIn(duration: Long = 250, listener: ((Boolean) -> Unit)? = null): Unit =
    GlassMotion.springFadeIn(this, duration, listener)

fun View.springFadeOut(duration: Long = 250, listener: ((Boolean) -> Unit)? = null): Unit =
    GlassMotion.springFadeOut(this, duration, listener)

fun View.springSlideY(targetY: Float, listener: ((Boolean) -> Unit)? = null): Unit =
    GlassMotion.springSlideY(this, targetY, listener)

fun View.springScale(targetScale: Float, listener: ((Boolean) -> Unit)? = null): Unit =
    GlassMotion.springScale(this, targetScale, listener)

fun View.lightImpact(): Unit = GlassHaptics.lightImpact(this)
fun View.mediumImpact(): Unit = GlassHaptics.mediumImpact(this)
fun View.heavyImpact(): Unit = GlassHaptics.heavyImpact(this)
fun View.selectionChanged(): Unit = GlassHaptics.selectionChanged(this)

/**
 * Apply spring-based fade in/out to BottomSheetDialog
 */
@SuppressLint("RestrictedApi")
fun com.google.android.material.bottomsheet.BottomSheetDialog.applyGlassSheetAnimation() {
    window?.let { window ->
        val decorView = window.decorView
        // Set initial state for spring animation
        decorView.alpha = 0f
        decorView.doOnNextLayout {
            decorView.springFadeIn()
        }

        setOnDismissListener { dialog ->
            decorView.springFadeOut {
                if (it) dialog.dismiss()
            }
        }
    }
}

/**
 * Apply haptics to BottomNavigationView item selection
 */
fun com.google.android.material.bottomnavigation.BottomNavigationView.applyGlassTabHaptics() {
    setOnNavigationItemSelectedListener { item ->
        // Trigger light impact haptic on tab switch
        item.view?.lightImpact()
        // Note: Return false to allow default navigation handling
        false
    }
}

/**
 * Apply haptics to MaterialAlertDialogBuilder buttons
 */
fun androidx.appcompat.app.AlertDialog.Builder.applyGlassButtonHaptics() {
    // This is a marker - actual haptics applied in GlassAlertDialogBuilder.show()
}

/**
 * Apply spring animation to ViewPropertyAnimator (for existing animations)
 */
fun android.view.ViewPropertyAnimator.withSpring(): android.view.ViewPropertyAnimator {
    // Note: ViewPropertyAnimator doesn't support spring physics directly.
    // Use GlassMotion.springAnimate() instead for spring physics.
    return this
}