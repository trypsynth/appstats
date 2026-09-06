package fyi.quin.appstats.data

import fyi.quin.appstats.model.ApkFacts
import fyi.quin.appstats.model.Confidence
import fyi.quin.appstats.model.Detection
import fyi.quin.appstats.model.Toolkit
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

private const val AGP_METADATA = "META-INF/com/android/build/gradle/app-metadata.properties"
private const val KOTLIN_METADATA = "kotlin-tooling-metadata.json"
private const val BASELINE_PROFILE = "assets/dexopt/baseline.prof"

private data class Rule(val toolkit: Toolkit, val confidence: Confidence)

private val exactNames = mapOf(
	"assets/index.android.bundle" to Rule(Toolkit.REACT_NATIVE, Confidence.LIKELY),
	"assets/www/cordova.js" to Rule(Toolkit.CORDOVA, Confidence.LIKELY),
	"capacitor.config.json" to Rule(Toolkit.CAPACITOR, Confidence.LIKELY),
	"assets/capacitor.config.json" to Rule(Toolkit.CAPACITOR, Confidence.LIKELY),
	"assets/app/package.json" to Rule(Toolkit.NATIVESCRIPT, Confidence.LIKELY),
	"META-INF/androidx.compose.ui_ui.version" to Rule(Toolkit.COMPOSE, Confidence.POSSIBLE),
	"META-INF/androidx.appcompat_appcompat.version" to Rule(Toolkit.VIEWS, Confidence.POSSIBLE),
)

private val nativeLibs = mapOf(
	"libflutter.so" to Rule(Toolkit.FLUTTER, Confidence.CERTAIN),
	"libreactnativejni.so" to Rule(Toolkit.REACT_NATIVE, Confidence.CERTAIN),
	"libreactnative.so" to Rule(Toolkit.REACT_NATIVE, Confidence.CERTAIN),
	"libhermes.so" to Rule(Toolkit.REACT_NATIVE, Confidence.LIKELY),
	"libunity.so" to Rule(Toolkit.UNITY, Confidence.CERTAIN),
	"libunreal.so" to Rule(Toolkit.UNREAL, Confidence.CERTAIN),
	"libue4.so" to Rule(Toolkit.UNREAL, Confidence.CERTAIN),
	"libgodot_android.so" to Rule(Toolkit.GODOT, Confidence.CERTAIN),
	"libdmengine.so" to Rule(Toolkit.DEFOLD, Confidence.CERTAIN),
	"libcocos2djs.so" to Rule(Toolkit.COCOS2D, Confidence.CERTAIN),
	"libcocos2dcpp.so" to Rule(Toolkit.COCOS2D, Confidence.CERTAIN),
	"libgdx.so" to Rule(Toolkit.LIBGDX, Confidence.CERTAIN),
	"libcorona.so" to Rule(Toolkit.SOLAR2D, Confidence.CERTAIN),
	"liblove.so" to Rule(Toolkit.LOVE2D, Confidence.CERTAIN),
	"libsdl2.so" to Rule(Toolkit.SDL, Confidence.CERTAIN),
	"libsdl3.so" to Rule(Toolkit.SDL, Confidence.CERTAIN),
	"libmonodroid.so" to Rule(Toolkit.DOTNET, Confidence.LIKELY),
	"libandroidx.graphics.path.so" to Rule(Toolkit.COMPOSE, Confidence.LIKELY),
)

private val libPrefixes = listOf(
	"libqt" to Rule(Toolkit.QT, Confidence.CERTAIN),
	"libskiko" to Rule(Toolkit.COMPOSE_MULTIPLATFORM, Confidence.CERTAIN),
)

private val prefixes = listOf(
	"assemblies/microsoft.maui." to Rule(Toolkit.MAUI, Confidence.CERTAIN),
	"assemblies/xamarin.forms." to Rule(Toolkit.XAMARIN, Confidence.CERTAIN),
	"assemblies/avalonia." to Rule(Toolkit.AVALONIA, Confidence.CERTAIN),
	"assets/flutter_assets/" to Rule(Toolkit.FLUTTER, Confidence.LIKELY),
	"assets/bin/data/" to Rule(Toolkit.UNITY, Confidence.LIKELY),
	"assets/public/" to Rule(Toolkit.CAPACITOR, Confidence.POSSIBLE),
)

private val versionPattern = Regex("^[0-9][0-9A-Za-z.\\-+_]{0,63}$")

private val toolkitArtifacts = mapOf(
	Toolkit.COMPOSE to listOf(
		"androidx.compose.ui:ui",
		"androidx.compose.runtime:runtime",
		"androidx.compose.foundation:foundation",
	),
	Toolkit.VIEWS to listOf("androidx.appcompat:appcompat"),
)

private class Scan {
	val hits = LinkedHashMap<Toolkit, Detection>()
	val abis = LinkedHashSet<String>()
	val libs = LinkedHashSet<String>()
	val libraries = sortedMapOf<String, String>()
	var agp: String? = null
	var kotlin: String? = null
	var gradle: String? = null
	var dexCount = 0
	var baselineProfile = false
	var kotlinCode = false
	var readable = false
}

