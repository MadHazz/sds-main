# Versioning & Upgrade Policy

Defines how SDS versions are numbered and what must stay stable for an
in-place upgrade to preserve a screen's identity and cached media.

- **Current release:** `versionName` `1.0.0` · `versionCode` `10000`
- Set in [`app/build.gradle`](../app/build.gradle) under `defaultConfig`.

## Scheme

| Field | Format | Purpose |
| --- | --- | --- |
| `versionName` | `MAJOR.MINOR.PATCH` (SemVer) | Operator-facing label; Android ignores it for upgrade decisions |
| `versionCode` | `MAJOR * 10000 + MINOR * 100 + PATCH` | Integer Android compares to decide whether an install is an upgrade |

| Version | `versionCode` |
| --- | --- |
| `1.0.0` | `10000` |
| `1.0.1` | `10001` |
| `1.3.0` | `10300` |
| `1.3.7` | `10307` |
| `2.0.0` | `20000` |

Bounds: `MINOR`/`PATCH` are `0-99`; `MAJOR` stays well below `2000` so the result fits
a signed 32-bit int (Play Store's own cap is `2,100,000,000`).

## When to bump which part

| Change | Bump | Examples |
| --- | --- | --- |
| Breaking change | `MAJOR` | API contract change, `minSdk` raise, cache/manifest format change, or any change that forces re-identification or discards cached media |
| Backward-compatible feature | `MINOR` | New template support, new operator flow, added setting |
| Backward-compatible fix | `PATCH` | Bug fix, security patch, copy change; no API or cache-format change |

Rule of thumb: if an upgraded device could lose its session or cached playlist, it is
a `MAJOR` bump and must be called out in the release notes.

## Rules

1. **Never decrease `versionCode`.** Android rejects an install whose `versionCode` is
   lower than the installed one (a downgrade). A higher or **equal** code installs fine.
2. **Never reuse an `applicationId` for different content.** `com.DevCiplak.advdisplay`
   is the app identity; changing it produces a separate app and loses all data.
3. **Every distributed build gets a version.** Increment at least `PATCH` for each build
   handed to an operator, so the installed version is always identifiable.
4. **Pre-releases share the target release's `versionCode`.** Give an RC/beta the
   `versionName` suffix (for example `1.0.1-rc1`) but the `versionCode` of the release it
   targets. Equal codes install over each other, so the GA build installs over its RC.
   Never ship an RC to production; never give an RC a code above its GA build.

## Upgrade & Data Preservation

An in-place upgrade (same `applicationId`, **same signing certificate**) preserves:

| Data | Where | Survives upgrade |
| --- | --- | --- |
| Screen code | `SharedPreferences` `advForward` → `codes` | ✅ |
| Device ID | `advForward` → `deviceId`, `installationDeviceId` | ✅ |
| Menu type | `advForward` → `menuType` | ✅ |
| Cached media | `<externalFilesDir>/<CODE>/` | ✅ |
| Legacy `.mp4` files | `<externalFilesDir>/<CODE>/` | ✅ restored when no `playlist.json` exists |

Both the old Java client and the Kotlin client used the same preferences file
(`advForward`) and the same media directory (`getExternalFilesDir(null)/<CODE>`), and the
cache falls back to scanning for complete `*.mp4` files when no manifest is present. So a
correctly signed upgrade keeps the screen configured and its media playable.

### The signing key is the linchpin

Android only performs an in-place upgrade when the new APK is signed with the **same
certificate** as the installed app. If the key differs, the install fails with
`INSTALL_FAILED_UPDATE_INCOMPATIBLE`, and the operator must uninstall first. Uninstalling
removes both the `SharedPreferences` file and the app-specific external-files directory,
so **the screen identifier, device ID, and cached media are all lost.**

Consequences:

- **Determine which key signed the currently installed builds before the first release.**
  If those installs are debug-signed, either keep signing with that key or plan a one-time
  uninstall + re-identification for every device.
- The deployment keystore must never be rotated casually. Rotating it is a `MAJOR`-scale
  event requiring a re-identification migration on every screen.
- `allowBackup=false` and the data-extraction rules exclude cloud backup and device
  transfer, so upgrade preservation depends **solely** on the in-place install.

## Release Checklist Tie-In

Before tagging a release:

- [ ] `versionCode` is greater than any code already installed on production devices.
- [ ] `versionName` matches the intended release label.
- [ ] Same deployment keystore as all previous releases.
- [ ] Verified an existing installation upgrades with identifier, device ID, and cached
      media intact (see [VERIFICATION.md](VERIFICATION.md), M3 item 2).

## Changelog

| Date | Change |
| --- | --- |
| 2026-09-28 | Scheme defined. Adopted `MAJOR*10000 + MINOR*100 + PATCH`; set `1.0.0` / `10000`. Documented the signing-key upgrade constraint. |
