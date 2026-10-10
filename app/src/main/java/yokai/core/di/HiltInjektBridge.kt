package yokai.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eu.kanade.tachiyomi.core.preference.PreferenceStore
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.library.CustomMangaManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.source.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.data.Database
import yokai.domain.base.BasePreferences
import yokai.domain.storage.StorageManager

/**
 * Phase 2 bridge, ruling (c) SPLIT.
 *
 * These `@Provides` methods deliberately delegate to [Injekt], which is still backed by the Koin
 * modules registered in `App.onCreate()`. That means Hilt-injected code and the ~740 legacy
 * `Injekt.get()` call sites receive the **same** singleton instances — one object graph, not two.
 * (A hand-written Hilt graph mirroring the Koin one would double every singleton, so the Koin
 * modules are NOT converted here.)
 *
 * The intended migration direction is therefore: each `@Provides` below is flipped from
 * `Injekt.get()` to a real Hilt binding one at a time, and the call site count drops to zero.
 */
@Module
@InstallIn(SingletonComponent::class)
object HiltInjektBridgeModule {

    @Provides
    fun providePreferenceStore(): PreferenceStore = Injekt.get()

    @Provides
    fun providePreferencesHelper(): PreferencesHelper = Injekt.get()

    @Provides
    fun provideBasePreferences(): BasePreferences = Injekt.get()

    @Provides
    fun provideNetworkPreferences(): NetworkPreferences = Injekt.get()

    @Provides
    fun provideStorageManager(): StorageManager = Injekt.get()

    @Provides
    fun provideNetworkHelper(): NetworkHelper = Injekt.get()

    @Provides
    fun provideSourceManager(): SourceManager = Injekt.get()

    @Provides
    fun provideDatabase(): Database = Injekt.get()

    @Provides
    fun provideDownloadManager(): DownloadManager = Injekt.get()

    @Provides
    fun provideCustomMangaManager(): CustomMangaManager = Injekt.get()
}

/**
 * Typed access into the Hilt component for legacy code.
 *
 * Hilt `@EntryPoint` methods must declare a concrete return type and that type must exist in the
 * Hilt graph — there is no reified `get<T>()`. This file therefore cannot generically back
 * `Injekt.get()`; instead it exposes the specific bindings supplied by [HiltInjektBridgeModule].
 */
@Deprecated("Migrate to @Inject. EntryPoint shim removed after Phase 4.")
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HiltInjektBridge {
    val preferenceStore: PreferenceStore
    val preferencesHelper: PreferencesHelper
    val basePreferences: BasePreferences
    val networkPreferences: NetworkPreferences
    val storageManager: StorageManager
    val networkHelper: NetworkHelper
    val sourceManager: SourceManager
    val database: Database
    val downloadManager: DownloadManager
    val customMangaManager: CustomMangaManager
}
