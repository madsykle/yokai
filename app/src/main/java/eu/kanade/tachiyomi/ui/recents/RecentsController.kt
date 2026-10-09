package eu.kanade.tachiyomi.ui.recents

import android.app.Activity
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.RecyclerView
import android.view.VelocityTracker
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
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
import eu.kanade.tachiyomi.ui.source.browse.ProgressItem
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
import eu.kanade.tachiyomi.util.view.fullAppBarHeight
import eu.kanade.tachiyomi.util.view.hide
import eu.kanade.tachiyomi.util.view.isCollapsed
import eu.kanade.tachiyomi.util.view.isControllerVisible
import eu.kanade.tachiyomi.util.view.isExpanded
import eu.kanade.tachiyomi.util.view.isHidden
import eu.kanade.tachiyomi.util.view.moveRecyclerViewUp
import eu.kanade.tachiyomi.util.view.onAnimationsFinished
import eu.kanade.tachiyomi.util.view.scrollViewWith
import eu.kanade.tachiyomi.util.view.setAction
import eu.kanade.tachiyomi.util.view.setOnQueryTextChangeListener
import eu.kanade.tachiyomi.util.view.setPositiveButton
import eu.kanade.tachiyomi.util.view.setStyle
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.updateGradiantBGRadius
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import eu.kanade.tachiyomi.widget.LinearLayoutManagerAccurateOffset
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.launch
import dev.icerock.moko.resources.compose.stringResource
import yokai.i18n.MR
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

    private var progressItem: ProgressItem? = null
    override var presenter = RecentsPresenter()
    private var snack: Snackbar? = null
    private var lastChapterId: Long? = null
    private var showingDownloads = false
    private var headerHeight = 0
    private var ogRadius = 0f
    private var deviceRadius = 0f to 0f
    private var lastScale = 1f

    private var query = ""
        set(value) {
            field = value
            presenter.query = value
        }

    override val mainRecycler: RecyclerView
        get() = binding.recycler

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

    /**
     * The bar's state holder, published out of composition so the View scroll listener can reach
     * it. Assigned from a `SideEffect` — writing a field directly in the composable body would be
     * a side effect in a spot the compiler may skip.
     */
    private var largeTitleState: LargeTitleBarState? = null

    /**
     * Cumulative scroll offset fed to [LargeTitleBarState.onScroll].
     *
     * An accumulator, not a reading of `RecyclerView.computeVerticalScrollOffset()`: that reports
     * 0 for a list too short to scroll, which would snap the title open. Reset in
     * [setUpCupertinoChrome] alongside the fresh adapter, so a re-created view starts at 0 and any
     * restored scroll position arrives as real `dy` in [setUpCupertinoChromeScroll].
     */
    private var chromeScrollOffsetPx = 0f

    private val chromeVelocityTracker = VelocityTracker()

    /** Push the presenter's view type into Compose state, if it has moved. */
    private fun syncChromeFromPresenter() {
        if (presenter.viewType != chromeViewType) chromeViewType = presenter.viewType
    }

    /**
     * Mount the Cupertino chrome into [RecentsControllerBinding.cupertinoChrome].
     *
     * A `ComposeView` declared in `recents_controller.xml` rather than a base-class swap: this
     * controller holds a ViewBinding that 108 call sites depend on, 75 of them the download
     * bottom sheet, which is out of scope for session A. Putting the chrome inside the existing
     * CoordinatorLayout keeps `binding.root` — and with it the sheet, the adapter, the endless
     * scroll listener and predictive back — exactly as they were.
     */
    private fun setUpCupertinoChrome() {
        binding.cupertinoChrome.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
        )
        chromeViewType = presenter.viewType
        chromeScrollOffsetPx = 0f
        binding.cupertinoChrome.setContent {
            RecentsChrome(
                viewType = chromeViewType,
                onViewTypeSelected = { selected ->
                    setViewType(selected)
                    chromeViewType = selected
                },
            )
        }
    }

    @Composable
    private fun RecentsChrome(
        viewType: RecentsViewType,
        onViewTypeSelected: (RecentsViewType) -> Unit,
    ) {
        val barState = rememberLargeTitleBarState()
        SideEffect { largeTitleState = barState }

        Column {
            LargeTitleBar(
                title = binding.root.context.getString(MR.strings.recents),
                state = barState,
            )
            SegmentedControl(
                segments = RecentsViewType.entries.map {
                    SegmentedControlSegment(it, stringResource(it.stringRes))
                },
                selected = viewType,
                onSelectionChange = onViewTypeSelected,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = CupertinoSegmentedInset),
            )
        }
    }

    /**
     * Feed the large title from the RecyclerView.
     *
     * [LargeTitleBarState.onScroll] wants the **cumulative** offset in px, not a per-frame delta —
     * the collapse fraction is derived from it (`LargeTitleBarMetrics.collapseFraction`).
     *
     * `LargeTitleBarState.velocityCapture()` is a `NestedScrollConnection` and so only works for
     * Compose scrollables; a `VelocityTracker` is the View-side equivalent, because RecyclerView
     * exposes no velocity of its own. Signs line up: RecyclerView reports `dy > 0` when the user
     * scrolls down, the same direction `onSettle` documents for its argument.
     */
    private fun setUpCupertinoChromeScroll() {
        binding.recycler.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    chromeVelocityTracker.addMovement(dy.toFloat())
                    chromeScrollOffsetPx += dy
                    largeTitleState?.onScroll(chromeScrollOffsetPx)
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                    chromeVelocityTracker.computeCurrentVelocity(1000)
                    largeTitleState?.onSettle(chromeVelocityTracker.yVelocity)
                    chromeVelocityTracker.recycle()
                }
            },
        )
    }

    /**
     * Height the chrome occupies, in dp, excluding the status-bar inset.
     *
     * Constant because the bar pins at [LargeTitleBarMetrics.BarHeight] expanded or collapsed —
     * only its type size animates — and the segmented control never changes height either. So
     * this is safe to read once rather than observe.
     */
    private val chromeHeightDp = LargeTitleBarMetrics.BarHeight.value +
        CupertinoSegmentedInset.value * 2 +
        SegmentedControlHeight.value

    companion object {
        /** Vertical gap above and below the segmented control. */
        private val CupertinoSegmentedInset = 8.dp

        /** Mirrors `SegmentedControlMetrics.Height`; restated here to avoid the metrics import. */
        private val SegmentedControlHeight = 32.dp
    }

    /**
     * Called when view is created
     *
     * @param view created view
     */
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        setUpCupertinoChrome()
        setUpCupertinoChromeScroll()
        // Initialize adapter
        val isReturning = this::adapter.isInitialized
        adapter = RecentMangaAdapter(this)
        adapter.setPreferenceFlows()
        binding.recycler.adapter = adapter
        binding.recycler.layoutManager = LinearLayoutManagerAccurateOffset(view.context)
        binding.recycler.setHasFixedSize(true)
        binding.recycler.recycledViewPool.setMaxRecycledViews(0, 0)
        binding.recycler.addItemDecoration(RecentMangaDivider(view.context))
        binding.recycler.onAnimationsFinished {
            (activity as? MainActivity)?.splashState?.ready = true
        }
        adapter.isSwipeEnabled = true
        adapter.itemTouchHelperCallback.setSwipeFlags(
            if (view.resources.isLTR) ItemTouchHelper.LEFT else ItemTouchHelper.RIGHT,
        )
        binding.swipeRefresh.setStyle()
        scrollViewWith(
            binding.recycler,
            swipeRefreshLayout = binding.swipeRefresh,
            ignoreInsetVisibility = true,
            afterInsets = {
                // The legacy app bar is hidden in favour of the Cupertino chrome, so the list's
                // top padding is the chrome's height rather than the toolbar's. Keeping the old
                // appBarHeight here would leave a dead toolbar-sized gap above the first row.
                val chromeHeight = (chromeHeightDp * view.resources.displayMetrics.density).toInt()
                val systemInsets = it.ignoredSystemInsets
                // Write-only before this change and still is (nothing reads `headerHeight`); kept
                // so the diff stays about the chrome rather than about deleting pre-existing state.
                headerHeight = systemInsets.top + chromeHeight
                // controller_container is full-height and the app bar only overlays it, so the
                // chrome — not the window — has to absorb the status bar.
                binding.cupertinoChrome.updatePaddingRelative(top = systemInsets.top)
                binding.recycler.updatePaddingRelative(
                    top = systemInsets.top + chromeHeight,
                    bottom = activityBinding?.bottomNav?.height ?: systemInsets.bottom,
                )
                binding.downloadBottomSheet.sheetLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    height = chromeHeight + systemInsets.top
                }
                val bigToolbarHeight = fullAppBarHeight ?: 0

                binding.recentsEmptyView.updatePadding(
                    top = bigToolbarHeight + systemInsets.top,
                    bottom = activityBinding?.bottomNav?.height ?: systemInsets.bottom,
                )
                binding.progress.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = (bigToolbarHeight + systemInsets.top) / 2
                }
                if (activityBinding?.bottomNav == null) {
                    setBottomPadding()
                }
                deviceRadius = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val wInsets = it.toWindowInsets()
                    val lCorner = wInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
                    val rCorner = wInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)
                    (lCorner?.radius?.toFloat() ?: 0f) to (rCorner?.radius?.toFloat() ?: 0f)
                } else {
                    ogRadius to ogRadius
                }
            },
            onBottomNavUpdate = {
                setBottomPadding()
            },
        )

        if (!isReturning && adapter.itemCount == 0) {
            activityBinding?.appBar?.y = 0f
            activityBinding?.appBar?.updateAppBarAfterY(binding.recycler)
            activityBinding?.appBar?.lockYPos = true
        }
        viewScope.launchUI {
            val height =
                activityBinding?.bottomNav?.height ?: view.rootWindowInsetsCompat?.getInsets(
                    systemBars(),
                )?.bottom ?: 0
            binding.recycler.updatePaddingRelative(bottom = height)
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
        } else {
            binding.recentsFrameLayout.alpha = 0f
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
            LibraryUpdateJob.isRunningFlow(view.context).collect {
                binding.swipeRefresh.isRefreshing = it
            }
        }
        binding.swipeRefresh.setOnRefreshListener {
            if (!LibraryUpdateJob.isRunning(view.context)) {
                binding.swipeRefresh.isRefreshing = true
                snack?.dismiss()
                snack = view.snack(MR.strings.updating_library) {
                    anchorView =
                        if (binding.downloadBottomSheet.root.sheetBehavior.isCollapsed()) {
                            binding.downloadBottomSheet.root
                        } else {
                            activityBinding?.bottomNav ?: binding.downloadBottomSheet.root
                        }
                    setAction(MR.strings.cancel) {
                        LibraryUpdateJob.stop(context)
                        viewScope.launchUI {
                            NotificationReceiver.dismissNotification(
                                context,
                                Notifications.ID_LIBRARY_PROGRESS,
                            )
                        }
                    }
                }
                LibraryUpdateJob.startNow(view.context)
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
            activityBinding?.appBar?.isInvisible = showingDownloads
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
        binding.swipeRefresh.isRefreshing = refresh
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) { }

    override fun shouldMoveItem(fromPosition: Int, toPosition: Int) = true

    override fun onActionStateChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        binding.swipeRefresh.isEnabled = actionState != ItemTouchHelper.ACTION_STATE_SWIPE ||
            binding.swipeRefresh.isRefreshing
    }

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
        binding.recycler.updatePaddingRelative(
            bottom = if (sheetIsHidden) {
                activityBinding?.bottomNav?.height ?: cInsets.getInsets(systemBars()).bottom
            } else {
                peekHeight
            },
        )
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
        if (!binding.progress.isVisible && recents.isNotEmpty()) {
            (activity as? MainActivity)?.showNotificationPermissionPrompt()
        }
        binding.progress.isVisible = false
        binding.recentsFrameLayout.alpha = 1f
        adapter.removeAllScrollableHeaders()
        adapter.updateDataSet(recents)
        adapter.onLoadMoreComplete(null)
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
        if (recents.isEmpty()) {
            binding.recentsEmptyView.show(
                if (!isSearching()) {
                    Icons.Filled.HistoryToggleOff
                } else {
                    Icons.Filled.SearchOff
                },
                if (isSearching()) {
                    MR.strings.no_results_found
                } else {
                    when (presenter.viewType) {
                        RecentsViewType.Updates -> MR.strings.no_recent_chapters
                        RecentsViewType.History -> MR.strings.no_recently_read_manga
                        else -> MR.strings.no_recent_read_updated_manga
                    }
                },
            )
        } else {
            binding.recentsEmptyView.hide()
        }
        val isSearchExpanded = activityBinding?.searchToolbar?.isSearchExpanded == true
        if (shouldMoveToTop) {
            if (isSearchExpanded) {
                moveRecyclerViewUp(scrollUpAnyway = true)
            } else {
                (binding.recycler.layoutManager as? LinearLayoutManager)
                    ?.scrollToPositionWithOffset(0, 0)
            }
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
        val holder = binding.recycler.findViewHolderForItemId(item.id!!) as? RecentMangaHolder ?: return
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

    private fun refreshItem(chapterId: Long) {
        val recentItemPos = adapter.currentItems.indexOfFirst {
            it is RecentMangaItem &&
                it.mch.chapter.id == chapterId
        }
        if (recentItemPos > -1) adapter.notifyItemChanged(recentItemPos)
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

    override fun onCoverClick(position: Int) {
        val manga = (adapter.getItem(position) as? RecentMangaItem)?.mch?.manga ?: return
        router.pushController(MangaDetailsController(manga).withFadeTransaction())
    }

    override fun onRemoveHistoryClicked(position: Int) {
        onItemLongClick(position)
    }

    override fun onSubChapterClicked(position: Int, chapter: Chapter, view: View) {
        val manga = (adapter.getItem(position) as? RecentMangaItem)?.mch?.manga ?: return
        openChapter(view, manga, chapter)
    }

    override fun areExtraChaptersExpanded(position: Int): Boolean {
        if (alwaysExpanded()) return true
        val item = (adapter.getItem(position) as? RecentMangaItem) ?: return false
        val date = presenter.dateFormat.format(item.mch.history.last_read)
        val invertDefault = !adapter.collapseGrouped
        return presenter.expandedSectionsMap["${item.mch.manga} - $date"]?.xor(invertDefault)
            ?: invertDefault
    }

    override fun updateExpandedExtraChapters(position: Int, expanded: Boolean) {
        if (alwaysExpanded()) return
        val item = (adapter.getItem(position) as? RecentMangaItem) ?: return
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

    override fun onItemLongClick(position: Int) {
        val item = adapter.getItem(position) as? RecentMangaItem ?: return
        showRemoveHistoryDialog(item.mch.manga, item.mch.history, item.mch.chapter)
    }

    override fun onItemLongClick(position: Int, chapter: ChapterHistory): Boolean {
        val history = chapter.history ?: return false
        val item = adapter.getItem(position) as? RecentMangaItem ?: return false
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

    override fun markAsRead(position: Int) {
        val preferences = presenter.preferences
        val item = adapter.getItem(position) as? RecentMangaItem ?: return
        val holder = binding.recycler.findViewHolderForAdapterPosition(position)
        val holderId = (holder as? RecentMangaHolder)?.chapterId
        adapter.notifyItemChanged(position)
        val transition = TransitionSet().addTransition(androidx.transition.Fade())
        transition.duration = view!!.resources.getInteger(AR.integer.config_shortAnimTime)
            .toLong()
        androidx.transition.TransitionManager.beginDelayedTransition(binding.recycler, transition)
        if (holderId == -1L) return
        val chapter = holderId?.let { item.mch.extraChapters.find { holderId == it.id } }
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
                query = it ?: return@setOnQueryTextChangeListener false
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
        val view = view ?: return
        if (presenter.finished ||
            BackupRestoreJob.isRunning(view.context.applicationContext) ||
            (presenter.viewType == RecentsViewType.GroupedAll && !isSearching())
        ) {
            loadNoMore()
            return
        }
        presenter.requestNext()
    }

    private fun loadNoMore() {
        adapter.onLoadMoreComplete(null)
    }

    /**
     * Sets a new progress item and reenables the scroll listener.
     */
    private fun resetProgressItem() {
        adapter.onLoadMoreComplete(null)
        progressItem = ProgressItem()
        adapter.setEndlessScrollListener(this, progressItem!!)
    }
}
