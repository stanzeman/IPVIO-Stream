package com.ipvio.stream

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed as rowItemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Live TV as tiles (T-060, switch "Live TV als Kacheln" in Settings > Live TV). The list view stays unchanged.
 *
 * Top: a bar with TV guide, search, favorites, multi view, source and back to the list view (like the movie tab).
 * Below: the categories as tiles (name, number of channels, a few logos). OK on a tile shows its channels as cards
 * (logo, name, quality, current show with progress, next show). OK on a card plays the channel, the menu key opens
 * the channel menu of the list view, Back goes back to the categories.
 */
@Composable
internal fun LiveGrid(liveAll: List<Item>, groupsAll: Map<String, List<Item>>, favsAll: List<Item>, onOpen: (Item) -> Unit, onPlayNow: (Item) -> Unit) {
    val ctx = LocalContext.current
    val srcNames = PlaylistStore.sourceNames
    // Playlists used by Live TV (upstream T-051, same as the list view): source indices, empty = all shown playlists
    val srcVer = AreaSource.version
    val srcK = remember(srcVer, srcNames, PlaylistMode.version) { AreaSource.sources(ctx, SectionSourceLogic.SECTION_LIVE) }
    // Same channel sets as the list view (private categories only in their own category)
    val live = remember(liveAll, srcK, PrivateCats.version) { PrivateCats.filter(if (srcK.isEmpty()) liveAll else liveAll.filter { it.src in srcK }) }
    val groups = remember(groupsAll, srcK) {
        if (srcK.isEmpty()) groupsAll else LinkedHashMap<String, List<Item>>().also { m ->
            for ((g, l) in groupsAll) { val f = l.filter { it.src in srcK }; if (f.isNotEmpty()) m[g] = f }
        }
    }
    val genres = remember(live) { LiveGenre.split(live) }
    val favs = remember(favsAll, srcK) { if (srcK.isEmpty()) favsAll else favsAll.filter { it.src in srcK } }
    CatFavs.init(ctx)
    ChannelLists.init(ctx)
    HiddenCats.init(ctx)
    CatOrder.init(ctx)
    SideEffect { ContentFocus.tab = 3 }
    val byId = remember(live) { lazy { live.associateBy { it.id } } }
    val myLists = ChannelLists.lists.toList()
    val favCats = CatFavs.list.filter { it in groups && it !in HiddenCats.list }

    // Back from the player: the last category opens again and the last channel gets the focus
    val restoreId = remember { LastFocus.pending }
    var cat by remember { mutableStateOf(if (restoreId != null) LiveMemory.sel(ctx)?.takeIf { it != "HOME" && it != "SEARCH" } else null) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showMulti by remember { mutableStateOf(false) }
    if (showMulti) MultiViewScreen(live) { showMulti = false }
    // Category the focus returns to after Back
    var lastCat by remember { mutableStateOf<String?>(null) }
    var backTick by remember { mutableIntStateOf(0) }

    fun labelOf(k: String): String = when {
        k == FAV_CAT -> Lang.tr("Favoriten")
        k == LiveGridLogic.ALL -> Lang.tr("Alle Sender")
        LiveGenre.isGenreKey(k) -> LiveGenre.byKey(k)?.label?.let { Lang.tr(it) } ?: k
        else -> ChannelLists.byKey(k)?.name ?: k
    }
    fun channelsOf(k: String): List<Item> {
        val ml = ChannelLists.byKey(k)
        return when {
            k == FAV_CAT -> favs
            k == LiveGridLogic.ALL -> live
            LiveGenre.isGenreKey(k) -> genres[k].orEmpty()
            ml != null -> ChannelListLogic.resolve(ml, byId.value)
            else -> groups[k].orEmpty()
        }
    }

    val tiles = remember(live, groups, genres, favs, myLists, favCats, HiddenCats.list.toList(), CatOrder.version) {
        LiveGridLogic.tiles(
            favs.size,
            myLists.map { LiveGridLogic.Tile(ChannelListLogic.key(it), it.name, ChannelListLogic.resolve(it, byId.value).size) },
            live.size,
            genres.keys.filter { it !in HiddenCats.list }.map { LiveGridLogic.Tile(it, it, genres[it]?.size ?: 0) },
            favCats.map { LiveGridLogic.Tile(it, it, groups[it]?.size ?: 0) },
            CatOrder.sorted(CatOrder.LIVE, groups.keys.toList()).filter { it !in favCats && it !in HiddenCats.list }.map { LiveGridLogic.Tile(it, it, groups[it]?.size ?: 0) }
        )
    }

    // Most watched channels (T-068): row above the category tiles, only when there is watch time
    val topCh = remember(live, LiveWatch.version) { LiveWatch.top(ctx, { byId.value[it] }) }

    // Channels of the open category (quality variants of one channel as one card, like the list view)
    val k = cat
    val raw = if (k == null) emptyList() else channelsOf(k)
    val bundleOn = General.bundleLive && k != FAV_CAT && (k == null || ChannelLists.byKey(k) == null)
    val bundled = remember(raw, bundleOn) { if (bundleOn) LiveBundleLogic.bundle(raw) else null }
    val channels = bundled?.list ?: raw
    val variantsOf = bundled?.variants.orEmpty()

    // Search: channel names, in the background
    val hits by produceState(emptyList<Item>(), live, query, searching) {
        if (!searching || query.isBlank()) { value = emptyList(); return@produceState }
        kotlinx.coroutines.delay(150)
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { ChannelSearch.filter(live, query) }
    }

    fun play(ch: Item, list: List<Item>, key: String) {
        ZapContext.list = list; ZapContext.key = key
        LiveMemory.setSel(ctx, key)
        onOpen(ch)
    }

    BackHandler(enabled = searching || cat != null) {
        if (searching) searching = false else { lastCat = cat; cat = null; backTick++ }
    }

    val catGrid = remember { LazyGridState() }
    val chGrid = remember { LazyGridState() }
    val firstTile = remember { FocusRequester() }
    val restoreReq = remember { FocusRequester() }

    Column(Modifier.fillMaxSize().padding(start = dpx(40), end = dpx(40), top = dpx(24))) {
        // Top bar: where you are, then the actions (like the movie tab)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dpx(12))) {
            Text(
                raw = true, text = when { searching -> Lang.tr("Sender suchen"); k != null -> labelOf(k); else -> Lang.tr("Live TV") },
                color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(dpx(16)))
            PillButton("TV-Guide", icon = AIcon.Grid) {
                val src = channels.ifEmpty { live }
                val ch = History.ids.firstNotNullOfOrNull { id -> src.firstOrNull { it.id == id && !ParentalLogic.isAdult(it.group, it.title) } } ?: src.firstOrNull()
                if (ch != null) { ZapContext.list = src; ZapContext.key = k ?: LiveGridLogic.ALL; AppNav.openGuide = true; onOpen(ch) }
            }
            PillButton("Suche", icon = AIcon.Edit) { searching = true }
            if (favs.isNotEmpty()) PillButton("Favoriten", icon = AIcon.Star) { searching = false; cat = FAV_CAT }
            PillButton("Mehrfachansicht", icon = AIcon.Layers) { showMulti = true }
            if (srcNames.size > 1) PillButton(
                Lang.tr("Quelle") + ": " + (if (srcK.isEmpty()) Lang.tr("Alle") else srcK.sorted().mapNotNull { srcNames.getOrNull(it) }.joinToString(", ").ifEmpty { Lang.tr("Keine Playlist") }),
                icon = AIcon.Sliders, raw = true
            ) {
                // One press: All -> first playlist -> next -> ... -> All (same as the list view)
                val cur = if (srcK.size == 1) srcK.first() else -1
                val next = cur + 1
                AreaSource.selectSource(ctx, SectionSourceLogic.SECTION_LIVE, if (next >= srcNames.size) -1 else next)
                // The open category may not exist in the other source: back to the tiles
                if (cat != FAV_CAT && cat != LiveGridLogic.ALL && cat?.let { ChannelLists.byKey(it) } == null) cat = null
            }
            PillButton("Listenansicht", icon = AIcon.Lists) { General.setLiveGrid(ctx, false) }
        }
        Spacer(Modifier.height(dpx(18)))
        when {
            searching -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(dpx(24))) {
                Column(Modifier.width(dpx(520)).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(query, { query = it.take(40) }, "Sender suchen …", Modifier.fillMaxWidth(), height = 52.dp)
                    SearchKeyboard(
                        Modifier.fillMaxWidth(),
                        onKey = { query = (query + it).take(40) },
                        onSpace = { if (query.isNotEmpty() && !query.endsWith(" ")) query += " " },
                        onDelete = { query = query.dropLast(1) },
                        onClear = { query = "" },
                        onPhone = null, onVoice = null,
                        firstKey = ContentFocus.req, onFocusChange = {}, onRight = { false }
                    )
                }
                ChannelGrid(hits, rememberLazyGrid(), null, null, variantsOf = emptyMap(), modifier = Modifier.weight(1f)) { ch -> play(ch, hits, "SEARCH") }
                LaunchedEffect(Unit) { repeat(10) { kotlinx.coroutines.delay(60); if (runCatching { ContentFocus.req.requestFocus() }.getOrDefault(false)) return@LaunchedEffect } }
            }
            k == null -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(dpx(250)), state = catGrid, modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(dpx(16)), verticalArrangement = Arrangement.spacedBy(dpx(16)),
                    contentPadding = PaddingValues(bottom = dpx(48))
                ) {
                    if (topCh.isNotEmpty()) item(key = "top", span = { GridItemSpan(maxLineSpan) }) {
                        TopChannelsRow(topCh, restoreId, restoreReq) { ch ->
                            // Back from the player: the tiles again (not a category), focus on this card
                            ZapContext.list = topCh; ZapContext.key = null
                            LiveMemory.setSel(ctx, "HOME")
                            onOpen(ch)
                        }
                    }
                    itemsIndexed(tiles, key = { _, t -> t.key }) { i, t ->
                        val back = t.key == lastCat
                        CategoryTile(
                            labelOf(t.key),
                            t.count,
                            remember(t.key, t.count, Xmltv.channelCount) { LiveGridLogic.preview(channelsOf(t.key), { Xmltv.logo(it) }) },
                            (if (i == 0 && topCh.isEmpty()) Modifier.focusRequester(ContentFocus.req) else Modifier).then(if (back) Modifier.focusRequester(firstTile) else Modifier)
                        ) { searching = false; cat = t.key }
                    }
                }
                // Back from a category: its tile gets the focus again
                LaunchedEffect(backTick) {
                    val lc = lastCat ?: return@LaunchedEffect
                    val idx = tiles.indexOfFirst { it.key == lc }
                    if (idx >= 0) runCatching { catGrid.scrollToItem(idx) }
                    repeat(10) { kotlinx.coroutines.delay(50); if (runCatching { firstTile.requestFocus() }.getOrDefault(false)) return@LaunchedEffect }
                }
            }
            else -> {
                val ck = k ?: LiveGridLogic.ALL
                if (channels.isEmpty()) Text("In dieser Kategorie gibt es keine Sender.", color = Muted, fontSize = 18.sp)
                val restoreIdx = LiveGridLogic.indexOf(channels, restoreId)
                ChannelGrid(
                    channels, chGrid, ChannelLists.byKey(ck), if (ck == FAV_CAT) channels else null, variantsOf,
                    firstMod = Modifier.focusRequester(ContentFocus.req),
                    restore = if (restoreIdx >= 0) restoreIdx to restoreReq else null,
                    onVariant = { v -> play(v, channels, ck) }
                ) { ch -> play(ch, channels, ck) }
                // Entering a category: focus on the restored channel or the first card
                LaunchedEffect(ck) {
                    if (restoreIdx >= 0) {
                        runCatching { chGrid.scrollToItem(restoreIdx) }
                        repeat(15) { kotlinx.coroutines.delay(60); if (runCatching { restoreReq.requestFocus() }.getOrDefault(false)) { LastFocus.pending = null; return@LaunchedEffect } }
                    }
                    runCatching { chGrid.scrollToItem(0) }
                    repeat(15) { kotlinx.coroutines.delay(60); if (runCatching { ContentFocus.req.requestFocus() }.getOrDefault(false)) return@LaunchedEffect }
                }
            }
        }
    }
}

