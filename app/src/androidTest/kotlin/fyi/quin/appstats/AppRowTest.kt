package fyi.quin.appstats

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fyi.quin.appstats.data.PermissionNames
import fyi.quin.appstats.model.ApkFacts
import fyi.quin.appstats.model.AppRecord
import fyi.quin.appstats.model.Confidence
import fyi.quin.appstats.model.Detection
import fyi.quin.appstats.model.Toolkit
import fyi.quin.appstats.ui.AppRow
import fyi.quin.appstats.ui.RowActions
import fyi.quin.appstats.ui.Sort
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun hasCustomAction(label: String) = SemanticsMatcher("has custom action $label") { node ->
	node.config.getOrElse(SemanticsActions.CustomActions) { emptyList() }.any { it.label == label }
}

private val sample = AppRecord(
	packageName = "com.example.sample",
	label = "Sample App",
	versionName = "1.0",
	versionCode = 7,
	minSdk = 26,
	targetSdk = 36,
	firstInstall = 0,
	lastUpdate = 0,
	installer = "test",
	isSystem = false,
	isDebuggable = false,
	apkBytes = 2048,
	splitCount = 0,
	permissions = listOf("android.permission.INTERNET"),
	activities = 1,
	services = 0,
	receivers = 0,
	providers = 0,
	certSha256 = "AABBCCDDEEFF00112233",
	facts = ApkFacts(
		detections = listOf(Detection(Toolkit.FLUTTER, Confidence.CERTAIN, "lib/arm64-v8a/libflutter.so")),
		abis = listOf("arm64-v8a"),
		nativeLibraries = listOf("libflutter.so", "libsqlite.so"),
		readable = true,
	),
)

private val versioned = sample.copy(
	facts = sample.facts!!.copy(
		detections = listOf(
			Detection(Toolkit.COMPOSE, Confidence.CERTAIN, "resource string/m3c_dialog", "1.8.3"),
		),
		libraries = sortedMapOf(
			"androidx.appcompat:appcompat" to "1.7.0",
			"androidx.compose.ui:ui" to "1.8.3",
		),
		agpVersion = "8.11.1",
		kotlinVersion = "2.1.20",
		dexCount = 3,
		hasBaselineProfile = true,
		usesKotlin = true,
	),
)

private val noop = RowActions(toggle = {}, open = {}, copy = {}, info = {})

@RunWith(AndroidJUnit4::class)
class AppRowTest {
	@get:Rule
	val rule = createComposeRule()

	@Test
	fun theCollapsedRowIsOneNodeThatReadsTheWholeApp() {
		rule.setContent { AppRow(sample, expanded = false, sort = Sort.NAME, actions = noop) }
		rule.onNode(hasText("Sample App", substring = true))
			.assert(hasText("Flutter", substring = true))
			.assert(hasText("version 1.0", substring = true))
			.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
			.assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
	}

	@Test
	fun theOpenRowHeaderOffersCollapse() {
		rule.setContent { AppRow(sample, expanded = true, sort = Sort.NAME, actions = noop) }
		rule.onNode(hasText("Sample App", substring = true))
			.assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
	}

	@Test
	fun everyFactCanCopyItselfAndCollapseTheRow() {
		rule.setContent { AppRow(sample, expanded = true, sort = Sort.NAME, actions = noop) }
		listOf("Target SDK 36", "Minimum SDK 26", "Package name com.example.sample", "1 activity")
			.forEach { line ->
				rule.onNodeWithText(line)
					.assert(hasCustomAction("Copy"))
					.assert(hasCustomAction("Collapse app"))
					.assert(hasCustomAction("Open app"))
			}
	}

	@Test
	fun theButtonRowIsInvisibleToTalkBack() {
		rule.setContent { AppRow(sample, expanded = true, sort = Sort.NAME, actions = noop) }
		rule.onAllNodesWithText("Open").assertCountEquals(0)
		rule.onAllNodesWithText("Copy name").assertCountEquals(0)
		rule.onAllNodesWithText("App info").assertCountEquals(0)
	}

	@Test
	fun thereAreNoGroupTitlesToSwipePast() {
		rule.setContent { AppRow(sample, expanded = true, sort = Sort.NAME, actions = noop) }
		listOf("Build", "Install", "Contents").forEach {
			rule.onAllNodesWithText(it).assertCountEquals(0)
		}
	}

