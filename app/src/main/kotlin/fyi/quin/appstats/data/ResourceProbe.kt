package fyi.quin.appstats.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import fyi.quin.appstats.model.Confidence
import fyi.quin.appstats.model.Detection
import fyi.quin.appstats.model.Toolkit

private val composeNames = listOf(
	"m3c_dialog",
	"m3c_bottom_sheet_pane_title",
	"m3c_date_picker_title",
	"m3c_search_bar_search",
	"m3c_snackbar_dismiss",
	"m3c_tooltip_pane_description",
	"close_sheet",
	"dropdown_menu",
	"navigation_menu",
	"range_start",
	"range_end",
	"switch_role",
	"template_percent",
	"default_error_message",
	"in_progress",
)

private val viewsNames = listOf(
	"abc_action_mode_done",
	"abc_action_bar_home_description",
	"abc_searchview_description_search",
	"abc_toolbar_collapse_description",
)

/**
 * Resource names survive R8, because AGP does not obfuscate them. That makes this the only
 * marker that still separates Compose from Views in a minified release build.
 */
object ResourceProbe {
	fun probe(pm: PackageManager, app: ApplicationInfo): List<Detection> {
		val resources = try {
			pm.getResourcesForApplication(app)
		} catch (_: Exception) {
			return emptyList()
		}
		val found = mutableListOf<Detection>()
		composeNames.firstOrNull { resources.getIdentifier(it, "string", app.packageName) != 0 }
			?.let { found.add(Detection(Toolkit.COMPOSE, Confidence.CERTAIN, "resource string/$it")) }
		viewsNames.firstOrNull { resources.getIdentifier(it, "string", app.packageName) != 0 }
			?.let { found.add(Detection(Toolkit.VIEWS, Confidence.CERTAIN, "resource string/$it")) }
		return found
	}
}
