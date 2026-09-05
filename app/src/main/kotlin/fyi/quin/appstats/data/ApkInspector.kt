package fyi.quin.appstats.data

import fyi.quin.appstats.model.ApkFacts
import fyi.quin.appstats.model.Confidence
import fyi.quin.appstats.model.Detection
import fyi.quin.appstats.model.Toolkit
import java.io.File
import java.util.zip.ZipFile

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
	"libmonodroid.so" to Rule(Toolkit.MAUI, Confidence.CERTAIN),
	"libgodot_android.so" to Rule(Toolkit.GODOT, Confidence.CERTAIN),
	"libandroidx.graphics.path.so" to Rule(Toolkit.COMPOSE, Confidence.LIKELY),
)

private val prefixes = listOf(
	"assets/flutter_assets/" to Rule(Toolkit.FLUTTER, Confidence.LIKELY),
	"assets/bin/data/" to Rule(Toolkit.UNITY, Confidence.LIKELY),
	"assemblies/" to Rule(Toolkit.MAUI, Confidence.LIKELY),
	"assets/public/" to Rule(Toolkit.CAPACITOR, Confidence.POSSIBLE),
	"res/layout/" to Rule(Toolkit.VIEWS, Confidence.POSSIBLE),
)

object ApkInspector {
	fun inspect(paths: List<String>): ApkFacts {
		val hits = LinkedHashMap<Toolkit, Detection>()
		val abis = LinkedHashSet<String>()
		val libs = LinkedHashSet<String>()
		var readable = false
		for (path in paths) {
			val file = File(path)
			if (!file.canRead()) continue
			try {
				ZipFile(file).use { zip ->
					readable = true
					val names = zip.entries()
					while (names.hasMoreElements()) {
						scan(names.nextElement().name, hits, abis, libs)
					}
				}
			} catch (_: Exception) {
				continue
			}
		}
		return ApkFacts(
			detections = hits.values.sortedWith(
				compareBy({ it.confidence.ordinal }, { !it.toolkit.primary }, { it.toolkit.label })
			),
			abis = abis.sorted(),
			nativeLibraries = libs.sorted(),
			readable = readable,
		)
	}

	private fun scan(
		name: String,
		hits: MutableMap<Toolkit, Detection>,
		abis: MutableSet<String>,
		libs: MutableSet<String>,
	) {
		exactNames[name]?.let { record(hits, it, name) }
		if (name.startsWith("lib/")) {
			val rest = name.substring(4)
			val slash = rest.indexOf('/')
			if (slash > 0) {
				abis.add(rest.substring(0, slash))
				val lib = rest.substring(slash + 1)
				libs.add(lib)
				val lower = lib.lowercase()
				nativeLibs[lower]?.let { record(hits, it, name) }
				if (lower.startsWith("libqt")) {
					record(hits, Rule(Toolkit.QT, Confidence.CERTAIN), name)
				}
			}
			return
		}
		val lower = name.lowercase()
		for ((prefix, rule) in prefixes) {
			if (lower.startsWith(prefix)) {
				record(hits, rule, name)
				return
			}
		}
	}

	private fun record(hits: MutableMap<Toolkit, Detection>, rule: Rule, evidence: String) {
		val existing = hits[rule.toolkit]
		if (existing != null && existing.confidence.ordinal <= rule.confidence.ordinal) return
		hits[rule.toolkit] = Detection(rule.toolkit, rule.confidence, evidence)
	}
}
