# MusicU

A native Android music player starter with an Android 12 / Material You-inspired interface.

## Build with GitHub Actions
1. Create a new GitHub repository.
2. Upload the contents of this ZIP to the repository root.
3. Open the **Actions** tab and run **Build MusicU APK** (or push a commit).
4. Download `MusicU-debug-apk` from the completed workflow's Artifacts section.

The output is a debug APK suitable for testing and sideloading.

## Build locally
Requires JDK 17 and Android SDK Platform 35:
```bash
gradle assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Notes
- Minimum Android version: 8.0 (API 26).
- The app requests permission to read local audio files.
- This starter plays local audio while the app is active. Background playback, media notification controls, playlists, artwork extraction, and persistent favourites are not included yet.
