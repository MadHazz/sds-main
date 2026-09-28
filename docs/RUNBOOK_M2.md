# M2 — Real API Integration Runbook

Procedure for validating the AdvDisplay Android client against the **real** API (the fixture suite
only proves the client works against an in-process MockWebServer). Execute phases in order and
record evidence in the [evidence log](#evidence-log).

Related: [API handoff](API_HANDOFF.md) · [Verification](VERIFICATION.md) · [Operator guide](USER_GUIDE.md)

---

## 1. Prerequisites

Inputs that must come from the API administrator / operators:

| Input | Notes |
| --- | --- |
| Production base URL | HTTPS, ends with `/`. Default is `https://sds.par-crm.com/` |
| Admin PIN | 4–12 digits, not `1234`. Needed only to exit to setup between tests |
| Screen code — **web** | `is_menu: "0"` screen |
| Screen code — **template 2** | Single-image slideshow, ≥ 2 media entries to prove ordering |
| Screen code — **template 3** | Three-panel, exactly slots 1/2/3 |
| Screen code — **template 4** | Video, ≥ 2 clips to prove playback order |
| Device under test | Physical signage unit preferred; API 35 emulator acceptable for phase 0–1 smoke |

Confirm each code is assigned and populated **before** starting. A code with no content
returns `Status: false` and cannot validate the happy path.

## 2. Environment setup

```bash
# Build against the real API with a non-default PIN. Keep the PIN in your shell env, not in git.
export ADVDISPLAY_BASE_URL="https://sds.par-crm.com/"
read -s ADVDISPLAY_ADMIN_PIN && export ADVDISPLAY_ADMIN_PIN

./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Confirm the compiled configuration (the values are baked into the APK):

```bash
aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk | grep -i "^package"
# Expect: versionCode='10000' versionName='1.0.0'

# Confirm the baked API URL and that it is NOT the fixture URL:
unzip -p app/build/outputs/apk/debug/app-debug.apk classes.dex | strings | grep -m1 'sds.par-crm.com'
```

> If you previously ran the fixture suite, rebuild **without**
> `-PADVDISPLAY_BASE_URL=http://127.0.0.1:18080/`. The fixture URL is loopback-only and release-blocking.

Capture the app's own device ID (needed to replay calls with curl):

```bash
adb shell run-as com.rihlahidali.advdisplay cat shared_prefs/advForward.xml
# Read installationDeviceId / deviceId → use as <UID> below
```

## 3. Phase 0 — Pre-flight API checks (curl, no app)

Run these first. They isolate server problems from client problems. All API calls are `GET`.

```bash
BASE="https://sds.par-crm.com/"
UID="<device-id-from-above>"
CODE="<screen-code>"
PAGE="<page-code-from-Display/Menu>"
```

### 3.1 Identification

```bash
curl -sS "$BASE"Display/GetMenuType?u=$UID\&c=$CODE | jq .
```

Expected shape — `is_menu` selects the mode (`"0"` web, `"1"` image/video):

```json
{ "Status": true, "is_menu": "1" }
```

Check: HTTP 200; `Status` is boolean `true` (not the string `"true"`); `is_menu` is one of
`"0"`/`"1"`. Any other value is rejected by the client.

### 3.2 Playlist

```bash
curl -sS "$BASE"Display/Menu?u=$UID\&c=$CODE | jq .
```

Expected shape — note the key casing (`page_detail`, `page_code`, `template_id`, `channel_data`, `refresh_rate`):

```json
{
  "Status": true,
  "page_detail": [ { "page_code": "VIDEO_PAGE", "template_id": "4" } ],
  "channel_data": { "refresh_rate": "5000" }
}
```

Check: `page_detail` order is the intended playback order; `template_id` is `"2"`, `"3"`, or
`"4"` and **all entries match the same mode**; `channel_data.refresh_rate` is present and
positive.

### 3.3 Page media

```bash
curl -sS "$BASE"Display/GetPage?u=$UID\&c=$PAGE | jq .
```

Note: here `c` is the **page code**, not the screen code.

Video (template 4) — array order is playback order:

```json
{ "Status": true, "page_data": [ { "filename": "intro-v1.mp4" }, { "filename": "https://media.example.com/promo-v2.mp4" } ] }
```

Single image (template 2) — each entry is its own slideshow slide:

```json
{ "Status": true, "page_data": [ { "filename": "poster-v1.png" } ] }
```

Three-panel (template 3) — exactly one image per slot, all three present:

```json
{ "Status": true, "page_data": [ { "filename": "left-v1.png", "slot": "1" }, { "filename": "header-v1.png", "slot": "2" }, { "filename": "right-v1.png", "slot": "3" } ] }
```

### 3.4 Media fetchability

```bash
# Relative filename resolves beneath <base>assets/contents/
curl -sSI "$BASE"assets/contents/poster-v1.png | head -20

# Absolute URL (separate media host) — verify TLS is valid
curl -sSI "https://media.example.com/promo-v2.mp4" | head -20
```

Check: HTTP 200; a real image/video `Content-Type` (not `text/html`); `Content-Length`
non-zero and accurate. A sign-in page or JSON error here becomes an undecodable media file.

### 3.5 Web display

```bash
curl -sS "$BASE"display?c=$CODE\&u=$UID | head -40
```

Check: returns the rendered HTML display over HTTPS, with no redirect to an external login
origin (the client blocks off-origin top-level navigation).

**Phase 0 gate:** all four modes return the expected shapes. Do not proceed until they do.

## 4. Phase 1 — Per-mode on-device verification

Reset between modes. To return to setup, press Android **Back** during playback and enter the
admin PIN. To clear everything (destructive), use Android **Clear storage**.

For **each** mode below, record the outcome in the evidence log.

### 4.1 Web display (`is_menu: "0"`)

1. Enter the web screen code → **Identify**.
2. Expect the assigned page to render at the base origin.
3. Turn off Wi-Fi → the page errors; expect a **Retry** button, not a blank screen.
4. Restore network → expect automatic recovery within ~30 s, or tap **Retry**.
5. On-page link to an external origin (if any) must be blocked, not opened.
6. Kill and reopen the app → it returns to web display without re-identification.

### 4.2 Single image / template 2

1. Enter the template 2 code → **Identify**.
2. Expect each `page_data` entry to display in source order, advancing on `refresh_rate`.
3. Note actual on-screen dwell per image and compare to `channel_data.refresh_rate`
   (values ≥ 1000 are milliseconds; see [§6.3](#63-refresh_rate-units)).
4. Enable airplane mode → cached images keep advancing after restart.
5. Confirm no torn/partial images (a partial download would decode as blank).

### 4.3 Three-panel / template 3

1. Enter the template 3 code → **Identify**.
2. Expect left / top / right populated from slots 1 / 2 / 3 respectively, all three at once.
3. Restart the app → the three panels restore from cache.
4. If any slot is missing server-side, expect a recoverable error and **no** cache wipe.

### 4.4 Video / template 4

1. Enter the video code → **Identify**.
2. Expect clips to play in `page_data` order, looping at the end.
3. Background the app → resume → playback continues sensibly (not stuck).
4. Restart the app → cached video plays without re-download.
5. Verify clips decode on the target hardware (no transcode in-client). Capture codec details:

```bash
adb shell dumpsys media.extractor | grep -i -m5 'VIDEO\|mp4' || true
ffprobe -hide_banner "https://media.example.com/promo-v2.mp4" 2>&1 | grep -i 'stream\|codec'
```

6. If a clip is unplayable, expect the client to skip it and retry after all clips fail.

### 4.5 Mode-change guard

On a **configured** screen whose server mode changed (e.g. images → video), the client must
show *"The screen's playback mode changed. Return to setup and identify again."* and keep the
old cached playlist playing. It must not silently delete content.

## 5. Phase 2 — Error-contract and cache-safety tests

The core guarantee: **a failed or partial refresh never destroys a working playlist.**
For each case, start from a screen with content already cached and playing.

| # | Inject | Expected |
| --- | --- | --- |
| 1 | Return `{"Status": false, "Message": "..."}` on `Display/Menu` | Recoverable error / silent retry; cached playlist keeps playing |
| 2 | Return HTTP 500 on `Display/GetPage` | Same; no pruning of cached media |
| 3 | Return `page_data: []` (empty) | Treated as a failure, **not** a remote erase; cache intact |
| 4 | Return a truncated body (bad `Content-Length`) | Download rejected; cache intact |
| 5 | Return `Content-Type: text/html` with an error page | Download rejected; cache intact |
| 6 | Remove one of three template-3 slots | Whole update rejected; previous playlist intact |
| 7 | Change a filename's bytes **without** changing the URL | **Client re-uses the cached bytes** — confirm this is the documented immutability rule; version the URL to force an update |
| 8 | Point at an HTTP (non-HTTPS) media URL | Rejected; cleartext not permitted |
| 9 | Redirect HTTPS → HTTP | Rejected; redirect disabled |
| 10 | Expired/invalid TLS certificate | Connection refused; no bypass (the client never disables verification) |

Record which of these the server can actually be coaxed into producing.

## 6. Phase 3 — Open questions to close with the server team

### 6.1 Server-side authorization (blocking for private content)

Screen code + device ID (`u`) **are not authorization** — both are guessable/shareable, and
none of the calls carry a token. Decide and document: enrollment, revocation, and media access.
Do not expose private content until this is agreed.

### 6.2 Media URL immutability

The client re-uses a non-empty cached file for an identical URL. Replacing content **requires
a new filename or URL query version**. Confirm the server/CDN enforces this and that operators
never overwrite a filename in place.

### 6.3 `refresh_rate` units

The client treats values ≥ 1000 as milliseconds and values < 1000 as seconds (legacy
compatibility), defaults invalid/≤0 to 5 s, and clamps to 1–86400 s. This is ambiguous for
values like `900`. Agree an explicit unit (prefer **milliseconds**) and record it here.

### 6.4 Error message hygiene

`Message` is operator-facing and displayed in the app. Confirm it never includes stack traces,
credentials, or private server details.

## 7. Evidence log

One row per check. Attach screenshots/logcat where useful.

| # | Mode | Endpoint / action | Command or step | Observed | Expected | Pass | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | — | `GetMenuType` | §3.1 | | | ☐ | |
| 2 | — | `Menu` | §3.2 | | | ☐ | |
| 3 | web | `GetPage` | §3.3 | | | ☐ | |
| 4 | t2 | `GetPage` | §3.3 | | | ☐ | |
| 5 | t3 | `GetPage` | §3.3 | | | ☐ | |
| 6 | t4 | `GetPage` | §3.3 | | | ☐ | |
| 7 | web | web display | §3.5, §4.1 | | | ☐ | |
| 8 | t2 | slideshow order | §4.2 | | | ☐ | |
| 9 | t3 | three panels | §4.3 | | | ☐ | |
| 10 | t4 | video order/loop | §4.4 | | | ☐ | |
| 11 | all | offline relaunch | §4.2–4.4 | | | ☐ | |
| 12 | all | error/cache safety | §5 | | | ☐ | |

Useful capture commands:

```bash
# App + network log while reproducing an issue
adb logcat -c && adb logcat | grep -iE 'advdisplay|okhttp|webview|media'

# Cached media + manifest. Primary location is the app-specific EXTERNAL files dir
# (PlaylistCache uses getExternalFilesDir(null)/<CODE>, falling back to internal filesDir).
adb shell ls -l /sdcard/Android/data/com.rihlahidali.advdisplay/files/<CODE>/
adb shell cat /sdcard/Android/data/com.rihlahidali.advdisplay/files/<CODE>/playlist.json | jq .

# Internal-storage fallback (debug builds only)
adb shell run-as com.rihlahidali.advdisplay ls -l files/<CODE>/
```

## 8. Exit criteria

M2 is complete when:

- [ ] All four modes (web, template 2, template 3, video) verified end-to-end on-device
      against the real API, with evidence recorded above.
- [ ] Every row of the [error/cache-safety table](#5-phase-2--error-contract-and-cache-safety-tests)
      passes: no failure path destroys a working playlist.
- [ ] The mode-change guard behaves as specified.
- [ ] §6.1 authorization decision agreed (or private content explicitly deferred).
- [ ] §6.2 media immutability and §6.3 `refresh_rate` units confirmed in writing.
- [ ] `docs/API_HANDOFF.md` updated with any deviations found.

## 9. Troubleshooting

| Symptom | Likely cause | Check |
| --- | --- | --- |
| Identify fails immediately | Code unassigned, or wrong base URL baked in | §3.1; re-verify `BuildConfig.API_BASE_URL` (§2) |
| HTTP 200 but "empty response" | Null/empty body; client treats it as failure | `curl -sS -i` and inspect the raw body |
| Media never plays | Relative path not under `assets/contents/`, or error page returned | §3.4; check `Content-Type` and `Content-Length` |
| Web display blank | Off-origin redirect or cert error | §3.5; `adb logcat \| grep -i ssl` |
| Slides change too fast/slow | `refresh_rate` unit ambiguity | §6.3 |
| Old content keeps playing after an update | Failed/partial refresh retained the previous playlist (by design) | §5; verify the server now returns a complete playlist |
| "Playback mode changed" on a screen you just reconfigured | Server mode changed without re-identification | Re-run **Identify** from setup |

## Changelog

| Date | Change |
| --- | --- |
| 2026-09-28 | Created. Phased procedure grounded in the client's actual endpoints and validation rules. |
