package com.ipvio.stream

/**
 * Rules for the stored copy of a guide file (T-055). Without Android, testable.
 *
 * Before, the app downloaded every XMLTV file again at each start (often tens of megabytes). Now a downloaded file is
 * kept and read again for [MAX_AGE_MS]; only then, or when the user presses "Neu laden", it is downloaded again.
 * If a download fails, a copy up to [STALE_MAX_MS] old is still read (an older guide is better than none).
 */
object EpgCacheLogic {
    /** A stored guide file younger than this is read instead of downloading it again. */
    const val MAX_AGE_MS = 12 * 3_600_000L

    /** If the download fails, a stored copy up to this age is still used. */
    const val STALE_MAX_MS = 7 * 24 * 3_600_000L

    /** File name prefix of stored guide files (all in the app's cache folder). */
    const val PREFIX = "epg_"

    /** Name of the stored copy of a guide address (hash of the trimmed address, never the address itself). */
    fun fileName(url: String): String {
        val u = url.trim()
        return PREFIX + Integer.toHexString(u.hashCode()) + "_" + u.length + ".xml"
    }

    /** true = read the stored copy instead of downloading ([ageMs] = age of the copy, negative = clock went back). */
    fun reuse(ageMs: Long, force: Boolean): Boolean = !force && ageMs in 0 until MAX_AGE_MS

    /** true = a stored copy of this age may still be used when the download failed. */
    fun fallback(ageMs: Long): Boolean = ageMs in 0 until STALE_MAX_MS

    /** Stored copies to delete: guide files whose address is no longer in use ([names] = files in the folder). */
    fun unused(names: List<String>, urls: List<String>): List<String> {
        val keep = urls.map { fileName(it) }.toSet()
        return names.filter { it.startsWith(PREFIX) && it.endsWith(".xml") && it !in keep }
    }

    /** A channel icon address is used only if it is a plain web address. */
    fun iconUrl(src: String?): String? {
        val s = src?.trim() ?: return null
        return s.takeIf { (it.startsWith("http://", true) || it.startsWith("https://", true)) && it.none { c -> c.isWhitespace() } }
    }
}
