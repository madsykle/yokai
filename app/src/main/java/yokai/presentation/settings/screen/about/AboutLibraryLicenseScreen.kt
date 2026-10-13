package yokai.presentation.settings.screen.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.google.android.material.textview.MaterialTextView
import dev.icerock.moko.resources.compose.stringResource
import yokai.i18n.MR
import yokai.presentation.AppBarType
import yokai.presentation.YokaiScaffold
import yokai.presentation.component.ToolTipButton
import yokai.presentation.core.enterAlwaysAppBarScrollBehavior

@Composable
fun AboutLibraryLicenseScreen(
    name: String,
    website: String?,
    license: String,
    onNavigateUp: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    YokaiScaffold(
        onNavigationIconClicked = onNavigateUp,
        title = name,
        appBarType = AppBarType.SMALL,
        scrollBehavior = enterAlwaysAppBarScrollBehavior(),
        actions = {
            if (website != null) {
                ToolTipButton(
                    toolTipLabel = stringResource(MR.strings.website),
                    icon = Icons.Outlined.Public,
                    buttonClicked = { uriHandler.openUri(website) },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            HtmlLicenseText(html = license)
        }
    }
}

@Composable
private fun HtmlLicenseText(html: String) {
    AndroidView(
        factory = {
            MaterialTextView(it)
        },
        update = {
            it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        },
    )
}
