# Release Signing Walkthrough

How to sign SDS release builds, why the signing key matters more than it looks, and how to
verify the result. Every command here was executed against this repository before being
documented.

- **Config file:** `keystore.properties` at the repo root (gitignored)
- **Template:** [`keystore.properties.example`](../keystore.properties.example)
- **Wired in:** [`app/build.gradle`](../app/build.gradle) (`hasReleaseSigning` → `signingConfigs.release`)

Related: [Versioning & upgrade policy](VERSIONING.md#the-signing-key-is-the-linchpin) · [M3 runbook §A.0](RUNBOOK_M3.md#a0-determine-what-signed-the-currently-installed-build)

---

## 1. Why this matters

Android installs an update **only** if it is signed with the **same certificate** as the
installed app. If the key changes, the install fails:

```
INSTALL_FAILED_UPDATE_INCOMPATIBLE
```

The operator then has to uninstall first — which deletes the `advForward` preferences and the
app-specific external-files directory, wiping the **screen identifier, device ID, and cached
media**. A changed key therefore means a one-time manual re-identification on every device.

So a signing key is not just a build detail; it is the identity of the app across its lifetime.

> Debug builds sign with `~/.android/debug.keystore` (alias `androiddebugkey`, password
> `android`). That key is shared and machine-specific — **never** use it for anything an
> operator installs.

## 2. Step 0 — Find out what signed the current installs

**Do this before generating anything.** It decides whether you can reuse an existing key or
must plan a migration.

On a device that already has SDS installed:

```bash
PKG=com.DevCiplak.advdisplay

# Pull the installed APK (path differs per Android version)
APK_PATH=$(adb shell pm path $PKG | sed 's/package://' | tr -d '\r' | head -1)
adb pull "$APK_PATH" /tmp/installed.apk

# Print its signing certificate
apksigner verify --print-certs /tmp/installed.apk | grep -i 'DN\|SHA-256'
```

Then compare that SHA-256 with your candidates. Normalise both before comparing (strip colons,
lowercase):

```bash
# Normalised digest of the installed APK
apksigner verify --print-certs /tmp/installed.apk \
  | sed -n 's/.*SHA-256 digest: //p' | tr -d ':' | tr 'A-F' 'a-f'

# Normalised digest of the Android debug keystore (if you suspect debug signing)
keytool -list -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -storepass android -keypass android \
  | sed -n 's/.*SHA256: //p' | tr -d ':' | tr 'A-F' 'a-f'

# Normalised digest of your deployment keystore (once it exists)
keytool -list -v -keystore /path/to/release.jks -alias sds-release -storepass '<store-pass>' \
  | sed -n 's/.*SHA256: //p' | tr -d ':' | tr 'A-F' 'a-f'
```

Note `keytool` prints `SHA256:` while `apksigner` prints `SHA-256 digest:` — different labels,
same digest.

**Decide now:**

| Finding | Action |
| --- | --- |
| Installed builds match your deployment keystore | Reuse it. Installs upgrade in place. |
| Installed builds are **debug-signed** and few devices exist | Plan a one-time uninstall + re-identification, then move to the deployment key. |
| Installed builds are debug-signed but many are in the field | Consider keeping that key for continuity (it is not secret — anyone can extract it), or budget a site visit per device. |
| Installed builds use an unknown/lost key | Treat as a fresh deployment: uninstall + re-identify every device. |

## 3. Step 1 — Generate the deployment keystore

Run once, from a machine you control. Keep the output **outside** the repository.

```bash
keytool -genkeypair -v \
  -keystore ~/keys/sds-release.jks \
  -storetype PKCS12 \
  -alias sds-release \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=SDS Signage, OU=Ops, O=AdvDisplay, L=<City>, ST=<State>, C=<CC>"
```

`keytool` prompts for **two** passwords: the store password and the key password. Use a long,
unique passphrase from your password manager, and record both.

| Flag | Why |
| --- | --- |
| `-storetype PKCS12` | Well-supported modern format (JDK 9+ default); explicit for determinism |
| `-keyalg RSA -keysize 2048` | Widely accepted by Android and tooling |
| `-validity 10000` | ~27 years. An expired key cannot sign; Android also rejects certs expiring too soon |
| `-alias sds-release` | Stable alias; changing it can break signing configs |

Verify it was created:

```bash
keytool -list -v -keystore ~/keys/sds-release.jks -alias sds-release | grep -i 'Alias\|Valid from\|Signature algorithm'
```

> **Back up the keystore and both passwords now** — see [§7](#7-step-5--back-up-the-keystore).
> Losing this file is equivalent to losing the ability to update the app in place.

## 4. Step 2 — Configure `keystore.properties`

Copy the template and fill it in:

```bash
cp keystore.properties.example keystore.properties
```

```properties
storeFile=/Users/you/keys/sds-release.jks
storePassword=<store-pass>
keyAlias=sds-release
keyPassword=<key-pass>
```

- `storeFile` may be **absolute** or **relative to the repository root**.
- All four keys are required. If any is missing, Gradle silently falls back to an **unsigned**
  release build (no error) — confirm with [§6](#6-step-4--verify-the-signature).
- This file is gitignored (along with `*.jks`/`*.keystore`), but it holds plaintext passwords:
  keep it to a trusted machine and never paste it into a ticket or chat.

Confirm it will not be committed:

```bash
git check-ignore -v keystore.properties
```

## 5. Step 3 — Build the signed release

Release builds also enforce two other guards, so supply them here as well:

- `SDS_ADMIN_PIN` — 4–12 digits, **not** `1234` (the debug default)
- `SDS_BASE_URL` — must be **HTTPS**

```bash
export SDS_ADMIN_PIN='<your-6-digit-pin>'
export SDS_BASE_URL='https://sds.par-crm.com/'

./gradlew :app:assembleRelease
```

Gradle-property form works too (properties win over environment variables):

```bash
./gradlew :app:assembleRelease -PSDS_ADMIN_PIN=<pin> -PSDS_BASE_URL=https://sds.par-crm.com/
```

Output:

| Situation | Artifact |
| --- | --- |
| `keystore.properties` present and complete | `app/build/outputs/apk/release/app-release.apk` (**signed**) |
| File missing or incomplete | `app/build/outputs/apk/release/app-release-unsigned.apk` (**not** installable) |

The filename alone tells you which happened — but verify anyway (§6).

## 6. Step 4 — Verify the signature

Never ship on the strength of the filename. Check the actual certificate:

```bash
APK=app/build/outputs/apk/release/app-release.apk
apksigner verify --print-certs "$APK" | grep -i 'DN\|SHA-256'
```

Expected output (digest will be yours):

```
Signer #1 certificate DN: CN=SDS Signage, OU=Ops, O=AdvDisplay, L=..., ST=..., C=...
Signer #1 certificate SHA-256 digest: 719c6c252a76e0779f48e29eb84f51d5b1b2691c6634d6c2b0dfb2109541890c
```

Then confirm it matches your keystore exactly:

```bash
A=$(apksigner verify --print-certs "$APK" | sed -n 's/.*SHA-256 digest: //p' | tr -d ':' | tr 'A-F' 'a-f')
K=$(keytool -list -v -keystore ~/keys/sds-release.jks -alias sds-release -storepass '<store-pass>' \
      | sed -n 's/.*SHA256: //p' | tr -d ':' | tr 'A-F' 'a-f')
[ "$A" = "$K" ] && echo "MATCH" || echo "MISMATCH — do not ship"
```

Also confirm the build carries the intended configuration and version:

```bash
aapt2 dump badging "$APK" | grep -i '^package'
# Expect versionCode='10000' versionName='1.0.0'

# A release APK must NOT report application-debuggable (debug builds do):
aapt2 dump badging "$APK" | grep -qi 'application-debuggable' \
  && echo "UNEXPECTED: debuggable build" || echo "not debuggable (correct)"

# Sanity-check the API URL baked into the build
unzip -p "$APK" classes.dex | strings | grep -m1 'sds.par-crm.com'
```

## 7. Step 5 — Back up the keystore

Treat the keystore and its two passwords as production secrets, because they are:

- [ ] Keystore file stored in a password manager or encrypted vault (**two** independent copies)
- [ ] Both passwords recorded separately from the file
- [ ] The certificate SHA-256 digest recorded in your runbook / secrets store
- [ ] Confirmed the file is **not** in git: `git log --all -- '*.jks' '*.keystore' keystore.properties`
- [ ] A named owner for the key, and a successor who can access it

> Anyone holding the keystore and passwords can publish updates that Android accepts as yours.
> Losing it means you can never update an installed app in place again. Both failure modes are
> permanent.

## 8. Step 6 — Test the upgrade (and record the outcome)

Signing is only proven by an actual upgrade. Full procedure in
[RUNBOOK_M3 §A](RUNBOOK_M3.md#4-phase-a--install-upgrade-and-identity-preservation); the short
version:

```bash
# 1. Note the current state
adb shell run-as com.DevCiplak.advdisplay cat shared_prefs/advForward.xml
adb shell ls -l /sdcard/Android/data/com.DevCiplak.advdisplay/files/<CODE>/

# 2. Install the new signed APK over the existing one
adb install -r app/build/outputs/apk/release/app-release.apk

# 3. Confirm: no re-identification prompt, same device ID, cached media intact
```

Record the signing-certificate decision from [§2](#2-step-0--find-out-what-signed-the-current-installs).

## 9. Optional — Sign in CI

Not currently wired. If you automate release builds, never commit the keystore; provide it
through secrets and materialise it only for the build.

Add repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

```bash
# One-time: encode the keystore for the secret (then delete the temp file)
base64 -i ~/keys/sds-release.jks -o /tmp/ks.b64
# paste the contents of /tmp/ks.b64 into the KEYSTORE_BASE64 secret
rm -f /tmp/ks.b64
```

Workflow step (sketch):

```yaml
      - name: Restore signing keystore
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
        run: |
          set -euo pipefail
          echo "$KEYSTORE_BASE64" | base64 --decode > "$RUNNER_TEMP/release.jks"
          cat > keystore.properties <<EOF
          storeFile=$RUNNER_TEMP/release.jks
          storePassword=${{ secrets.KEYSTORE_PASSWORD }}
          keyAlias=${{ secrets.KEY_ALIAS }}
          keyPassword=${{ secrets.KEY_PASSWORD }}
          EOF

      - name: Build signed release
        env:
          SDS_ADMIN_PIN: ${{ secrets.SDS_ADMIN_PIN }}
          SDS_BASE_URL: ${{ secrets.SDS_BASE_URL }}
        run: ./gradlew :app:assembleRelease
```

Guardrails if you take this route: gate it to a protected tag or manual dispatch, keep the
artifact restricted, and never echo the secrets. The base64 round-trip was verified to restore
a byte-identical, usable keystore.

## 10. Command reference

```bash
# Inspect any keystore
keytool -list -v -keystore <file>.jks -alias <alias>

# Inspect any APK's signer
apksigner verify --print-certs <file>.apk

# Confirm signing files are ignored
git check-ignore -v keystore.properties

# Prove nothing signing-related was ever committed
git log --all --oneline -- '*.jks' '*.keystore' keystore.properties

# Signed release
SDS_ADMIN_PIN=<pin> SDS_BASE_URL=https://sds.par-crm.com/ ./gradlew :app:assembleRelease
```

## 11. Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Different key than the installed app | §2; uninstall + re-identify, or re-sign with the original key |
| Output is `app-release-unsigned.apk` | `keystore.properties` missing or incomplete | All four keys required; run `git check-ignore -v keystore.properties` |
| `Keystore was tampered with, or password was incorrect` | Wrong store/key password, or PKCS12 key password differs from store | Re-enter carefully; regenerate if genuinely lost |
| `Failed to read key <alias>` | `keyAlias` mismatch | `keytool -list -keystore <file>` to list aliases |
| Release build fails on configuration | Missing/invalid `SDS_ADMIN_PIN` or non-HTTPS `SDS_BASE_URL` | Use a 4–12 digit PIN ≠ `1234` and an HTTPS URL |
| `apksigner: command not found` | Not on `PATH` | Use `$ANDROID_HOME/build-tools/<version>/apksigner` |
| Digest mismatch but you expected a match | Trailing newline/whitespace, or comparing uppercase | Normalise with `tr -d ':' \| tr 'A-F' 'a-f'` |

## Changelog

| Date | Change |
| --- | --- |
| 2026-09-28 | Created. Every command validated against this repository (keystore generation, `keystore.properties`, signed build, `apksigner` verification, digest comparison, base64 round-trip). |
