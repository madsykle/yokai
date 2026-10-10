package eu.kanade.tachiyomi.ui.recents

import android.app.Activity
import android.text.format.DateUtils
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import java.util.Date
import yokai.presentation.theme.CupertinoColors
import yokai.presentation.theme.CupertinoType
import yokai.presentation.theme.components.CupertinoEmptyState
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.RoundedCorner
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.transition.TransitionSet
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.ChapterHistory
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.seriesType
import eu.kanade.tachiyomi.data.download.DownloadJob
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.databinding.RecentsControllerBinding
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.ui.base.SmallToolbarInterface
import eu.kanade.tachiyomi.ui.base.controller.BaseCoroutineController
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.main.BottomSheetController
import eu.kanade.tachiyomi.ui.main.FloatingSearchInterface
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.RootSearchInterface
import eu.kanade.tachiyomi.ui.main.TabbedInterface
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.recents.options.TabbedRecentsOptionsSheet
import eu.kanade.tachiyomi.util.chapter.updateTrackChapterMarkedAsRead
import eu.kanade.tachiyomi.util.system.addCheckBoxPrompt
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getBottomGestureInsets
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.ignoredSystemInsets
import eu.kanade.tachiyomi.util.system.isLTR
import eu.kanade.tachiyomi.util.system.isPromptChecked
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.util.system.setCustomTitleAndMessage
import eu.kanade.tachiyomi.util.system.spToPx
import eu.kanade.tachiyomi.util.system.toInt
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.collapse
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.util.view.hide
import eu.kanade.tachiyomi.util.view.isCollapsed
import eu.kanade.tachiyomi.util.view.isControllerVisible
import eu.kanade.tachiyomi.util.view.isExpanded
import eu.kanade.tachiyomi.util.view.isHidden
import eu.kanade.tachiyomi.util.view.setAction
import eu.kanade.tachiyomi.util.view.setOnQueryTextChangeListener
import eu.kanade.tachiyomi.util.view.setPositiveButton
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.updateGradiantBGRadius
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.launch
import dev.icerock.moko.resources.compose.stringResource
import yokai.i18n.MR
import yokai.presentation.theme.YokaiTheme
import yokai.presentation.theme.components.CupertinoSearchBar
import yokai.presentation.theme.components.LargeTitleBar
import yokai.presentation.theme.components.LargeTitleBarMetrics
import yokai.presentation.theme.components.LargeTitleBarState
import yokai.presentation.theme.components.SegmentedControl
import yokai.presentation.theme.components.SegmentedControlSegment
import yokai.presentation.theme.components.rememberLargeTitleBarState
import yokai.util.lang.getString
import android.R as AR

/**
 * Fragment that shows recently read manga.
 * Uses R.layout.fragment_recently_read.
 * UI related actions should be called from here.
 */
