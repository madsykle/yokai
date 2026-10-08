package yokai.presentation.theme.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import yokai.presentation.theme.CupertinoColors
import yokai.presentation.theme.CupertinoType

/**
 * A Cupertino search bar.
 *
 * Flush, no elevation, no tonal surface, no Material search-view semantics. The bar has **no
 * background at rest** — it appears only when content has scrolled underneath it.
 *
 * ## Why the query is hoisted but focus is not
 *
 * `query` and `onQueryChange` are the caller's. All three of the app's search screens already
 * hold the query in a presenter with side effects welded to it — `LibraryController.search()`
 * (`:1444`) toggles a category row, scrolls to position 0 and mutates the adapter's scrollable
 * headers; `GlobalSearchController` pushes the query *into* the view with `setQuery(...)`
 * (`:190`) and handles URL/extension-intent cases itself. A bar that owned the query would have
 * to reconcile its own state against that on every keystroke.
 *
 * Focus is the opposite case: no consumer has an opinion about whether the field is focused, so
 * making it hoisted would just push IME bookkeeping onto three screens.
 *
 * @param placeholder hint text, already resolved. The component holds no string resources.
 * @param onSubmit IME search action. Null hides nothing but leaves the action unhandled, which is
 *   right for a screen that navigates away instead of running a query.
 * @param onCancel Cancel button. **Null hides the button** — a Cancel that does nothing is worse
 *   than no Cancel. `BrowseController` needs no cancel, which is why this is nullable rather than
 *   a separate `showCancel` flag that could disagree with the callback.
 * @param contentScrolledUnder fed by the caller, who already knows it from their scroll state.
 * @param cancelLabel the Cancel button's text, already resolved by the caller. A component that
 *   hard-coded "Cancel" would be untranslatable, and this app is fully localised.
 */
@Composable
fun CupertinoSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    cancelLabel: String,
    modifier: Modifier = Modifier,
    onSubmit: ((String) -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    contentScrolledUnder: Boolean = false,
    autoFocus: Boolean = false,
) {
    val colors = CupertinoColors.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var focused by remember { mutableStateOf(false) }

    // rememberUpdatedState, not remember: these lambdas capture caller state that changes every
    // recomposition, and a plain remember would pin the first one forever.
    val currentQuery by rememberUpdatedState(query)
    val currentChange by rememberUpdatedState(onQueryChange)
    val currentSubmit by rememberUpdatedState(onSubmit)
    val currentCancel by rememberUpdatedState(onCancel)

    LaunchedEffect(autoFocus) {
        if (autoFocus) focusRequester.requestFocus()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SearchBarMetrics.Height)
            .background(
                color = colors.surfaceChrome.copy(
                    alpha = SearchBarMetrics.barAlpha(contentScrolledUnder),
                ),
                shape = RoundedCornerShape(SearchBarMetrics.Radius),
            )
            .padding(horizontal = SearchBarMetrics.HorizontalInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The iOS "disc": a translucent pill behind the field. Present on focus, which is what
        // makes focusing read as a change of state rather than just a cursor appearing.
        val fieldBackground = if (focused) colors.surfaceRaised else Color.Transparent

        BasicTextField(
            value = query,
            onValueChange = { currentChange(it) },
            singleLine = true,
            textStyle = CupertinoType.subhead.copy(color = colors.labelPrimary),
            cursorBrush = SolidColor(colors.labelPrimary),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                capitalization = KeyboardCapitalization.Sentences,
            ),
            keyboardActions = KeyboardActions(onSearch = { currentSubmit?.invoke(currentQuery) }),
            modifier = Modifier
                .weight(1f)
                .height(SearchBarMetrics.Height)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .background(
                    color = fieldBackground,
                    shape = RoundedCornerShape(SearchBarMetrics.DiscRadius),
                )
                .padding(horizontal = SearchBarMetrics.HorizontalInset),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        BasicText(
                            text = placeholder,
                            // labelSecondary, not a disabled label: the hint is not disabled text,
                            // it is the absence of text.
                            style = CupertinoType.subhead.copy(color = colors.labelSecondary),
                        )
                    }
                    innerTextField()
                }
            },
        )

        if (SearchBarMetrics.showClear(query)) {
            SearchGlyph(
                vector = CupertinoSearchIcons.Clear,
                width = CupertinoSearchIcons.ClearWidth,
                height = CupertinoSearchIcons.ClearHeight,
                tint = colors.labelSecondary,
                onClick = {
                    // Always clears, including whitespace-only: a stray space is invisible in the
                    // field and impossible for the user to delete by aiming.
                    currentChange("")
                },
            )
        } else {
            // Keeps the magnifier out of the layout entirely when an X would show, so the field
            // does not change width as the query grows.
            Image(
                painter = rememberVectorPainter(CupertinoSearchIcons.Magnifier),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.labelSecondary),
                modifier = Modifier.size(
                    CupertinoSearchIcons.MagnifierWidth,
                    CupertinoSearchIcons.MagnifierHeight,
                ),
            )
        }

        if (SearchBarMetrics.showCancel(focused, currentCancel != null)) {
            BasicText(
                text = cancelLabel,
                style = CupertinoType.subhead.copy(color = colors.labelPrimary),
                modifier = Modifier
                    .padding(start = SearchBarMetrics.TrailingSpacing)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        // No haptic: §5 gives the switch toggle none because its animation is the
                        // feedback, and the same argument holds here — the field collapsing back
                        // to its unfocused disc is the acknowledgement.
                        focusManager.clearFocus()
                        currentCancel?.invoke()
                    },
            )
        }
    }
}

/**
 * A tappable glyph, sized in both axes.
 *
 * The glyph is drawn at its natural size. No 44dp padding wrapper: that would widen the field by
 * 32dp for an X that is 12dp wide, and the bar is only 36dp tall to begin with.
 */
@Composable
private fun SearchGlyph(
    vector: ImageVector,
    width: Dp,
    height: Dp,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width, height)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(vector),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(width, height),
        )
    }
}
