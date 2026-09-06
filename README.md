# appstats

A TalkBack first app information viewer for Android. It tells you what UI toolkit each app on your phone is built with, and the rest of what the system knows about it.

Built with Kotlin and Jetpack Compose. Gradle only, no Android Studio needed.

## What it does

Every app is one item in a list. Tap it and it opens in place; tap it again and it closes. Each open app tells you:

- The UI toolkit it was built with, its version where the APK reveals it, how sure we are, and the exact evidence.
- The Android Gradle Plugin, Kotlin, and Gradle versions that built it.
- Target SDK, minimum SDK, and the native code ABIs.
- How many dex files it has, and whether it ships a baseline profile.
- Install date, update date, and what installed it.
- APK size, number of APK files, and version code.
- System app or user app. Debuggable or not.
- Package name, activity and service and receiver and provider counts, and the signing certificate.
- Every permission it asks for, in Android's own words.
- Every native library it links against.
- Every bundled library it declares a version for, with that version. Often over a hundred of them.

You can copy any single line, as well as open the app or jump to its system settings page.

Search by name or package name. Filter by home screen apps, user apps, system apps, or every package on the device. Sort by name, toolkit, size, or update date, in either direction. When you sort by size or date, the value you sorted on appears in each row.

## Toolkits it detects

appstats reads each APK as a ZIP file and looks for marker files, then asks Android for the app's resources and looks for names that only one toolkit ships.

| Toolkit | Marker |
|---|---|
| Flutter | `lib/*/libflutter.so`, `assets/flutter_assets/` |
| React Native | `lib/*/libreactnativejni.so`, `lib/*/libhermes.so`, `assets/index.android.bundle` |
| Jetpack Compose | resource `m3c_dialog` or `close_sheet`, `lib/*/libandroidx.graphics.path.so`, `META-INF/androidx.compose.ui_ui.version` |
| Android Views | resource `abc_action_mode_done`, `META-INF/androidx.appcompat_appcompat.version` |
| Compose Multiplatform | `lib/*/libskiko*.so` |
| Unity | `lib/*/libunity.so`, `assets/bin/Data/` |
| Unreal | `lib/*/libUnreal.so`, `lib/*/libUE4.so` |
| Godot | `lib/*/libgodot_android.so` |
| Defold | `lib/*/libdmengine.so` |
| Cocos2d-x | `lib/*/libcocos2djs.so`, `lib/*/libcocos2dcpp.so` |
| libGDX | `lib/*/libgdx.so` |
| Solar2D | `lib/*/libcorona.so` |
| Love2D | `lib/*/liblove.so` |
| SDL | `lib/*/libSDL2.so`, `lib/*/libSDL3.so` |
| .NET MAUI | `assemblies/Microsoft.Maui.*` |
| Xamarin.Forms | `assemblies/Xamarin.Forms.*` |
| Avalonia | `assemblies/Avalonia.*` |
| .NET for Android | `lib/*/libmonodroid.so` |
| Qt | `lib/*/libQt*.so` |
| Capacitor | `capacitor.config.json`, `assets/public/` |
| Cordova | `assets/www/cordova.js` |
| NativeScript | `assets/app/package.json` |

An app can match several rows, and appstats shows them all rather than picking one and hiding the rest. Most apps are mixed: on a normal phone, over half ship both Compose and Views. Each match keeps the evidence that caused it, so when a guess looks wrong you can see exactly why it was made.

Confidence is one of three levels. Certain comes from a linked native library or a resource name. Likely comes from an asset folder. Possible comes from a version stamp that a release build can strip.

Resource names are the strongest signal, because R8 renames code but never renames resources. That is what lets appstats tell Compose from Views in a minified release build, where no file marker survives.

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