class RecentsController(bundle: Bundle? = null) :
    BaseCoroutineController<RecentsControllerBinding, RecentsPresenter>(bundle),
    RecentMangaAdapter.RecentsInterface,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    FlexibleAdapter.OnItemMoveListener,
    FlexibleAdapter.EndlessScrollListener,
    TabbedInterface,
    RootSearchInterface,
    FloatingSearchInterface,
    BottomSheetController {

    init {
        setHasOptionsMenu(true)
        retainViewMode = RetainViewMode.RETAIN_DETACH
    }

    /** Adapter containing the recent manga. */
    private lateinit var adapter: RecentMangaAdapter
    var displaySheet: TabbedRecentsOptionsSheet? = null

    override var presenter = RecentsPresenter()
    private var snack: Snackbar? = null
    private var lastChapterId: Long? = null

    /** True while a library update is running. Drawn as an indicator; no gesture wired yet. */
    private var listIsRefreshing by mutableStateOf(false)
    private var showingDownloads = false
    private var ogRadius = 0f
    private var deviceRadius = 0f to 0f
    private var lastScale = 1f

    /**
     * Snapshot-backed so the chrome's `CupertinoSearchBar` recomposes as the user types.
     * [applyQuery] is the only writer, so the presenter's copy can never drift from the chrome's.
     */
    private var query by mutableStateOf("")

    /**
     * Single entry point for the query, shared by the Cupertino bar and the legacy `SearchView`
     * listener. Forwards to the presenter exactly as the original setter did, so this is the same
     * filtering pathway — not a second one.
     */
    private fun applyQuery(value: String) {
        query = value
        presenter.query = value
    }

    // `mainRecycler` is deliberately not overridden. The legacy app bar is hidden in favour of
    // the Cupertino chrome, so `BaseController.mainRecyclerView` returning null here only means
    // `MainActivity.requestApplyInsets()` / `updateAppBarAfterY()` no-op for Recents, which is
    // correct: there is no app bar for them to move. Every consumer is already null-safe
    // (`ControllerExtensions.kt:708,712`; `MainActivity.kt:691,693,707`).

    override fun getTitle(): String? {
        return view?.context?.getString(MR.strings.recents)
    }

    override fun getSearchTitle(): String? {
        return searchTitle(
            view?.context?.getString(
                when (presenter.viewType) {
                    RecentsViewType.History -> MR.strings.history
                    RecentsViewType.Updates -> MR.strings.updates
                    else -> MR.strings.updates_and_history
                },
            )?.lowercase(Locale.ROOT),
        )
    }

    override fun createBinding(inflater: LayoutInflater) =
        RecentsControllerBinding.inflate(inflater)

    /**
     * The Cupertino chrome replaces the legacy app bar, toolbar and view-type tab strip.
     *
     * `BaseController.hideLegacyAppBar()` (`BaseController.kt:147`) sets `isVisible = false` —
     * INVISIBLE, not GONE. That is enough here because `controller_container` is `match_parent`
     * constrained to the parent's top (`main_activity.xml:11-19`) and the app bar merely overlays
     * it, so no dead layout band is left behind. The chrome therefore owns the top inset itself.
     *
     * `main_tabs` is nested inside `ExpandedAppBarLayout` (`main_activity.xml:217`, under the
     * `app_bar` opened at line 32), so hiding the app bar hides the tab strip with it.
     * [onChangeStarted] still clears the tabs, because `tabs_frame_layout` is `gone` by default
     * and `MainActivity.showTabBar(true)` can make it visible again.
     */
    override val shouldHideLegacyAppBar = true

    /**
     * View type as Compose state.
     *
     * [RecentsPresenter.viewType] is a plain `var`, so reading it inside composition would never
     * recompose when it changed. Holding it here means the segmented control reflects the
     * presenter. Changes made outside the control (back-handler jumps, item taps) go through
     * [syncChromeFromPresenter].
     */
    private var chromeViewType by mutableStateOf(RecentsViewType.GroupedAll)

    /** Push the presenter's view type into Compose state, if it has moved. */
    private fun syncChromeFromPresenter() {
        if (presenter.viewType != chromeViewType) chromeViewType = presenter.viewType
    }

    /**
     * Mount [RecentsScreen] into [RecentsControllerBinding.cupertinoChrome].
     *
     * A `ComposeView` declared in `recents_controller.xml` rather than a base-class swap: this
     * controller holds a ViewBinding that the download bottom sheet alone accounts for, and
     * keeping `binding.root` keeps the sheet, its BottomSheetBehavior child ordering and the
     * controller lifecycle exactly as they were.
     */
    private fun setUpCupertinoScreen() {
        binding.cupertinoChrome.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
        )
        chromeViewType = presenter.viewType
        recentsRows = buildRecentsRows(presenter.recentItems)
        binding.cupertinoChrome.setContent {
            // Same wrapper every Compose screen enters (BaseComposeController.kt:30). Also where
            // LocalCupertinoColors comes from, i.e. why this screen follows dark mode at all —
            // without it the components resolve to LightCupertinoColors unconditionally.
            YokaiTheme {
                RecentsScreen(
                    rows = recentsRows,
                    viewType = chromeViewType,
                    adapter = adapter,
                    emptyStateIcon = if (isSearching()) Icons.Filled.SearchOff else Icons.Filled.HistoryToggleOff,
                    emptyStateTitle = when {
                        isSearching() -> stringResource(MR.strings.no_results_found)
                        else -> when (presenter.viewType) {
                            RecentsViewType.Updates -> stringResource(MR.strings.no_recent_chapters)
                            RecentsViewType.History -> stringResource(MR.strings.no_recently_read_manga)
                            else -> stringResource(MR.strings.no_recent_read_updated_manga)
                        }
                    },
                    isLoading = recentsLoading,
                    topInsetPx = listTopInsetPx,
                    bottomInsetPx = listBottomInsetPx,
                    // Read so a display-preference change recomposes the list; the rows re-bind
                    // through AndroidView's `update` block.
                    preferencesVersion = preferencesVersion,
                    onViewTypeSelected = { selected ->
                        setViewType(selected)
                        chromeViewType = selected
                    },
                    onQueryChange = { new ->
                        if (query != new) {
                            applyQuery(new)
                            resetProgressItem()
                            refresh()
                        }
                    },
                    onQueryCancel = {
                        if (query.isNotEmpty()) {
                            applyQuery("")
                            resetProgressItem()
                            refresh()
                        }
                    },
                    onLoadMore = ::requestNextPage,
                )
            }
        }
    }

    /**
     * Feed the large title from the LazyColumn.
     *
     * Two separate concerns, deliberately not one:
     *
     * - **Where it is.** [LargeTitleBarState.onScroll] wants the cumulative offset in px, not a
     *   per-frame delta, because the collapse fraction is derived from it. A LazyColumn has no
     *   `computeVerticalScrollOffset`, and `firstVisibleItemIndex * itemHeight` is a lie the
     *   moment rows differ in height — so the offset is accumulated from the nested-scroll
     *   deltas the list itself consumed.
     * - **When it settles.** `velocityCapture()` is a `NestedScrollConnection`, so the fling
     *   velocity finally has somewhere to come from now that both live in one composition.
     */
    @Composable
    private fun rememberTitleCollapse(state: LargeTitleBarState): Modifier {
        val offset = remember { mutableStateOf(0f) }
        val connection = remember(state) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    offset.value += consumed.y
                    state.onScroll(offset.value)
                    return Offset.Zero
                }
            }
        }
        // `nestedScroll` has no vararg overload; each connection is its own modifier, and both end up
        // in the same dispatcher's chain.
        return Modifier.nestedScroll(state.velocityCapture()).nestedScroll(connection)
    }

    /**
     * Height the chrome occupies, in dp, excluding the status-bar inset.
     *
     * Constant because the bar pins at [LargeTitleBarMetrics.BarHeight] expanded or collapsed —
     * only its type size animates — and the segmented control never changes height either. So
     * this is safe to read once rather than observe.
     */
    private val chromeHeightDp = LargeTitleBarMetrics.BarHeight.value +
        SearchBarInset.value +
        CupertinoSearchBarHeight.value +
        CupertinoSegmentedInset.value * 2 +
        SegmentedControlHeight.value

    companion object {
        /** Vertical gap above and below the segmented control. */
        private val CupertinoSegmentedInset = 8.dp

        /** Mirrors `SegmentedControlMetrics.Height`; restated here to avoid the metrics import. */
        private val SegmentedControlHeight = 32.dp

        /** Mirrors `SearchBarMetrics.Height`; restated here to avoid the metrics import. */
        private val CupertinoSearchBarHeight = 36.dp

        /** Gap between the title bar and the search field. */
        private val SearchBarInset = 8.dp

        /**
         * How close to the end of the list a page request may fire, in rows.
         *
         * Replaces `FlexibleAdapter.EndlessScrollListener`, whose default was a pixel distance.
         * A row count rather than a pixel one because the rows are not a fixed height — the
         * type-1 rows in particular vary with the cover.
         */
        private const val PREFETCH_DISTANCE = 3
    }

    /**
     * What the LazyColumn is showing, flattened.
     *
     * The sections live in the data rather than in the adapter: `RecentsPresenter` puts a
     * `RecentMangaHeaderItem` or a `DateItem` on every `RecentMangaItem` it emits
     * (`RecentsPresenter.kt:383-419`), so the whole hierarchy is derivable here.
     *
     * Identity is a string built from the values themselves, never `equals()`. `RecentMangaHeaderItem.equals`
     * compares `recentsType == recentsType` inside its `other is LibraryHeaderItem` branch and has no
     * `RecentMangaHeaderItem` branch at all (`RecentMangaHeaderItem.kt:47-52`), so every header compares
     * unequal to itself and every section would repeat. Filed for B1c.
     */
    private sealed interface RecentsRow {
        val key: String

        data class Section(val recentsType: Int) : RecentsRow {
            override val key: String get() = "section-$recentsType"
        }

        data class DateSection(val date: Date, val addedString: Boolean, val showLastUpdated: Boolean) : RecentsRow {
            override val key: String get() = "date-${date.time}"
        }

        data class Manga(val item: RecentMangaItem) : RecentsRow {
            override val key: String get() = "manga-${item.mch.manga.id}-${item.chapter.id}"
        }

        data class Footer(val recentsType: Int) : RecentsRow {
            override val key: String get() = "footer-$recentsType"
        }
    }

    /**
     * Live type-1 rows, keyed by [RecentsRow.Manga.key].
     *
     * Replaces `RecyclerView.findViewHolderForItemId` for the download-progress path
     * ([updateChapterDownload]) and the cover-outline path ([RecentMangaAdapter.setPreferenceFlows]).
     *
     * Owned by the controller, not by a `remember`: both callers run outside composition. Entries are
     * strong references, so [DisposableEffect]'s `onDispose` is what keeps a scrolled-away row from
     * pinning its inflated view tree for the controller's lifetime.
     */
    private val holderRegistry = mutableMapOf<String, RecentMangaHolder>()

    private fun buildRecentsRows(recents: List<RecentMangaItem>): List<RecentsRow> = buildList {
        var lastHeaderKey: String? = null
        var headerCount = 0
        recents.forEach { item ->
            when (val header = item.header) {
                is RecentMangaHeaderItem -> {
                    val key = "section-${header.recentsType}"
                    if (key != lastHeaderKey) {
                        add(RecentsRow.Section(header.recentsType))
                        lastHeaderKey = key
                        headerCount++
                    }
                }
                is DateItem -> {
                    val key = "date-${header.date.time}"
                    if (key != lastHeaderKey) {
                        // Only the very first header carries the library-update timestamp, which is
                        // what `DateItem.Holder` did with `bindingAdapterPosition == 0`.
                        add(RecentsRow.DateSection(header.date, header.addedString, headerCount == 0))
                        lastHeaderKey = key
                        headerCount++
                    }
                }
                else -> Unit
            }
            if (item.mch.manga.id == null) {
                add(RecentsRow.Footer((item.header as? RecentMangaHeaderItem)?.recentsType ?: 0))
            } else {
                add(RecentsRow.Manga(item))
            }
        }
    }

    /**
     * Rows the LazyColumn is showing, rebuilt in [setUpCupertinoScreen] and [showLists].
     *
     * Held as one immutable list rather than a live adapter, so Compose sees a single atomic
     * swap — the presenter only ever publishes a complete list (`RecentsPresenter.kt:437-446`),
     * and there is nothing incremental for the list to animate.
     */
    private var recentsRows by mutableStateOf(emptyList<RecentsRow>())

    /** True while the first page is in flight and there is nothing to show yet. */
    private var recentsLoading by mutableStateOf(false)

    /** Bumped by a display-preference change so every row re-binds. See [setUpCupertinoScreen]. */
    private var preferencesVersion by mutableIntStateOf(0)

    /**
     * Bottom of the list, in px.
     *
     * The bottom navigation bar is a plain View in the activity, not a window inset, so
     * `LocalWindowInsets` cannot see it. This is `activityBinding.bottomNav.height`, falling back
     * to the system bar inset — the same two values `setBottomPadding` uses for the sheet.
     */
    private var listBottomInsetPx by mutableStateOf(0)

    /** Status-bar inset, so the chrome clears the status bar. See [setUpInsets]. */
    private var listTopInsetPx by mutableStateOf(0)

    /** Bumped when the screen should scroll the list back to the first row. See [showLists]. */
    private var scrollToTopSignal by mutableIntStateOf(0)

    /**
     * One page request at a time.
     *
     * The LazyColumn near-end effect fires whenever the last row is within [PREFETCH_DISTANCE] of the
     * viewport, which is not once per page — `totalItemsCount` grows as the page arrives, so the
     * condition stays true and re-arms. Cleared in [showLists], not on a timer.
     */
    private var pagingRequestInFlight = false

    /**
     * Called when view is created
     *
     * @param view created view
     */
    /**
     * Replaces `scrollViewWith`, which cannot outlive the RecyclerView: its first parameter is a
     * `ScrollingView` (`ControllerExtensions.kt:278`) and Compose has no equivalent.
     *
     * Three of its four consumers are View-side and stay here — the sheet's peek height
     * ([setBottomPadding]), `deviceRadius`, and the sheet's own top height. The fourth, the list's
     * bottom padding, becomes Compose state.
     *
     * The bottom value is the bottom navigation bar's **height**, not a window inset, so it cannot
     * go through `Modifier.windowInsetsPadding` either; it is passed in explicitly.
     */
    private fun setUpInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            listTopInsetPx = insets.ignoredSystemInsets.top
            // The chrome is match_parent now and pads itself by listTopInsetPx, so it needs no
            // margin: adding one here would inset it twice.
            binding.downloadBottomSheet.sheetLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                height = (chromeHeightDp * view.resources.displayMetrics.density).toInt() +
                    insets.ignoredSystemInsets.top
            }
            deviceRadius = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val wInsets = insets.toWindowInsets()
                val lCorner = wInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
                val rCorner = wInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)
                (lCorner?.radius?.toFloat() ?: 0f) to (rCorner?.radius?.toFloat() ?: 0f)
            } else {
                ogRadius to ogRadius
            }
            setBottomPadding()
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        // Initialize adapter
        val isReturning = this::adapter.isInitialized
        adapter = RecentMangaAdapter(this)
        // With no RecyclerView, notifyDataSetChanged() redraws nothing, so preference changes are
        // routed back here: the counter is read by RecentsScreen's call site, which recomposes
        // and re-runs every AndroidView row's `update`.
        adapter.onDataInvalidated = { preferencesVersion++ }
        adapter.onOutlineChanged = { holderRegistry.values.forEach(RecentMangaHolder::updateCards) }
        adapter.setPreferenceFlows()
        setUpInsets(view)
        setUpCupertinoScreen()
        // The splash used to wait for the RecyclerView's first item animation, which no longer
        // exists; first layout is the equivalent "the list has something on screen" signal.
        view.post { (activity as? MainActivity)?.splashState?.ready = true }
        if (!isReturning && adapter.itemCount == 0) {
            activityBinding?.appBar?.y = 0f
            activityBinding?.appBar?.lockYPos = true
        }
        viewScope.launchUI {
            val height =
                activityBinding?.bottomNav?.height ?: view.rootWindowInsetsCompat?.getInsets(
                    systemBars(),
                )?.bottom ?: 0
            setPadding(binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable == true)
            binding.downloadBottomSheet.dlRecycler.updatePaddingRelative(
                bottom = height,
            )
            val isExpanded = binding.downloadBottomSheet.root.sheetBehavior.isExpanded()
            binding.downloadBottomSheet.dlRecycler.alpha = isExpanded.toInt().toFloat()
            binding.downloadBottomSheet.titleText.alpha = (!isExpanded).toInt().toFloat()
            binding.downloadBottomSheet.sheetToolbar.alpha = isExpanded.toInt().toFloat()
            if (binding.downloadBottomSheet.root.sheetBehavior.isCollapsed()) {
                if (hasQueue()) {
                    binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable =
                        false
                } else {
                    binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable =
                        true
                    binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.state =
                        BottomSheetBehavior.STATE_HIDDEN
                }
            } else if (binding.downloadBottomSheet.root.sheetBehavior.isHidden()) {
                if (!hasQueue()) {
                    binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.skipCollapsed =
                        true
                } else {
                    binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.skipCollapsed =
                        false
                    binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.state =
                        BottomSheetBehavior.STATE_COLLAPSED
                }
            }
            updateTitleAndMenu()
        }

        if (presenter.recentItems.isNotEmpty()) {
            adapter.updateDataSet(presenter.recentItems)
            recentsRows = buildRecentsRows(presenter.recentItems)
        } else {
            recentsLoading = presenter.isLoading
        }

        binding.downloadBottomSheet.dlBottomSheet.onCreate(this)

        binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.addBottomSheetCallback(
            object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, progress: Float) {
                    val height =
                        binding.root.height - binding.downloadBottomSheet.dlRecycler.paddingTop
                    // Doing some fun math to hide the tab bar just as the title text of the
                    // dl sheet is under the toolbar
                    val cap = height * (1 / 12600f) + 479f / 700
                    binding.downloadBottomSheet.titleText.alpha = 1 - max(0f, progress / cap)
                    binding.downloadBottomSheet.sheetToolbar.alpha = max(0f, progress / cap)
                    binding.downloadBottomSheet.pill.alpha = binding.downloadBottomSheet.titleText.alpha * 0.25f
                    binding.downloadBottomSheet.dlRecycler.alpha = progress * 10
                    val oldShow = showingDownloads
                    showingDownloads = progress > 0.92f
                    if (!isControllerVisible) {
                        return
                    }
                    binding.downloadBottomSheet.root.apply {
                        if (lastScale != 1f && scaleY != 1f) {
                            val scaleProgress = ((1f - progress) * (1f - lastScale)) + lastScale
                            scaleX = scaleProgress
                            scaleY = scaleProgress
                            for (i in 0 until childCount) {
                                val childView = getChildAt(i)
                                childView.scaleY = scaleProgress
                            }
                        }
                    }
                    if (isControllerVisible) {
                        activityBinding?.appBar?.alpha = (1 - progress * 3) + 0.5f
                    }
                    binding.downloadBottomSheet.root.updateGradiantBGRadius(
                        ogRadius,
                        deviceRadius,
                        progress,
                        binding.downloadBottomSheet.sheetLayout,
                    )
                    if (oldShow != showingDownloads) {
                        updateTitleAndMenu()
                        (activity as? MainActivity)?.reEnableBackPressedCallBack()
                    }
                }

                override fun onStateChanged(p0: View, state: Int) {
                    if (this@RecentsController.view == null) return
                    if (state == BottomSheetBehavior.STATE_EXPANDED || state == BottomSheetBehavior.STATE_COLLAPSED) {
                        showingDownloads = state == BottomSheetBehavior.STATE_EXPANDED
                        updateTitleAndMenu()
                    }

                    if (isControllerVisible) {
                        activityBinding?.tabsFrameLayout?.isVisible =
                            state != BottomSheetBehavior.STATE_EXPANDED
                    }
                    binding.downloadBottomSheet.dlBottomSheet.apply {
                        if ((
                            state == BottomSheetBehavior.STATE_COLLAPSED ||
                                state == BottomSheetBehavior.STATE_EXPANDED ||
                                state == BottomSheetBehavior.STATE_HIDDEN
                            ) &&
                            scaleY != 1f
                        ) {
                            scaleX = 1f
                            scaleY = 1f
                            pivotY = 0f
                            translationX = 0f
                            for (i in 0 until childCount) {
                                val childView = getChildAt(i)
                                childView.scaleY = 1f
                            }
                        }
                    }

                    if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                        if (hasQueue()) {
                            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable =
                                false
                        } else {
                            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable =
                                true
                            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.state =
                                BottomSheetBehavior.STATE_HIDDEN
                        }
                    } else if (state == BottomSheetBehavior.STATE_HIDDEN) {
                        if (!hasQueue()) {
                            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.skipCollapsed =
                                true
                        } else {
                            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.skipCollapsed =
                                false
                            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.state =
                                BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }

                    if (presenter.downloadManager.hasQueue()) {
                        binding.downloadBottomSheet.downloadFab.alpha = 1f
                        if (state == BottomSheetBehavior.STATE_EXPANDED) {
                            binding.downloadBottomSheet.downloadFab.show()
                        } else {
                            binding.downloadBottomSheet.downloadFab.hide()
                        }
                    }

                    binding.downloadBottomSheet.sheetLayout.isClickable =
                        state == BottomSheetBehavior.STATE_COLLAPSED
                    binding.downloadBottomSheet.sheetLayout.isFocusable =
                        state == BottomSheetBehavior.STATE_COLLAPSED
                    setPadding(binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable == true)
                }
            },
        )
        viewScope.launch {
            // Refresh state, not the gesture. The SwipeRefreshLayout that used to host it is gone
            // with the rest of the RecyclerView tree, so pulling no longer starts a library
            // update; the `LibraryUpdateJob` collectors above and below still reflect one that
            // started elsewhere. B1c restores the gesture.
            LibraryUpdateJob.isRunningFlow(view.context).collect {
                listIsRefreshing = it
            }
        }

        ogRadius = view.resources.getDimension(R.dimen.rounded_radius)
        setSheetToolbar()
        if (showingDownloads) {
            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.expand()
        }
        setPadding(binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable == true)

        binding.downloadBottomSheet.root.sheetBehavior?.isGestureInsetBottomIgnored = true
    }

    private fun setSheetToolbar() {
        binding.downloadBottomSheet.sheetToolbar.title = view?.context?.getString(MR.strings.download_queue)
        binding.downloadBottomSheet.sheetToolbar.overflowIcon?.setTint(view?.context?.getResourceColor(R.attr.actionBarTintColor) ?: Color.BLACK)
        binding.downloadBottomSheet.sheetToolbar.setOnMenuItemClickListener { item ->
            return@setOnMenuItemClickListener binding.downloadBottomSheet.dlBottomSheet.onOptionsItemSelected(item)
        }
        binding.downloadBottomSheet.sheetToolbar.setNavigationOnClickListener {
            if (binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable == true) {
                binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.hide()
            } else {
                binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.collapse()
            }
        }
    }

    fun updateTitleAndMenu() {
        if (isControllerVisible) {
            val activity = (activity as? MainActivity) ?: return
            // `shouldHideLegacyAppBar` already hid the app bar in `onCreateView` (BaseLegacyController ->
            // setAppBarVisibility -> hideLegacyAppBar), but this line re-showed it on every call
            // because `showingDownloads` is false in normal use. That is how the Material toolbar
            // and its search field kept drawing over the Cupertino chrome.
            activityBinding?.appBar?.isInvisible = showingDownloads || shouldHideLegacyAppBar
            (activity as? MainActivity)?.setStatusBarColorTransparent(showingDownloads)
            setTitle()
        }
    }

    private fun setBottomPadding() {
        val bottomBar = activityBinding?.bottomNav
        val pad = bottomBar?.translationY?.minus(bottomBar.height) ?: 0f
        val padding = max(
            (-pad).toInt(),
            view?.rootWindowInsetsCompat?.getBottomGestureInsets() ?: 0,
        )
        binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.peekHeight = 48.spToPx + padding
        binding.downloadBottomSheet.fastScroller.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = -pad.toInt()
        }
        binding.downloadBottomSheet.dlRecycler.updatePaddingRelative(
            bottom = max(
                -pad.toInt(),
                view?.rootWindowInsetsCompat?.getInsets(systemBars())?.bottom ?: 0,
            ) + binding.downloadBottomSheet.downloadFab.height + 20.dpToPx,
        )
        binding.downloadBottomSheet.downloadFab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = max(
                -pad.toInt(),
                view?.rootWindowInsetsCompat?.getInsets(systemBars())?.bottom ?: 0,
            ) + 16.dpToPx
        }
        setPadding(binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable == true)
    }

    fun setRefreshing(refresh: Boolean) {
        listIsRefreshing = refresh
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) { }

    override fun shouldMoveItem(fromPosition: Int, toPosition: Int) = true

    // Swipe-to-mark-read regressed with the ItemTouchHelper FlexibleAdapter used to own; this
    // callback existed only to suspend pull-to-refresh while a swipe was in flight. B1c restores
    // the gesture as a Compose SwipeToDismiss and reinstates the pause with it.
    override fun onActionStateChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) = Unit

    override fun canStillGoBack(): Boolean {
        return showingDownloads ||
            presenter.uiPreferences.recentsViewType().get() != presenter.viewType.mainValue
    }

    override fun handleOnBackStarted(backEvent: BackEventCompat) {
        if (showingDownloads) {
            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.startBackProgress(backEvent)
        }
    }

    override fun handleOnBackProgressed(backEvent: BackEventCompat) {
        if (showingDownloads) {
            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.updateBackProgress(backEvent)
        } else {
            super.handleOnBackProgressed(backEvent)
        }
    }

    override fun handleOnBackCancelled() {
        if (showingDownloads) {
            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.cancelBackProgress()
        } else {
            super.handleOnBackCancelled()
        }
    }

    override fun handleBack(): Boolean {
        if (showingDownloads) {
            binding.downloadBottomSheet.dlBottomSheet.dismiss()
            return true
        }
        val viewType = RecentsViewType.valueOf(presenter.uiPreferences.recentsViewType().get())
        if (viewType != presenter.viewType) {
            tempJumpTo(viewType)
            return true
        }
        return false
    }

    fun setPadding(sheetIsHidden: Boolean) {
        val peekHeight = binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.peekHeight ?: 0
        val cInsets = view?.rootWindowInsetsCompat ?: return
        // The one place the list's bottom padding is written. Not `updatePaddingRelative` any more:
        // the LazyColumn takes its insets as `contentPadding`, so this is Compose state and the
        // bottom navigation bar's height goes in explicitly rather than through a window inset.
        listBottomInsetPx = if (sheetIsHidden) {
            activityBinding?.bottomNav?.height ?: cInsets.getInsets(systemBars()).bottom
        } else {
            peekHeight
        }
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        if (!isBindingInitialized) return
        if (!presenter.isLoading) {
            refresh()
        }
        setBottomPadding()
        binding.downloadBottomSheet.dlBottomSheet.update(!presenter.downloadManager.isPaused())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBindingInitialized) {
            binding.downloadBottomSheet.root.onDestroy()
        }
        snack?.dismiss()
        snack = null
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        displaySheet?.dismiss()
        displaySheet = null
    }

    fun refresh() = presenter.getRecents()

    fun showLists(
        recents: List<RecentMangaItem>,
        hasNewItems: Boolean,
        shouldMoveToTop: Boolean = false,
    ) {
        if (view == null) return
        // The page landed, so a near-end effect is allowed to ask for the next one.
        pagingRequestInFlight = false
        if (!recentsLoading && recents.isNotEmpty()) {
            (activity as? MainActivity)?.showNotificationPermissionPrompt()
        }
        recentsLoading = false
        adapter.removeAllScrollableHeaders()
        adapter.updateDataSet(recents)
        adapter.onLoadMoreComplete(null)
        recentsRows = buildRecentsRows(recents)
        if (isControllerVisible) {
            activityBinding?.appBar?.lockYPos = false
        }
        if (!hasNewItems || presenter.viewType == RecentsViewType.GroupedAll ||
            recents.isEmpty()
        ) {
            loadNoMore()
        } else if (presenter.viewType != RecentsViewType.GroupedAll) {
            resetProgressItem()
        }
        if (shouldMoveToTop) {
            scrollToTopSignal++
        }
        if (lastChapterId != null) {
            refreshItem(lastChapterId ?: 0L)
            lastChapterId = null
        }
    }

    fun updateChapterDownload(download: Download) {
        if (view == null || !this::adapter.isInitialized) return
        val id = download.chapter.id ?: return
        val item = adapter.getItemByChapterId(id) ?: return
        // Looked up in the registry the AndroidView factory fills, not with
        // findViewHolderForItemId. Keyed by the row key rather than by chapter id: `item.id` is
        // `chapter.id` via `Chapter by chapter` in BaseChapterItem, so it is null on the footer
        // row's blank chapter and would collide across a rebuild.
        val holder = holderRegistry["manga-${item.mch.manga.id}-${item.chapter.id}"] ?: return
        if (item.id == id) {
            holder.notifyStatus(download.status, download.progress, download.chapter.read, true)
        } else {
            holder.notifySubStatus(
                download.chapter,
                download.status,
                download.progress,
                download.chapter.read,
                true,
            )
        }
    }

    fun updateDownloadStatus(isRunning: Boolean) {
        binding.downloadBottomSheet.dlBottomSheet.update(isRunning)
    }

    /**
     * Re-binds the rows after a swipe changed a chapter's read state.
     *
     * Was `adapter.notifyItemChanged(position)`, which with no RecyclerView redraws nothing.
     * Bumping the preference counter recomposes the screen and re-runs every `AndroidView`
     * `update`, which is the same visible result and does not depend on a holder existing yet.
     */
