package fyi.quin.appstats.model

data class ApkFacts(
	val detections: List<Detection>,
	val abis: List<String>,
	val nativeLibraries: List<String>,
	val readable: Boolean,
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
