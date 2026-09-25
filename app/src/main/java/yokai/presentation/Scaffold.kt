package yokai.presentation

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import dev.icerock.moko.resources.compose.stringResource
import yokai.i18n.MR
import yokai.presentation.component.ToolTipButton
import yokai.presentation.core.JayAppBarScrollBehavior
import yokai.presentation.core.JayExpandedTopAppBar
import yokai.presentation.core.JayTopAppBar
import yokai.presentation.core.enterAlwaysAppBarScrollBehavior
import yokai.presentation.theme.glassBackdropSource
import yokai.presentation.theme.rememberGlassBackdropState

@Composable
fun YokaiScaffold(
    onNavigationIconClicked: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    scrollBehavior: JayAppBarScrollBehavior? = null,
    fab: @Composable () -> Unit = {},
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    navigationIconLabel: String = stringResource(MR.strings.back),
    actions: @Composable RowScope.() -> Unit = {},
    appBarType: AppBarType = AppBarType.LARGE,
    snackbarHost: @Composable () -> Unit = {},
    textFieldState: TextFieldState? = null,
    searchResult: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehaviorOrDefault = scrollBehavior ?: enterAlwaysAppBarScrollBehavior()
    val view = LocalView.current
    val useDarkIcons = MaterialTheme.colorScheme.surface.luminance() > .5
    val (color, scrolledColor) = getTopAppBarColor(title)

    // iOS 27 Liquid Glass (DESIGN.md §3): one source per screen, sampled by the glass chrome.
    val glassBackdrop = rememberGlassBackdropState()

    // DESIGN.md §5.1 "scroll-edge effect": the content's top inset has to stay at the
    // *expanded* app bar height while the bar collapses over it. Material's Scaffold instead
    // passes the bar's current, shrinking height, which keeps the content's top edge glued to
    // the bar's bottom edge - nothing would ever travel behind the glass. iOS large titles
    // behave the way this implements: the inset is the large-title height and never shrinks.
    var expandedAppBarHeight by remember { mutableIntStateOf(0) }

    SideEffect {
        val activity  = view.context as Activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM)
                activity.window.statusBarColor = Color.Transparent.toArgb()
            WindowInsetsControllerCompat(activity.window, view).isAppearanceLightStatusBars = useDarkIcons
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehaviorOrDefault.nestedScrollConnection),
        floatingActionButton = fab,
        topBar = {
            Box(
                Modifier.onSizeChanged { size ->
                    // Only ever grow: the bar reports a smaller height as it collapses, and the
                    // sticky inset has to keep describing the expanded state.
                    if (size.height > expandedAppBarHeight) expandedAppBarHeight = size.height
                },
            ) {
                when (appBarType) {
                    AppBarType.SMALL -> JayTopAppBar(
                        title = {
                            Text(text = title)
                        },
                        // modifier = Modifier.statusBarsPadding(),
                        colors = topAppBarColors(
                            containerColor = color,
                            scrolledContainerColor = scrolledColor,
                        ),
                        navigationIcon = {
                            ToolTipButton(
                                toolTipLabel = navigationIconLabel,
                                icon = navigationIcon,
                                buttonClicked = onNavigationIconClicked,
                            )
                        },
                        scrollBehavior = scrollBehaviorOrDefault,
                        backdrop = glassBackdrop,
                        actions = actions,
                        textFieldState = textFieldState,
                        searchResult = searchResult,
                    )
                    AppBarType.LARGE -> JayExpandedTopAppBar(
                        title = {
                            Text(text = title)
                        },
                        // modifier = Modifier.statusBarsPadding(),
                        colors = topAppBarColors(
                            containerColor = color,
                            scrolledContainerColor = scrolledColor,
                        ),
                        navigationIcon = {
                            ToolTipButton(
                                toolTipLabel = navigationIconLabel,
                                icon = navigationIcon,
                                buttonClicked = onNavigationIconClicked,
                            )
                        },
                        scrollBehavior = scrollBehaviorOrDefault,
                        backdrop = glassBackdrop,
                        actions = actions,
                        textFieldState = textFieldState,
                        searchResult = searchResult,
                    )
                    AppBarType.NONE -> {}
                }
            }
        },
        snackbarHost = snackbarHost,
        content = { paddingValues ->
            val layoutDirection = LocalLayoutDirection.current
            val stickyTopPadding = with(LocalDensity.current) { expandedAppBarHeight.toDp() }
            val glassPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection),
                top = maxOf(paddingValues.calculateTopPadding(), stickyTopPadding),
                end = paddingValues.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding(),
            )

            Box(
                Modifier.then(
                    // Screens without an app bar have no glass chrome to feed, so skip the
                    // extra graphics layer entirely.
                    if (appBarType == AppBarType.NONE) Modifier
                    else Modifier.glassBackdropSource(glassBackdrop),
                ),
            ) {
                content(glassPadding)
            }
        },
    )
}

@Composable
fun getTopAppBarColor(title: String): Pair<Color, Color> {
    return when (title.isEmpty()) {
        true -> Color.Transparent to Color.Transparent
        false -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.primaryContainer
    }
}

enum class AppBarType {
    // FIXME: Delete "NONE" later
    NONE,
    SMALL,
    LARGE,
}