private fun refreshItem(chapterId: Long) {
        if (adapter.getItemByChapterId(chapterId) != null) preferencesVersion++
    }

    override fun downloadChapter(position: Int) {
        val view = view ?: return
        val item = adapter.getItem(position) as? RecentMangaItem ?: return
        val chapter = item.chapter
        val manga = item.mch.manga
        if (item.status != Download.State.NOT_DOWNLOADED && item.status != Download.State.ERROR) {
            presenter.deleteChapter(chapter, manga)
        } else {
            if (item.status == Download.State.ERROR) {
                DownloadJob.start(view.context)
            } else {
                presenter.downloadChapter(manga, chapter)
            }
        }
    }

    override fun startDownloadNow(position: Int) {
        val chapter = (adapter.getItem(position) as? RecentMangaItem)?.chapter ?: return
        presenter.startDownloadChapterNow(chapter)
    }

    override fun downloadChapter(position: Int, chapter: Chapter) {
        val view = view ?: return
        val item = adapter.getItem(position) as? RecentMangaItem ?: return
        val manga = item.mch.manga
        val status = item.downloadInfo.find { it.chapterId == chapter.id }?.status ?: return
        if (status != Download.State.NOT_DOWNLOADED && status != Download.State.ERROR) {
            presenter.deleteChapter(chapter, manga)
        } else {
            if (status == Download.State.ERROR) {
                DownloadJob.start(view.context)
            } else {
                presenter.downloadChapter(manga, chapter)
            }
        }
    }

    override fun startDownloadNow(position: Int, chapter: Chapter) {
        presenter.startDownloadChapterNow(chapter)
    }

    override fun onCoverClick(item: RecentMangaItem) {
        router.pushController(MangaDetailsController(item.mch.manga).withFadeTransaction())
    }

    override fun onRemoveHistoryClicked(item: RecentMangaItem) {
        onItemLongClick(item)
    }

    override fun onSubChapterClicked(item: RecentMangaItem, chapter: Chapter, view: View) {
        openChapter(view, item.mch.manga, chapter)
    }

    override fun areExtraChaptersExpanded(item: RecentMangaItem): Boolean {
        if (alwaysExpanded()) return true
        val date = presenter.dateFormat.format(item.mch.history.last_read)
        val invertDefault = !adapter.collapseGrouped
        return presenter.expandedSectionsMap["${item.mch.manga} - $date"]?.xor(invertDefault)
            ?: invertDefault
    }

    override fun updateExpandedExtraChapters(item: RecentMangaItem, expanded: Boolean) {
        if (alwaysExpanded()) return
        val date = presenter.dateFormat.format(item.mch.history.last_read)
        val invertDefault = !adapter.collapseGrouped
        presenter.expandedSectionsMap["${item.mch.manga} - $date"] = expanded.xor(invertDefault)
    }

    fun tempJumpTo(viewType: RecentsViewType) {
        presenter.toggleGroupRecents(viewType, false)
        activityBinding?.mainTabs?.run { selectTab(getTabAt(viewType.mainValue)) }
        (activity as? MainActivity)?.reEnableBackPressedCallBack()
        updateTitleAndMenu()
        // Back-handler jumps and item taps move the view type without going through the
        // segmented control, so push it into Compose state here too or the control goes stale.
        syncChromeFromPresenter()
    }

    private fun setViewType(viewType: RecentsViewType) {
        if (viewType != presenter.viewType) {
            presenter.toggleGroupRecents(viewType)
            updateTitleAndMenu()
        }
    }

    override fun getViewType(): RecentsViewType = presenter.viewType

    override fun scope() = viewScope

    override fun onItemClick(view: View?, position: Int): Boolean {
        val item = adapter.getItem(position) ?: return false
        if (item is RecentMangaItem) {
            if (item.mch.manga.id == null) {
                val headerItem = adapter.getHeaderOf(item) as? RecentMangaHeaderItem
                tempJumpTo(
                    when (headerItem?.recentsType) {
                        RecentMangaHeaderItem.NEW_CHAPTERS -> RecentsViewType.Updates
                        RecentMangaHeaderItem.CONTINUE_READING -> RecentsViewType.History
                        else -> return false
                    },
                )
            } else {
                if (activity == null) return false
                openChapter(view?.findViewById(R.id.main_view), item.mch.manga, item.chapter)
            }
        } else if (item is RecentMangaHeaderItem) return false
        return true
    }

    private fun openChapter(view: View?, manga: Manga, chapter: Chapter) {
        val activity = activity ?: return
        activity.apply {
            if (view != null) {
                val (intent, bundle) = ReaderActivity
                    .newIntentWithTransitionOptions(activity, manga, chapter, view)
                startActivity(intent, bundle)
            } else {
                val intent = ReaderActivity.newIntent(activity, manga, chapter)
                startActivity(intent)
            }
        }
    }

    /**
     * [FlexibleAdapter]'s long-click listener, which is position-keyed and shared with every other
     * tab's adapter. Kept as an override so the library interface stays satisfied; the position is
     * resolved once and handed to the item-keyed overload below.
     */
    override fun onItemLongClick(position: Int) {
        val item = adapter.getItem(position) as? RecentMangaItem ?: return
        onItemLongClick(item)
    }

    private fun onItemLongClick(item: RecentMangaItem) {
        showRemoveHistoryDialog(item.mch.manga, item.mch.history, item.mch.chapter)
    }

    override fun onItemLongClick(item: RecentMangaItem, chapter: ChapterHistory): Boolean {
        val history = chapter.history ?: return false
        if (history.id != null) {
            showRemoveHistoryDialog(item.mch.manga, history, chapter)
        }
        return history.id != null
    }

    private fun showRemoveHistoryDialog(manga: Manga, history: History, chapter: Chapter) {
        val activity = activity ?: return
        if (history.id != null) {
            activity.materialAlertDialog()
                .setCustomTitleAndMessage(
                    MR.strings.reset_chapter_question,
                    activity.getString(
                        MR.strings.this_will_remove_the_read_date_for_x_question,
                        chapter.name,
                    ),
                )
                .addCheckBoxPrompt(
                    activity.getString(
                        MR.strings.reset_all_chapters_for_this_,
                        manga.seriesType(activity),
                    ),
                )
                .setNegativeButton(AR.string.cancel, null)
                .setPositiveButton(MR.strings.reset) { dialog, _ ->
                    removeHistory(manga, history, dialog.isPromptChecked)
                }
                .show()
        }
    }

    private fun removeHistory(manga: Manga, history: History, all: Boolean) {
        if (all) {
            // Reset last read of chapter to 0L
            presenter.removeAllFromHistory(manga.id!!)
        } else {
            // Remove all chapters belonging to manga from library
            presenter.removeFromHistory(history)
        }
    }

    override fun markAsRead(item: RecentMangaItem) {
        val preferences = presenter.preferences
        // Which chapter the finger was on, published by the holder at touch time. This used to be
        // `findViewHolderForAdapterPosition(position).chapterId`, which reached into the view tree
        // to ask a question about the data.
        val holderId = adapter.activeChapterIdFor(item.mch.manga.id ?: 0L)
        val position = adapter.positionOf(item)
        if (position >= 0) {
            adapter.notifyItemChanged(position)
        }
        val transition = TransitionSet().addTransition(androidx.transition.Fade())
        transition.duration = view!!.resources.getInteger(AR.integer.config_shortAnimTime)
            .toLong()
        androidx.transition.TransitionManager.beginDelayedTransition(binding.root, transition)
        if (holderId == -1L) return
        val chapter = holderId?.let { id -> item.mch.extraChapters.find { id == it.id } }
            ?: item.chapter
        val manga = item.mch.manga
        val lastRead = chapter.last_page_read
        val pagesLeft = chapter.pages_left
        lastChapterId = chapter.id
        val wasRead = chapter.read
        presenter.markChapterRead(chapter, !wasRead)
        snack = view?.snack(
            if (wasRead) {
                MR.strings.marked_as_unread
            } else {
                MR.strings.marked_as_read
            },
            Snackbar.LENGTH_INDEFINITE,
        ) {
            anchorView = activityBinding?.bottomNav
            var undoing = false
            setAction(MR.strings.undo) {
                presenter.markChapterRead(chapter, wasRead, lastRead, pagesLeft)
                undoing = true
            }
            addCallback(
                object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        if (!undoing && !wasRead) {
                            if (preferences.removeAfterMarkedAsRead().get()) {
                                lastChapterId = chapter.id
                                presenter.deleteChapter(chapter, manga)
                            }
                            updateTrackChapterMarkedAsRead(preferences, chapter, manga.id) {
                                (router.backstack.lastOrNull()?.controller as? MangaDetailsController)?.presenter?.fetchTracks()
                            }
                        }
                    }
                },
            )
        }
        (activity as? MainActivity)?.setUndoSnackBar(snack)
    }

    private fun isSearching() = query.isNotEmpty()
    override fun alwaysExpanded() =
        query.isNotEmpty() || (presenter.viewType.isHistory && !presenter.groupHistory.isByTime)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.recents, menu)

        val searchItem = activityBinding?.searchToolbar?.searchItem
        val searchView = activityBinding?.searchToolbar?.searchView
        activityBinding?.searchToolbar?.setQueryHint(view?.context?.getString(MR.strings.search_recents), !isSearching())
        if (isSearching()) {
            searchItem?.expandActionView()
            searchView?.setQuery(query, true)
            searchView?.clearFocus()
        }
        setOnQueryTextChangeListener(activityBinding?.searchToolbar?.searchView) {
            if (query != it) {
                applyQuery(it ?: return@setOnQueryTextChangeListener false)
                resetProgressItem()
                refresh()
            }
            true
        }
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            if (type == ControllerChangeType.POP_ENTER) presenter.onCreate()
            binding.downloadBottomSheet.dlBottomSheet.dismiss()
            // The view-type TabLayout strip is superseded by the Cupertino segmented control.
            // Cleared explicitly rather than left to hideLegacyAppBar(): tabs_frame_layout is
            // gone by default, but MainActivity.showTabBar(true) can reveal it again.
            activityBinding?.mainTabs?.let { tabs ->
                tabs.removeAllTabs()
                tabs.clearOnTabSelectedListeners()
            }
            (activity as? MainActivity)?.showTabBar(false)
            syncChromeFromPresenter()
        } else {
            val lastController = router.backstack.lastOrNull()?.controller
            if (lastController !is DialogController) {
                (activity as? MainActivity)?.showTabBar(show = false, animate = lastController !is SmallToolbarInterface)
            }
            snack?.dismiss()
        }
        setBottomPadding()
    }

    override fun onChangeEnded(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeEnded(handler, type)
        if (type == ControllerChangeType.POP_ENTER) {
            setBottomPadding()
        }
        if (type.isEnter && isControllerVisible) {
            updateTitleAndMenu()
        }
    }

    fun hasQueue() = presenter.downloadManager.hasQueue()

    override fun showSheet() {
        if (!isBindingInitialized) return
        if (binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable == false || hasQueue()) {
            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.expand()
        }
    }

    override fun hideSheet() {
        if (!isBindingInitialized) return
        if (binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable == true) {
            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.hide()
        } else {
            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.collapse()
        }
    }

    override fun toggleSheet() {
        if (showingDownloads) {
            binding.downloadBottomSheet.dlBottomSheet.dismiss()
        } else {
            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.expand()
        }
    }

    override fun expandSearch() {
        if (showingDownloads) {
            binding.downloadBottomSheet.dlBottomSheet.dismiss()
        } else {
            activityBinding?.searchToolbar?.menu?.findItem(R.id.action_search)?.expandActionView()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.display_options -> {
                displaySheet = TabbedRecentsOptionsSheet(
                    this,
                    (presenter.viewType.mainValue - 1).coerceIn(0, 2),
                )
                displaySheet?.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun noMoreLoad(newItemsSize: Int) {}

    override fun onLoadMore(lastPosition: Int, currentPage: Int) {
        requestNextPage()
    }

    /**
     * Asks the presenter for the next page, at most once per page.
     *
     * Guard carried over verbatim from the old `onLoadMore` (`RecentsController.kt:1257-1263`).
     * The `EndlessScrollListener` interface stays implemented because `FlexibleAdapter` requires
     * it, but nothing reaches it now: the LazyColumn's near-end effect calls this instead, and it
     * fires far more often than "one page at a time".
     */
    private fun requestNextPage() {
        val view = view ?: return
        if (pagingRequestInFlight) return
        if (presenter.finished ||
            BackupRestoreJob.isRunning(view.context.applicationContext) ||
            (presenter.viewType == RecentsViewType.GroupedAll && !isSearching())
        ) {
            loadNoMore()
            return
        }
        pagingRequestInFlight = true
        presenter.requestNext()
    }

    private fun loadNoMore() {
        pagingRequestInFlight = false
    }

    /**
     * Re-arms pagination after a query change or a view-type switch.
     *
     * Used to set a `ProgressItem` and hand it to the adapter's endless-scroll listener. Both
     * halves are gone: the row is now drawn by the screen when [listIsRefreshing] or
     * [presenter] is loading, and the near-end effect is the trigger.
     */
    private fun resetProgressItem() {
        pagingRequestInFlight = false
    }

    /**
     * The whole Recents body: chrome, list, and the two overlays the old XML used to host as
     * Views.
     *
     * One composition rather than four sibling Views, so the chrome sits *above* the list in the
     * same coordinate space and [CupertinoSearchBar]'s `contentScrolledUnder` finally means what
     * it says — nothing scrolls under a bar that is not drawn over the list.
     */
    @Composable
    private fun RecentsScreen(
        rows: List<RecentsRow>,
        viewType: RecentsViewType,
        adapter: RecentMangaAdapter,
        emptyStateIcon: ImageVector,
        emptyStateTitle: String,
        isLoading: Boolean,
        topInsetPx: Int,
        bottomInsetPx: Int,
        preferencesVersion: Int,
        onViewTypeSelected: (RecentsViewType) -> Unit,
        onQueryChange: (String) -> Unit,
        onQueryCancel: () -> Unit,
        onLoadMore: () -> Unit,
    ) {
        val context = LocalContext.current
        val density = LocalDensity.current
        val barState = rememberLargeTitleBarState()
        val listState = rememberLazyListState()
        val collapse = rememberTitleCollapse(barState)
        // Captured once per composition, matching `DateItem.Holder`, so every header in the
        // composition agrees on what "yesterday" means.
        val nowMillis = remember { System.currentTimeMillis() }
        // `Int.toDp()` is a `Density` member, and neither `Modifier.padding` nor `PaddingValues` is
        // a density scope.
        val topPadding = with(density) { topInsetPx.toDp() }
        val bottomPadding = with(density) { bottomInsetPx.toDp() }

        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .collect { if (!it) barState.onSettle() }
        }
        LaunchedEffect(listState) {
            snapshotFlow {
                val info = listState.layoutInfo
                val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= info.totalItemsCount - PREFETCH_DISTANCE
            }.collect { nearEnd -> if (nearEnd) onLoadMore() }
        }
        LaunchedEffect(scrollToTopSignal) {
            if (scrollToTopSignal > 0) listState.scrollToItem(0)
        }

        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(top = topPadding)) {
                LargeTitleBar(
                    title = context.getString(MR.strings.recents),
                    state = barState,
                )
                CupertinoSearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    placeholder = getSearchTitle().orEmpty(),
                    cancelLabel = stringResource(MR.strings.cancel),
                    onCancel = onQueryCancel,
                    // The chrome is a sibling Column above the list, not a bar drawn over it, so
                    // content genuinely never scrolls underneath. This is the `true` the session-A
                    // commit had to hard-code until the LazyColumn landed.
                    contentScrolledUnder = false,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = SearchBarInset),
                )
                SegmentedControl(
                    segments = RecentsViewType.entries.map {
                        SegmentedControlSegment(it, stringResource(it.stringRes))
                    },
                    selected = viewType,
                    onSelectionChange = onViewTypeSelected,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = CupertinoSegmentedInset,
                    ),
                )
            }

            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = collapse.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomPadding),
                ) {
                    rows.forEach { row ->
                        when (row) {
                            is RecentsRow.Section -> stickyHeader(key = row.key) {
                                BasicText(
                                    text = stringResource(
                                        when (row.recentsType) {
                                            RecentMangaHeaderItem.NEW_CHAPTERS -> MR.strings.new_chapters
                                            RecentMangaHeaderItem.NEWLY_ADDED -> MR.strings.newly_added
                                            else -> MR.strings.continue_reading
                                        },
                                    ),
                                    style = CupertinoType.title3.copy(
                                        color = CupertinoColors.current.labelPrimary,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CupertinoColors.current.surface)
                                        .padding(start = 12.dp, top = 5.dp, bottom = 8.dp),
                                )
                            }

                            is RecentsRow.DateSection -> stickyHeader(key = row.key) {
                                val relative = DateUtils.getRelativeTimeSpanString(
                                    row.date.time,
                                    nowMillis,
                                    DateUtils.DAY_IN_MILLIS,
                                )
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(CupertinoColors.current.surface)
                                        .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 4.dp),
                                ) {
                                    BasicText(
                                        text = if (row.addedString) {
                                            context.getString(MR.strings.fetched_, relative)
                                        } else {
                                            relative.toString()
                                        },
                                        style = CupertinoType.title3.copy(
                                            color = CupertinoColors.current.labelSecondary,
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    if (row.showLastUpdated && adapter.lastUpdatedTime > 0L) {
                                        BasicText(
                                            text = context.timeSpanFromNow(
                                                MR.strings.updates_last_update_info,
                                                adapter.lastUpdatedTime,
                                            ),
                                            style = CupertinoType.footnote.copy(
                                                color = CupertinoColors.current.labelTertiary,
                                            ),
                                            modifier = Modifier.padding(top = 4.dp),
                                        )
                                    }
                                }
                            }

                            is RecentsRow.Footer -> item(key = row.key) {
                                RecentsFooterRow(
                                    recentsType = row.recentsType,
                                    onClick = {
                                        tempJumpTo(
                                            when (row.recentsType) {
                                                RecentMangaHeaderItem.NEW_CHAPTERS -> RecentsViewType.Updates
                                                else -> RecentsViewType.History
                                            },
                                        )
                                    },
                                )
                            }

                            is RecentsRow.Manga -> item(key = row.key) {
                                RecentsMangaRow(
                                    item = row.item,
                                    adapter = adapter,
                                    rebindToken = preferencesVersion,
                                )
                            }
                        }
                    }
                }

                if (rows.isEmpty() && !isLoading) {
                    CupertinoEmptyState(
                        icon = emptyStateIcon,
                        title = emptyStateTitle,
                    )
                }
                if (isLoading || listIsRefreshing) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        CupertinoLoadingIndicator(Modifier.padding(top = 24.dp))
                    }
                }
            }
        }
    }

    /**
     * A type-1 row, still the inflated `RecentMangaHolder`.
     *
     * `AndroidView` rather than a Compose row because B1b does not restyle rows. B1c replaces this
     * with a real composable, at which point the registry and the `RecentMangaHolder` plumbing go
     * with it.
     *
     * The shared-element transition still works because the click hands [openChapter] the row's own
     * `main_view`, which is attached to this hierarchy — the reader takes the `View` by identity
     * and the `transitionName` rides along in the bundle.
     */
    @Composable
    private fun RecentsMangaRow(item: RecentMangaItem, adapter: RecentMangaAdapter, rebindToken: Int) {
        val key = "manga-${item.mch.manga.id}-${item.chapter.id}"
        AndroidView(
            factory = { ctx ->
                val view = LayoutInflater.from(ctx)
                    .inflate(item.getLayoutRes(), null, false)
                holderRegistry[key] = RecentMangaHolder(view, adapter)
                view.setOnClickListener {
                    openChapter(it.findViewById(R.id.main_view), item.mch.manga, item.chapter)
                }
                view.setOnLongClickListener {
                    onItemLongClick(item)
                    true
                }
                view
            },
            update = { view ->
                val holder = holderRegistry[key]
                // A display-preference change alters neither this row's key nor its item, so
                // nothing here would otherwise tell the holder it was bound under different
                // settings. `preferencesVersion` is the parameter that changes, and this is what
                // carries it down.
                if (holder != null && holder.boundRebindToken != rebindToken) {
                    holder.boundRebindToken = rebindToken
                }
                holder?.bind(item)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        DisposableEffect(key) {
            onDispose { holderRegistry.remove(key) }
        }
    }

    /** The "View history" / "View all updates" row at the end of a section. */
    @Composable
    private fun RecentsFooterRow(recentsType: Int, onClick: () -> Unit) {
        val accent = CupertinoColors.current.accent
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 24.dp, top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_arrow_forward_24dp),
                contentDescription = null,
                colorFilter = ColorFilter.tint(accent),
                modifier = Modifier.size(24.dp),
            )
            BasicText(
                text = stringResource(
                    if (recentsType == RecentMangaHeaderItem.NEW_CHAPTERS) {
                        MR.strings.view_all_updates
                    } else {
                        MR.strings.view_history
                    },
                ),
                style = CupertinoType.body.copy(color = accent),
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }

    /**
     * The indeterminate spinner.
     *
     * Hand-rolled rather than `androidx.compose.material3.CircularProgressIndicator`: material3
     * is off-limits on Cupertino screens, and a Material pull-to-refresh indicator would also
     * imply a gesture that B1b does not have.
     */
    @Composable
    private fun CupertinoLoadingIndicator(modifier: Modifier = Modifier) {
        val transition = rememberInfiniteTransition(label = "recents-spinner")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
            label = "recents-spinner-angle",
        )
        // Resolved outside the Canvas lambda: `CupertinoColors.current` is a @Composable getter and
        // DrawScope is not a composable scope.
        val color = CupertinoColors.current.labelTertiary
        Canvas(modifier.size(24.dp)) {
            drawArc(
                color = color,
                startAngle = angle,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}
