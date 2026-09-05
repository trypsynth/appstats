package fyi.quin.appstats.ui

import fyi.quin.appstats.model.AppRecord

enum class Sort(
	val label: String,
	val ascendingLabel: String,
	val descendingLabel: String,
	val defaultAscending: Boolean,
) {
	NAME("Name", "A to Z", "Z to A", true),
	TOOLKIT("Toolkit", "A to Z", "Z to A", true),
	SIZE("Size", "smallest first", "largest first", false),
	UPDATED("Last updated", "oldest first", "newest first", false);

	fun directionLabel(ascending: Boolean): String {
		return if (ascending) ascendingLabel else descendingLabel
	}
}

data class SortOption(val sort: Sort, val ascending: Boolean) {
	val label: String
		get() = "${sort.label}, ${sort.directionLabel(ascending)}"
}

val sortOptions: List<SortOption> = Sort.entries.flatMap {
	listOf(SortOption(it, it.defaultAscending), SortOption(it, !it.defaultAscending))
}

fun sortedStat(record: AppRecord, sort: Sort): String? {
	return when (sort) {
		Sort.SIZE -> formatBytes(record.apkBytes)
		Sort.UPDATED -> "updated ${formatDate(record.lastUpdate)}"
		Sort.NAME, Sort.TOOLKIT -> null
	}
}
