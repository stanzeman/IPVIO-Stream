# T-036 – vportal source (movies and series from a key-based portal)

## Status
**PLAN ONLY. Not approved. No code written.** Needs maintainer approval (CLAUDE.md sections 5 and 6: new playlist source in the IPTV core).

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
- **Unverified:** how many titles a large category returns with `limit` 0 (only a category with 8 entries was seen). If it is thousands, paging by `offset`/`limit` is needed (the Stalker code already loads in chunks with pauses).
