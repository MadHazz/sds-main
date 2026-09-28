# M3 — Hardware Validation & Soak Runbook

Validates the client on **real signage hardware** over the on-site transport. The fixture suite
and M2 prove correctness against the API; this proves it survives reality — upgrades, flaky
networks, codecs, remote controls, heat, and time.

Maps to [VERIFICATION.md → Required Before Deployment](VERIFICATION.md).
Related: [M2 runbook](RUNBOOK_M2.md) · [Versioning & upgrade policy](VERSIONING.md) · [Operator guide](USER_GUIDE.md)

---

## 1. Prerequisites

- [ ] A release candidate APK built against the **production HTTPS API** with a non-default PIN.
- [ ] The **deployment keystore** available (or a staging key) — see Phase A.
- [ ] At least one device representative of the fleet, ideally two (oldest + newest supported).
- [ ] The on-site network transport available: Wi-Fi **and** Ethernet where the site uses it.
- [ ] The **actual TV remote** for the target devices (not `adb input`).
- [ ] Host tooling: `adb`, `apksigner`/`keytool`, `ffprobe`/`ffmpeg` (for codec fixtures).
- [ ] Content prepared: real screen codes for web, template 2, template 3, and video.

## 2. Device matrix

Fill this in and run the phases on each row. Behaviour varies by SoC, WebView version, and remote.

| # | Model | Android / API | WebView version | Remote type | Transport | Role |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | | | | | Wi-Fi / Ethernet | Primary |
| 2 | | | | | | Oldest supported |

Capture the basics per device:

```bash
PKG=com.DevCiplak.advdisplay
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell dumpsys package com.google.android.webview | grep -m1 versionName
adb shell wm size
```

## 3. Expected runtime behaviour (reference)

Testers need to know the intended timing, otherwise "it didn't update" looks like a bug.

| Behaviour | Expected | Source |
| --- | --- | --- |
| Playlist sync while visible | every **300 s** (5 min) | `MenuSliderActivity`, `VideoPlayerActivity` |
| Retry after an error or empty content | every **30 s** | same |
| WebView auto-retry when failed | every **30 s** while online | `WebViewActivity` |
| Slideshow page dwell | `channel_data.refresh_rate` | `ContentRepository.parseRefreshRateSeconds` |
| Unplayable video | skipped; after **all** clips fail, retry after 30 s | `VideoPlayerActivity` |
| Screen stays awake | `FLAG_KEEP_SCREEN_ON` while visible | `PlaybackActivity`, `MainIdentifierActivity` |
| Backgrounding | sync and playback stop (lifecycle-scoped) | `repeatOnLifecycle(STARTED)` |
| Offline with cache | plays cache; shows an offline notice | `PlaybackViewModel` |

Sync only runs while the player is **visible**; it stops when backgrounded and resumes on return.

## 4. Phase A — Install, upgrade, and identity preservation

