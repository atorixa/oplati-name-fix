# Releasing

Everything below assumes the working copy is clean and the build succeeds locally.

## 1. Local build

```powershell
pwsh -NoProfile -File ./build.ps1 -Sdk "$env:ANDROID_SDK_ROOT"
```

The APK lands in `build/OplatiNameFix.apk`. Check the size and the hash:

```powershell
Get-FileHash build/OplatiNameFix.apk -Algorithm SHA256
```

## 2. Bump the version

Two places must match:

- `app/AndroidManifest.xml` → `android:versionName` and `android:versionCode`
  (`versionCode` must always increase, the store and `pm install -r` rely on it).
- `CHANGELOG.md` → a new `## [x.y]` section with the date.

## 3. Commit and tag

```powershell
git add -A
git commit -m "Release x.y"
git tag -a vx.y -m "vx.y"
git push origin main
git push origin vx.y
```

The tag triggers `.github/workflows/build.yml`, which builds the APK and attaches it to
a GitHub Release.

## 4. Signing key

The workflow signs with the repository key when the `KEYSTORE_B64` secret is present:

```powershell
# once, on the machine that owns the key
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore.jks")) | Set-Clipboard
```

Paste the clipboard contents into *Settings → Secrets and variables → Actions → New
repository secret* as `KEYSTORE_B64`, and set `KEYSTORE_PASS` / `KEYSTORE_ALIAS` as
repository variables if they differ from `android` / `test`.

**Keep `keystore.jks` out of the repository.** It is in `.gitignore` on purpose: without
the same key, an update cannot be installed over an existing copy — Android rejects it
with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

Without the secret the workflow still builds, but signs with a throwaway key and prints
a warning; such an APK is fine for testing, not for updates.

## 5. Manual release without Actions

```powershell
# build, then create the release in the web UI and upload the file yourself
gh release create vx.y build/OplatiNameFix.apk --title "vx.y" --notes-file CHANGELOG.md
```
