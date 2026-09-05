# appstats

A TalkBack first app information viewer for Android. It tells you what UI toolkit each app on your phone is built with, and the rest of what the system knows about it.

Built with Kotlin and Jetpack Compose. Gradle only, no Android Studio needed.

## What it does

Every app is one item in a list. Tap it and it opens in place; tap it again and it closes. Each open app tells you:

- The UI toolkit it was built with, how sure we are, and the file that proved it.
- Target SDK, minimum SDK, and the native code ABIs.
- Install date, update date, and what installed it.
- APK size, number of APK files, and version code.
- System app or user app. Debuggable or not.
- Package name, activity and service and receiver and provider counts, and the signing certificate.
- Every permission it asks for, in Android's own words.
- Every native library it links against.

You can copy any single line, as well as open the app or jump to its system settings page.

Search by name or package name. Filter by home screen apps, user apps, system apps, or every package on the device. Sort by name, toolkit, size, or update date, in either direction. When you sort by size or date, the value you sorted on appears in each row.

## Toolkits it detects

appstats opens each APK as a ZIP file and looks for marker files.

| Toolkit | Marker |
|---|---|
| Flutter | `lib/*/libflutter.so`, `assets/flutter_assets/` |
| React Native | `lib/*/libreactnativejni.so`, `lib/*/libhermes.so`, `assets/index.android.bundle` |
| Jetpack Compose | `lib/*/libandroidx.graphics.path.so`, `META-INF/androidx.compose.ui_ui.version` |
| Android Views | `META-INF/androidx.appcompat_appcompat.version`, `res/layout/` |
| Unity | `lib/*/libunity.so`, `assets/bin/Data/` |
| Unreal | `lib/*/libUnreal.so`, `lib/*/libUE4.so` |
| .NET MAUI | `lib/*/libmonodroid.so`, `assemblies/` |
| Capacitor | `capacitor.config.json`, `assets/public/` |
| Cordova | `assets/www/cordova.js` |
| Godot | `lib/*/libgodot_android.so` |
| Qt | `lib/*/libQt*.so` |
| NativeScript | `assets/app/package.json` |

An app can match several rows, and appstats shows them all rather than picking one and hiding the rest. A Flutter app still ships AppCompat. Each match keeps the file that caused it, so when a guess looks wrong you can see exactly why it was made.

Confidence is one of three levels. Certain comes from a linked native library. Likely comes from an asset folder. Possible comes from a version stamp that a release build can strip.

Some apps cannot be told apart. A minified release build often keeps no marker that separates Compose from Views, and those apps read as "Android native" instead of a guess.

## Building

You need JDK 17 and an Android SDK with platform 36 and build tools 36. Put your SDK path in `local.properties`:

```
sdk.dir=C:/android-sdk
```

Then:

```
./gradlew assembleDebug        # build
./gradlew installDebug         # build and install on the connected device
```

For a phone on wireless debugging:

```
adb mdns services
adb connect <ip>:<port>
export ANDROID_SERIAL=<the mdns name from that list>
```

To build a signed release APK, put your signing details in `~/.gradle/gradle.properties`, outside the repository, so they can never be committed:

```
appstats.storeFile=/path/to/your.jks
appstats.storePassword=...
appstats.keyAlias=...
appstats.keyPassword=...
```

Then `./gradlew assembleRelease`. Without those properties the release build still works, and produces an unsigned APK.

## Tests

```
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

## Licence

MIT. See [LICENSE](LICENSE).

Copyright (c) 2026 Quin Gillespie.
