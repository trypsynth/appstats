package fyi.quin.appstats.ui

enum class Filter(val label: String, val one: String, val many: String) {
	LAUNCHER("Home screen apps", "home screen app", "home screen apps"),
	USER("User apps", "user app", "user apps"),
	SYSTEM("System apps", "system app", "system apps"),
	ALL("All packages", "package", "packages"),
}
