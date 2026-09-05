package fyi.quin.appstats.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(vm: AppViewModel = viewModel()) {
	val state by vm.state.collectAsState()
	val visible = remember(state) { state.visible }
	val context = LocalContext.current
	Scaffold(
		containerColor = MaterialTheme.colorScheme.surface,
		topBar = {
			TopAppBar(
				title = { Text("appstats", fontWeight = FontWeight.SemiBold) },
				colors = TopAppBarDefaults.topAppBarColors(
					containerColor = MaterialTheme.colorScheme.surface,
				),
				actions = {
					TextButton(onClick = vm::collapseAll) { Text("Collapse all") }
				},
			)
		}
	) { padding ->
		Column(Modifier.fillMaxSize().padding(padding)) {
			OutlinedTextField(
				value = state.query,
				onValueChange = vm::setQuery,
				label = { Text("Search apps") },
				singleLine = true,
				modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
			)
			Row(Modifier.fillMaxWidth()) {
				FilterButton(state.filter, vm::setFilter)
				SortButton(state, vm::setSorting)
			}
			Text(
				text = statusLine(state, visible.size),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier
					.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
					.semantics { liveRegion = LiveRegionMode.Polite },
			)
			LazyColumn(Modifier.fillMaxSize()) {
				items(visible, key = { it.packageName }) { record ->
					AppRow(
						record = record,
						expanded = state.expanded.contains(record.packageName),
						sort = state.sort,
						actions = RowActions(
							toggle = { vm.toggle(record.packageName) },
							open = { openApp(context, record.packageName) },
							copy = { copyText(context, it) },
							info = { openSettings(context, record.packageName) },
						),
					)
				}
			}
		}
	}
}

@Composable
private fun FilterButton(current: Filter, onPick: (Filter) -> Unit) {
	var open by remember { mutableStateOf(false) }
	TextButton(onClick = { open = true }, modifier = Modifier.padding(start = 8.dp)) {
		Text("Show: ${current.label}")
	}
	if (open) {
		AlertDialog(
			onDismissRequest = { open = false },
			title = { Text("Show which apps") },
			text = {
				Column(
					Modifier
						.verticalScroll(rememberScrollState())
						.selectableGroup()
				) {
					Filter.entries.forEach { filter ->
						Choice(
							label = filter.label,
							selected = filter == current,
							onPick = { open = false; onPick(filter) },
						)
					}
				}
			},
			confirmButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
		)
	}
}

@Composable
private fun SortButton(state: UiState, onPick: (SortOption) -> Unit) {
	var open by remember { mutableStateOf(false) }
	val current = "${state.sort.label}, ${state.sort.directionLabel(state.ascending)}"
	TextButton(
		onClick = { open = true },
		modifier = Modifier.padding(start = 8.dp),
	) {
		Text("Sort: $current")
	}
	if (open) {
		AlertDialog(
			onDismissRequest = { open = false },
			title = { Text("Sort apps by") },
			text = {
				Column(
					Modifier
						.verticalScroll(rememberScrollState())
						.selectableGroup()
				) {
					sortOptions.forEach { option ->
						Choice(
							label = option.label,
							selected = option.sort == state.sort && option.ascending == state.ascending,
							onPick = { open = false; onPick(option) },
						)
					}
				}
			},
			confirmButton = {
				TextButton(onClick = { open = false }) { Text("Cancel") }
			},
		)
	}
}

@Composable
private fun Choice(label: String, selected: Boolean, onPick: () -> Unit) {
	Row(
		Modifier
			.fillMaxWidth()
			.heightIn(min = 48.dp)
			.selectable(selected = selected, role = Role.RadioButton, onClick = onPick)
			.padding(vertical = 4.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		RadioButton(selected = selected, onClick = null)
		Text(label, Modifier.padding(start = 12.dp))
	}
}

private fun statusLine(state: UiState, shown: Int): String {
	if (state.loading) return "Scanning apps"
	val direction = state.sort.directionLabel(state.ascending)
	val counted = plural(shown, state.filter.one, state.filter.many)
	return "$counted, sorted by ${state.sort.label}, $direction"
}

private fun openApp(context: Context, packageName: String) {
	val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
	context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun openSettings(context: Context, packageName: String) {
	val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
		.setData(Uri.fromParts("package", packageName, null))
		.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	context.startActivity(intent)
}

private fun copyText(context: Context, text: String) {
	val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
	clipboard.setPrimaryClip(ClipData.newPlainText("appstats", text))
}
