package yokai.presentation.theme

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

/**
 * Haptic vocabulary (`docs/DESIGN_CUPERTINO.md` §5). Four functions, one per interaction class.
 *
 * ## Why four and not seven
 *
 * §5 is a **table**, not a spectrum: every interaction has exactly one answer. `hapticHeavy`,
 * `hapticSuccess` and `hapticError` are deliberately **not implemented** — deferred until a
 * screen actually needs one, because at that point the concrete use tells us the right answer
 * and today it does not:
 *
 * - **Heavy** — `View.performHapticFeedback` has no heavy constant distinct from `LONG_PRESS`.
 *   Mapping `heavy -> LONG_PRESS` would make it a synonym for [hapticMedium], which is worse
 *   than not having it. If a screen needs a real heaviness it is a `Vibrator` path with an
 *   explicit settings check.
 * - **Success** — `CONFIRM` is the plausible mapping, but [hapticWarning] already owns it.
 * - **Error** — `REJECT` looks right, but it is API 30+ and the AOSP server-side mapping for it
 *   (`PhoneWindowManager.getVibrationEffect`, android-10.0.0_r1:5333) is `EFFECT_DOUBLE_CLICK`,
 *   which is not obviously an error to a user who cannot see the screen.
 *
 * **Do not invent these mappings.** Add them when a screen needs one.
 *
 * ## Why `LocalView` + `performHapticFeedback`, not `Vibrator`
 *
 * `View.performHapticFeedback` routes to the window manager, which checks the user's touch
 * feedback setting and drops the request when it is off —
 * `PhoneWindowManager.performHapticFeedbackLw` reads
 * `Settings.System.HAPTIC_FEEDBACK_ENABLED` and returns `false` when disabled
 * (android-9.0.0_r1:8045-8048). §5 requires "all haptics respect the system touch-feedback
 * setting", and this path satisfies that by construction.
 *
 * `Vibrator` / `VibratorManager` **bypass** that check. Using one would mean re-implementing
 * the setting lookup by hand — more code, and a way to get it wrong. Compose has no
 * `LocalHapticFeedback`; `LocalView` is the idiomatic route.
 *
 * ## Accessibility
 *
 * Never haptic-gate a destructive action without an on-screen consequence too. Haptics are
 * ambient reinforcement, not the only channel.
 */

/**
 * Destructive confirm — the only heavy haptic in the app. **Reserve it.**
 *
 * If it fires routinely it stops meaning anything, and a delete you cannot feel is a delete
 * people get wrong.
 *
 * ### Why this one needs a version guard
 *
 * `CONFIRM` is **first public in API 30** (verified against AOSP
 * `frameworks/base/api/current.txt` at `android-8.0.0_r1` … `android-11.0.0_r1`, where the
 * field is absent from `HapticFeedbackConstants` through API 29 and present at 30). Calling it
 * unguarded would not throw — it would compile against our compileSdk 36 and then do nothing on
 * an older device:
 *
 * - `View.performHapticFeedback(int, int)` does **no validation** — it null-checks
 *   `mAttachInfo`, checks the per-view setting, then forwards the raw int verbatim to
 *   `mRootCallbacks.performHapticFeedback(constant, ignoreGlobal)`
 *   (android-9.0.0_r1, `core/java/android/view/View.java:23668`).
 * - The decision happens server-side in `PhoneWindowManager.getVibrationEffect(int effectId)`:
 *
 *   | API | source | `CONFIRM` case | net effect |
 *   |---|---|---|---|
 *   | 26 | android-8.0.0_r1:7710 | absent -> `default: return null` | **silent no-op** |
 *   | 27 | android-8.1.0_r1 | absent -> `default: return null` | **silent no-op** |
 *   | 28 | android-9.0.0_r1:8085 | `-> VibrationEffect.EFFECT_CLICK` | works |
 *   | 29 | android-10.0.0_r1:5327 | `-> EFFECT_CLICK` | works |
 *   | 30+ | public API | `-> EFFECT_CLICK` | works |
 *
 *   `performHapticFeedbackLw` returns `false` when the effect is null, so on Android 8.0 and
 *   8.1 the destructive confirm would be **completely silent** — no crash, no log, no buzz.
 *
 * ### Why the guard is at 30 and not 28
 *
 * AOSP 28 and 29 do handle `CONFIRM`, but that is an *unpublicised internal* implementation
 * detail. OEM forks at those levels are under no obligation to share it, and keying off it
 * would be exactly the kind of assumption that works on the test device and fails in the field.
 * The guard keys on the public contract instead.
 *
 * The practical cost is nil and arguably a gain: on API 28-29 callers get `LONG_PRESS` ->
 * `EFFECT_HEAVY_CLICK` rather than `EFFECT_CLICK`, which is more emphatic — and more appropriate
 * for the one interaction this haptic is reserved for.
 */
@Composable
fun hapticWarning() {
    val view = LocalView.current
    val constant =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
    view.performHapticFeedback(constant)
}

/**
 * Tab switch, sheet dismiss.
 *
 * Dismiss reads as "acknowledged, gone" against [hapticSelection]'s "something is arriving" for
 * a sheet's present. That asymmetry is **independent of motion**: §4.2 rule 4 governs springs
 * (the dismiss animation is 180/0.90 against the present's 300/0.75) and says nothing about
 * which haptic fires. An earlier draft of §5 cross-referenced the two and got it backwards;
 * the doc is fixed and now forbids reintroducing that link.
 */
@Composable
fun hapticLight() {
    LocalView.current.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
}

/**
 * Long-press to select, drag-reorder pickup. One gesture, one buzz.
 */
@Composable
fun hapticMedium() {
    LocalView.current.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

/**
 * Segment change, sheet present, search committed.
 *
 * Fires *while* a control is being dragged, so it must stay small.
 *
 * On stock Android this is barely distinguishable from [hapticLight]: `PhoneWindowManager`
 * maps both `CLOCK_TICK` and `CONTEXT_CLICK` to the same
 * `VibrationEffect.EFFECT_TICK` (android-9.0.0_r1:8072-8074). They stay separate anyway
 * because they mean different things in §5 and because OEM servers remap these freely — code
 * to the meaning, not to the waveform.
 */
@Composable
fun hapticSelection() {
    LocalView.current.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}