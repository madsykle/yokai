package yokai.presentation.extension.repo

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import uy.kohesive.injekt.injectLazy
import yokai.domain.extension.repo.interactor.CreateExtensionRepo
import yokai.domain.extension.repo.interactor.DeleteExtensionRepo
import yokai.domain.extension.repo.interactor.GetExtensionRepo
import yokai.domain.extension.repo.interactor.ReplaceExtensionRepo
import yokai.domain.extension.repo.interactor.UpdateExtensionRepo
import yokai.domain.extension.repo.model.ExtensionRepo
import yokai.i18n.MR

/**
 * State holder for the extension repo screen.
 *
 * Was a Voyager `StateScreenModel` kept alive by `rememberScreenModel`; it is now a plain class
 * built over the caller's [CoroutineScope] (the screen passes `rememberCoroutineScope()`), with
 * snapshot state instead of a `StateFlow`. The lifecycle is the same as before — the old
 * `rememberScreenModel` was also scoped to the composition.
 */
class ExtensionRepoScreenModel(private val scope: CoroutineScope) {

    private val extensionManager: ExtensionManager by injectLazy()

    private val getExtensionRepo: GetExtensionRepo by injectLazy()
    private val createExtensionRepo: CreateExtensionRepo by injectLazy()
    private val deleteExtensionRepo: DeleteExtensionRepo by injectLazy()
    private val replaceExtensionRepo: ReplaceExtensionRepo by injectLazy()
    private val updateExtensionRepo: UpdateExtensionRepo by injectLazy()

    // Compose-native snapshot state, exposed as the domain value directly. `State` inside this
    // class body resolves to the nested sealed interface below, never to
    // androidx.compose.runtime.State, so the type argument on mutableStateOf is always written out.
    // Do not "simplify" it back to `mutableStateOf(State.Loading)`.
    var state: ExtensionRepoScreenModel.State by mutableStateOf(State.Loading)
        private set

    private val eventChannel = Channel<ExtensionRepoEvent>(Channel.BUFFERED)
    val event: Flow<ExtensionRepoEvent> = eventChannel.receiveAsFlow()

    init {
        scope.launchIO {
            getExtensionRepo.subscribeAll().collectLatest { repos ->
                state = State.Success(repos = repos.toImmutableList())
                extensionManager.refreshTrust()
            }
        }
    }

    fun addRepo(url: String) {
        scope.launchIO {
            when (val result = createExtensionRepo.await(url)) {
                is CreateExtensionRepo.Result.Success -> {
                    eventChannel.send(ExtensionRepoEvent.Success)
                    extensionManager.findAvailableExtensions()
                }
                is CreateExtensionRepo.Result.InvalidUrl,
                is CreateExtensionRepo.Result.Error,
                -> eventChannel.send(ExtensionRepoEvent.InvalidUrl)
                is CreateExtensionRepo.Result.RepoAlreadyExists ->
                    eventChannel.send(ExtensionRepoEvent.RepoAlreadyExists)
                is CreateExtensionRepo.Result.DuplicateFingerprint -> {
                    eventChannel.send(
                        ExtensionRepoEvent.ShowDialog(RepoDialog.Conflict(result.oldRepo, result.newRepo)),
                    )
                }
            }
        }
    }

    fun replaceRepo(newRepo: ExtensionRepo) {
        scope.launchIO {
            replaceExtensionRepo.await(newRepo)
        }
    }

    fun refreshRepos() {
        val status = state

        if (status is State.Success) {
            scope.launchIO {
                updateExtensionRepo.awaitAll()
            }
        }
    }

    fun deleteRepo(url: String) {
        scope.launchIO {
            deleteExtensionRepo.await(url)
            extensionManager.findAvailableExtensions()
        }
    }

    sealed interface State {

        @Immutable
        data object Loading : State

        @Immutable
        data class Success(
            val repos: ImmutableList<ExtensionRepo>,
        ) : State {

            val isEmpty: Boolean
                get() = repos.isEmpty()
        }
    }
}

sealed class RepoDialog {
    data class Conflict(val oldRepo: ExtensionRepo, val newRepo: ExtensionRepo) : RepoDialog()
}

sealed class ExtensionRepoEvent {
    sealed class LocalizedMessage(val stringRes: StringResource) : ExtensionRepoEvent()
    data object InvalidUrl : LocalizedMessage(MR.strings.invalid_repo_url)
    data object RepoAlreadyExists : LocalizedMessage(MR.strings.repo_already_exists)
    data class ShowDialog(val dialog: RepoDialog) : ExtensionRepoEvent()
    data object Success : ExtensionRepoEvent()
}
