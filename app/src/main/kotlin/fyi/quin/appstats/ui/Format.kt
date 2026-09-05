package fyi.quin.appstats.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

fun formatBytes(bytes: Long): String {
	if (bytes < 1024) return "$bytes bytes"
	val kb = bytes / 1024.0
	if (kb < 1024) return String.format(Locale.getDefault(), "%.0f KB", kb)
	val mb = kb / 1024.0
	if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)
	return String.format(Locale.getDefault(), "%.2f GB", mb / 1024.0)
}

fun formatDate(millis: Long): String {
	if (millis <= 0L) return "unknown"
	return dateFormat.format(Date(millis))
}

fun shortHash(hex: String): String {
	if (hex.length < 16) return hex
	return hex.substring(0, 16).chunked(4).joinToString(" ")
}

fun plural(count: Int, one: String, many: String): String {
	return if (count == 1) "1 $one" else "$count $many"
}
