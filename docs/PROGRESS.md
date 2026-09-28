# SDS Android — Progress & Release Plan

Living document. Update the **Status Snapshot** and task checkboxes as work lands.
Related docs: [README](../README.md) · [Operator guide](USER_GUIDE.md) · [API handoff](API_HANDOFF.md) · [Verification](VERIFICATION.md)

- **Owner:** Ahmad
- **Last updated:** 2026-09-28
- **Release target:** **v1.0.0 GA — 2026-11-20**

---

## Status Snapshot (2026-09-28)

| Area | State | Evidence |
| --- | --- | --- |
| Kotlin migration (Java → Kotlin) | ✅ Complete & committed | All `*.java` under `main` replaced by `*.kt` (`6ef0ffc`) |
| Build | ✅ Green | `./gradlew qualityCheck :app:assembleDebug` → `BUILD SUCCESSFUL` |
| JVM tests | ✅ 39 passing | `app/src/test/...` (9 files) |
| Emulator fixture tests | ✅ 7 passing (API 35) | `PlayerFlowTest.kt` + `ExampleInstrumentedTest` |
| Lint | ✅ 0 errors, 8 advisory warnings | `app/build/reports/lint-results-debug.html` |
| Security hardening | ✅ Present | `SecureTransport`, `WebNavigationPolicy`, `AdminGate`, release guard |
| Docs | ✅ Written & committed | `README.md` + `docs/` (`1af8431`) |
| Version control | ✅ Landed | 5 commits `7411ce7`..`1af8431` on `main` |
| Clean-checkout build | ✅ Verified | Fresh clone of `1af8431` → `BUILD SUCCESSFUL` (50 tasks), APK produced |
| CI | ✅ Green on GitHub | `.github/workflows/android.yml` — run `36400390759` passed; uploads APK + reports |
| Release signing | 🟡 Wiring done, keystore pending | Opt-in via gitignored `keystore.properties`; deployment keystore not yet created |
| Real API integration | ❌ Not done | Only local MockWebServer fixture validated |
| Hardware validation | ❌ Not done | No physical signage device soak |

**Build facts:** `applicationId com.DevCiplak.advdisplay` · `versionCode 1` / `versionName "1.0"` ·
`minSdk 22` · `targetSdk/compileSdk 33` · Kotlin 1.9.24 · AGP 8.12.3 · Gradle 8.13 · JDK 17.

---

## Milestones & Targets

| # | Milestone | Target | Exit criteria |
| --- | --- | --- | --- |
| **M0** | Land the migration | ✅ 2026-09-28 | All work committed in logical commits; build + tests green from a clean checkout |
| **M1** | Repo & release hardening | 2026-10-11 | CI runs `qualityCheck` on push/PR; release signing + versioning strategy configured; no stray artifacts tracked |
| **M2** | Real API integration | 2026-10-25 | Web, template 2, template 3, and video identifiers verified against the real API on-device |
| **M3** | Hardware validation & soak | 2026-11-08 | Remote flows, network loss/reconnect, codec coverage, low storage, and long-running soak pass |
| **M4** | Release candidate & GA | 2026-11-20 | Signed release APK, final HTTPS URL, non-default PIN, production soak, docs delivered, `v1.0.0` tagged |

---

## M0 — Land the migration ✅ (completed 2026-09-28)

Goal: remove the risk of losing the completed Kotlin rewrite by committing it safely.

- [x] **Fix index/.gitignore contradiction** — unstaged the 7 `.idea/*` files so the ignore
      rules take effect; `.gitignore` now also ignores `.idea/studiobot.xml`.
- [x] **Exclude `android/FakeDependency.jar`** — deleted (22-byte empty zip, referenced nowhere).
- [x] **Keep build caches out** — `build/`, `caches/`, `daemon/`, `native/`, `wrapper/`,
      `local.properties` remain ignored.
- [x] **Committed in logical chunks:**
  - [x] `7411ce7` build — toolchain, quality gates, release safety
  - [x] `6ef0ffc` refactor — Java → Kotlin migration
  - [x] `c2368da` feat — layouts, themes, manifests
  - [x] `1d32d0a` test — JVM + instrumentation coverage
  - [x] `1af8431` docs — README, guides, handoff, verification, roadmap
- [x] **Verified from a clean checkout** — fresh clone of `1af8431` with `local.properties`:
      `./gradlew qualityCheck :app:assembleDebug` → `BUILD SUCCESSFUL` (50 tasks), APK produced.
- [ ] **Push to `origin`** — 5 commits ahead of `origin/main`. Pending decision: push to `main`
      or open a feature branch + PR.

Notes:

