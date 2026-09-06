package fyi.quin.appstats.model

enum class Confidence(val label: String) {
	CERTAIN("certain"),
	LIKELY("likely"),
	POSSIBLE("possible"),
}

enum class Toolkit(val label: String, val primary: Boolean) {
	FLUTTER("Flutter", true),
	REACT_NATIVE("React Native", true),
	COMPOSE_MULTIPLATFORM("Compose Multiplatform", true),
	UNITY("Unity", true),
	UNREAL("Unreal Engine", true),
	GODOT("Godot", true),
	DEFOLD("Defold", true),
	COCOS2D("Cocos2d-x", true),
	LIBGDX("libGDX", true),
	SOLAR2D("Solar2D", true),
	LOVE2D("Love2D", true),
	SDL("SDL", true),
	MAUI(".NET MAUI", true),
	XAMARIN("Xamarin.Forms", true),
	AVALONIA("Avalonia", true),
	DOTNET(".NET for Android", true),
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
	val version: String? = null,
) {
	val label: String
		get() = if (version == null) toolkit.label else "${toolkit.label} $version"
}
