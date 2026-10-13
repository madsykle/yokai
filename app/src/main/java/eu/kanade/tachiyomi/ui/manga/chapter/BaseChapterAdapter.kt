package eu.kanade.tachiyomi.ui.manga.chapter

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.database.models.Chapter

open class BaseChapterAdapter<T : IFlexible<*>>(
    obj: DownloadInterface,
) : FlexibleAdapter<T>(null, obj, true) {

    val baseDelegate = obj

    /**
     * Resolves an item's adapter position from the data rather than from a holder.
     *
     * `FlexibleAdapter.positionOf` reads the attached RecyclerView, so it returns `NO_POSITION`
     * for any holder that has left one. Item-keyed callers go through this instead.
     */
    internal fun positionOf(item: BaseChapterItem<*, *>): Int = currentItems.indexOfFirst { it == item }

    interface DownloadInterface {
        fun downloadChapter(position: Int)
        fun startDownloadNow(position: Int)
    }

    interface GroupedDownloadInterface : DownloadInterface {
        fun downloadChapter(position: Int, chapter: Chapter)
        fun startDownloadNow(position: Int, chapter: Chapter)
    }
}
