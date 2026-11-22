# Oplati Name Fix

An Xposed module for the **Oplati** app (`by.lwo.oplati`) that rewrites the name
shown on screen. The account, the server and the app's own data are untouched:
nothing is uploaded and nothing is written into the target app's storage.

Built for the case where the account carries an old name but the phone should
show the new one.

## What it does

- Hooks `TextView.setText(...)` — rewrites text right before it is drawn.
- Hooks `androidx.compose.ui.text.AnnotatedString` — same for Compose screens
  (installed lazily, once the class is actually loaded).
- Replacements come from settings: name, surname, avatar initials and arbitrary
  `old=new` pairs. No personal value is hardcoded in the source.
- One-pass, case-aware replacement: `NAME` → `NAME`, `Name` → `Name`,
  `name` → `name`. No runaway chains, because the pattern refuses to match the
  tail of an already replaced word.
- Initials are handled separately: a short standalone string (up to 6 chars) is
  compared against the configured pair, so the avatar circle in the header and
  in the profile changes together with the name.

## Settings

- **Name the app shows now** — what the app currently displays.
- **Name to display** — what should be displayed instead.
- **Surname the app shows now** / **Surname to display**
- **Avatar initials now** / **Initials to display**
- **Extra replacements, one per line** — free-form `old=new` pairs.

Defaults are neutral (`Имя` / `Фамилия` / `ИФ`). While the "now" fields are
empty, the module changes nothing.

Open the screen from the Xposed manager (Vector / LSPosed) next to the module
entry, or directly:

```
adb shell am start -n by.atorixa.oplatinamefix/.SettingsActivity
```

## Requirements

- Android 8.1+ (API 27), verified on Android 17 QPR2 (Pixel 8).
- Any Xposed framework with legacy module support: Vector, LSPosed.
- Zygisk provider under KernelSU: NeoZygisk (ReZygisk is not usable on Android 17
  QPR2 — it lacks the new `app-specialize` signatures).

## Install

1. Build the APK (see below) or grab it from Releases.
2. `adb install -r OplatiNameFix.apk`
3. In the Xposed manager enable the module and set its scope to **`by.lwo.oplati` only**.
4. Restart the Oplati app.

## Build

Requires JDK 17+, Android SDK with `platforms/android-36` and `build-tools`
(d8, aapt2, zipalign, apksigner). No Gradle.

```powershell
pwsh -NoProfile -File build.ps1 -Sdk "C:\Android\sdk"
```

Output: `build/OplatiNameFix.apk`.

The script runs `javac` → `d8` → `aapt2 link` → injects `classes.dex` and
`assets/xposed_init` into the APK → `zipalign` → `apksigner`.

## Rollback

Disable the module in the Xposed manager and restart the app, or
`adb uninstall by.atorixa.oplatinamefix`.

## Layout

```
app/
  AndroidManifest.xml        module manifest, settings provider, settings screen
  assets/xposed_init         legacy module entry point
  src/by/atorixa/oplatinamefix/
    NameFix.java             hooks and config loading
    NameTransformer.java     single-pass, case-aware replacement
    Config.java              settings model
    ConfigStore.java         reads/writes config.properties
    ConfigProvider.java      read-only bridge into the target process
    SettingsActivity.java    settings screen (pure code, no layout resources)
stubs/                       Xposed API stubs, compile-only
build.ps1                    Gradle-free build
```

## License

MIT.
