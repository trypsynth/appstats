package fyi.quin.appstats

import fyi.quin.appstats.model.ApkFacts
import fyi.quin.appstats.model.AppRecord
import fyi.quin.appstats.model.Confidence
import fyi.quin.appstats.model.Detection
import fyi.quin.appstats.model.Toolkit

private const val DAY = 86_400_000L

internal val flutterApp = AppRecord(
	packageName = "com.example.notes",
	label = "Pocket Notes",
	versionName = "3.14.2",
	versionCode = 31402,
	minSdk = 24,
	targetSdk = 35,
	firstInstall = 1_760_000_000_000L,
	lastUpdate = 1_760_000_000_000L + 30 * DAY,
	installer = "com.android.vending",
	isSystem = false,
	isDebuggable = false,
	apkBytes = 48_234_496,
	splitCount = 2,
	permissions = listOf(
		"android.permission.INTERNET",
		"android.permission.CAMERA",
		"android.permission.RECORD_AUDIO",
	),
	activities = 12,
	services = 4,
	receivers = 3,
	providers = 2,
	certSha256 = "2F19ADEB284EB36F91C4A7550D3E88A1",
	facts = ApkFacts(
		detections = listOf(
			Detection(Toolkit.FLUTTER, Confidence.CERTAIN, "lib/arm64-v8a/libflutter.so"),
			Detection(Toolkit.VIEWS, Confidence.POSSIBLE, "META-INF/androidx.appcompat_appcompat.version"),
		),
		abis = listOf("arm64-v8a", "armeabi-v7a"),
		nativeLibraries = listOf("libapp.so", "libflutter.so", "libsqlite3.so"),
		libraries = sortedMapOf("androidx.appcompat:appcompat" to "1.7.0"),
		agpVersion = "8.11.1",
		kotlinVersion = "2.1.20",
		dexCount = 3,
		hasBaselineProfile = true,
		usesKotlin = true,
		readable = true,
	),
)

internal val nativeApp = AppRecord(
	packageName = "com.android.chrome",
	label = "Chrome",
	versionName = "152.0.7977.75",
	versionCode = 797775,
	minSdk = 26,
	targetSdk = 36,
	firstInstall = 1_700_000_000_000L,
	lastUpdate = 1_770_000_000_000L,
	installer = "com.android.vending",
	isSystem = true,
	isDebuggable = false,
	apkBytes = 312_000_000,
	splitCount = 3,
	permissions = emptyList(),
	activities = 88,
	services = 21,
	receivers = 9,
	providers = 6,
	certSha256 = "AABBCCDDEEFF00112233445566778899",
	facts = ApkFacts(abis = listOf("arm64-v8a"), nativeLibraries = listOf("libchrome.so"), dexCount = 12, readable = true),
)

internal val composeApp = AppRecord(
	packageName = "com.google.android.deskclock",
	label = "Clock",
	versionName = "9.1",
	versionCode = 91,
	minSdk = 31,
	targetSdk = 36,
	firstInstall = 1_690_000_000_000L,
	lastUpdate = 1_768_000_000_000L,
	installer = "not recorded",
	isSystem = true,
	isDebuggable = false,
	apkBytes = 9_800_000,
	splitCount = 0,
	permissions = listOf("android.permission.VIBRATE"),
	activities = 6,
	services = 2,
	receivers = 4,
	providers = 1,
	certSha256 = "1122334455667788990011223344556677",
	facts = ApkFacts(
		detections = listOf(
			Detection(Toolkit.COMPOSE, Confidence.LIKELY, "lib/arm64-v8a/libandroidx.graphics.path.so"),
		),
		abis = listOf("arm64-v8a"),
		nativeLibraries = listOf("libandroidx.graphics.path.so"),
		libraries = sortedMapOf("androidx.compose.ui:ui" to "1.8.3"),
		readable = true,
	),
)
