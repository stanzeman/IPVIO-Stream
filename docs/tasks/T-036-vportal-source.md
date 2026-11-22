# T-036 – vportal source (movies and series from a key-based portal)

## Status
**Implemented on this branch, awaiting maintainer approval and a device test.** Not built with Gradle, not device tested. The plan below is what was built, with these decisions:
- Input: the existing playlist address field (no new button, no new UI strings). The stored text is the pasted text, normalized (`PlaylistLogic.normalizeUrl`).
- Movies: HLS address plus the fragment `#/movie/` so the list parser files them under Movies. A fragment is never sent to the server. `TapLogic.tappable` ignores it, so portal movies get no live-buffer or recording button.
- Series: placeholder `portal://<sourceId>/series/<fid>.ser` plus "S01E01" in the name; `LazySeries` loads the episodes with `{"cmd":"flick","fid":..}` when the series is opened. One season, numbered in portal order.
- Films filed as series: the portal lists some films (e.g. in the movies category) as a series with one episode. For categories 1, 3 and 5 the app asks `flick` for every such entry (at most 1,500 per load) and lists it as a movie when it has exactly one file. Other categories are not checked. Unverified: whether other film categories exist.
- Loading: all pages of every category (safety limit 700 pages = 21,000 titles per category; the biggest category seen has 13,148 titles = about 440 requests), 150 ms between pages, wait on HTTP 429, cool-down of 30 s after 3 categories in a row without an answer (at most twice). A cancelled load starts again from the beginning (no resume). Portal search and loading on demand are not part of this version. Unmeasured: duration of the first full load and memory use with tens of thousands of titles; to be checked on the device.
- Files: `PortalLogic.kt`, `PortalApi.kt`, `PlaylistLogic.kt`, `PlaylistStore.kt`, `LazySeries.kt`, `TapLogic.kt`, `PortalLogicTest.kt`, `CHANGELOG.md`.
- New user-visible texts (progress and errors) are German like the existing Stalker ones and have no translation yet.

### Device test steps
1. Settings > Playlists > add a playlist, paste the portal text, confirm. Expect groups with Russian names, movies with posters.
2. Open a movie and play it: expect HLS playback, seek works, no record button.
3. Open a series: expect episodes "Серия 1…"; play two of them.
4. Reload the playlist: expect no crash; with Wi-Fi off expect an error that does not show the key.
5. Check Settings > Diagnostics: no key in any line.

## Goal
Let a user add a portal login of the form `portal::[key:<KEY>]http://<host>/api/v1/` (vportalka-style, no MAC address)
and browse/play its movies and series like an Xtream or Stalker source. Live TV stays on the user's M3U playlist.

## Observed protocol (captured from a Fire TV client, plain HTTP, **partly unverified**)
- Every call: `POST <base>/api/v1/`, JSON body, always containing `"key"`. Responses are gzip JSON.
- Entry `{"key"}` -> `{"type":"videoportal","items":[{"type":"category","title","request":{cmd:"flicks",fid,offset,limit[,vc]}}],"controls":{"search":{...},"filters":[...]}}`.
- Category `{"cmd":"flicks","fid":N,"offset":0,"limit":0}` -> `{"type":"category","items":[...],"count":N}`.
- Items: `type:"stream"` (movie, has `url` = HLS master.m3u8, `img`, `year`, `duration`, `description`, `agelimit`, `fhd`/`4k`/`hdr`) and `type:"multistream"` (series, no `url`).
- `{"cmd":"flick","fid":N}` on a movie -> same item plus `variants` {480,720,1080,auto} (HLS URLs). `getvmark` only stores the watch position.
- Playback is plain HLS; the CDN host differs from the API host; segment redirects are handled by ExoPlayer.
- `{"cmd":"flick","fid":<series fid>}` on a `multistream` -> `{"type":"multistream","title","img","imglr","fid","year","description","agelimit","items":[{"type":"stream","title":"<episode title>","request":{cmd:"flick",fid},"fid","url":<HLS master.m3u8>,"fhd","4k","hdr"}]}`. Verified with one series (22 episodes). Episodes carry their own `url`, so no extra call is needed to play. The episode list is flat: no season level was seen in this sample (**other series may differ, unverified**). Episode titles are Russian text such as "Series 1".
- **Unknown:** behaviour of `limit`/`offset` paging (the app always sends `limit` 0), `search` response, whether HLS URLs expire, whether series with seasons exist.

