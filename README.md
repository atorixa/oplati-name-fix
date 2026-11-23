# Oplati Name Fix

[![build](https://github.com/atorixa/oplati-name-fix/actions/workflows/build.yml/badge.svg)](https://github.com/atorixa/oplati-name-fix/actions/workflows/build.yml)

A small Xposed module that changes the **name shown on screen** inside the Oplati app (`by.lwo.oplati`) — for people who want the app to display a different first name, surname or avatar initials than the ones on the account.

It rewrites text at render time only. Nothing is sent anywhere, nothing in your account, profile, database or network traffic changes.

> Русская версия: [README.ru.md](README.ru.md)

---

## At a glance

| | |
|---|---|
| Target app | `by.lwo.oplati` (Oplati) — tested with 1.46.1 |
| Framework | Vector (LSPosed fork) and LSPosed — **legacy Xposed API 93** |
| Android | 9 (API 28) and newer; tested on Android 17 QPR2 |
| Zygisk | any working implementation (tested with NeoZygisk 2.4) |
| Permissions | **none** — the manifest declares no `<uses-permission>` at all |
| Network | none — no internet code paths, no sockets |
| Storage | one private `key=value` file in the module's own data directory |
| License | MIT |

---

## Why

Oplati always shows the name stored on the account. There is no setting to display something else, and the profile is server-side, so editing the app's files is pointless: the next sync overwrites them.

This module leaves the account alone and edits only the strings that reach the screen. From the app's point of view nothing happened — it still holds the real profile, still signs requests with the real data, still passes its own checks. Only the pixels differ.

---

## Requirements

- Root with a working Zygisk provider: KernelSU, KernelSU Next, SukiSU, APatch or Magisk.
- One Zygisk implementation, exactly one: NeoZygisk, ReZygisk, Zygisk Next or Magisk's built-in.
- An Xposed framework that can load **legacy API 93** modules: Vector or LSPosed.
- Oplati installed.

Verified stack (works end to end):

| Component | Version |
|---|---|
| Device | Google Pixel 8 (`shiba`) |
| ROM | Android 17 QPR2, `CP4A.261105.040.A1` / 16581829 |
| Root | KernelSU 3.3.1 (`me.weishu.kernelsu`) |
| Zygisk | NeoZygisk 2.4 (289) |
| Xposed | Vector 2.2 (3080) |
| Oplati | 1.46.1 (210010469) |

Newer combinations should work as long as the three layers above are functional. If Xposed modules in general do not run on your device, this one will not either — fix the framework first.

---

## Install

1. Download `OplatiNameFix.apk` from Releases.
2. Install it as a normal app:

   ```sh
   adb install -r OplatiNameFix.apk
   ```

   or tap the file on the phone.
3. Open Vector (or LSPosed) → **Modules** → enable **Oplati Name Fix**.
4. Open its scope and tick **Oplati** (`by.lwo.oplati`). The module's own app is always in scope and needs no action.
5. Reboot, or at minimum force-stop Oplati so it starts with the module loaded.
6. Launch Oplati. The name should already be replaced with the neutral default.

The module ships with placeholder defaults — `Имя` / `Фамилия` / `ИФ` — so a fresh install visibly does something instead of silently doing nothing. Change them right after install.

---

## Settings

Open the settings screen from Vector's module page (gear button) or from a shell. The module has no launcher icon — see [No launcher icon](#no-launcher-icon):

```sh
adb shell am start -n love.atorixa.oplatinamefix/.SettingsActivity
```

| Field | Meaning |
|---|---|
| **Name the app shows now** | The first name currently displayed by Oplati. Case does not matter. |
| **Name to display** | What should appear instead. |
| **Surname the app shows now** | The surname currently displayed. |
| **Surname to display** | What should appear instead. |
| **Avatar initials now** | The 1–2 characters in the profile circle, e.g. `AS`. |
| **Initials to display** | What the circle should show, e.g. `AB`. |
| **Extra replacements** | Optional, one `old=new` per line, for any other string you want rewritten. |

Buttons:

- **Save** — writes the config and shows a toast.
- **Reset** — restores the neutral defaults.
- **Open Oplati** — launches the target app so you can check the result.

If the "now" fields are left empty, that replacement is skipped. The initials field is independent: when it is empty, the module derives initials automatically from the first letters of the configured name and surname.

After saving, Oplati must be reopened — screens that are already drawn keep their old text until they are recreated.

### No launcher icon

The settings screen is declared with the `INFO` category, not `LAUNCHER`. Android's `PackageManager.getLaunchIntentForPackage()` checks `INFO` first, so Vector (and LSPosed) still get a settings entry, while the app drawer stays clean.

Ways to open it:

- Vector → **Modules** → **Oplati Name Fix** → gear icon;
- shell: `adb shell am start -n love.atorixa.oplatinamefix/.SettingsActivity`;
- any activity launcher app that can start exported activities.

---

## How it works

The module loads only into `by.lwo.oplati`. Inside that process it:

1. **Hooks `TextView.setText(CharSequence)`** on every `TextView` class the app touches. This covers classic views, the bottom sheets and the profile list.
2. **Lazily hooks Compose**: when the app's class loader can see `androidx.compose.ui.text.AnnotatedString`, its constructors are hooked too, so text built by Compose is rewritten as well. The hook is installed on first use and skipped silently on builds that do not ship Compose.
3. **Rewrites matching strings in a single pass.** All replacement pairs are compiled into one alternation and applied with `Matcher.appendReplacement`, so a replacement can never be matched again by a later pair — no `A → B → BB` cascades.
4. **Respects word boundaries.** Every alternative is fenced with `(?<![\p{L}\p{N}])` and `(?![\p{L}\p{N}])`, so a short old value cannot match the tail of an unrelated word.
5. **Restores letter case.** `ALLCAPS`, `Capitalised` and `lowercase` inputs produce the corresponding form of the replacement.
6. **Reads its config once per screen, not per string.** The values come from the module's own storage through a read-only `ContentProvider` that only answers the module itself and the target app.

What it deliberately does not do:

- no hooks on network, crypto, keystore, database or file APIs;
- no modification of the APK, its signature or its resources;
- no changes to the account, the profile or any request payload;
- no collection, logging or transmission of the strings it sees (only build-tag log lines counting what was replaced, and only when a debug build is used).

---

## Limits

- **Only what is drawn.** If Oplati builds an image, a PDF or a QR payload from the name, that content is untouched. The current version is text-only.
- **Only the two text paths above.** A future Oplati release that renders names through `Canvas.drawText`, a WebView or a native view would not be covered until the module adds a hook for it.
- **Only the scoped app.** Nothing outside `by.lwo.oplati` is affected.
- **Not a privacy tool.** The server, the account and the device's own storage still contain the real name. This module changes the display, not the identity.

---

## Build

The project builds with plain JDK + Android SDK command-line tools. No Gradle, no Android Studio.

```powershell
pwsh -NoProfile -File build.ps1 -Sdk "C:\path\to\android-sdk"
```

Output: `build\OplatiNameFix.apk`.

Required SDK packages:

```sh
sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"
```

The script runs `javac` → `d8` → `aapt2 link` → injects `classes.dex` and `assets/xposed_init` into the APK → `zipalign` → `apksigner`. The keystore is read from `keystore.jks`; create your own before building:

```sh
keytool -genkeypair -keystore keystore.jks -alias test -keyalg RSA -keysize 2048 -validity 10000
```

Note: `keystore.jks` is git-ignored on purpose. Never publish a key you sign releases with.

---

## Troubleshooting

**Nothing changes at all.**

1. Confirm the module is enabled in Vector and that Oplati is in its scope.
2. Check that the framework works: `logcat | grep -i vector` should show the module being loaded into the app process.
3. Confirm the config was saved:

   ```sh
   adb shell su -c 'cat /data/data/love.atorixa.oplatinamefix/files/config.properties'
   ```

4. Force-stop and reopen Oplati.

**Header changes but the profile screen does not (or the other way round).**

The screen was already drawn. Reopen the app; every screen reads the config when it is created.

**The avatar circle still shows the old letters.**

Fill **Avatar initials now** explicitly. Many apps compute initials from a field the module never sees, so the automatic pair cannot be inferred.

**The app crashes after enabling.**

Disable the module, force-stop the app, then report the crash with a log:

```sh
adb logcat -d | grep -i -E 'OplatiNameFix|AndroidRuntime'
```

**Log lines for debugging.**

```sh
adb logcat -s OplatiNameFix
```

---

## Privacy

- The APK requests no permissions and contains no networking code.
- The config lives in the module's private storage: nobody but the module and the target app can read it.
- Nothing is uploaded, echoed or logged with your values. Build-tag logs print counts and package names only.
- Uninstalling the module removes the APK and its data; Oplati itself is untouched.

---

## Disclaimer

Unofficial community module. Not affiliated with, endorsed by or supported by LWO or the Oplati team. Use at your own risk: it hooks the UI of a financial app, and a future app update may break it.

If the app starts behaving oddly, disable the module first — that single step rules it in or out.

---

## License

MIT © atorixa
