# Footbolive Android App

Android WebView wrapper for https://deeprows.github.io/Footbolive/index.html

Features:
- D logo loading screen
- WebView with JavaScript/DOM storage
- New-window redirects handled in an in-app popup with an X button
- HTML5 fullscreen handled through WebChromeClient
- Landscape orientation during fullscreen
- GitHub Actions cloud APK build

## Build without Android Studio

Push this repository to GitHub. Then open **Actions → Build APK → Run workflow**.

The workflow uploads `app-debug.apk` as an artifact.
