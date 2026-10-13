package eu.kanade.tachiyomi.ui.setting

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import yokai.presentation.settings.ComposableSettings
import yokai.presentation.settings.screen.SettingsAdvancedRoute
import yokai.presentation.settings.screen.SettingsAdvancedScreen
import yokai.presentation.settings.screen.SettingsDataRoute
import yokai.presentation.settings.screen.SettingsDataScreen
import yokai.presentation.settings.screen.StorybookRoute
import yokai.presentation.settings.screen.advanced.StoryBookScreen

/**
 * Transitional Conductor shell for the settings Compose island (Phase 3).
 *
 * Same shape as `AboutController`: the Conductor controller stays because `SettingsMainController`
 * pushes it, and the island's screens (Advanced -> Storybook, Data) are Navigation 3 — a single
 * `NavBackStack` hosted by a [NavDisplay] here.
 */
abstract class SettingsComposeController: BaseComposeController(), SettingsControllerInterface {
    override fun getTitle(): String? = __getTitle()
    override fun getSearchTitle(): String? = __getTitle()

    fun setTitle() = __setTitle()

    abstract fun getComposableSettings(): ComposableSettings

    @Composable
    override fun ScreenContent() {
        val backStack = rememberNavBackStack(getComposableSettings().route)

        // Back pops the Nav3 stack first; only once it is at the root do we hand back to Conductor
        // so the controller that pushed us (SettingsMainController) is popped.
        val onBack: () -> Unit = {
            if (backStack.size > 1) backStack.removeLastOrNull() else router.handleBack()
        }

        NavDisplay(
            backStack = backStack,
            onBack = onBack,
            // Keeps the crossfade look this island had under Voyager's CrossfadeTransition.
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            popTransitionSpec = { fadeIn() togetherWith fadeOut() },
            predictivePopTransitionSpec = { fadeIn() togetherWith fadeOut() },
            entryProvider = entryProvider<NavKey> {
                entry<SettingsAdvancedRoute> {
                    SettingsAdvancedScreen.Content(
                        onOpenStorybook = { backStack.add(StorybookRoute) },
                        onNavigateUp = onBack,
                    )
                }
                entry<SettingsDataRoute> {
                    SettingsDataScreen.Content(
                        // This screen has no nested destinations.
                        onOpenStorybook = {},
                        onNavigateUp = onBack,
                    )
                }
                entry<StorybookRoute> {
                    StoryBookScreen(onNavigateUp = onBack)
                }
            },
        )
    }
}