## Design (mirrors Stalker, smallest change)
- Stored login: `portal:` + base URL + `#key=` + KEY (encrypted like all logins). New `PortalLogic.kt` (pure, no Android): parse the pasted string, encode/decode, map JSON items to list entries.
- `PortalApi.kt` (OkHttp): POST helper, gzip, polite pause between requests, stop on HTTP 429 with cooldown, cache of category responses, resume-safe loading.
- Load path: `PlaylistStore.download` gets one more branch next to Stalker; the portal is converted to M3U text. Movies carry their HLS URL; series get a lazy placeholder resolved in `LazySeries` once the `flick` format is known.
- UI: one more source button in `AddPlaylistScreen.kt` ("Portal"), single input field for the pasted string. New UI strings need maintainer decision (UI strings are protected).
- Titles from the portal are Russian; no translation is attempted.

## Affected files
`PortalLogic.kt` (new), `PortalApi.kt` (new), `PlaylistStore.kt`, `LazySeries.kt`, `AddPlaylistScreen.kt`, `Dict*.kt` (new label), `PortalLogicTest.kt` (new), `CHANGELOG.md`, `docs/`.

## Risks
- Key is a credential: never logged, masked in errors (CLAUDE.md section 10).
- Plain HTTP portal: needs the existing http consent flow (T-026-tls-consent).
- Season structure unverified; protected IPTV core touched.

## Acceptance criteria
1. Pasted portal string is accepted, malformed strings are rejected without a crash.
2. Categories list; a movie plays via HLS.
3. A series opens with seasons and episodes and plays.
4. Existing Xtream, Stalker and M3U sources behave as before.

## Open input (not blocking)
One `search` response, and a sample of a series that has several seasons if one exists.

## Tests
Pure JVM tests for parsing, encoding and item mapping only. Gradle build and device test are not possible here; test steps will follow with the implementation.

## Categories (design)
- Source: the `items` of type `category` in the entry response; each carries its own `request` (`cmd:"flicks"`, `fid`, `offset`, `limit`, sometimes `vc`). The request object is replayed as is, plus `key`.
- Group name in the app = category title (Russian, not translated). Entries of one category share this group.
- Kind per entry comes from the item type, not from the category: `stream` -> Movies, `multistream` -> Series. So the "series" and "cartoon series" categories fill the Series tab and the others the Movies tab.
- Skipped: `fid` 10001 ("continue watching", depends on the account's history). `fid` 10 (seasonal, `vc:1`) is kept but may change or disappear.
- Items have no genre field; only year filters exist in `controls.filters`. They are not used in the first version.
- Search: `controls.search.request` (`cmd:"search"`, `query`) could feed the app search later; response format unverified.
- **Paging (verified for one category):** a large category (13148 titles, `count`) returns only the first 30 items, newest first (descending `fid`), and ends with a pseudo item `{"type":"next","title":"next","request":{"offset":30,"limit":0}}`. The next page is requested by merging that `request` into the category request (`cmd`, `fid`, `key`, new `offset`). Which fields the app sends for the next page is **unverified** (assumed: same body with `offset` replaced). The `next` item is not a title and must be dropped. The last page presumably has no `next` item (unverified).
- Consequence: loading a whole big category is ~440 requests. Do not load everything at start. Load the first page(s) per category, continue when the user scrolls (like `StalkerMore`), with a pause between pages, stop on HTTP 429 with a cooldown, cache pages, and resume after an abort.
