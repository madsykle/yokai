package yokai.presentation.settings.screen

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 routes for the settings Compose island (Phase 3).
 *
 * These replace the Voyager `Screen` stack that `SettingsComposeController` used to host. Same
 * rationale as [yokai.presentation.settings.screen.about.AboutRoutes]: [NavKey] is a marker
 * interface, and `@Serializable` is what allows Nav3 to persist the back stack through process
 * death.
 *
 * All three are payload-free objects, so the default `clazzContentKey`
 * (`Pair("$key", "$key::class")`) is safe here — there is nothing large to bake into the
 * saveable-state registry key.
 */

@Serializable
data object SettingsAdvancedRoute : NavKey

@Serializable
data object SettingsDataRoute : NavKey

@Serializable
data object StorybookRoute : NavKey
