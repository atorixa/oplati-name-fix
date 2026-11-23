# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project uses [Semantic Versioning](https://semver.org/).

## [1.3] - 2026-11-23

### Changed

- **No launcher icon.** The settings activity now declares `android.intent.category.INFO`
  instead of `LAUNCHER`. `PackageManager.getLaunchIntentForPackage()` resolves `INFO` first,
  so the gear button in Vector / LSPosed still opens the settings, while the app drawer
  stays clean. Opening the screen directly still works:
  `adb shell am start -n love.atorixa.oplatinamefix/.SettingsActivity`.
  Text replacement itself is unchanged.

## [1.2] - 2026-11-22

### Changed

- **Application ID renamed** from `by.atorixa.oplatinamefix` to `love.atorixa.oplatinamefix`.
  The old ID claimed the `atorixa.by` domain, which the author does not own; the new one
  matches `atorixa.love`, so the module can pass the LSPosed / Vector catalogue domain
  verification. Update path: uninstall 1.1 first - the renamed package installs as a
  separate app and starts with an empty configuration.

## [1.1] - 2026-11-22

### Added

- **Settings screen** (`by.atorixa.oplatinamefix/.SettingsActivity`) with fields for the
  current and the displayed first name, surname, avatar initials and a free-form
  "extra replacements" list. The screen is reachable from the Vector / LSPosed module
  entry (it exposes a `LAUNCHER` activity so the manager can resolve it) and from
  `adb shell am start -n by.atorixa.oplatinamefix/.SettingsActivity`.
- **Read-only `ContentProvider`** (`by.atorixa.oplatinamefix.config`) that hands the
  configuration from the settings app to the hooked process. Only the module itself and
  the target package may query it.
- **Universal initials**: the avatar badge is rewritten from its own field instead of a
  hard-coded pair, and an automatic first-letter pair is derived from the configured
  names when no explicit pair is given.
- Cross-platform `build.ps1` (Windows and Linux/macOS tool names) with the signing key
  taken from `KEYSTORE_FILE` / `KEYSTORE_PASS` / `KEYSTORE_ALIAS`.
- GitHub Actions workflow: builds on every push and pull request, uploads the APK as an
  artifact and attaches it to a GitHub Release for `v*` tags.

### Changed

- Configuration is stored as a plain `key=value` file
  (`files/config.properties`, UTF-8) instead of `SharedPreferences`, so the hooked
  process can read it reliably across app sandboxes.
- Replacement is now a **single pass** over the text with `Matcher.appendReplacement`.
  The pattern is fenced with `(?<![\p{L}\p{N}])` / `(?![\p{L}\p{N}])`, so a token can
  never be replaced twice and `Ann` inside `Anna` is left alone.
- The module ships **no personal data**: the built-in defaults are generic
  (`Имя` / `Фамилия` / `ИФ`), and every real value is entered by the user on the device.
- User interface is English only, uses the system day/night theme and is built entirely
  in code (no layout resources to keep the APK tiny).

### Fixed

- Hooks are installed from `Activity.onCreate` / `onResume` instead of process start,
  because the framework loads legacy modules after the process has already started.
- `ClassNotFoundException` in the provider when it tried to log through the hook class.

## [1.0] - 2026-11-22

### Added

- First working build: hooks `TextView#setText` and (lazily) `AnnotatedString` inside
  `by.lwo.oplati` and rewrites the displayed first name, surname and avatar initials.
- Nothing is written to the account, the server or the app's own data — the rewrite is
  purely on screen.
