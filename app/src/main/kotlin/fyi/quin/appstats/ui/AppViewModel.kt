package fyi.quin.appstats.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fyi.quin.appstats.data.PackageScanner
import fyi.quin.appstats.model.AppRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private const val WORKERS = 4

data class UiState(
	val loading: Boolean = true,
	val apps: List<AppRecord> = emptyList(),
	val query: String = "",
	val sort: Sort = Sort.NAME,
	val ascending: Boolean = Sort.NAME.defaultAscending,
	val filter: Filter = Filter.LAUNCHER,
	val expanded: Set<String> = emptySet(),
) {
	val visible: List<AppRecord>
		get() {
			val q = query.trim().lowercase()
			val filtered = if (q.isEmpty()) apps else apps.filter {
				it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
			}
			val ordered = when (sort) {
				Sort.NAME -> filtered.sortedBy { it.label.lowercase() }
				Sort.TOOLKIT -> filtered.sortedWith(
					compareBy({ it.headline.label.lowercase() }, { it.label.lowercase() })
				)
				Sort.SIZE -> filtered.sortedBy { it.apkBytes }
				Sort.UPDATED -> filtered.sortedBy { it.lastUpdate }
			}
			return if (ascending) ordered else ordered.reversed()
		}
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
	private val scanner = PackageScanner(app)
	private val cache = ConcurrentHashMap<String, AppRecord>()
	private val _state = MutableStateFlow(UiState())
	private var scan: Job? = null
	val state: StateFlow<UiState> = _state.asStateFlow()

	init {
		startScan(Filter.LAUNCHER)
	}

	fun setQuery(value: String) = _state.update { it.copy(query = value) }

	fun setSorting(option: SortOption) = _state.update {
		it.copy(sort = option.sort, ascending = option.ascending)
	}

	fun setFilter(value: Filter) {
		if (_state.value.filter == value) return
		_state.update { it.copy(filter = value, loading = true) }
		startScan(value)
	}

	fun toggle(packageName: String) = _state.update {
		val next = it.expanded.toMutableSet()
		if (!next.remove(packageName)) next.add(packageName)
		it.copy(expanded = next)
	}

	fun collapseAll() = _state.update { it.copy(expanded = emptySet()) }

	private fun startScan(filter: Filter) {
		scan?.cancel()
		scan = viewModelScope.launch { load(filter) }
	}

	private suspend fun load(filter: Filter) {
		val names = withContext(Dispatchers.IO) { packagesFor(filter) }
		val missing = names.filterNot { cache.containsKey(it) }
		withContext(Dispatchers.IO) {
			coroutineScope {
				(0 until WORKERS).map { worker ->
					async {
						for (i in missing.indices) {
							if (i % WORKERS != worker) continue
							val name = missing[i]
							val record = scanner.record(name) ?: continue
							cache[name] = record.copy(facts = scanner.facts(name))
						}
					}
				}.awaitAll()
			}
		}
		_state.update {
			it.copy(apps = names.mapNotNull { name -> cache[name] }, loading = false)
		}
	}

	private fun packagesFor(filter: Filter): List<String> {
		if (filter == Filter.LAUNCHER) return scanner.launchablePackages()
		val all = scanner.allPackages()
		return when (filter) {
			Filter.USER -> all.filterNot { it.second }
			Filter.SYSTEM -> all.filter { it.second }
			else -> all
		}.map { it.first }
	}
}
