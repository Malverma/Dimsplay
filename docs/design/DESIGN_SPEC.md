# Screen Dimmer: Design Spec (for Cursor / Claude)

Mockup: `main-screen.png` (the Penpot board is named "Screen Dimmer – Main", 360×800 dp).
The app is **dark mode only**. Don't add a light theme.

## Prompt to paste into Cursor

> Build a native Android app in Kotlin + Jetpack Compose called "Screen Dimmer" that exactly matches
> `DESIGN_SPEC.md` and `main-screen.png` in this folder. It dims the screen below the system's minimum
> brightness by drawing a black, non-touchable overlay over everything. Dark theme only.
> minSdk 26, targetSdk 35. Output a buildable Gradle project and tell me how to run `./gradlew assembleDebug`.

## Color tokens

| Token     | Hex       | Use                                      |
|-----------|-----------|------------------------------------------|
| bg        | `#0E0F13` | Screen background, switch knob           |
| surface   | `#16181D` | Cards                                    |
| track     | `#262931` | Ring track, slider inactive track        |
| text      | `#F2F3F5` | Primary text                             |
| muted     | `#8A8F98` | Subtitle, captions                       |
| faint     | `#5E636D` | Slider min/max labels                    |
| accent    | `#A78BFA` | Ring progress, slider fill, value, switch|
| white     | `#FFFFFF` | Slider thumb                             |

Font: **Inter** (or the system default sans if you'd rather not bundle a font).

## Layout (dp, top-left origin, screen width 360)

1. **Header**, 24 from the left edge
   - Title "Screen Dimmer": 24sp, SemiBold (600), `text`, top 56
   - Subtitle "Go darker than your phone allows": 14sp, Regular, `muted`, top 92
2. **Progress ring**: centered horizontally, center at y=310
   - Diameter 220 (radius 110), stroke 12, track `track`
   - Progress arc in `accent` with round caps, starting at 12 o'clock and going clockwise, sweep = 360° × (level / 100)
   - Center value "40%": 64sp Bold (700), `text`
   - Caption "DIM LEVEL": 12sp SemiBold, letter spacing 2sp, `muted`, about 8dp below the value
3. **Slider card**: x 24, y 500, 312×132, radius 20, `surface`, padding 20
   - Row: "Dim level" (16sp Medium, `text`) on the left, "40%" (16sp SemiBold, `accent`) on the right
   - Slider 24dp below the row: track height 8, radius 4, inactive `track`, active `accent`
   - Thumb: 28dp white circle, drop shadow (y 2, blur 8, black 50%)
   - "0%" and "90%" labels under the track: 12sp Medium, `faint`
4. **Toggle card**: x 24, y 648, 312×76, radius 20, `surface`, padding 20
   - "Dimmer active" (16sp Medium, `text`), with "Overlay is running" / "Overlay is off" below it (13sp, `muted`)
   - Switch on the right: 52×30, radius 15. When on, the track is `accent` and the 24dp knob is `bg`.
     When off, the track is `track` and the knob is `muted`.

## Behavior

- Slider range **0–90%**, step 1. Capping at 90 stops the user from blacking out the screen completely.
- Overlay alpha = `level / 100`, with black color `#000000`.
- The ring, the card value, and the overlay all update live while the slider is dragged.
- The toggle starts or stops the overlay. Save the level and on/off state (DataStore or SharedPreferences) and restore them on launch.
- The status/navigation bars use `bg` so the app looks edge-to-edge dark.

## Android implementation notes

- Permission: `SYSTEM_ALERT_WINDOW`. If `Settings.canDrawOverlays()` is false, open
  `ACTION_MANAGE_OVERLAY_PERMISSION` for the package when the user turns the toggle on.
- Overlay: a `View` with a black background added through `WindowManager` using
  `TYPE_APPLICATION_OVERLAY`, `MATCH_PARENT` × `MATCH_PARENT`, and flags
  `FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_NO_LIMITS`,
  with `PixelFormat.TRANSLUCENT`. Set the alpha on the view (or `layoutParams.alpha`, which Android 12+ caps at 0.8 for untrusted touches, so prefer view alpha).
  To also cover the status/nav bars, make the overlay taller than the screen or use `layoutInDisplayCutoutMode = SHORT_EDGES`.
- Run the overlay inside a **foreground service** (`FOREGROUND_SERVICE` plus the `specialUse` type on API 34+)
  with an ongoing notification that has a "Turn off" action, so the user can always get out.
- `POST_NOTIFICATIONS` runtime permission on API 33+.
- Optional extra: a Quick Settings tile that toggles the overlay.
