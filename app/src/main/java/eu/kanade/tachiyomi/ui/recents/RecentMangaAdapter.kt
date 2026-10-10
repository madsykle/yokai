package eu.kanade.tachiyomi.ui.recents

import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.core.preference.Preference
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.ChapterHistory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.changesIn
import eu.kanade.tachiyomi.ui.manga.chapter.BaseChapterAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.injectLazy
import yokai.domain.recents.RecentsPreferences
import yokai.domain.ui.UiPreferences
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class RecentMangaAdapter(val delegate: RecentsInterface) :
    BaseChapterAdapter<IFlexible<*>>(delegate) {

    val preferences: PreferencesHelper by injectLazy()
    val uiPreferences: UiPreferences by injectLazy()
    val recentsPreferences: RecentsPreferences by injectLazy()

    var showDownloads = recentsPreferences.showRecentsDownloads().get()
    var showRemoveHistory = recentsPreferences.showRecentsRemHistory().get()
    var showTitleFirst = recentsPreferences.showTitleFirstInRecents().get()
    var showUpdatedTime = preferences.showUpdatedTime().get()
    var uniformCovers = uiPreferences.uniformGrid().get()
    var showOutline = uiPreferences.outlineOnCovers().get()
    var sortByFetched = preferences.sortFetchedTime().get()
    var lastUpdatedTime = preferences.libraryUpdateLastTimestamp().get()
    private var collapseGroupedUpdates = preferences.collapseGroupedUpdates().get()
    private var collapseGroupedHistory = preferences.collapseGroupedHistory().get()
    val collapseGrouped: Boolean
        get() = if (viewType.isHistory) {
            collapseGroupedHistory
        } else {
            collapseGroupedUpdates
        }

    val viewType: RecentsViewType
        get() = delegate.getViewType()

    val decimalFormat = DecimalFormat(
        "#.###",
        DecimalFormatSymbols()
            .apply { decimalSeparator = '.' },
    )
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        setDisplayHeadersAtStartUp(true)
    }

    fun setPreferenceFlows() {
        recentsPreferences.showRecentsDownloads().register { showDownloads = it }
        recentsPreferences.showRecentsRemHistory().register { showRemoveHistory = it }
        recentsPreferences.showTitleFirstInRecents().register { showTitleFirst = it }
        preferences.showUpdatedTime().register { showUpdatedTime = it }
        uiPreferences.uniformGrid().register { uniformCovers = it }
        preferences.collapseGroupedUpdates().register { collapseGroupedUpdates = it }
        preferences.collapseGroupedHistory().register { collapseGroupedHistory = it }
        preferences.sortFetchedTime().changesIn(delegate.scope()) { sortByFetched = it }
        uiPreferences.outlineOnCovers().register(false) {
            showOutline = it
            (0 until itemCount).forEach { i ->
                (recyclerView.findViewHolderForAdapterPosition(i) as? RecentMangaHolder)?.updateCards()
            }
        }
        preferences.libraryUpdateLastTimestamp().changesIn(delegate.scope()) {
            lastUpdatedTime = it
            if (viewType.isUpdates) {
                notifyItemChanged(0)
            }
        }
    }

    fun getItemByChapterId(id: Long): RecentMangaItem? {
        return currentItems.find {
            val item = (it as? RecentMangaItem) ?: return@find false
            return@find id == item.chapter.id || id in item.mch.extraChapters.map { ch -> ch.id }
        } as? RecentMangaItem
    }

    private fun <T> Preference<T>.register(notify: Boolean = true, onChanged: (T) -> Unit) {
        changes()
            .drop(1)
            .onEach {
                onChanged(it)
                if (notify) {
                    notifyDataSetChanged()
                }
            }
            .launchIn(delegate.scope())
    }

    interface RecentsInterface : GroupedDownloadInterface {
        fun onCoverClick(item: RecentMangaItem)
        fun onRemoveHistoryClicked(item: RecentMangaItem)
        fun onSubChapterClicked(item: RecentMangaItem, chapter: Chapter, view: View)
        fun updateExpandedExtraChapters(item: RecentMangaItem, expanded: Boolean)
        fun areExtraChaptersExpanded(item: RecentMangaItem): Boolean
        fun markAsRead(item: RecentMangaItem)
        fun alwaysExpanded(): Boolean
        fun scope(): CoroutineScope
        fun getViewType(): RecentsViewType
        fun onItemLongClick(item: RecentMangaItem, chapter: ChapterHistory): Boolean
    }

    /**
     * Which chapter the user's finger is currently resting on, per manga. `null` means the main
     * row rather than one of its expanded sub-chapters.
     *
     * This used to be read back off the holder with
     * `findViewHolderForAdapterPosition(position).chapterId`, which tied the swipe-to-mark-read
     * path to the RecyclerView and would not survive the row moving into a LazyColumn. The holder
     * now publishes here at touch time instead, so the controller never has to find a view.
     */
    private val activeChapterIds = mutableMapOf<Long, Long?>()

    internal fun setActiveChapterId(mangaId: Long, chapterId: Long?) {
        activeChapterIds[mangaId] = chapterId
    }

    internal fun activeChapterIdFor(mangaId: Long): Long? = activeChapterIds[mangaId]

    fun positionOf(item: RecentMangaItem): Int = currentItems.indexOfFirst { it == item }

    /**
     * Selection state, keyed by item. [FlexibleAdapter] keys it by adapter position, which is a
     * property of the RecyclerView rather than of the data.
     *
     * Kept as a real lookup rather than deleted: nothing on this screen activates selection today,
     * but that is an inference about FlexibleAdapter's own behaviour that has not been checked.
     * See the B2 pre-flight note in PROGRESS.md — if selection really is dead, the
     * `Download.State.CHECKED` branch in the holder can go with it.
     */
    fun isSelected(item: RecentMangaItem): Boolean {
        val position = positionOf(item)
        return position >= 0 && super.isSelected(position)
    }

    override fun onItemSwiped(position: Int, direction: Int) {
        super.onItemSwiped(position, direction)
        when (direction) {
            ItemTouchHelper.LEFT -> (getItem(position) as? RecentMangaItem)?.let(delegate::markAsRead)
            ItemTouchHelper.RIGHT -> (getItem(position) as? RecentMangaItem)?.let(delegate::markAsRead)
        }
    }

    enum class ShowRecentsDLs {
        None,
        OnlyUnread,
        OnlyDownloaded,
        UnreadOrDownloaded,
        All,
    }
}
