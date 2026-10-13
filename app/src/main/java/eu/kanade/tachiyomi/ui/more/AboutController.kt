package eu.kanade.tachiyomi.ui.more

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import eu.kanade.tachiyomi.data.updater.AppDownloadInstallJob
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.view.setNegativeButton
import eu.kanade.tachiyomi.util.view.setPositiveButton
import eu.kanade.tachiyomi.util.view.setTitle
import io.noties.markwon.Markwon
import yokai.i18n.MR
import yokai.presentation.settings.screen.about.AboutLibraryLicenseRoute
import yokai.presentation.settings.screen.about.AboutLibraryLicenseScreen
import yokai.presentation.settings.screen.about.AboutLicenseRoute
import yokai.presentation.settings.screen.about.AboutLicenseScreen
import yokai.presentation.settings.screen.about.AboutRoute
import yokai.presentation.settings.screen.about.AboutScreen
import android.R as AR

/**
 * Transitional Conductor shell for the About island (Phase 3).
 *
 * [AboutController] stays on Conductor for now — per the Phase 3 ruling it is only here because
 * `MainActivity` pushes it, and it is removed when the last Conductor caller is migrated. The
 * island itself (About -> Licenses -> Library license) is Navigation 3: a single `NavBackStack`
 * hosted by a [NavDisplay] here.
 */
class AboutController : BaseComposeController() {

    @Composable
    override fun ScreenContent() {
        val backStack = rememberNavBackStack(AboutRoute)

        // Back pops the Nav3 stack first; only once it is at the root do we hand back to Conductor
        // so the controller that pushed us (MainActivity / SettingsMainController) is popped.
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
                entry<AboutRoute> {
                    AboutScreen(onOpenLicenses = { backStack.add(AboutLicenseRoute) })
                }
                entry<AboutLicenseRoute> {
                    AboutLicenseScreen(
                        onNavigateUp = onBack,
                        onOpenLibraryLicense = { name, website, license ->
                            backStack.add(
                                AboutLibraryLicenseRoute(name = name, website = website, license = license),
                            )
                        },
                    )
                }
                // clazzContentKey is set explicitly on purpose: the default is
                // `Pair("$key", "$key::class")`, which would bake the entire license HTML string
                // into the saveable-state registry key.
                entry<AboutLibraryLicenseRoute>(
                    clazzContentKey = { "${it.name}|${it.website}" },
                ) {
                    AboutLibraryLicenseScreen(
                        name = it.name,
                        website = it.website,
                        license = it.license,
                        onNavigateUp = onBack,
                    )
                }
            },
        )
    }

    @Deprecated("Use [DialogHostState.showNewUpdateDialog] instead", ReplaceWith("DialogHostState.showNewUpdateDialog()"))
    class NewUpdateDialogController(bundle: Bundle? = null) : DialogController(bundle) {

        constructor(body: String, url: String, isBeta: Boolean?) : this(
            Bundle().apply {
                putString(BODY_KEY, body)
                putString(URL_KEY, url)
                putBoolean(IS_BETA, isBeta == true)
            },
        )

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val info = activity!!.parseReleaseNotes(args.getString(BODY_KEY) ?: "")

            val isOnA12 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val isBeta = args.getBoolean(IS_BETA, false)
            return activity!!.materialAlertDialog()
                .setTitle(
                    if (isBeta) {
                        MR.strings.new_beta_version_available
                    } else {
                        MR.strings.new_version_available
                    },
                )
                .setMessage(info)
                .setPositiveButton(if (isOnA12) MR.strings.update else MR.strings.download) { _, _ ->
                    val appContext = applicationContext
                    if (appContext != null) {
                        // Start download
                        val url = args.getString(URL_KEY) ?: ""
                        AppDownloadInstallJob.start(appContext, url, true)
                    }
                }
                .setNegativeButton(MR.strings.ignore, null)
                .create()
        }

        override fun onAttach(view: View) {
            super.onAttach(view)
            (dialog?.findViewById(AR.id.message) as? TextView)?.movementMethod =
                LinkMovementMethod.getInstance()
        }

        companion object {
            const val BODY_KEY = "NewUpdateDialogController.body"
            const val URL_KEY = "NewUpdateDialogController.key"
            const val IS_BETA = "NewUpdateDialogController.is_beta"
        }
    }
}

fun Context.parseReleaseNotes(releaseNotes: String): Spanned {
    val releaseBody = releaseNotes.replace("""---(\R|.)*Checksums(\R|.)*""".toRegex(), "")
    return Markwon.create(this).toMarkdown(releaseBody)
}
