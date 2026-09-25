package eu.kanade.tachiyomi.ui.main

/**
 * Floating glass nav geometry (DESIGN.md §5.1), kept as pure functions so the arithmetic can be
 * unit-tested: the parts that are wrong when they are wrong (a list that cannot be scrolled to
 * its last row, a pill that eats a tap) are very hard to catch by eye.
 */
internal object FloatingNavInsets {

    /**
     * Bottom inset the screens' scrollables have to reserve so their last row clears the
     * floating pill.
     *
     * Zero when there is no floating nav at all - the side-navigation layout keeps the rail off
     * the content, so nothing overlaps and nothing needs reserving.
     */
    fun insetFor(hasFloatingNav: Boolean, navTotalHeightPx: Int, systemBottomInsetPx: Int): Int {
        return if (hasFloatingNav) navTotalHeightPx + systemBottomInsetPx else 0
    }

    /**
     * Whether a scrollable's bottom padding needs touching.
     *
     * Padding is only ever *increased*: screens that already reserve their own bottom space
     * (download bar, FAB, source list) keep the larger of the two values, so the pill never
     * covers something a screen deliberately placed there.
     */
    fun needsBottomPadding(currentPaddingPx: Int, insetPx: Int): Boolean =
        insetPx > currentPaddingPx
}
