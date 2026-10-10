package yokai.presentation.settings.screen.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.util.htmlReadyLicenseContent
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.R
import yokai.i18n.MR
import yokai.presentation.AppBarType
import yokai.presentation.YokaiScaffold
import yokai.presentation.core.pinnedAppBarScrollBehavior

@Composable
fun AboutLicenseScreen(
    onNavigateUp: () -> Unit,
    onOpenLibraryLicense: (name: String, website: String?, license: String) -> Unit,
) {
    val libraries by produceLibraries(R.raw.aboutlibraries)

    YokaiScaffold(
        onNavigationIconClicked = onNavigateUp,
        title = stringResource(MR.strings.open_source_licenses),
        appBarType = AppBarType.SMALL,
        scrollBehavior = pinnedAppBarScrollBehavior(),
    ) { innerPadding ->
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            onLibraryClick = {
                onOpenLibraryLicense(
                    it.name,
                    it.website,
                    it.licenses.firstOrNull()?.htmlReadyLicenseContent.orEmpty(),
                )
            }
        )
    }
}
