# Orientation Wallpaper

A small Android live wallpaper. You pick one image for portrait and one for landscape, and it switches when the device rotates. The same image can be used for both.

[MIT License](LICENSE). Copyright (c) 2026 J T.

There are no Google Play Services, Firebase, or Play Billing dependencies. Images are copied into app-private storage and are not uploaded.

## Use

1. Install the app (Obtainium, or a release APK).
2. Choose a portrait image and a landscape image. **Use one image for both** copies a single pick into both slots.
3. Tap **Set wallpaper**. Android opens the system live-wallpaper confirm screen for this app.

The copies stay in the app, so deleting the originals does not clear the wallpaper. PNG, JPEG, WebP, AVIF, and HEIC/HEIF are accepted. AVIF and HEIC only decode on Android versions that include those decoders. The bitmap is scaled down to about the screen size (long edge capped at 4096px) and drawn center-cropped, so it fills the display without stretching.

Changing the images reloads a wallpaper that is already set. The service is direct-boot aware and reads device-protected storage, so the images are available on the lock screen.

Phones and large screens (including a Pixel Tablet) use the wallpaper surface size. Wider than tall, or square, shows the landscape image. Taller shows the portrait image. The picker activity is not locked to an orientation and lays the two previews side by side when the window is wide.

## Build

JDK 21. Android SDK platform `android-37.2` and build-tools `37.0.0`.

```sh
./gradlew assembleRelease test lint assertNoGooglePlayServices
```

`targetSdk` is 37 (Android 17, the current stable platform). `compileSdk` is 37.2 because the current stable Jetpack Compose release requires 37.1 or newer. `minSdk` is 26.

`versionName` defaults to `1.0.0`. Pass `-PversionName=1.2.3` (a leading `v` is stripped) to override it. `versionCode` is `major * 1_000_000 + minor * 1_000 + patch`, so it increases for every normal semver bump while minor and patch stay below 1000. Play's ceiling is 2100000000.

Without the signing secrets below, `assembleRelease` signs with the debug keystore so the project still builds. Do not publish that APK. Updates will not replace a copy signed with the release key.

## Signing secrets

Create one PKCS12 keystore and keep it. If it is lost, installed copies cannot be updated in place; users would have to uninstall first.

```sh
keytool -genkeypair -v \
  -keystore release.keystore \
  -storetype PKCS12 \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -alias orientation-wallpaper
```

Base64-encode it for GitHub Actions. Gradle accepts wrapped or single-line Base64.

```sh
# Linux
base64 -w 0 release.keystore

# macOS
base64 < release.keystore | tr -d '\n'
```

Repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | Base64 of the PKCS12 keystore |
| `SIGNING_KEYSTORE_PASSWORD` | Store password |
| `SIGNING_KEY_ALIAS` | Key alias (`orientation-wallpaper` in the command above) |
| `SIGNING_KEY_PASSWORD` | Key password |

Do not commit `release.keystore`. Store a copy somewhere you control, along with the passwords.

`SIGNING_KEYSTORE_PATH` may be set instead of `SIGNING_KEYSTORE_BASE64` for a local release build. If any signing variable is set, the rest of the password and alias variables are required.

## Releases and Obtainium

Pushing a tag `v1.0.0`, `v1.0.1`, `v1.1.0`, … runs [`.github/workflows/release.yml`](.github/workflows/release.yml). The tag must be `v` plus `MAJOR.MINOR.PATCH`. The workflow checks the four secrets, builds a signed release APK whose `versionName` matches the tag, and attaches `OrientationWallpaper-<version>.apk` plus `SHA256SUMS` to the GitHub Release.

[Obtainium](https://github.com/ImranR98/Obtainium):

1. Add `https://github.com/jeremytieman/orientation-wallpaper`.
2. Set the APK filter regex to `OrientationWallpaper-.*\.apk` so Obtainium ignores `SHA256SUMS` and GitHub's source archives.
3. Leave prereleases off. Tags of the form above are stable releases.

The signing certificate has to stay the same across those releases or Android will reject the update.

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on pull requests and branch pushes: assemble (debug and release), unit tests, Android lint, and a check that the release classpath does not contain Play services, Firebase, or Play libraries.

## Layout

`:core` is plain Kotlin: orientation choice, center-crop and sample-size math, EXIF orientation mapping, image-header sniffing, versionCode, and the on-disk image store. `:app` is Jetpack Compose plus a `WallpaperService`.

`OrientationRenderer` is the per-orientation draw seam. `StaticImageRenderer` is the still-image implementation. Either slot can later be replaced with an animated renderer without changing how the engine picks one from the surface size.

Images are chosen with the Storage Access Framework (`ACTION_OPEN_DOCUMENT`), not the photo picker. The backported photo picker can depend on Play services; this app does not. Picked bytes are copied into device-protected storage before the source URI goes away.

API 28+ decodes with `ImageDecoder` (it applies EXIF orientation). API 26–27, and any file `ImageDecoder` refuses, use `BitmapFactory` and `ExifInterface`.
