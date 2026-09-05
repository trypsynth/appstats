package fyi.quin.appstats.model

enum class Confidence(val label: String) {
	CERTAIN("certain"),
	LIKELY("likely"),
	POSSIBLE("possible"),
}

enum class Toolkit(val label: String, val primary: Boolean) {
	FLUTTER("Flutter", true),
	REACT_NATIVE("React Native", true),
	UNITY("Unity", true),
	UNREAL("Unreal Engine", true),
	MAUI(".NET MAUI", true),
	GODOT("Godot", true),
	QT("Qt", true),
	CAPACITOR("Capacitor", true),
	CORDOVA("Cordova", true),
	NATIVESCRIPT("NativeScript", true),
	COMPOSE("Jetpack Compose", false),
	VIEWS("Android Views", false),
}

data class Headline(
	val label: String,
	val confidence: Confidence?,
)

data class Detection(
	val toolkit: Toolkit,
	val confidence: Confidence,
	val evidence: String,
)