- Three tracked `.idea/*` files (`compiler.xml`, `gradle.xml`, `misc.xml`) still carry
  machine-local IDE state (IDE JDK 17 → 21) and are intentionally left uncommitted.
- If the team prefers no IDE state in the repo at all, untrack the remaining `.idea/*` files
  with `git rm -r --cached .idea` in a follow-up.

---

## M1 — Repo & release hardening (target 2026-10-11)

- [x] **Add CI** (`.github/workflows/android.yml`):
  - [x] JDK 17 + Android SDK setup (`actions/setup-java`, `android-actions/setup-android`)
  - [x] Run `./gradlew qualityCheck` and `:app:assembleDebug`
  - [x] Upload lint report, JVM test report, and debug APK as artifacts
  - [x] Gradle dependency caching via `gradle/actions/setup-gradle`
  - [x] Confirm the workflow is green on GitHub — run
        [36400390759](https://github.com/MadHazz/sds-main/actions/runs/36400390759) passed all steps
        and uploaded `app-debug` (6.0 MB), `jvm-test-report`, and `lint-results-debug` artifacts
- [ ] **Release signing:**
  - [ ] Generate/obtain the deployment keystore (never commit it)
  - [x] Wire signing via a gitignored `keystore.properties` (template: `keystore.properties.example`)
  - [x] Verify `./gradlew :app:validateReleaseConfiguration -PSDS_ADMIN_PIN=1234` still **fails** (guard works)
  - [x] Confirm release build requires HTTPS `SDS_BASE_URL` and a 4–12 digit PIN ≠ `1234`
  - [x] Verified end-to-end with a throwaway keystore: signed release APK produced and signature confirmed via `apksigner`
- [ ] **Versioning:** define the `versionCode`/`versionName` bump policy; confirm the documented
      upgrade path preserves identifier, device ID, and cached MP4s.
- [ ] **Branch protection** (optional): require CI to pass before merge to `main`.

Verification performed locally on 2026-09-28: `qualityCheck` and `:app:assembleDebug` pass;
`assembleRelease` without `keystore.properties` emits `app-release-unsigned.apk`; with a
`keystore.properties` it emits a signed `app-release.apk`. All test signing artifacts were removed.

---

## M2 — Real API integration (target 2026-10-25)

Validates the client against the real backend described in [API_HANDOFF.md](API_HANDOFF.md).

- [ ] Obtain **real identifiers** for each mode: web, template 2, template 3, template 4 (video).
- [ ] Verify each mode end-to-end on-device against the production API over HTTPS:
  - [ ] Identification (`Display/GetMenuType`) → correct mode selection
  - [ ] Playlist (`Display/Menu`) → order preserved, unsupported/mixed modes rejected
  - [ ] Page media (`Display/GetPage`) → relative + absolute HTTPS URLs resolve correctly
  - [ ] Web display (`display`) → navigation restricted to the configured origin
- [ ] Confirm server behavior on the documented error contract (`Status: false`, `Message`,
      non-2xx, empty/null bodies) — none should clear cached content.
- [ ] **Agree server-side authorization** (currently undefined): enrollment, revocation, and media
      access. Screen code + `u` alone are **not** authorization. Decide before exposing private content.
- [ ] Confirm **media URL immutability/versioning** on the server; changing bytes requires a new
      filename/version (the client reuses cached bytes for an identical URL).
- [ ] Resolve the `refresh_rate` unit convention (prefer explicit milliseconds server-side).

---

## M3 — Hardware validation & soak (target 2026-11-08)

Maps to [VERIFICATION.md → Required Before Deployment](VERIFICATION.md). Run on the **real
signage hardware**, not only an emulator.

- [ ] Real identifiers for web, template 2, template 3, video on actual API + devices.
- [ ] Upgrade an existing installation without losing identifier/device ID or cached MP4s.
- [ ] Offline relaunch; network loss during download; server errors; reconnection over Wi-Fi
      **and** the on-site transport (Ethernet where applicable).
- [ ] Playlist mutations: reorder, add/remove media; confirm a failed page never deletes old content.
- [ ] Video: looping, background/resume, restart, unsupported codecs, low-storage failures.
- [ ] TV remote: Back/PIN, invite replacement/cancellation, text input, Retry.
- [ ] WebView: certificate errors and navigation policy.
- [ ] Long-running playback **soak** (temperature + memory), unattended for an extended period.
- [ ] Confirm Android 5 (API 22) minimum-hardware/WebView compatibility if still in support scope.

---

## M4 — Release candidate & GA (target 2026-11-20)

- [ ] Sign with the **deployment keystore**.
- [ ] Verify the final APK uses the **production HTTPS API URL** and a **non-default PIN**.
- [ ] Confirm the shipped APK is **not** the loopback fixture build
      (rebuild without `-PSDS_BASE_URL=http://127.0.0.1:18080/`).
- [ ] Operator documentation delivered ([USER_GUIDE.md](USER_GUIDE.md)); PIN handling agreed with the operator.
- [ ] RC soak on production hardware for an agreed window with sign-off.
- [ ] Tag `v1.0.0`; publish release notes + APK to the deployment channel.
- [ ] Out-of-scope for v1.0.0 (explicitly deferred): device-owner kiosk lockdown, boot auto-launch,
      Play Store submission, server-side fleet monitoring/cross-device attribution.

---

## Known Issues / Debt

| # | Item | Severity | Milestone |
| --- | --- | --- | --- |
| 1 | ~~Completed work uncommitted on `main`~~ resolved in M0 | ✅ Done | M0 |
| 2 | ~~`.idea/*` staged though `.gitignore` ignores them~~ resolved in M0 | ✅ Done | M0 |
| 3 | ~~`android/FakeDependency.jar` stray artifact~~ removed in M0 | ✅ Done | M0 |
| 4 | ~~No CI pipeline~~ workflow green on GitHub (run `36400390759`) | ✅ Done | M1 |
| 5 | Release signing wired; deployment keystore still to be created | 🟡 Medium | M1/M4 |
| 6 | Only fixture-local API validated | 🔴 High | M2 |
| 7 | No physical-device soak | 🔴 High | M3 |
| 8 | Server-side authorization undefined | 🟡 Medium | M2 |
| 9 | 8 advisory lint warnings (old-Android attrs, icon padding, wrapper version) | 🟢 Low | Backlog |
| 10 | `refresh_rate` unit ambiguity in API | 🟢 Low | M2 |

---

## Risks

| Risk | Impact | Mitigation |
| --- | --- | --- |
| ~~Work lost before first commit~~ mitigated | Low | M0 landed; push the 5 commits to `origin` |
| Real API contract differs from `API_HANDOFF.md` | High | M2 early integration testing; document deviations |
| Device codec/WebView variance on signage hardware | High | M3 broad hardware matrix; no transcode in client |
| Cleartext/TLS misconfiguration on site | High | Enforce HTTPS; debug-only loopback allowance |
| Release PIN / URL misconfigured in shipped APK | High | `validateReleaseConfiguration` guard + M4 checklist |
| Server auth treated as done by identifier alone | High | Block private-content rollout until M2 auth decision |

---

## Verification Commands

```bash
# Compile + JVM tests + Android Lint (Kotlin warnings are errors; lint errors fail)
./gradlew qualityCheck :app:assembleDebug

# Emulator integration tests against the in-process fixture (destructive to the test install)
./gradlew :app:connectedDebugAndroidTest -PSDS_BASE_URL=http://127.0.0.1:18080/

# Restore the normal API URL after the fixture run
./gradlew :app:assembleDebug

# Release guard must fail with the debug default PIN
./gradlew :app:validateReleaseConfiguration -PSDS_ADMIN_PIN=1234

# Signed release (after M1 signing setup)
./gradlew :app:assembleRelease
```

Reports: JVM `app/build/reports/tests/testDebugUnitTest/index.html` ·
Lint `app/build/reports/lint-results-debug.html` ·
Instrumentation `app/build/reports/androidTests/connected/debug/index.html` ·
APK `app/build/outputs/apk/debug/app-debug.apk`.

---

## Changelog

| Date | Change |
| --- | --- |
| 2026-09-28 | Created. 39 JVM + 7 instrumentation tests green; build passing; work uncommitted. Targets M0–M4 set for v1.0.0 GA on 2026-11-20. |
| 2026-09-28 | **M0 complete.** Landed the Kotlin migration and docs in 5 commits (`7411ce7`..`1af8431`); unstaged the ignored `.idea/*` files; removed stray `android/FakeDependency.jar`; verified `qualityCheck :app:assembleDebug` from a clean clone. Push to `origin` pending. |
| 2026-09-28 | **M1 in progress.** Added `.github/workflows/android.yml` (qualityCheck + assembleDebug + artifacts, Gradle caching); wired opt-in release signing via gitignored `keystore.properties` with `keystore.properties.example`; verified the release guard and a signed release APK end-to-end with a throwaway keystore. Deployment keystore, versioning policy, and branch protection still open. |
| 2026-09-28 | **CI green.** First run failed because `setup-android@v3` installs the removed `tools` package; fixed by moving to `@v4` and installing `platforms;android-33` explicitly, and bumped checkout/setup-java/upload-artifact/setup-gradle to current majors. Run `36400390759` passed in 3m11s with all three artifacts uploaded. |
