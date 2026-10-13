package eu.kanade.tachiyomi.ui.manga.chapter

import android.view.View
import androidx.appcompat.widget.PopupMenu
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import yokai.i18n.MR
import yokai.util.lang.getString

open class BaseChapterHolder(
    view: View,
    private val adapter: BaseChapterAdapter<*>,
) : BaseFlexibleViewHolder(view, adapter) {

    init {
        view.findViewById<View>(R.id.download_button)?.setOnClickListener { downloadOrRemoveMenu(it) }
    }

    /**
     * Position-keyed overload, still the entry point for every holder a RecyclerView drives.
     * Resolves the item from the adapter exactly as before, then delegates. Behaviour is
     * unchanged for `DownloadHolder`, `ChapterHolder` and `RecentMangaFooterHolder`.
     */
    internal fun downloadOrRemoveMenu(
        downloadButton: View,
        extraChapter: Chapter? = null,
        extraStatus: Download.State? = null,
    ) {
        val resolved = adapter.getItem(flexibleAdapterPosition) as? BaseChapterItem<*, *> ?: return
        downloadOrRemoveMenu(downloadButton, resolved, extraStatus, extraChapter)
    }

    /**
     * Item-keyed overload.
     *
     * `flexibleAdapterPosition` is a property of the RecyclerView rather than of the data, so a
     * holder that has left one cannot resolve its own item. Recents supplies the item directly
     * rather than letting this look it up.
     *
     * [item] is the row this menu acts on, and supplies both the status (when [extraStatus] is
     * null) and the download target. [extraChapter] is only set when a grouped holder is asking
     * for a chapter other than its own, which no Recents row does.
     */
    internal fun downloadOrRemoveMenu(
        downloadButton: View,
        item: BaseChapterItem<*, *>,
        extraStatus: Download.State? = null,
        extraChapter: Chapter? = null,
    ) {
        val position = adapter.positionOf(item)
        if (position < 0) return

        val chapterStatus = extraStatus ?: item.status
        if (chapterStatus == Download.State.NOT_DOWNLOADED || chapterStatus == Download.State.ERROR) {
            if (extraChapter != null) {
                (adapter.baseDelegate as? BaseChapterAdapter.GroupedDownloadInterface)
                    ?.downloadChapter(position, extraChapter)
            } else {
                adapter.baseDelegate.downloadChapter(position)
            }
        } else {
            downloadButton.post {
                // Create a PopupMenu, giving it the clicked view for an anchor
                val popup = PopupMenu(downloadButton.context, downloadButton)

                // Inflate our menu resource into the PopupMenu's Menu
                popup.menuInflater.inflate(R.menu.chapter_download, popup.menu)

                popup.menu.findItem(R.id.action_start).isVisible = chapterStatus == Download.State.QUEUE

                // Hide download and show delete if the chapter is downloaded
                if (chapterStatus != Download.State.DOWNLOADED) {
                    popup.menu.findItem(R.id.action_delete).title = downloadButton.context.getString(
                        MR.strings.cancel,
                    )
                }

                // Set a listener so we are notified if a menu item is clicked
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_delete -> {
                            if (extraChapter != null) {
                                (adapter.baseDelegate as? BaseChapterAdapter.GroupedDownloadInterface)
                                    ?.downloadChapter(position, extraChapter)
                            } else {
                                adapter.baseDelegate.downloadChapter(position)
                            }
                        }
                        R.id.action_start -> {
                            if (extraChapter != null) {
                                (adapter.baseDelegate as? BaseChapterAdapter.GroupedDownloadInterface)
                                    ?.startDownloadNow(position, extraChapter)
                            } else {
                                adapter.baseDelegate.startDownloadNow(position)
                            }
                        }
                    }
                    true
                }

                // Finally show the PopupMenu
                popup.show()
            }
        }
    }
}