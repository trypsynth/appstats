package fyi.quin.appstats.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fyi.quin.appstats.model.AppRecord
import fyi.quin.appstats.model.Confidence

@Composable
fun AppRow(
	record: AppRecord,
	expanded: Boolean,
	sort: Sort,
	actions: RowActions,
) {
	val headline = record.headline
	val stat = sortedStat(record, sort)
	val subtitle = if (stat == null) "version ${record.versionName}" else "version ${record.versionName}, $stat"
	val spoken = "${record.label}, ${headline.label}, $subtitle"
	val custom = rememberActions(spoken, actions)
	val header = remember { FocusRequester() }
	Card(
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainer,
		),
		modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
	) {
		Column(
			Modifier
				.fillMaxWidth()
				.focusRequester(header)
				.semantics(mergeDescendants = true) {
					heading()
					if (expanded) {
						collapse { actions.toggle(); true }
					} else {
						expand { actions.toggle(); true }
					}
					customActions = custom
				}
				.clickable(
					onClickLabel = if (expanded) "collapse the details" else "expand the details",
					role = Role.Button,
					onClick = { actions.toggle() },
				)
				.heightIn(min = 48.dp)
				.padding(PaddingValues(horizontal = 16.dp, vertical = 12.dp))
		) {
			Row(
				Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
					text = record.label,
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.SemiBold,
					modifier = Modifier.weight(1f, fill = false),
				)
				Pill(headline.label, headline.confidence)
			}
			Text(
				text = subtitle,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(top = 2.dp),
			)
		}
		if (expanded) {
			AppDetail(
				record = record,
				actions = actions,
				onCollapseApp = {
					actions.toggle()
					runCatching { header.requestFocus() }
				},
			)
		}
	}
}

@Composable
private fun Pill(text: String, confidence: Confidence?) {
	val scheme = MaterialTheme.colorScheme
	val background = when (confidence) {
		Confidence.CERTAIN -> scheme.primaryContainer
		Confidence.LIKELY -> scheme.secondaryContainer
		Confidence.POSSIBLE -> scheme.tertiaryContainer
		null -> scheme.surfaceVariant
	}
	val foreground = when (confidence) {
		Confidence.CERTAIN -> scheme.onPrimaryContainer
		Confidence.LIKELY -> scheme.onSecondaryContainer
		Confidence.POSSIBLE -> scheme.onTertiaryContainer
		null -> scheme.onSurfaceVariant
	}
	Surface(color = background, contentColor = foreground, shape = RoundedCornerShape(50)) {
		Text(
			text = text,
			style = MaterialTheme.typography.labelMedium,
			modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
		)
	}
}

@Composable
fun rememberActions(
	copyTarget: String,
	actions: RowActions,
	appCollapse: Boolean = false,
	onAppCollapse: () -> Unit = {},
	innerLabel: String? = null,
	onInner: () -> Unit = {},
): List<CustomAccessibilityAction> {
	val current by rememberUpdatedState(actions)
	val collapseApp by rememberUpdatedState(onAppCollapse)
	val inner by rememberUpdatedState(onInner)
	return remember(copyTarget, appCollapse, innerLabel) {
		buildList {
			add(CustomAccessibilityAction("Copy") { current.copy(copyTarget); true })
			if (innerLabel != null) {
				add(CustomAccessibilityAction(innerLabel) { inner(); true })
			}
			if (appCollapse) {
				add(CustomAccessibilityAction("Collapse app") { collapseApp(); true })
			}
			add(CustomAccessibilityAction("Open app") { current.open(); true })
			add(CustomAccessibilityAction("System app info") { current.info(); true })
		}
	}
}

data class RowActions(
	val toggle: () -> Unit,
	val open: () -> Unit,
	val copy: (String) -> Unit,
	val info: () -> Unit,
)
