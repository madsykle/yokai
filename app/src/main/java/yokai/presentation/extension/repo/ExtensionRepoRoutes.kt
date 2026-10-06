package yokai.presentation.extension.repo

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 route for the extension repo island (Phase 3).
 *
 * Replaces the single Voyager `Screen` that `ExtensionRepoController` used to host. [repoUrl] is
 * the deep-link payload (`MainActivity` pushes `ExtensionRepoController(repoUrl)` for
 * `tachiyomi://add-repo?url=…`), so it travels on the key rather than on a constructor argument —
 * which is why it survives process death, unlike the Voyager screen it replaces.
 */
@Serializable
data class ExtensionRepoRoute(
    val repoUrl: String? = null,
) : NavKey
