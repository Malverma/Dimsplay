# Dimsplay — Product & Technical Specification

**Version:** 0.1 (draft)
**Date:** 2026-10-01
**Platform:** Android (distributed as a sideloadable `.apk`)

---

## 1. Overview

Dimsplay is a small Android app that makes the screen darker than the
system brightness control allows. The user opens the app, moves one slider,
and a translucent black layer is drawn over everything on screen. The layer
stays on while the user switches to other apps, until they turn it off.

### 1.1 Goals

- Dim the screen below the system's minimum brightness, for use at night or in dark rooms.
- A single, obvious control: one slider plus an on/off toggle.
- Keep the dim layer active over all apps until the user stops it.
- Small APK (target < 3.5 MB). Network access is used only to show a banner ad (§4.6).
  Dimsplay itself collects no user data and has no analytics.

### 1.2 Non-goals (v1)

- Color temperature / blue-light filtering.
- Scheduling (e.g. dim automatically at sunset).
- Per-app dim profiles.
- Publishing on Google Play (v1 is a sideloaded APK; see §9).

---

## 2. Users & core use cases

| # | Use case | Expected result |
|---|----------|-----------------|
| U1 | Reading in bed, screen too bright at minimum brightness | Opens Dimsplay, drags slider, screen gets darker immediately |
| U2 | Leaves the app to use the browser | Dimming stays applied over the browser |
| U3 | Wants to turn dimming off quickly from any app | Taps "Stop" in the persistent notification |
| U4 | Reopens the app later | Slider shows the last-used level |
| U5 | First launch | App explains and requests the "Display over other apps" permission |

---

## 3. Functional requirements

### 3.1 Main screen (GUI)

A single-activity screen containing:

1. **Title**: "Dimsplay".
2. **On/Off switch** — starts or stops the dim overlay.
3. **Dim slider**
   - Range 0–100 (%), step 1.
   - 0 = no dimming, 100 = maximum dimming (see §4.2 for what "maximum" means).
   - Shows the current value as a label, e.g. `Dim: 45%`.
   - Changes apply **live** while dragging, with no "Apply" button.
   - Moving the slider while the switch is off turns the switch on.
4. **Extra dim switch** — "Also lower system brightness" (see §4.2.1).
   Turning it on the first time asks for the "Modify system settings"
   permission. If the permission is refused, the switch goes back to off.
   Turning it on while the main switch is off also turns dimming on, because
   Extra dim only acts while dimming runs.
5. **Permission banner** — shown only when the overlay permission is missing,
   with a "Grant permission" button that opens the system settings page.
6. **Ad banner** — anchored to the bottom of the screen, below the controls
   (§4.6). It takes no space until an ad has loaded.

In portrait, everything fits on one screen without scrolling: the progress
ring takes the height left over and shrinks on short screens.

Rough layout:

```
┌──────────────────────────────┐
│  Dimsplay                    │
│                              │
│  Dimming            [ ON ]   │
│                              │
│  Dim: 45%                    │
│  ○━━━━━━━━━●─────────────○   │
│                              │
│  Extra dim          [ OFF ]  │
│  Also lower system brightness│
│                              │
│  (permission banner, if any) │
└──────────────────────────────┘
```

### 3.2 Dim overlay

- A full-screen, black, non-interactive view drawn over all apps.
- Opacity is set from the slider value (mapping in §4.2).
- Touches pass through the overlay to whatever app is underneath.
- Covers the status bar area and stays applied across rotation.
- Survives the activity being closed; it is owned by a foreground service (§4.3).

### 3.3 Persistent notification

While dimming is active, a notification is shown (required for foreground
services) with:

- Text: `Dimming at 45%`.
- Actions: **Stop**, **−10%**, **+10%**.
- Tapping the notification body opens the main screen.

### 3.4 Persistence

- Save the last slider value and the Extra dim setting to `SharedPreferences`
  (or DataStore).
- On app launch, restore the slider position and the Extra dim setting.
- Dimming **always starts off** when the app is launched. The on/off state is
  not saved. If the service is already running (the user left the app and came
  back), the switch shows it as on.
- v1 does not restart dimming after a device reboot.

### 3.5 Safety: never leave the user stuck in the dark

- The main screen controls must always stay usable. The app's own activity
  is drawn under the overlay too, but the opacity cap (§4.2) keeps it readable.
- The notification "Stop" action is always available.
- Extra dim restores the original brightness whenever dimming stops (§4.2.1).
- Optional (later): a "panic" reset — if the app is opened and the slider is at
  100%, show a large "Reset" button.

---

## 4. Technical design

### 4.1 Stack

| Item | Choice |
|------|--------|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Min SDK | 26 (Android 8.0) — required for `TYPE_APPLICATION_OVERLAY` |
| Target / Compile SDK | 35 |
| Build | Gradle (Kotlin DSL), Android Gradle Plugin |
| Output | Signed release `.apk` |
| Dependencies | AndroidX core, Compose BOM, Activity Compose, AndroidX Fragment, Google Mobile Ads Next-Gen SDK (§4.6). No other third-party libraries. |

