package fyi.quin.appstats.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import fyi.quin.appstats.data.PermissionNames
import fyi.quin.appstats.model.AppRecord

private val lineInset = PaddingValues(start = 16.dp, end = 16.dp, top = 3.dp, bottom = 3.dp)

private data class Line(val label: String = "", val value: String, val tail: String = "") {
	val text: String
		get() = (if (label.isEmpty()) "" else "$label ") + value + tail
}

@Composable
fun AppDetail(record: AppRecord, actions: RowActions, onCollapseApp: () -> Unit) {
	val pm = LocalContext.current.packageManager
	val permissions = remember(record.packageName) {
		record.permissions.map { PermissionNames.label(pm, it) }.sorted()
	}
	val nativeLibraries = record.facts?.nativeLibraries.orEmpty()
	val bundled = remember(record.packageName, record.facts) {
		record.facts?.libraries.orEmpty().map { "${it.key} ${it.value}" }
	}
	Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
		Separator()
		Group(buildLines(record), actions, onCollapseApp)
		val tooling = toolingLines(record)
		if (tooling.isNotEmpty()) {
			Separator()
			Group(tooling, actions, onCollapseApp)
		}
		Separator()
		Group(installLines(record), actions, onCollapseApp)
		Separator()
		Group(contentLines(record), actions, onCollapseApp)
		SubList(
			title = "${plural(permissions.size, "permission", "permissions")} requested",
			items = permissions,
			noun = "the permission list",
			collapseLabel = "Collapse permissions",
			resetKey = record.packageName,
			actions = actions,
			onCollapseApp = onCollapseApp,
		)
		SubList(
			title = "Links against ${plural(nativeLibraries.size, "native library", "native libraries")}",
			items = nativeLibraries,
			noun = "the native library list",
			collapseLabel = "Collapse native libraries",
			resetKey = record.packageName,
			actions = actions,
			onCollapseApp = onCollapseApp,
		)
		SubList(
			title = plural(bundled.size, "bundled library", "bundled libraries"),
			items = bundled,
			noun = "the bundled library list",
			collapseLabel = "Collapse bundled libraries",
			resetKey = record.packageName,
			actions = actions,
			onCollapseApp = onCollapseApp,
		)
		Row(
			Modifier
				.fillMaxWidth()
				.clearAndSetSemantics { }
				.padding(start = 8.dp, end = 8.dp),
			horizontalArrangement = Arrangement.spacedBy(4.dp),
		) {
			TextButton(onClick = actions.open) { Text("Open") }
			TextButton(onClick = { actions.copy(record.packageName) }) { Text("Copy name") }
			TextButton(onClick = actions.info) { Text("App info") }
		}
	}
}

@Composable
private fun Separator() {
	HorizontalDivider(
		color = MaterialTheme.colorScheme.outlineVariant,
		modifier = Modifier.padding(horizontal = 16.dp),
	)
}

@Composable
private fun Group(lines: List<Line>, actions: RowActions, onCollapseApp: () -> Unit) {
	Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
		lines.forEach { Fact(it, actions, onCollapseApp) }
	}
}

@Composable
private fun Fact(
	line: Line,
	actions: RowActions,
	onCollapseApp: () -> Unit,
	modifier: Modifier = Modifier,
	innerLabel: String? = null,
	onInner: () -> Unit = {},
) {
	val scheme = MaterialTheme.colorScheme
	val custom = rememberActions(
		copyTarget = line.text,
		actions = actions,
		appCollapse = true,
		onAppCollapse = onCollapseApp,
		innerLabel = innerLabel,
		onInner = onInner,
	)
	val styled = buildAnnotatedString {
		if (line.label.isNotEmpty()) {
			withStyle(SpanStyle(color = scheme.onSurfaceVariant)) { append("${line.label} ") }
		}
		val weight = if (line.label.isEmpty()) FontWeight.Normal else FontWeight.Medium
		withStyle(SpanStyle(color = scheme.onSurface, fontWeight = weight)) {
			append(line.value)
		}
		if (line.tail.isNotEmpty()) {
			withStyle(SpanStyle(color = scheme.onSurfaceVariant)) { append(line.tail) }
		}
	}
	Text(
		text = styled,
		style = MaterialTheme.typography.bodyMedium,
		modifier = modifier
			.fillMaxWidth()
			.semantics { customActions = custom }
			.padding(lineInset),
	)
}

