package yokai.presentation.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.navigation3.runtime.NavKey
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import yokai.presentation.component.preference.Preference

/**
 * Describes one settings screen: its title, its preference tree and its Navigation 3 [route].
 *
 * No longer a Voyager `Screen` — it is now a plain description rendered by whichever Nav3 entry
 * hosts it. [Content] is the single rendering path, mirroring the `Content()` this class used to
 * override on Voyager's `Screen`.
 *
 * [onOpenStorybook] is threaded as an explicit parameter rather than held as a field or read from
 * a `CompositionLocal`: these are `object` singletons, so a field would leak the callback across
 * recompositions.
 */
abstract class ComposableSettings {

    @Composable
    @ReadOnlyComposable
    abstract fun getTitleRes(): StringResource

    /** The Navigation 3 key this screen is hosted under. */
    abstract val route: NavKey

    @Composable
    abstract fun getPreferences(onOpenStorybook: () -> Unit): List<Preference>

    @Composable
    open fun RowScope.AppBarAction() {}

    @Composable
    fun Content(
        onOpenStorybook: () -> Unit,
        onNavigateUp: () -> Unit,
    ) {
        SettingsScaffold(
            title = stringResource(getTitleRes()),
            itemsProvider = { getPreferences(onOpenStorybook) },
            appBarActions = { AppBarAction() },
            onNavigateUp = onNavigateUp,
        )
    }

    companion object {
        // HACK: for the background blipping thingy.
        // The title of the target PreferenceItem
        // Set before showing the destination screen and reset after
        // See BasePreferenceWidget.highlightBackground
        var highlightKey: String? = null
    }
}
