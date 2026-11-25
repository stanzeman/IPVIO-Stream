package com.ipvio.stream

/**
 * Sort of a category view (T-062). Without Android, testable.
 *
 * A category opens with "Newly added". When someone picks another sort there (for example "Popularity"), the next category
 * of the same type (movies, series, live TV) opens with that sort again, until it is changed or reset.
 */
object BrowseSortLogic {
    /** Sort a category view brings along when it has no own one ("Newly added"). */
    const val CATEGORY_DEFAULT = 0

    /**
     * Sort a view starts with. [remembered] = last sort picked in a category of this type (-1 = none), [setting] = fixed
     * default from the settings (-1 = none), [requested] = the sort the view brings along, [count] = number of sorts.
     * A remembered sort only applies to category views (those that bring the category default); lists with their own
     * order (Top lists, search, "New titles") keep it unless the fixed default says otherwise, as before.
     */
    fun initial(remembered: Int, setting: Int, requested: Int, count: Int): Int = when {
        requested == CATEGORY_DEFAULT && remembered in 0 until count -> remembered
        setting in 0 until count -> setting
        else -> requested.coerceIn(0, count - 1)
    }

    /** true = remember a sort picked in this view (only category views, see [initial]). */
    fun remembers(requested: Int): Boolean = requested == CATEGORY_DEFAULT
}
