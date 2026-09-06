package fyi.quin.appstats

import fyi.quin.appstats.model.ApkFacts
import fyi.quin.appstats.model.AppRecord
import fyi.quin.appstats.model.Confidence
import fyi.quin.appstats.model.Detection
import fyi.quin.appstats.model.Toolkit
import fyi.quin.appstats.ui.Sort
import fyi.quin.appstats.ui.UiState
import fyi.quin.appstats.ui.sortedStat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun record(
	name: String,
	bytes: Long = 0,
	updated: Long = 0,
	toolkit: Toolkit? = null,
) = AppRecord(
	packageName = "pkg.$name",
	label = name,
	versionName = "1.0",
	versionCode = 1,
	minSdk = 26,
	targetSdk = 36,
	firstInstall = 0,
	lastUpdate = updated,
	installer = "test",
	isSystem = false,
	isDebuggable = false,
	apkBytes = bytes,
	splitCount = 0,
	permissions = emptyList(),
	activities = 0,
	services = 0,
	receivers = 0,
	providers = 0,
	certSha256 = "AB",
	facts = toolkit?.let {
		ApkFacts(detections = listOf(Detection(it, Confidence.CERTAIN, "lib/x/y.so")), readable = true)
	},
)

class SortTest {
	private val apps = listOf(
		record("Charlie", bytes = 300, updated = 30),
		record("alpha", bytes = 100, updated = 10),
		record("Bravo", bytes = 200, updated = 20),
	)

	@Test
	fun sortsByNameIgnoringCase() {
		val state = UiState(loading = false, apps = apps, sort = Sort.NAME, ascending = true)
		assertEquals(listOf("alpha", "Bravo", "Charlie"), state.visible.map { it.label })
	}

	@Test
	fun descendingReversesTheOrder() {
		val state = UiState(loading = false, apps = apps, sort = Sort.NAME, ascending = false)
		assertEquals(listOf("Charlie", "Bravo", "alpha"), state.visible.map { it.label })
	}

	@Test
	fun sortsBySizeSmallestFirstWhenAscending() {
		val state = UiState(loading = false, apps = apps, sort = Sort.SIZE, ascending = true)
		assertEquals(listOf(100L, 200L, 300L), state.visible.map { it.apkBytes })
	}

	@Test
	fun sizeAndUpdatedDefaultToLargestAndNewestFirst() {
		assertTrue(!Sort.SIZE.defaultAscending)
		assertTrue(!Sort.UPDATED.defaultAscending)
		assertTrue(Sort.NAME.defaultAscending)
		assertTrue(Sort.TOOLKIT.defaultAscending)
	}

	@Test
	fun searchMatchesLabelAndPackageName() {
		val state = UiState(loading = false, apps = apps, query = "brav")
		assertEquals(listOf("Bravo"), state.visible.map { it.label })
		assertEquals(1, state.copy(query = "pkg.alpha").visible.size)
	}

	@Test
	fun theSortedStatIsShownOnlyForSizeAndDate() {
		val app = record("alpha", bytes = 2048, updated = 0)
		assertEquals("2 KB", sortedStat(app, Sort.SIZE))
		assertNull(sortedStat(app, Sort.NAME))
		assertNull(sortedStat(app, Sort.TOOLKIT))
		assertTrue(sortedStat(app, Sort.UPDATED)!!.startsWith("updated "))
	}

	@Test
	fun toolkitSortUsesTheHeadlineLabel() {
		val list = listOf(
			record("one", toolkit = Toolkit.UNITY),
			record("two", toolkit = Toolkit.FLUTTER),
		)
		val state = UiState(loading = false, apps = list, sort = Sort.TOOLKIT, ascending = true)
		assertEquals(listOf("Flutter", "Unity"), state.visible.map { it.headline.label })
	}
}