### 4.2 Dimming method

**Primary method: overlay window.**

- Add a `View` with a black background via `WindowManager` using
  `TYPE_APPLICATION_OVERLAY`.
- Window flags: `FLAG_NOT_TOUCHABLE`, `FLAG_NOT_FOCUSABLE`,
  `FLAG_LAYOUT_IN_SCREEN`, `FLAG_LAYOUT_NO_LIMITS`.
- Pixel format `PixelFormat.TRANSLUCENT`; size `MATCH_PARENT` × `MATCH_PARENT`.
- On API 28+, set `layoutInDisplayCutoutMode` to `SHORT_EDGES` (or `ALWAYS` on 30+) so the overlay also covers the notch area.

**Opacity cap (important):** On Android 12+ the system blocks touches from
reaching apps under an overlay whose opacity is above **0.8**
(`InputManager#getMaximumObscuringOpacityForTouch`). Above that, the phone
stops responding to touch. So:

```
overlayAlpha = (slider / 100f) * 0.80f
```

Slider 100% maps to alpha 0.80. This is a hard limit in code, not a setting.

#### 4.2.1 Extra dim (system brightness)

When the Extra dim switch is on and dimming is active, Dimsplay also turns
the system brightness down to its minimum, in addition to the overlay.

- Requires the `WRITE_SETTINGS` special permission, checked with
  `Settings.System.canWrite()` and requested with
  `Settings.ACTION_MANAGE_WRITE_SETTINGS`.
- When dimming starts, save the current `SCREEN_BRIGHTNESS` and
  `SCREEN_BRIGHTNESS_MODE` (auto/manual) to prefs. Then set the mode to manual
  and the brightness to the minimum value.
- When dimming stops, or Extra dim is switched off, restore both saved values
  and clear them from prefs.
- **Crash recovery:** if the service dies without restoring, the saved values
  are still in prefs. On the next app launch or service start, if saved values
  exist and dimming is not active, restore them right away.
- If the user changes brightness manually while dimming is active, leave
  their change alone. Still restore the saved original value when dimming stops.
- The slider controls only the overlay. Extra dim is either on or off.

### 4.3 Components

```
MainActivity (Compose UI)
   │  start/stop/update intents
   ▼
DimService (foreground service)
   ├── OverlayController   → adds/updates/removes the overlay view via WindowManager
   ├── BrightnessController → lowers/restores system brightness (Extra dim)
   ├── NotificationHelper  → builds the persistent notification and its actions
   └── DimPrefs            → reads/writes level, Extra dim setting, saved brightness
```

- **`DimService`**
  - Foreground service, `foregroundServiceType="specialUse"` (Android 14+),
    with a `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` explaining "screen dimming overlay".
  - Intent actions: `ACTION_START`, `ACTION_STOP`, `ACTION_SET_LEVEL(level)`,
    `ACTION_STEP(+/-10)`.
  - Removes the overlay in `onDestroy()`.
- **State sharing**: the service exposes its current level as a `StateFlow`
  (e.g. through a small singleton repository) so the slider stays in sync
  when the level is changed from the notification.

### 4.4 Permissions

| Permission | Why | How it is granted |
|------------|-----|-------------------|
| `SYSTEM_ALERT_WINDOW` | Draw the overlay over other apps | User enables it in settings via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`; check with `Settings.canDrawOverlays()` |
| `FOREGROUND_SERVICE` | Keep the overlay alive | Normal permission (granted at install) |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14+ FGS type | Normal permission |
| `POST_NOTIFICATIONS` | Show the controls notification (Android 13+) | Runtime prompt on first start; dimming still works if denied |
| `WRITE_SETTINGS` | Extra dim (system brightness reduction) | Requested only when the user turns on Extra dim, via `Settings.ACTION_MANAGE_WRITE_SETTINGS` |

| `INTERNET`, `ACCESS_NETWORK_STATE` | Load the banner ad (§4.6) | Normal permissions, merged in from the ads SDK |
| `com.google.android.gms.permission.AD_ID` | Ad serving by the ads SDK | Normal permission, merged in from the ads SDK |

Network access is used only by the ads SDK. Dimsplay's own code makes no
network requests.

### 4.5 Edge cases

| Case | Behavior |
|------|----------|
| Overlay permission revoked while running | Service catches the failure, stops itself, and the UI shows the permission banner |
| Rotation / screen-size change | Overlay is `MATCH_PARENT`, so it resizes automatically |
| App swiped from recents | Service keeps running; the notification is still shown |
| Service killed by the system | Overlay disappears (safe failure). No automatic restart in v1. Saved system brightness is restored on next launch (§4.2.1) |
| `WRITE_SETTINGS` revoked while Extra dim is on | Overlay keeps working; Extra dim switches off and the original brightness cannot be restored, so show a short notice |
| Slider dragged quickly | Update `alpha` on the existing view; never remove and re-add the overlay per frame |
| Screenshots / screen recording | The overlay will appear in captures. Documented as a known limitation |
| No internet connection | The app works fully; the banner is not shown and takes no space. Failed ad loads are retried with backoff (15 s, doubling up to 5 min) and the banner appears once a load succeeds |

### 4.6 Ads

- One anchored adaptive banner from the Google Mobile Ads Next-Gen SDK
  (`com.google.android.libraries.ads.mobile.sdk`), shown at the bottom of the
  main screen above the navigation bar. No other ad formats.
- The SDK is initialized on a background thread at app start. The banner is
  requested only after initialization completes.
- Ads never block or delay dimming. Every app feature works offline.
- **Data:** Dimsplay itself does not collect, store or send any user data.
  The ads SDK does send device data, such as the advertising ID, IP address
  and device information, to Google to serve ads. The Google Play data safety
  form and a privacy policy must disclose this before a Play release. Serving
  personalized ads to users in the EEA/UK also requires a consent prompt
  (Google's UMP SDK).
- Development uses Google's demo app ID and banner unit ID
  (`res/values/admob.xml`), which only serve test ads. Replace them with
  Dimsplay's own AdMob IDs before release.

---

## 5. Non-functional requirements

- **Responsiveness:** visible change within one frame (≈16 ms) of slider movement.
- **Battery:** no polling or wake locks. Idle CPU use is about zero while dimming.
- **Size:** release APK < 3.5 MB, with R8 minification and resource shrinking on
  (about 3 MB with the ads SDK).
- **Accessibility:** the slider has a content description and announces its
  value. All controls are at least 48 dp touch targets.
- **Theming:** follows system light/dark mode.
- **Localization:** all strings in `strings.xml` (English only for v1).

---

## 6. Project structure

```
Dimsplay/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/io/github/malverma/dimsplay/
        │   ├── MainActivity.kt
        │   ├── ui/DimScreen.kt
        │   ├── service/DimService.kt
        │   ├── service/OverlayController.kt
        │   ├── service/BrightnessController.kt
        │   ├── service/NotificationHelper.kt
        │   └── data/DimPrefs.kt
        └── res/ (strings, icons, themes)
