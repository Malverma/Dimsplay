# Dimsplay

Android app that dims the screen below the system's minimum brightness by drawing a
translucent black overlay over every app. See [SPEC.md](SPEC.md) for behavior and
[docs/design](docs/design) for the UI mockups.

## Build

Requires JDK 17 and the Android SDK (platform 35). Point Gradle at the SDK with
`ANDROID_HOME` or a `local.properties` file containing `sdk.dir=/path/to/sdk`.

```sh
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # JVM unit tests
./gradlew connectedDebugAndroidTest  # Compose UI tests, needs a device or emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Release

Create `keystore.properties` in the project root (it is git-ignored):

```properties
storeFile=dimsplay.jks
storePassword=...
keyAlias=dimsplay
keyPassword=...
```

Then run `./gradlew assembleRelease`; the signed APK is
`app/build/outputs/apk/release/app-release.apk`. Without that file the release APK is built unsigned.

Keep the keystore and its passwords backed up somewhere safe. Updates to an installed app
must be signed with the same key, so losing it means users have to uninstall and reinstall.

## Install on a phone

1. Copy `app-release.apk` to the phone, or run `adb install app-release.apk`.
2. Allow your file manager or browser to "Install unknown apps" when Android asks.
3. Open Dimsplay, tap **Grant permission**, and enable "Display over other apps" for it.

## Known limitations

- The overlay shows up in screenshots and screen recordings.
- Dimming is capped at 80% overlay opacity, because Android 12+ blocks touches under
  darker overlays.
- Dimming does not resume after a reboot.