/**
 * Row "Most watched channels" above the category tiles (T-068): up to 10 channel cards, most watched first.
 * The first card takes the content focus; after the player the card of the played channel gets it back.
 */
@Composable
private fun TopChannelsRow(list: List<Item>, restoreId: String?, restoreReq: FocusRequester, onClick: (Item) -> Unit) {
    val restoreIdx = LiveGridLogic.indexOf(list, restoreId)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(dpx(10))) {
        Text("Meistgesehene Sender", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(dpx(16)), contentPadding = PaddingValues(vertical = dpx(6))) {
            rowItemsIndexed(list, key = { _, ch -> ch.id }) { i, ch ->
                val m = Modifier.width(dpx(300))
                    .then(if (i == 0) Modifier.focusRequester(ContentFocus.req) else Modifier)
                    .then(if (i == restoreIdx) Modifier.focusRequester(restoreReq) else Modifier)
                ChannelCard(ch, m, null, null, emptyList(), {}) { onClick(ch) }
            }
        }
    }
    if (restoreIdx >= 0) LaunchedEffect(Unit) {
        repeat(15) { kotlinx.coroutines.delay(60); if (runCatching { restoreReq.requestFocus() }.getOrDefault(false)) { LastFocus.pending = null; return@LaunchedEffect } }
    }
}

