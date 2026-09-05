package fyi.quin.appstats.data

import android.content.pm.PackageManager
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object PermissionNames {
	private val cache = ConcurrentHashMap<String, String>()

	fun label(pm: PackageManager, name: String): String {
		return cache.getOrPut(name) {
			val fromSystem = try {
				pm.getPermissionInfo(name, 0).loadLabel(pm).toString()
			} catch (_: Exception) {
				null
			}
			if (fromSystem.isNullOrBlank() || fromSystem == name) pretty(name) else sentence(fromSystem)
		}
	}

	fun pretty(name: String): String {
		val tail = name.substringAfterLast('.')
		val words = tail.split('_').filter { it.isNotEmpty() }
		if (words.isEmpty()) return name
		return sentence(words.joinToString(" ") { it.lowercase(Locale.ROOT) })
	}

	private fun sentence(text: String): String {
		return text.replaceFirstChar { it.titlecase(Locale.getDefault()) }
	}
}