Depends on the signing-key constraint in [VERSIONING.md](VERSIONING.md#the-signing-key-is-the-linchpin).

### A.0 Determine what signed the *currently installed* build

Do this **before** installing anything new, on a device with the existing app installed.

```bash
PKG=com.DevCiplak.advdisplay
APK_PATH=$(adb shell pm path $PKG | sed 's/package://' | tr -d '\r' | head -1)
adb pull "$APK_PATH" /tmp/installed.apk
apksigner verify --print-certs /tmp/installed.apk
# Record the SHA-256 digest.
```

Compare with the deployment keystore:

```bash
keytool -list -v -keystore /path/to/release.jks -alias <alias> | grep -i 'SHA256'
```

- [ ] Digests **match** → installs upgrade in place. Proceed to A.2.
- [ ] Digests **differ** → the install will fail with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
      Decide with the operator: keep the original key, or accept a one-time uninstall +
      re-identification on every device (which wipes identity and cached media).

### A.1 Fresh install

- [ ] `adb install -r <apk>` on a clean device; confirm it appears in the Leanback **and**
      standard launchers.
- [ ] Identify a screen; confirm content begins and the on-screen behaviour matches §3.

### A.2 In-place upgrade (the critical test)

Run on a device with a **previous version already configured and playing cached media**.

1. Note the current screen code and evidence it is playing from cache (airplane mode works).
2. Record the pre-upgrade state:

```bash
adb shell run-as $PKG cat shared_prefs/advForward.xml 2>/dev/null \
  || adb shell cat /data/data/$PKG/shared_prefs/advForward.xml 2>/dev/null
adb shell ls -l /sdcard/Android/data/$PKG/files/<CODE>/
```

3. Install the new APK over the existing one (same signature):

```bash
adb install -r app-release.apk
```

4. **Expected:** no uninstall prompt; after launch the screen stays configured with the
   **same identifier and device ID**, and cached media is intact.

- [ ] Identifier preserved (no re-identification prompt)
- [ ] Device ID preserved (`advForward` → `deviceId` / `installationDeviceId` unchanged)
- [ ] Cached media still present in `/sdcard/Android/data/$PKG/files/<CODE>/`
- [ ] Legacy path: if the old build stored only `*.mp4` files and no `playlist.json`, the
      videos are still discovered and played (cache falls back to scanning complete `*.mp4`)

## 5. Phase B — Network resilience

Run per device and per transport. Use §3 timings as the yardstick.

| # | Scenario | Method | Expected |
| --- | --- | --- | --- |
| B1 | Offline relaunch | `adb shell svc wifi disable` (and unplug Ethernet), restart app | Cached content plays; offline notice; **no** data loss |
| B2 | Loss during download | Kill connectivity mid-sync | Download aborts; previous playlist keeps playing; no partial files left |
| B3 | Reconnect | Restore network | Sync recovers within 30 s (error state) or 300 s (normal) |
| B4 | Server error | Point at a failing endpoint / return 500 | Error surface; cache intact; retries |
| B5 | Wi-Fi → Ethernet switchover | Move between transports | Playback continues; sync resumes on the active transport |
| B6 | Flapping link | Toggle repeatedly | No crash, no ANR, no cache wipe |

```bash
adb shell svc wifi disable    # and: adb shell svc wifi enable
adb shell ip addr             # confirm interface state
adb shell dumpsys connectivity | grep -i -m5 'Active default network'
```

- [ ] All B1–B6 pass on each device/transport combination

## 6. Phase C — Content mutation & failure isolation

Core guarantee: see [RUNBOOK_M2 §5](RUNBOOK_M2.md#5-phase-2--error-contract-and-cache-safety-tests).
Here you confirm it survives real server edits.

- [ ] Reorder `page_detail` → playback order changes on the next sync (within 300 s).
- [ ] Add media → new items appear without losing the old ones.
- [ ] Remove media → old files are pruned **only after** a complete update commits.
- [ ] Make one page fail (missing file, 404) → the **whole** update is rejected and the
      previous playlist keeps playing. Confirm no old content was deleted.
- [ ] Return an empty playlist → treated as failure, not a remote erase.
- [ ] Change a media URL to a **new version** → the new bytes are downloaded (proves the
      versioning contract; reusing the same URL serves the cached copy by design).
- [ ] Change the screen's **mode** (images ↔ video) → the client refuses with
      *"The screen's playback mode changed. Return to setup and identify again."* and keeps
      the old content until the operator re-identifies.

```bash
# Confirm the manifest after each mutation
adb shell cat /sdcard/Android/data/$PKG/files/<CODE>/playlist.json | jq .
```

## 7. Phase D — Video & playback robustness

- [ ] **Looping:** plays clips in API order and loops back to the first at the end.
- [ ] **Background/resume:** leave the app, return → playback resumes sensibly (not stuck,
      not restarted mid-clip in a broken state).
- [ ] **Restart:** force-stop and relaunch → cached video plays without re-download.
- [ ] **Unsupported codec:** supply a clip the device cannot decode (e.g. an exotic codec or a
      corrupted file) → the client **skips** it, and after all clips fail retries after 30 s.
- [ ] **Low storage:** fill the data partition and trigger an update → graceful failure, cache
      preserved, server error surfaced. No crash, no wipe.

```bash
# Build an unsupported fixture
ffmpeg -i source.mp4 -c:v mpeg2video -c:a mp2 unsupported-codec.mp4

# Check free space, then fill the app's data partition carefully
adb shell df -h /data
adb shell dd if=/dev/zero of=/sdcard/filler bs=1m count=2048   # then delete it
rm -f /sdcard/filler   # cleanup (via adb shell rm)
```

## 8. Phase E — Remote control & operator flows

Use the **physical remote**. Keyboard remapping hides real problems.

| # | Flow | Steps | Expected |
| --- | --- | --- | --- |
| E1 | Exit to setup | Press Back → enter PIN → Continue | Returns to setup |
| E2 | Wrong PIN | Press Back → wrong PIN | Dialog stays; **screen stays configured** |
| E3 | Cancel | Press Back → Cancel | Returns to playback |
| E4 | Text input | Type an identifier with the remote | Readable field; leading/trailing spaces trimmed; letters upper-cased |
| E5 | Retry (web/slideshow) | Break then fix the server, press Retry | Recovers |
| E6 | Invite (share) | Tap **Invite Another Screen** | Share sheet, or clipboard fallback |
| E7 | Invite (receive) | Open `sds://join?c=CODE` on a clean device | Applies the code and identifies |
| E8 | Invite on configured screen | Open an invite link on a configured device | **Requires the admin PIN** before replacing setup |
| E9 | Cancel a replacement invite | Dismiss the PIN dialog | Existing screen unchanged |

```bash
# Only for sanity checks — E1–E9 must ultimately pass with the real remote
adb shell input keyevent KEYCODE_BACK
adb shell input keyevent KEYCODE_DPAD_DOWN
adb shell input keyevent KEYCODE_DPAD_CENTER
```

- [ ] All E1–E9 pass with the physical remote on each device

## 9. Phase F — WebView, TLS, and minimum-version compatibility

- [ ] Web display renders on each device's WebView version.
- [ ] Off-origin top-level navigation is **blocked**, not opened.
- [ ] `https` → `http` redirect is rejected.
- [ ] An expired/invalid certificate shows the certificate error; the client **never** bypasses it.
- [ ] Local file/content access and third-party cookies remain disabled.
- [ ] If Android 5 (API 22) is still in support scope, run Phases A–F on that device too —
      the code retains support but only API 35 emulator execution is currently recorded.

```bash
adb shell dumpsys package com.google.android.webview | grep -m1 versionName
ffprobe -hide_banner https://<host>/ 2>&1 | head -5   # inspect TLS/cert issues
```

## 10. Phase G — Long-running soak

Target: **minimum 72 hours** continuous playback on a release candidate, unattended.
M4's production soak is a separate, longer window.

Sample memory, temperature, and free space every 30 minutes. Save this as `soak-sample.sh`:

```bash
#!/usr/bin/env bash
# Appends a sample row every 30 min while the screen plays.
PKG=com.DevCiplak.advdisplay
OUT=soak.csv
[ -f "$OUT" ] || echo "timestamp,pss_kb,thermal_c,free_kb" > "$OUT"
while true; do
  TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  PSS=$(adb shell dumpsys meminfo "$PKG" | sed -n 's/.*TOTAL PSS:[[:space:]]*\([0-9]*\).*/\1/p' | head -1)
  TEMP=$(adb shell 'cat /sys/class/thermal/thermal_zone*/temp 2>/dev/null' \
         | awk 'BEGIN{m=0}{if($1>m)m=$1}END{if(m>0)printf "%.1f", m/1000}')
  FREE=$(adb shell df -k /sdcard | awk 'NR==2{print $4}')
  echo "$TS,${PSS:-},${TEMP:-},${FREE:-}" >> "$OUT"
  sleep 1800
done
```

Watch for:

- [ ] **Memory:** PSS stable over 72 h — a slow climb indicates a leak (repeated allocations
      in the video player or WebView are the usual suspects).
- [ ] **Temperature:** stays within the device's safe range; no thermal throttling stalls.
- [ ] **No crashes or ANRs:**

```bash
adb logcat -d | grep -iE 'FATAL EXCEPTION|ANR in com.DevCiplak.advdisplay'
```

- [ ] **Screen stays awake** for the whole run:

```bash
adb shell dumpsys power | grep -iE 'mWakefulness|mStayOn'
```

- [ ] **Playback continuity:** after 72 h the screen still cycles content correctly; syncs still
      land on schedule.
- [ ] **Storage:** cached media does not grow unboundedly across update cycles.

## 11. Evidence log

| # | Phase | Device | Scenario | Expected | Observed | Pass | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | A.0 | | Signing cert digest | matches keystore | | ☐ | |
| 2 | A.2 | | In-place upgrade preserves identity + cache | yes | | ☐ | |
| 3 | B | | Offline relaunch | cache plays | | ☐ | |
| 4 | B | | Loss during download | cache intact | | ☐ | |
| 5 | B | | Reconnect recovery | ≤ 30 s / 300 s | | ☐ | |
| 6 | C | | Failure isolation | no content deleted | | ☐ | |
| 7 | C | | Mode-change guard | refused | | ☐ | |
| 8 | D | | Unsupported codec | skipped + retry | | ☐ | |
| 9 | D | | Low storage | graceful, cache kept | | ☐ | |
| 10 | E | | Remote flows E1–E9 | all pass | | ☐ | |
| 11 | F | | WebView certs/nav | blocked/refused | | ☐ | |
| 12 | G | | 72 h soak | stable memory/temp | | ☐ | |

## 12. Exit criteria

M3 is complete when:

- [ ] Every phase passes on **each** device in the matrix, or deviations are documented and accepted.
- [ ] A signing-certificate decision is recorded (§A.0) and in-place upgrade is proven, or the
      re-identification migration plan is agreed.
- [ ] The 72-hour soak completes with stable memory, safe temperatures, and no crashes/ANRs.
- [ ] `docs/VERIFICATION.md` is updated with the outcome (devices, API, dates).
- [ ] Any client defects found are fixed and the affected phases re-run.

## 13. Troubleshooting

| Symptom | Likely cause | Check |
| --- | --- | --- |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Different signing key | Phase A.0; see [VERSIONING.md](VERSIONING.md) |
| Content never updates | Playing from cache because the server update fails | `logcat`; verify the page fetch with curl ([M2 §3](RUNBOOK_M2.md#3-phase-0--pre-flight-api-checks-curl-no-app)) |
| Blanks where images should be | Partial download or non-image response served | `curl -sSI` the media URL ([M2 §3.4](RUNBOOK_M2.md#34-media-fetchability)) |
| Video shows "unplayable" | Codec not supported by the device | `ffprobe` the clip; no transcode exists in-client |
| Web display blank | Off-origin redirect or TLS error | `logcat \| grep -i ssl`; Phase F |
| Slow creep in memory | Leak in playback or WebView | Phase G; capture `dumpsys meminfo` over time |
| Device sleeps mid-run | Keep-awake flag not applied | `dumpsys power`; confirm the player is foreground |

## Changelog

| Date | Change |
| --- | --- |
| 2026-09-28 | Created. Phases grounded in the client's actual timings (`MenuSliderActivity`, `VideoPlayerActivity`, `WebViewActivity`, `PlaybackViewModel`) and the VERIFICATION pre-deployment list. |