```

Application ID / package: `io.github.malverma.dimsplay`.
Repository: https://github.com/Malverma/Dimsplay
App label: **Dimsplay**. Launcher icon: default placeholder for now.

---

## 7. Build & distribution

- Debug build: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- Release build: `./gradlew assembleRelease`, signed with a local keystore
  configured through `keystore.properties`. The keystore and that file are git-ignored.
- Install: `adb install app-release.apk`, or copy the file to the device and
  allow "Install unknown apps".

---

## 8. Testing & acceptance criteria

### 8.1 Acceptance criteria

- [ ] On first launch without the overlay permission, the banner is shown and the button opens the correct settings page.
- [ ] After granting the permission and turning the switch on, the screen darkens visibly.
- [ ] Moving the slider changes darkness live. 0% shows no visible tint, and 100% is clearly darker than the system minimum.
- [ ] With dimming at 100% on Android 12+, touches still reach the apps underneath.
- [ ] Dimming stays on after pressing Home and opening another app.
- [ ] The notification shows the current level, and Stop / −10% / +10% work. The in-app slider follows these changes.
- [ ] Turning the switch off removes the overlay completely.
- [ ] Reopening the app restores the last slider value and Extra dim setting, and dimming starts off.
- [ ] Turning on Extra dim asks for "Modify system settings". Once granted, starting dimming drops the system brightness to its minimum.
- [ ] Stopping dimming restores the original brightness and auto-brightness mode exactly.
- [ ] Force-stopping the app while Extra dim is active, then reopening it, restores the original brightness.
- [ ] No crash when rotating, when revoking the permission while dimming is on, or when the notification permission is denied.
- [ ] Turning on Extra dim while dimming is off turns dimming on and lowers the system brightness.
- [ ] In portrait, all controls are visible without scrolling, with and without the permission banner.
- [ ] With internet, a banner ad appears at the bottom of the screen.
- [ ] With no internet, the app works fully and no empty space is shown where the ad would be. Turning the connection back on shows the banner without restarting the app.

### 8.2 Test matrix

Run the manual tests on emulators or devices running API 26, 30, 33 and 35
(the last two cover the notification-permission and FGS-type changes).

### 8.3 Automated tests

- Unit tests: slider-to-alpha mapping (including the 0.8 cap), saving/restoring settings, and the save/restore/crash-recovery logic for brightness.
- Compose UI test: slider value label updates, and the switch reflects state.

---

## 9. Future work

- Quick Settings tile to toggle dimming.
- Schedule (start/stop at set times).
- Warm color tint option.
- Restart dimming after reboot (`BOOT_COMPLETED`).
- Google Play release. This would need a review of the overlay and
  special-use foreground service policies.

---

## 10. Decisions

| Question | Decision |
|----------|----------|
| Resume dimming on app launch? | No. Dimming always starts off. |
| Minimum Android version | 8.0 (API 26). |
| System-brightness Extra dim in v1? | Yes (§4.2.1). |
| App name | Dimsplay. |
| Icon | Deferred; use the default placeholder. |
| Application ID | `io.github.malverma.dimsplay` (from the GitHub repo `Malverma/Dimsplay`). |
