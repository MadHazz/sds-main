# Client Verification

## Last Verified Run

On 2026-09-28:

- `qualityCheck :app:assembleDebug`: passed with the default HTTPS API configuration.
- JVM tests: 39 passed, 0 failed.
- API 35 emulator tests against the local fixture: 7 passed, 0 failed, 0 skipped
  (six player-flow tests and the existing package-context test).
- Lint: 0 errors, 8 advisory warnings for older-Android attributes, launcher icon padding,
  and the Gradle wrapper version. Transport enforcement also exists in OkHttp for older devices.
- Release configuration rejects the debug default PIN and an HTTP API URL.
- The final debug APK uses `https://sds.par-crm.com/`, not the fixture URL; debug PIN is `1234`.

The real API and physical signage hardware were not validated in this run.

## Automated Checks

```bash
./gradlew qualityCheck :app:assembleDebug
```

`qualityCheck` runs Kotlin compilation, JVM tests, and Android Lint. Kotlin warnings fail the
build. Lint errors fail the build; advisory warnings remain visible. The only third-party lint
exclusion targets Glide's unused notification target; AdvDisplay does not post notifications.

Reports:

- JVM: `app/build/reports/tests/testDebugUnitTest/index.html`
- Lint: `app/build/reports/lint-results-debug.html`
- APK: `app/build/outputs/apk/debug/app-debug.apk`

JVM regression coverage includes response validation and ordering, three-panel slots, legacy
refresh intervals, identifier/request races, cache commit/rollback/cancellation, offline state,
WebView origin checks, HTTPS transport, incomplete HTTP bodies, and prompt download cancellation.

## Emulator Integration Tests

Start a disposable Android emulator (the suite has been exercised on API 35), then run:

```bash
./gradlew :app:connectedDebugAndroidTest -PADVDISPLAY_BASE_URL=http://127.0.0.1:18080/
```

The tests start MockWebServer **inside the device** on port 18080, generate real PNG and H.264
MP4 fixtures, and exercise the installed activities. No separately running backend, external
media file, or production identifier is needed. Fixture tests skip if the exact URL above is
not configured. Use the default debug PIN when running this suite.

**Destructive to the test installation:** these tests clear the app session and the
`VIDEO_TEST`, `PANEL_TEST`, and `WEB_TEST` content directories. Do not run on an in-service screen.

Covered flows:

- An invite identifies a video screen and starts real video playback.
- A failed API refresh leaves cached video playing.
- An incorrect exit PIN preserves the session; a correct PIN returns to setup without deleting media.
- Three-panel images render and are restored after relaunch while page requests fail.
- A failed web display loads after Retry when the server recovers.
- An invalid invite shows a recoverable error on an unconfigured app.
- Invalid or replacement invites cannot bypass the PIN on a configured screen.

Report: `app/build/reports/androidTests/connected/debug/index.html`.

Afterward, rebuild without the fixture property before distributing an APK:

```bash
./gradlew :app:assembleDebug
```

## Release Guard

This must fail even if an environment PIN is set, because the explicit property uses the debug default:

```bash
./gradlew :app:validateReleaseConfiguration -PADVDISPLAY_ADMIN_PIN=1234
```

Release requires an explicitly supplied numeric PIN of 4-12 digits other than `1234`, and
an HTTPS API base URL. Supply your own release signing configuration; no production keystore
is committed. Never distribute the loopback fixture APK or the debug default PIN as production.

## Required Before Deployment

The fixture suite validates the client, not the availability or compatibility of the real API.

1. Use real identifiers for web, template 2, template 3, and video screens against the actual API.
2. Verify an existing installation upgrades without losing its identifier/device ID or cached MP4s.
3. Test offline relaunch, network loss during download, server errors, and reconnection using Wi-Fi
   and the transport used on site (including Ethernet where applicable).
4. Change playlist order, add/remove media, and confirm a failed page does not delete old content.
5. Test video looping, background/resume, restart, unsupported codecs, and low-storage failures.
6. Test Back/PIN, invite replacement/cancellation, input, and Retry using the actual TV remote.
7. Confirm WebView certificates/navigation and perform a long-running playback/temperature/memory soak.
8. Sign with the deployment keystore and verify the final API URL and non-default PIN.

Only API 35 emulator execution is recorded here. Android 5 support is retained in code, but
minimum-version hardware/WebView compatibility and production-device soak testing remain
separate checks. Device-owner lockdown, boot auto-launch, and app-store submission are not
implemented or implied by these client fixes.
