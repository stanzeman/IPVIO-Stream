package com.ipvio.stream

/** Pure logic of the block builder ([BlockLayout]); no Android imports, so JVM tests can cover it. */
object BlockLayoutLogic {
    /**
     * Hidden blocks when a layout is loaded (T-069). [savedHidden] is null when nothing was saved yet: then the defaults apply.
     * Otherwise the saved set wins, plus every default-hidden block that the saved order does not know yet (a block added in an
     * update). Without this a new block would appear on a home screen the user had already arranged.
     */
    fun hiddenOnLoad(savedOrder: List<String>, savedHidden: Set<String>?, defaultHidden: Set<String>): Set<String> {
        if (savedHidden == null) return defaultHidden
        return savedHidden + defaultHidden.filter { it !in savedOrder }
    }
}