@Composable
private fun SubList(
	title: String,
	items: List<String>,
	noun: String,
	collapseLabel: String,
	resetKey: String,
	actions: RowActions,
	onCollapseApp: () -> Unit,
) {
	var open by remember(resetKey) { mutableStateOf(false) }
	val header = remember(resetKey) { FocusRequester() }
	val custom = rememberActions(
		copyTarget = title,
		actions = actions,
		appCollapse = true,
		onAppCollapse = onCollapseApp,
	)
	Surface(
		color = MaterialTheme.colorScheme.surfaceContainerHighest,
		shape = RoundedCornerShape(10.dp),
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 12.dp, vertical = 4.dp)
			.focusRequester(header)
			.semantics {
				customActions = custom
				if (open) collapse { open = false; true } else expand { open = true; true }
			}
			.clickable(
				enabled = items.isNotEmpty(),
				onClickLabel = if (open) "collapse $noun" else "expand $noun",
				role = Role.Button,
				onClick = { open = !open },
			),
	) {
		Row(
			Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Text(
				text = title,
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = FontWeight.SemiBold,
				modifier = Modifier.weight(1f),
			)
			if (items.isNotEmpty()) Chevron(open)
		}
	}
	if (open) {
		Column(Modifier.padding(bottom = 6.dp)) {
			items.forEach {
				Fact(
					line = Line(value = it),
					actions = actions,
					onCollapseApp = onCollapseApp,
					modifier = Modifier.padding(start = 16.dp),
					innerLabel = collapseLabel,
					onInner = {
						open = false
						runCatching { header.requestFocus() }
					},
				)
			}
		}
	}
}

@Composable
private fun Chevron(open: Boolean) {
	val tint = MaterialTheme.colorScheme.onSurfaceVariant
	Canvas(modifier = Modifier.size(14.dp)) {
		val path = Path().apply {
			if (open) {
				moveTo(0f, size.height * 0.7f)
				lineTo(size.width / 2f, size.height * 0.3f)
				lineTo(size.width, size.height * 0.7f)
			} else {
				moveTo(0f, size.height * 0.3f)
				lineTo(size.width / 2f, size.height * 0.7f)
				lineTo(size.width, size.height * 0.3f)
			}
		}
		drawPath(path, color = tint, style = Stroke(width = 4f))
	}
}

private fun buildLines(record: AppRecord): List<Line> {
	val facts = record.facts
	val lines = mutableListOf<Line>()
	when {
		facts == null -> lines.add(Line(value = "Reading the APK"))
		!facts.readable -> lines.add(
			Line(value = "The APK could not be read, so no toolkit was detected")
		)
		facts.detections.isEmpty() -> lines.add(
			Line(
				value = "No toolkit marker found.",
				tail = " Release builds often strip them, so this is most likely plain Android UI code",
			)
		)
		else -> facts.detections.forEach {
			lines.add(
				Line(
					label = "Toolkit",
					value = it.label,
					tail = ", ${it.confidence.label}, from ${it.evidence}",
				)
			)
		}
	}
	lines.add(Line("Target SDK", "${record.targetSdk}"))
	lines.add(Line("Minimum SDK", "${record.minSdk}"))
	val abis = facts?.abis.orEmpty()
	lines.add(
		if (abis.isEmpty()) Line(value = "No native code")
		else Line("Native code for", abis.joinToString(", "))
	)
	return lines
}

private fun toolingLines(record: AppRecord): List<Line> {
	val facts = record.facts ?: return emptyList()
	if (!facts.readable) return emptyList()
	val lines = mutableListOf<Line>()
	facts.agpVersion?.let { lines.add(Line("Built with Android Gradle Plugin", it)) }
	facts.kotlinVersion?.let { lines.add(Line("Kotlin", it)) }
	facts.gradleVersion?.let { lines.add(Line("Gradle", it)) }
	if (facts.kotlinVersion == null && facts.usesKotlin) {
		lines.add(Line(value = "Contains Kotlin code"))
	}
	if (facts.dexCount > 0) {
		lines.add(Line(value = plural(facts.dexCount, "dex file", "dex files")))
	}
	if (facts.hasBaselineProfile) lines.add(Line(value = "Ships a baseline profile"))
	return lines
}

private fun installLines(record: AppRecord): List<Line> {
	return listOf(
		Line("Installed", formatDate(record.firstInstall)),
		Line("Updated", formatDate(record.lastUpdate)),
		Line("Installed by", record.installer),
		Line("Size", formatBytes(record.apkBytes)),
		Line(value = plural(record.splitCount + 1, "APK file", "APK files")),
		Line("Version code", "${record.versionCode}"),
		Line(value = if (record.isSystem) "System app" else "User app"),
		Line(value = if (record.isDebuggable) "Debuggable" else "Not debuggable"),
	)
}

private fun contentLines(record: AppRecord): List<Line> {
	return listOf(
		Line("Package name", record.packageName),
		Line(value = plural(record.activities, "activity", "activities")),
		Line(value = plural(record.services, "service", "services")),
		Line(value = plural(record.receivers, "receiver", "receivers")),
		Line(value = plural(record.providers, "provider", "providers")),
		Line("Signature starts with", shortHash(record.certSha256)),
	)
}
