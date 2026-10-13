# 0027. Brand mark, app icon and the launch intro

Status: Accepted
Date: 2026-10-13

## Context

The app had no brand mark, no launcher icon on either platform (Android used the default, iOS had no asset catalog)
and no launch screen (`UILaunchScreen: {}`). The mark, icon and launch sequence were designed and reviewed in the
Figma file "Rasoi Design System" (pages App Icon, Splash and Splash Motion). The chosen direction:

- **Mark:** a Devanagari र with a saffron "tadka" leaf, set in Baloo 2 ExtraBold (SIL OFL).
- **Icon:** a cream र and saffron leaf on a green tile.
- **Launch:** the र zooms in at the centre of a cream screen, the leaf lands on it, then the र slides left to become the
  R of "Rasoi" while "asoı" wipes in and the leaf moves over to dot the ı. The tagline "Aapke ghar ki rasoi" fades in
  at the bottom.

Platform launch screens are static: Android 12+ `SplashScreen` shows a background colour and an icon (240 dp with an
icon background, 160 dp visible); before Android 12 the window background shows; iOS `UILaunchScreen` shows a colour
and one centred image. Design-plan 7.1 also said "no decorative animation".

We considered playing a recording (video or Lottie) instead of drawing the intro in code. Video needs a player per
platform, separate light and dark files and decoding at cold start, and scales poorly. Lottie needs a new library
and a JSON parse at cold start, and draws the same vector paths a Compose `Canvas` does, so it is not smoother.

## Decision

- **Outlines as data.** The र, leaf and "asoı" outlines are exported from Figma as SVG path data into
  `core:designsystem` (`brand/BrandGeometry`, 108-unit adaptive-icon canvas). No font is bundled.
- **Tokens.** `AppColors.brandAccent` (saffron leaf) and `onBrandAccent` (its vein) are new, in light and dark. They
  are decorative and never carry meaning, so they are not in `ContrastTest`. The mark itself uses `primary` on
  `surface`.
- **The intro is code.** `brand/BrandIntroTimeline` is a pure function from elapsed time to a frame (unit-tested).
  `BrandIntro(elapsedMillis)` draws one frame and is in the catalog (launch, zoom, tadka, lockup) with screenshots;
  `AnimatedBrandIntro` plays it once in 1.2 s. Time is read only in the draw and layer phases, so playback redraws
  without recomposing, and the clock starts after the first frame so start-up work does not eat the zoom.
- **Hand-off.** Frame 0 is identical to the platform launch screen: the र alone, 240 dp, centred on the surface
  colour. `RasoiApp` draws the intro over the app (loading continues underneath) and fades it out. It plays once per
  launch (`rememberSaveable`, so not again on rotation). On Android 12+ `MainActivity` removes the system splash
  without its fade, which would otherwise cover the zoom. iOS always cross-fades the launch screen into the app, so
  `MainViewController` holds frame 0 for 400 ms (`introStartDelayMillis`) before the zoom starts.
- **Android** (`androidApp/src/main/res`): adaptive icon (`mipmap-anydpi-v26/ic_launcher`) with a green background,
  vector foreground inside the 66 dp safe zone and a monochrome layer for themed icons; `Theme.Rasoi` with
  `windowSplashScreen*` attributes on API 31+ and a `launch_screen` layer-list window background before that. No
  `core-splashscreen` dependency is needed. Colours are copied from the tokens with a comment pointing here.
- **iOS** (`iosApp/iosApp/Assets.xcassets`): a single 1024 px `AppIcon` without alpha, a `LaunchBackground` colour
  and a `LaunchMark` vector image (light and dark) used by `UILaunchScreen`. iOS reads SVG fills as Display P3, so the
  SVGs hold the P3 equivalents of `primary` (`#3E6A43` light, `#A1D2A6` dark); they render as the sRGB token, and the
  mark does not change colour when the intro takes over.
- **Copy.** The tagline is Hinglish by choice; it lives in `strings.xml` (`ds_brand_tagline`) like any other string.
  Design-plan 7.3 rule 8 (plain English) applies to UI copy, not the brand line.

## Consequences

- One brand animation exists, as a documented exception to "no decorative animation". Other screens keep the rule.
- Changing the mark means re-exporting outlines from Figma into `BrandGeometry`, the Android vectors and the iOS
  catalog, and re-rendering `AppIcon-1024.png`.
- The Android resource colours and the iOS colour set duplicate token values; a token change must update them too.
- With animations turned off in system settings, the Compose animation finishes at once and the intro is skipped.
- Revisit if the intro shows jank on low-end devices (measure with `dumpsys gfxinfo`); a baseline profile is the next
  lever, not a recording.
