# Walkers Welfare Club · Chengalpattu

Member registration app for the 2026–27 season: name, father's name, address, cell number, date of birth, date of marriage, mail ID, blood group, T-shirt and lower sizes, and a photo (up to 10 MB). Members get an illustrated ID card; organisers get a dashboard with kit-order counts, blood groups and CSV export.

## Install on Android

Download [`android/WalkersClub.apk`](android/WalkersClub.apk), open it on the phone and allow installs from that source when Android asks. Registrations are stored on the phone.

## Layout

- `index.html` — the whole app (one file). It runs as a claude.ai artifact and, inside the APK, in a WebView with the `WWCNative` bridge.
- `android/` — the Android wrapper: `MainActivity.java` (WebView, camera, gallery, on-phone storage, share sheet), `FilesProvider.java`, manifest, icon generator and build scripts.

## Build the APK

Needs the Android SDK (build-tools 36.1.0, platform android-35) and Android Studio's bundled JDK. No Gradle.

```bash
bash android/build.sh
```

`build.sh` bundles `index.html` with its fonts and the Motion library into `android/assets/www`, compiles, packages and signs `android/WalkersClub.apk`. The first build creates `android/walkersclub.keystore`; keep it private and reuse it, because updates must be signed with the same key.