	@Test
	fun sortingBySizePutsTheSizeInTheRow() {
		rule.setContent { AppRow(sample, expanded = false, sort = Sort.SIZE, actions = noop) }
		rule.onNode(hasText("Sample App", substring = true))
			.assert(hasText("2 KB", substring = true))
	}

	@Test
	fun permissionsReadAsWordsNotConstants() {
		val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
		val friendly = PermissionNames.label(pm, "android.permission.INTERNET")
		rule.setContent { AppRow(sample, expanded = true, sort = Sort.NAME, actions = noop) }
		rule.onNodeWithText("1 permission requested").performClick()
		rule.onAllNodesWithText("INTERNET").assertCountEquals(0)
		rule.onAllNodesWithText("android.permission.INTERNET").assertCountEquals(0)
		rule.onNodeWithText(friendly).assert(hasText(friendly))
		assertNotEquals("The system label should be used, not the fallback", "Internet", friendly)
	}

	@Test
	fun theToolkitLineCarriesTheLibraryVersion() {
		rule.setContent { AppRow(versioned, expanded = true, sort = Sort.NAME, actions = noop) }
		rule.onNode(hasText("Jetpack Compose 1.8.3", substring = true))
			.assert(hasText("certain", substring = true))
	}

	@Test
	fun theBuildToolingIsShown() {
		rule.setContent { AppRow(versioned, expanded = true, sort = Sort.NAME, actions = noop) }
		rule.onNodeWithText("Built with Android Gradle Plugin 8.11.1").assert(hasCustomAction("Copy"))
		rule.onNodeWithText("Kotlin 2.1.20").assert(hasCustomAction("Copy"))
		rule.onNodeWithText("3 dex files").assert(hasCustomAction("Copy"))
		rule.onNodeWithText("Ships a baseline profile").assert(hasCustomAction("Copy"))
	}

	@Test
	fun bundledLibrariesOpenIntoCoordinatesAndVersions() {
		rule.setContent { AppRow(versioned, expanded = true, sort = Sort.NAME, actions = noop) }
		rule.onNodeWithText("2 bundled libraries").performClick()
		rule.onNodeWithText("androidx.appcompat:appcompat 1.7.0")
			.assert(hasCustomAction("Collapse bundled libraries"))
		rule.onNodeWithText("androidx.compose.ui:ui 1.8.3").assert(hasCustomAction("Copy"))
	}

	@Test
	fun thePermissionCountIsItsOwnButton() {
		rule.setContent { AppRow(sample, expanded = true, sort = Sort.NAME, actions = noop) }
		rule.onNodeWithText("1 permission requested")
			.assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
	}

	@Test
	fun collapsingFromInsideASubListClosesOnlyThatList() {
		rule.setContent { AppRow(sample, expanded = true, sort = Sort.NAME, actions = noop) }
		rule.onNodeWithText("Links against 2 native libraries").performClick()
		rule.onNodeWithText("libflutter.so")
			.assert(hasCustomAction("Collapse native libraries"))
			.assert(hasCustomAction("Collapse app"))
			.assert(hasCustomAction("Copy"))
	}

	@Test
	fun collapsingALibraryLineClosesOnlyThatListAndKeepsTheAppOpen() {
		rule.setContent { AppRow(sample, expanded = true, sort = Sort.NAME, actions = noop) }
		rule.onNodeWithText("Links against 2 native libraries").performClick()
		val node = rule.onNodeWithText("libflutter.so").fetchSemanticsNode()
		val collapse = node.config[SemanticsActions.CustomActions]
			.first { it.label == "Collapse native libraries" }
		rule.runOnUiThread { collapse.action?.invoke() }
		rule.onAllNodesWithText("libflutter.so").assertCountEquals(0)
		rule.onNodeWithText("Links against 2 native libraries")
			.assert(hasText("Links against 2 native libraries"))
		rule.onNodeWithText("Target SDK 36").assert(hasText("Target SDK 36"))
	}

	@Test
	fun theLibraryListNamesEveryLinkedLibrary() {
		rule.setContent { AppRow(sample, expanded = true, sort = Sort.NAME, actions = noop) }
		rule.onNodeWithText("Links against 2 native libraries").performClick()
		rule.onNodeWithText("libflutter.so").assert(hasText("libflutter.so"))
		rule.onNodeWithText("libsqlite.so").assert(hasText("libsqlite.so"))
	}
}
