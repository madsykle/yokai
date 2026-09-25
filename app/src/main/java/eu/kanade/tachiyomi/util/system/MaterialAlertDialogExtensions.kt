package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckedTextView
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.databinding.CustomDialogTitleMessageBinding
import eu.kanade.tachiyomi.databinding.DialogQuadstateBinding
import eu.kanade.tachiyomi.databinding.DialogTextInputBinding
import eu.kanade.tachiyomi.widget.TriStateCheckBox
import eu.kanade.tachiyomi.widget.materialdialogs.TriStateMultiChoiceDialogAdapter
import eu.kanade.tachiyomi.widget.materialdialogs.TriStateMultiChoiceListener
import yokai.presentation.theme.GlassColors
import yokai.presentation.theme.GlassTier
import yokai.presentation.theme.glassTier
import yokai.presentation.theme.toArgbCompat
import yokai.util.lang.getString

/**
 * Custom MaterialAlertDialogBuilder that applies iOS 27 Liquid Glass styling on show.
 */
class GlassAlertDialogBuilder(context: Context) : MaterialAlertDialogBuilder(context) {
    override fun show(): AlertDialog {
        val dialog = super.show()
        dialog.applyGlassDialog()

        // iOS 27 Liquid Glass: haptic feedback on dialog buttons (§4.5).
        // Uses an OnTouchListener that returns false so Material's internal click
        // handler (dismiss + listener dispatch) is preserved — replacing
        // setOnClickListener here would make every dialog button dead.
        fun android.widget.Button.hapticOnPress() {
            setOnTouchListener { v, event ->
                if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
                    v.lightImpact()
                }
                false
            }
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.hapticOnPress()
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.hapticOnPress()
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.hapticOnPress()

        return dialog
    }
}

fun Context.materialAlertDialog(): MaterialAlertDialogBuilder = GlassAlertDialogBuilder(withOriginalWidth())

fun MaterialAlertDialogBuilder.addCheckBoxPrompt(
    stringRes: StringResource,
    isChecked: Boolean = false,
    listener: MaterialAlertDialogBuilderOnCheckClickListener? = null,
): MaterialAlertDialogBuilder {
    return addCheckBoxPrompt(context.getString(stringRes), isChecked, listener)
}

fun MaterialAlertDialogBuilder.addCheckBoxPrompt(
    text: CharSequence,
    isChecked: Boolean = false,
    listener: MaterialAlertDialogBuilderOnCheckClickListener? = null,
): MaterialAlertDialogBuilder {
    return setMultiChoiceItems(
        arrayOf(text),
        booleanArrayOf(isChecked),
    ) { dialog, _, checked ->
        listener?.onClick(dialog, checked)
    }
}

fun AlertDialog.disableItems(items: Array<String>) {
    val listView = listView ?: return
    listView.setOnHierarchyChangeListener(
        object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View) {
                val text = (child as? AppCompatCheckedTextView)?.text ?: return
                if (items.contains(text)) {
                    child.setOnClickListener(null)
                    child.isEnabled = false
                } else {
                    child.isEnabled = true
                }
            }

            override fun onChildViewRemoved(view: View?, view1: View?) {}
        },
    )
}

fun MaterialAlertDialogBuilder.setCustomTitleAndMessage(title: StringResource, message: String): MaterialAlertDialogBuilder {
    return setCustomTitle(
        (CustomDialogTitleMessageBinding.inflate(LayoutInflater.from(context))).apply {
            if (title.resourceId != 0) {
                alertTitle.text = context.getString(title)
            } else {
                alertTitle.isVisible = false
            }
            this.message.text = message
        }.root,
    )
}

fun MaterialAlertDialogBuilder.setCustomTitleAndMessage(title: Int, message: String): MaterialAlertDialogBuilder {
    return setCustomTitle(
        (CustomDialogTitleMessageBinding.inflate(LayoutInflater.from(context))).apply {
            if (title != 0) {
                alertTitle.text = context.getString(title)
            } else {
                alertTitle.isVisible = false
            }
            this.message.text = message
        }.root,
    )
}

/**
 * A variant of listItemsMultiChoice that allows for checkboxes that supports 3 states instead.
 */