@Composable
private fun rememberLazyGrid(): LazyGridState = remember { LazyGridState() }

/** Grid of channel cards. [restore] = index and focus requester of the channel to focus after coming back from the player. */
@Composable
private fun ChannelGrid(
    list: List<Item>, state: LazyGridState, inList: ChList?, favShown: List<Item>?, variantsOf: Map<String, List<Item>>,
    modifier: Modifier = Modifier.fillMaxSize(), firstMod: Modifier = Modifier, restore: Pair<Int, FocusRequester>? = null,
    onVariant: (Item) -> Unit = {}, onClick: (Item) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(dpx(300)), state = state, modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dpx(16)), verticalArrangement = Arrangement.spacedBy(dpx(16)),
        contentPadding = PaddingValues(bottom = dpx(48))
    ) {
        itemsIndexed(list, key = { i, ch -> ch.id + "#" + i }, contentType = { _, _ -> "ch" }) { i, ch ->
            val m = (if (i == 0) firstMod else Modifier).then(if (restore != null && restore.first == i) Modifier.focusRequester(restore.second) else Modifier)
            ChannelCard(ch, m, inList, favShown, variantsOf[ch.id].orEmpty(), onVariant) { onClick(ch) }
        }
    }
}

/** A category tile: name, number of channels and up to four channel logos. */
@Composable
private fun CategoryTile(label: String, count: Int, logos: List<String>, modifier: Modifier, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(DS.RadiusM)
    val sc by androidx.compose.animation.core.animateFloatAsState(if (f) focusScale(1.05f) else 1f, label = "lgt")
    Column(
        modifier.fillMaxWidth().height(dpx(150))
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(shape)
            .background(if (f) Color(0x33FFFFFF) else Color(0x14FFFFFF))
            .border(1.dp, if (f) Color(0x66FFFFFF) else Color.Transparent, shape)
            .onFocusChanged { f = it.isFocused }
            .clickable(onClick = onClick)
            .padding(dpx(14)),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(dpx(8))) {
            logos.forEach { lg ->
                Box(Modifier.size(dpx(44)).clip(RoundedCornerShape(DS.RadiusXs)).background(DS.Overlay2), contentAlignment = Alignment.Center) {
                    AsyncImage(lg, null, Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
                }
            }
        }
        Column {
            Text(raw = true, text = label, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(raw = true, text = "$count " + Lang.tr("Sender"), color = Muted, fontSize = 14.sp, maxLines = 1)
        }
    }
}

/** A channel card: logo, name, quality, current show with progress, next show. Menu key: the channel menu of the list view. */
@Composable
private fun ChannelCard(item: Item, modifier: Modifier, inList: ChList?, favShown: List<Item>?, variants: List<Item>, onVariant: (Item) -> Unit, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(DS.RadiusM)
    // Cached guide first (a card that comes into view is drawn once with its program), then a short wait so cards that only fly past cost nothing
    val epg by produceState(Epg.peek(item) ?: emptyList(), item.id, Xmltv.channelCount) {
        val cached = Epg.peek(item)
        value = cached ?: emptyList()
        if (cached != null) return@produceState
        kotlinx.coroutines.delay(150)
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { Epg.now(item) }
    }
    val sc by androidx.compose.animation.core.animateFloatAsState(if (f) focusScale(1.05f) else 1f, label = "lgc")
    Box {
        if (menu) ChannelMenu(item, inList, null, variants, onVariant, favShown) { menu = false }
        Column(
            modifier.fillMaxWidth().height(dpx(170))
                .graphicsLayer { scaleX = sc; scaleY = sc }
                .clip(shape)
                .background(if (f) Color(0x33FFFFFF) else Color(0x14FFFFFF))
                .border(1.dp, if (f) Color(0x66FFFFFF) else Color.Transparent, shape)
                .onFocusChanged { f = it.isFocused }
                .menuKeys(onMenu = { menu = true }, onLong = { menu = true })
                .clickable(onClick = onClick)
                .padding(dpx(14)),
            verticalArrangement = Arrangement.spacedBy(dpx(6))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dpx(12))) {
                val lg = Xmltv.logo(item)
                Box(Modifier.size(dpx(56)).clip(RoundedCornerShape(DS.RadiusS)).background(DS.Overlay2), contentAlignment = Alignment.Center) {
                    if (lg != null) AsyncImage(lg, null, Modifier.fillMaxSize().padding(5.dp), contentScale = ContentScale.Fit)
                    else Text(raw = true, text = LiveGridLogic.initials(item.title), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(raw = true, text = item.title, color = Tok.TextHi, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val q = remember(item.id) { LiveQuality.get(item.id) ?: item.quality }
                    val extra = listOfNotNull(q, if (variants.size > 1) "+" + (variants.size - 1) else null).joinToString("  ")
                    if (extra.isNotEmpty()) Text(raw = true, text = extra, color = Accent2, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                if (Library.isFav(item.id)) AppIcon(AIcon.Star, DS.Gold, size = 18.dp)
            }
            val nowE = Epg.current(epg)
            val nextE = if (nowE != null) epg.firstOrNull { it.start >= nowE.end } else null
            if (nowE != null) {
                Text(raw = true, text = "${hhmm(nowE.start)}–${hhmm(nowE.end)}  ${nowE.title}", color = Accent2, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50)).background(DS.Overlay3)) {
                    Box(Modifier.fillMaxWidth(LiveGridLogic.progress(System.currentTimeMillis() / 1000, nowE.start, nowE.end)).fillMaxHeight().background(Accent2))
                }
                if (nextE != null) Text(raw = true, text = "${hhmm(nextE.start)}  ${nextE.title}", color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else Text(raw = true, text = item.group, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
