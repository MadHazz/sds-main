# SDS Android (AdvDisplay)

An Android digital-signage player. Enter a screen identifier once; SDS remembers the
screen and opens its assigned web display, image slideshow, or video playlist.
This repository is the Android client only. The API is developed separately.

- [Progress and release plan](docs/PROGRESS.md)
- [Versioning and upgrade policy](docs/VERSIONING.md)
- [Operator guide](docs/USER_GUIDE.md)
- [API handoff and examples](docs/API_HANDOFF.md)
- [Verification and device checklist](docs/VERIFICATION.md)

## Implemented Client Flows

- Identification with validation, loading/error states, cancellation, and a stable device ID.
- Web display with restricted navigation, certificate-error handling, and manual/automatic retry.
- Single-image and three-panel templates, with automatic page advancement.
- Video looping in API order, resume after backgrounding, and bounded retry for unplayable files.
- Cached image/video playback while offline, including after restarting the app.
- Transactional media updates: all pages and downloads must succeed before replacing the playlist
  and pruning old files. A failed update leaves the last complete playlist available.
- PIN-protected reconfiguration; external invites cannot replace a configured screen without the PIN.
- `sds://join?c=CODE` setup links and Android's share sheet (clipboard fallback).

This is not a device-owner kiosk app: the PIN protects in-app reconfiguration, not Android Home,
Settings, uninstall, or physical access. Web display requires a working server connection.

## Build and Run

Use JDK 17, Android SDK 33, and the checked-in Gradle wrapper. Minimum supported Android API
is 22; compile/target SDK remains 33. The project uses Kotlin 1.9.24, AGP 8.12.3, and Gradle 8.13.
Open the repository root in Android Studio, or run:

```bash
./gradlew qualityCheck :app:assembleDebug
./gradlew :app:installDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

### Configuration

| Setting | Default | Override |
| --- | --- | --- |
| API base URL | `https://sds.par-crm.com/` | `SDS_BASE_URL` environment variable or `-PSDS_BASE_URL=...` |
| Admin PIN | `1234` in debug builds | `SDS_ADMIN_PIN` environment variable or `-PSDS_ADMIN_PIN=...` |

Gradle properties take precedence over environment variables. These values are compiled into
the APK, so changing them requires rebuilding. Do not commit real PINs or signing keys.
The bundled PIN is an operator guard, not a secret that resists APK reverse engineering.

Production API/media URLs must use HTTPS with valid certificates. Debug builds allow HTTP
only for `localhost`, `127.0.0.1`, and emulator-host alias `10.0.2.2`. TLS verification is never disabled.

Release tasks fail without an explicitly configured 4-12 digit PIN different from `1234`,
or when the API URL is not HTTPS. Configure the PIN in your local environment, then run:

```bash
./gradlew :app:assembleRelease
```

### Release signing

Signing is opt-in and never uses a committed key. Copy `keystore.properties.example` to
`keystore.properties` at the repository root (gitignored) and fill in your deployment keystore:

```properties
storeFile=/absolute/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

With that file present, `./gradlew :app:assembleRelease` produces a signed
`app-release.apk`. Without it, the release build succeeds but emits an unsigned
`app-release-unsigned.apk`, so debug builds and CI are unaffected. Keystores and
`keystore.properties` are gitignored; never commit them.

Validate on actual signage hardware before rollout. This work does not claim Play Store readiness.

## Continuous Integration

`.github/workflows/android.yml` runs on pushes to `main`, pull requests, and manual dispatch.
It sets up JDK 17 and Android SDK, then runs `./gradlew qualityCheck` and `:app:assembleDebug`,
uploading the lint report, JVM test report, and debug APK as artifacts.

## Verification

```bash
# Compiler warnings are errors; lint errors fail the build.
./gradlew qualityCheck

# End-to-end tests against an in-process API fixture, on a disposable emulator.
./gradlew :app:connectedDebugAndroidTest -PSDS_BASE_URL=http://127.0.0.1:18080/

# Restore the normal API URL after building the test fixture variant.
./gradlew :app:assembleDebug
```

The fixture tests exercise actual Android video decoding, all three image panels, cached
relaunch, WebView retry, and protected invite/exit flows. They clear the installed app's
session and test content. They skip unless the exact fixture URL above is configured.
They do not call the production API. See [verification details](docs/VERIFICATION.md).

## Architecture

Kotlin + Android Views, `ViewModel`/`StateFlow`, coroutines, Retrofit/OkHttp/Gson, and Glide.
No new backend service or UI framework is required.

```text
app/src/main/java/com/DevCiplak/advdisplay/
  adapter/      Local image/template rendering
  analytics/    Local invite-event counters
  constant/     Configured API URL and endpoint paths
  data/         Session persistence and transactional playlist cache
  model/        API DTOs and typed playlists
  network/      Retrofit service, transport policy, cancellable media downloader
  repository/   Response validation and playlist assembly
  security/     Operator PIN gate and WebView origin policy
  ui/           Setup, shared player lifecycle, video/image/web activities
  util/         Identifier and URL validation
  viewmodel/    Identification and playback state
```

Image/video playlists synchronize on entry and periodically while the player is visible
(normally five minutes; failed or empty state retries on a shorter 30-second cycle).
Image page timing comes from `channel_data.refresh_rate`. Cached files and `playlist.json`
live in the app-specific external-files directory under the screen code, with internal-storage
fallback. Clearing the session keeps cached media; clearing Android app storage removes both.
Previous-version MP4 downloads are restored on first launch when no playlist manifest exists.

## Sharing Measurement

Existing local `growthTelemetry` preferences record a count and last-seen timestamp for
`share_clicked`, `invite_link_opened`, and `invite_accepted`. Acceptance means successful
identification, not completion of media downloads. No telemetry is uploaded, and there is
no cross-device attribution or central analytics dashboard in this client.

## Remaining Integration Work

Implement/verify the separate API against [API_HANDOFF.md](docs/API_HANDOFF.md), supply real
content and identifiers, and agree on server-side authorization. Real-server integration,
release signing, and a hardware soak test are still required before deployment. Supported
template IDs are 2, 3, and 4; unsupported or mixed video/image modes are rejected rather
than silently deleting cached content.