@CheckResult
internal fun MaterialAlertDialogBuilder.setTriStateItems(
    message: String? = null,
    items: List<CharSequence>,
    disabledIndices: IntArray? = null,
    initialSelection: IntArray = IntArray(items.size),
    skipChecked: Boolean = false,
    selection: TriStateMultiChoiceListener?,
): MaterialAlertDialogBuilder {
    val binding = DialogQuadstateBinding.inflate(LayoutInflater.from(context))
    binding.list.layoutManager = LinearLayoutManager(context)
    binding.list.adapter = TriStateMultiChoiceDialogAdapter(
        dialog = this,
        items = items,
        disabledItems = disabledIndices,
        initialSelection = initialSelection,
        skipChecked = skipChecked,
        listener = selection,
    )
    val updateScrollIndicators = {
        binding.scrollIndicatorUp.isVisible = binding.list.canScrollVertically(-1)
        binding.scrollIndicatorDown.isVisible = binding.list.canScrollVertically(1)
    }
    binding.list.setOnScrollChangeListener { _, _, _, _, _ ->
        updateScrollIndicators()
    }
    binding.list.post {
        updateScrollIndicators()
    }

    if (message != null) {
        binding.message.text = message
        binding.message.isVisible = true
    }
    return setView(binding.root)
}

internal fun MaterialAlertDialogBuilder.setNegativeStateItems(
    items: List<CharSequence>,
    initialSelection: BooleanArray = BooleanArray(items.size),
    listener: DialogInterface.OnMultiChoiceClickListener,
): MaterialAlertDialogBuilder {
    return setTriStateItems(
        items = items,
        initialSelection = initialSelection.map {
            if (it) {
                TriStateCheckBox.State.IGNORE.ordinal
            } else {
                TriStateCheckBox.State.UNCHECKED.ordinal
            }
        }
            .toIntArray(),
        skipChecked = true,
    ) { _, _, _, index, state ->
        listener.onClick(null, index, state == TriStateCheckBox.State.IGNORE.ordinal)
    }
}

val DialogInterface.isPromptChecked: Boolean
    get() = (this as? AlertDialog)?.listView?.isItemChecked(0) ?: false

fun interface MaterialAlertDialogBuilderOnCheckClickListener {
    fun onClick(var1: DialogInterface?, var3: Boolean)
}

fun MaterialAlertDialogBuilder.setTextInput(
    hint: String? = null,
    prefill: String? = null,
    onTextChanged: (String) -> Unit,
): MaterialAlertDialogBuilder {
    val binding = DialogTextInputBinding.inflate(LayoutInflater.from(context))
    binding.textField.hint = hint
    binding.textField.editText?.apply {
        setText(prefill, TextView.BufferType.EDITABLE)
        doAfterTextChanged {
            onTextChanged(it?.toString() ?: "")
        }
        post {
            requestFocusFromTouch()
            context.getSystemService<InputMethodManager>()?.showSoftInput(this, 0)
        }
    }
    return setView(binding.root)
}

/**
 * Applies iOS 27 Liquid Glass styling to the created dialog.
 *
 * DESIGN.md §5.3: dialog surfaces are OPAQUE — the dim scrim behind the window
 * is what separates them from content (§1.2: no glass on glass). The previous
 * implementation painted tint on the FULL-SCREEN decorView, which washed the
 * entire app behind the dialog. Here the window background is replaced with a
 * rounded (28dp, §4.3) opaque surface carrying the §2.2 darkened rim.
 */
fun AlertDialog.applyGlassDialog() {
    val tier = glassTier()
    val isDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    val d = context.resources.displayMetrics.density
    val baseColor = if (isDark) GlassColors.GlassDarkBase else GlassColors.GlassLightBase

    val background = MaterialShapeDrawable().apply {
        shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setAllCornerSizes(28 * d)
            .build()
        fillColor = ColorStateList.valueOf(baseColor.toArgbCompat())
        // Mode-aware rim (see applyGlassDecorators): on the dark dialog surface a black rim
        // is invisible, so dark mode rims light.
        setStroke(
            1.coerceAtLeast((0.5 * d).toInt()).toFloat(),
            (if (isDark) GlassColors.DarkenedEdgeDark else GlassColors.DarkenedEdge).toArgbCompat(),
        )
    }
    window?.setBackgroundDrawable(background)
}

/**
 * Creates a MaterialAlertDialogBuilder with glass styling applied automatically.
 * (Same as materialAlertDialog() now - kept for backwards compatibility)
 */
fun Context.materialGlassAlertDialog() = materialAlertDialog()
