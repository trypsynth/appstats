package fyi.quin.appstats.model

data class ApkFacts(
	val detections: List<Detection> = emptyList(),
	val abis: List<String> = emptyList(),
	val nativeLibraries: List<String> = emptyList(),
	val libraries: Map<String, String> = emptyMap(),
	val agpVersion: String? = null,
	val kotlinVersion: String? = null,
	val gradleVersion: String? = null,
	val dexCount: Int = 0,
	val hasBaselineProfile: Boolean = false,
	val usesKotlin: Boolean = false,
	val readable: Boolean = false,
)

data class AppRecord(
	val packageName: String,
	val label: String,
	val versionName: String,
	val versionCode: Long,
	val minSdk: Int,
	val targetSdk: Int,
	val firstInstall: Long,
	val lastUpdate: Long,
	val installer: String,
	val isSystem: Boolean,
	val isDebuggable: Boolean,
	val apkBytes: Long,
	val splitCount: Int,
	val permissions: List<String>,
	val activities: Int,
	val services: Int,
	val receivers: Int,
	val providers: Int,
	val certSha256: String,
	val facts: ApkFacts?,
) {
	val headline: Headline
		get() {
			val f = facts ?: return Headline("Checking", null)
			if (!f.readable) return Headline("APK unreadable", null)
			f.detections.filter { it.toolkit.primary }.minByOrNull { it.confidence.ordinal }
				?.let { return Headline(it.toolkit.label, it.confidence) }
			f.detections.firstOrNull { it.toolkit == Toolkit.COMPOSE }
				?.let { return Headline(it.toolkit.label, it.confidence) }
			f.detections.firstOrNull { it.toolkit == Toolkit.VIEWS }
				?.let { return Headline(it.toolkit.label, it.confidence) }
			return Headline("Android native", null)
		}
}
