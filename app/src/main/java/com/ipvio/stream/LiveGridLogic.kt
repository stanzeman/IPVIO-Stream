package com.ipvio.stream

/**
 * Live TV as tiles (T-060). Without Android, testable.
 *
 * The tile view shows the categories of Live TV as tiles; OK on a tile opens the channels of that category as a grid of
 * channel cards. These rules decide which category tiles appear, in which order, and what a card shows.
 */
object LiveGridLogic {
    /** A category tile: [key] as in the list view (FAV_CAT, "ALL", own list key, genre key or provider category). */
    class Tile(val key: String, val label: String, val count: Int)

    /** Key of "All channels" (same as in the list view). */
    const val ALL = "ALL"

    /**
     * Order of the category tiles, like the list view: favorites (only if there are any), own lists, all channels,
     * genres, favorite categories, the other categories. Empty genres and categories are left out; a key only once.
     */
    fun tiles(favCount: Int, lists: List<Tile>, allCount: Int, genres: List<Tile>, favCats: List<Tile>, cats: List<Tile>): List<Tile> {
        val out = ArrayList<Tile>()
        if (favCount > 0) out.add(Tile(FAV_CAT, "Favoriten", favCount))
        out.addAll(lists)
        out.add(Tile(ALL, "Alle Sender", allCount))
        out.addAll(genres.filter { it.count > 0 })
        out.addAll(favCats.filter { it.count > 0 })
        out.addAll(cats.filter { it.count > 0 })
        val seen = HashSet<String>()
        return out.filter { seen.add(it.key) }
    }

    /** Up to [max] different logos of a category for its tile ([logo] = logo of a channel or null). */
    fun preview(items: List<Item>, logo: (Item) -> String?, max: Int = 4): List<String> {
        val out = LinkedHashSet<String>()
        for (i in items) {
            if (out.size >= max) break
            logo(i)?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        }
        return out.toList()
    }

    /** Share of a show that has run at [nowSec] (0 … 1). */
    fun progress(nowSec: Long, start: Long, end: Long): Float =
        if (end <= start) 0f else ((nowSec - start).toFloat() / (end - start)).coerceIn(0f, 1f)

    /** Position of the channel with [id] in [list] (to give it the focus when coming back from the player); -1 = not there. */
    fun indexOf(list: List<Item>, id: String?): Int = if (id == null) -1 else list.indexOfFirst { it.id == id }

    /** Short initials for a channel without logo ("Kabel Eins DE" → "KA"). */
    fun initials(title: String): String = title.trim().filter { it.isLetterOrDigit() }.take(2).uppercase().ifEmpty { "TV" }
}
