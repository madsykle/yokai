package yokai.presentation.extension.repo

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExtensionOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.util.compose.LocalDialogHostState
import eu.kanade.tachiyomi.util.compose.currentOrThrow
import eu.kanade.tachiyomi.util.isTablet
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import yokai.domain.DialogHostState
import yokai.domain.extension.repo.model.ExtensionRepo
import yokai.i18n.MR
import yokai.presentation.AppBarType
import yokai.presentation.YokaiScaffold
import yokai.presentation.component.EmptyScreen
import yokai.presentation.component.ToolTipButton
import yokai.presentation.core.enterAlwaysAppBarScrollBehavior
import yokai.presentation.extension.repo.component.ExtensionRepoInput
import yokai.presentation.extension.repo.component.ExtensionRepoItem
import android.R as AR

/**
 * Extension repo list / add-repo screen, hosted by the [ExtensionRepoRoute] Nav3 entry.
 *
 * Was a Voyager `Screen` pushed as the sole screen of `ExtensionRepoController`'s navigator.
 * [onBackPress] is the island's Nav3 `onBack`, replacing `LocalBackPress`.
 */
@Composable
fun ExtensionRepoScreen(
    title: String,
    repoUrl: String?,
    onBackPress: () -> Unit,
) {
    val context = LocalContext.current
    val alertDialog = LocalDialogHostState.currentOrThrow

    val scope = rememberCoroutineScope()
    val screenModel = remember(scope) { ExtensionRepoScreenModel(scope) }
    val state = screenModel.state

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // The deep-link repo URL must be added exactly once per controller instance. The old Voyager
    // screen self-nulled a `private var repoUrl` to achieve that, which died with the composition.
    // `rememberSaveable` is scoped to this Nav3 entry's contentKey by
    // SaveableStateHolderNavEntryDecorator (applied by default in NavDisplay), so the flag survives
    // configuration change and process death, and is discarded when the entry is popped.
    var repoUrlConsumed by rememberSaveable { mutableStateOf(false) }

    YokaiScaffold(
        onNavigationIconClicked = onBackPress,
        title = title,
        appBarType = AppBarType.SMALL,
        scrollBehavior = enterAlwaysAppBarScrollBehavior(
            canScroll = { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 },
        ),
        actions = {
            ToolTipButton(
                toolTipLabel = stringResource(MR.strings.refresh),
                icon = Icons.Outlined.Refresh,
                buttonClicked = {
                    context.toast("Refreshing...")  // TODO: Should be loading animation instead
                    screenModel.refreshRepos()
                },
            )
        },
    ) { innerPadding ->
        if (state is ExtensionRepoScreenModel.State.Loading) return@YokaiScaffold

        val repos = (state as ExtensionRepoScreenModel.State.Success).repos

        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            userScrollEnabled = true,
            verticalArrangement = Arrangement.Top,
            state = listState,
        ) {
            item {
                ExtensionRepoInput(
                    inputText = inputText,
                    inputHint = stringResource(MR.strings.label_add_repo),
                    onInputChange = { inputText = it },
                    onAddClick = { screenModel.addRepo(it) },
                )
            }

            if (repos.isEmpty()) {
                item {
                    EmptyScreen(
                        modifier = Modifier.fillParentMaxSize(),
                        image = Icons.Filled.ExtensionOff,
                        message = stringResource(MR.strings.information_empty_repos),
                        isTablet = isTablet(),
                    )
                }
                return@LazyColumn
            }

            repos.forEach { repo ->
                item {
                    ExtensionRepoItem(
                        extensionRepo = repo,
                        onDeleteClick = { repoToDelete ->
                            scope.launch { alertDialog.awaitExtensionRepoDeletePrompt(repoToDelete, screenModel) }
                        },
                    )
                }
            }
        }

        alertDialog.value?.invoke()
    }

    LaunchedEffect(repoUrl) {
        if (!repoUrlConsumed && repoUrl != null) {
            repoUrlConsumed = true
            screenModel.addRepo(repoUrl)
        }
    }

    LaunchedEffect(Unit) {
        screenModel.event.collectLatest { event ->
            when (event) {
                is ExtensionRepoEvent.LocalizedMessage -> context.toast(event.stringRes)
                is ExtensionRepoEvent.Success -> inputText = ""
                is ExtensionRepoEvent.ShowDialog -> {
                    when(event.dialog) {
                        is RepoDialog.Conflict -> {
                            alertDialog.awaitExtensionRepoReplacePrompt(
                                oldRepo = event.dialog.oldRepo,
                                newRepo = event.dialog.newRepo,
                                onMigrate = { screenModel.replaceRepo(event.dialog.newRepo) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun DialogHostState.awaitExtensionRepoReplacePrompt(
    oldRepo: ExtensionRepo,
    newRepo: ExtensionRepo,
    onMigrate: () -> Unit,
): Unit = dialog { cont ->
    AlertDialog(
        onDismissRequest = { cont.cancel() },
        confirmButton = {
            TextButton(
                onClick = {
                    onMigrate()
                    cont.cancel()
                },
            ) {
                Text(text = stringResource(MR.strings.action_replace_repo))
            }
        },
        dismissButton = {
            TextButton(onClick = { cont.cancel() }) {
                Text(text = stringResource(AR.string.cancel))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.action_replace_repo_title))
        },
        text = {
            Text(text = stringResource(MR.strings.action_replace_repo_message, newRepo.name, oldRepo.name))
        },
    )
}

private suspend fun DialogHostState.awaitExtensionRepoDeletePrompt(
    repoToDelete: String,
    screenModel: ExtensionRepoScreenModel,
): Unit = dialog { cont ->
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(MR.strings.confirm_delete_repo_title),
                fontStyle = MaterialTheme.typography.titleMedium.fontStyle,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
            )
        },
        text = {
            Text(
                text = stringResource(MR.strings.confirm_delete_repo, repoToDelete),
                fontStyle = MaterialTheme.typography.bodyMedium.fontStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
        },
        onDismissRequest = { cont.cancel() },
        confirmButton = {
            TextButton(
                onClick = {
                    screenModel.deleteRepo(repoToDelete)
                    cont.cancel()
                },
            ) {
                Text(
                    text = stringResource(MR.strings.delete),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { cont.cancel() }) {
                Text(
                    text = stringResource(MR.strings.cancel),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                )
            }
        },
    )
}
