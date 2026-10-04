package yokai.presentation.settings.screen.about

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 routes for the About island (Phase 3 pilot).
 *
 * Voyager's `Screen` + `uniqueScreenKey` is replaced by these typed keys. [NavKey] is a marker
 * interface; the `@Serializable` annotation is what allows Nav3 to persist the back stack through
 * process death via `rememberNavBackStack(SavedStateConfiguration, …)`.
 */

@Serializable
data object AboutRoute : NavKey

@Serializable
data object AboutLicenseRoute : NavKey

@Serializable
data class AboutLibraryLicenseRoute(
    val name: String,
    val website: String? = null,
    val license: String,
) : NavKey
