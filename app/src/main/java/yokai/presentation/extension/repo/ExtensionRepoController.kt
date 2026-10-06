package yokai.presentation.extension.repo

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController

/**
 * Transitional Conductor shell for the extension repo island (Phase 3).
 *
 * Same shape as `AboutController` and `SettingsComposeController`: the Conductor controller stays
 * because `MainActivity`, `BrowseController` and `SettingsBrowseController` all push it, and the
 * screen itself is Navigation 3 — a single `NavBackStack` hosted by a [NavDisplay] here.
 */
class ExtensionRepoController(private val repoUrl: String? = null) : BaseComposeController() {

    @Composable
    override fun ScreenContent() {
        val backStack = rememberNavBackStack(ExtensionRepoRoute(repoUrl))

        // This island is depth 1, so `onBack` always falls through to Conductor. The branch is kept
        // so the shape matches the other migrated islands if a nested destination is ever added.
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
                // clazzContentKey is set explicitly on purpose: the default is
                // `Pair("$key", "$key::class")`, which would bake the deep-linked repo URL into the
                // saveable-state registry key.
                entry<ExtensionRepoRoute>(
                    clazzContentKey = { it.repoUrl ?: "" },
                ) {
                    ExtensionRepoScreen(
                        // WART: hardcoded English string, not a string resource. Pre-existing;
                        // deliberately not changed by the migration.
                        title = "Extension Repos",
                        repoUrl = it.repoUrl,
                        onBackPress = onBack,
                    )
                }
            },
        )
    }
}