object ApkInspector {
	fun inspect(paths: List<String>): ApkFacts {
		val scan = Scan()
		for (path in paths) {
			val file = File(path)
			if (!file.canRead()) continue
			try {
				ZipFile(file).use { zip ->
					scan.readable = true
					val entries = zip.entries()
					while (entries.hasMoreElements()) {
						visit(zip, entries.nextElement(), scan)
					}
				}
			} catch (_: Exception) {
				continue
			}
		}
		return ApkFacts(
			detections = rank(scan),
			abis = scan.abis.sorted(),
			nativeLibraries = scan.libs.sorted(),
			libraries = scan.libraries,
			agpVersion = scan.agp,
			kotlinVersion = scan.kotlin,
			gradleVersion = scan.gradle,
			dexCount = scan.dexCount,
			hasBaselineProfile = scan.baselineProfile,
			usesKotlin = scan.kotlinCode || scan.kotlin != null,
			readable = scan.readable,
		)
	}

	fun withExtra(facts: ApkFacts, extra: List<Detection>): ApkFacts {
		if (extra.isEmpty()) return facts
		val merged = LinkedHashMap<Toolkit, Detection>()
		facts.detections.forEach { merged[it.toolkit] = it }
		extra.forEach { candidate ->
			val existing = merged[candidate.toolkit]
			if (existing == null || candidate.confidence.ordinal < existing.confidence.ordinal) {
				merged[candidate.toolkit] = candidate.copy(
					version = candidate.version ?: versionOf(facts.libraries, candidate.toolkit),
				)
			}
		}
		return facts.copy(detections = sorted(merged.values))
	}

	private fun visit(zip: ZipFile, entry: ZipEntry, scan: Scan) {
		val name = entry.name
		exactNames[name]?.let { record(scan, it, name) }
		if (name.endsWith(".dex")) scan.dexCount++
		if (name.startsWith("lib/")) {
			nativeEntry(name, scan)
			return
		}
		when {
			name == BASELINE_PROFILE -> scan.baselineProfile = true
			name.endsWith(".kotlin_module") -> scan.kotlinCode = true
			name.startsWith("META-INF/services/kotlinx.") -> scan.kotlinCode = true
			name == AGP_METADATA -> scan.agp = readProperty(zip, entry, "androidGradlePluginVersion")
			name == KOTLIN_METADATA -> readKotlinMetadata(zip, entry, scan)
			name.startsWith("META-INF/") && name.endsWith(".version") -> readLibrary(zip, entry, scan)
		}
		val lower = name.lowercase()
		for ((prefix, rule) in prefixes) {
			if (lower.startsWith(prefix)) {
				record(scan, rule, name)
				return
			}
		}
	}

	private fun nativeEntry(name: String, scan: Scan) {
		val rest = name.substring(4)
		val slash = rest.indexOf('/')
		if (slash <= 0) return
		scan.abis.add(rest.substring(0, slash))
		val lib = rest.substring(slash + 1)
		if (!lib.endsWith(".so")) return
		scan.libs.add(lib)
		val lower = lib.lowercase()
		nativeLibs[lower]?.let { record(scan, it, name) }
		libPrefixes.firstOrNull { lower.startsWith(it.first) }?.let { record(scan, it.second, name) }
	}

	private fun readLibrary(zip: ZipFile, entry: ZipEntry, scan: Scan) {
		if (entry.size > 128) return
		val value = readText(zip, entry) ?: return
		if (!versionPattern.matches(value)) return
		val key = entry.name.removePrefix("META-INF/").removeSuffix(".version")
		val split = key.indexOf('_')
		val coordinate = if (split <= 0) key else key.substring(0, split) + ":" + key.substring(split + 1)
		scan.libraries[coordinate] = value
	}

	private fun readKotlinMetadata(zip: ZipFile, entry: ZipEntry, scan: Scan) {
		val text = readText(zip, entry) ?: return
		scan.kotlin = jsonString(text, "buildPluginVersion")
		scan.gradle = jsonString(text, "buildSystemVersion")
	}

	private fun jsonString(text: String, key: String): String? {
		val pattern = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"([^\"]*)\"")
		return pattern.find(text)?.groupValues?.get(1)?.trim()?.ifBlank { null }
	}

	private fun readProperty(zip: ZipFile, entry: ZipEntry, key: String): String? {
		val text = readText(zip, entry) ?: return null
		return text.lineSequence()
			.firstOrNull { it.startsWith("$key=") }
			?.substringAfter('=')
			?.trim()
			?.ifBlank { null }
	}

	private fun readText(zip: ZipFile, entry: ZipEntry): String? {
		if (entry.size > 65536) return null
		return try {
			zip.getInputStream(entry).bufferedReader().use { it.readText().trim() }
		} catch (_: Exception) {
			null
		}
	}

	private fun record(scan: Scan, rule: Rule, evidence: String) {
		val existing = scan.hits[rule.toolkit]
		if (existing != null && existing.confidence.ordinal <= rule.confidence.ordinal) return
		scan.hits[rule.toolkit] = Detection(rule.toolkit, rule.confidence, evidence)
	}

	private fun rank(scan: Scan): List<Detection> {
		val withVersions = scan.hits.values.map {
			it.copy(version = versionOf(scan.libraries, it.toolkit))
		}
		return sorted(withVersions)
	}

	private fun sorted(detections: Collection<Detection>): List<Detection> {
		return detections.sortedWith(
			compareBy({ it.confidence.ordinal }, { !it.toolkit.primary }, { it.toolkit.label })
		)
	}

	private fun versionOf(libraries: Map<String, String>, toolkit: Toolkit): String? {
		return toolkitArtifacts[toolkit]?.firstNotNullOfOrNull { libraries[it] }
	}
}
