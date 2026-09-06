package fyi.quin.appstats

import fyi.quin.appstats.data.ApkInspector
import fyi.quin.appstats.model.Confidence
import fyi.quin.appstats.model.Toolkit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkInspectorTest {
	private fun fakeApkWith(vararg entries: Pair<String, String>): String {
		val file = File.createTempFile("fake", ".apk")
		file.deleteOnExit()
		ZipOutputStream(file.outputStream()).use { zip ->
			entries.forEach { (name, content) ->
				zip.putNextEntry(ZipEntry(name))
				zip.write(content.toByteArray())
				zip.closeEntry()
			}
		}
		return file.absolutePath
	}

	private fun fakeApk(vararg entries: String): String {
		return fakeApkWith(*entries.map { it to "x" }.toTypedArray())
	}

	@Test
	fun findsFlutterFromNativeLibrary() {
		val facts = ApkInspector.inspect(listOf(fakeApk("lib/arm64-v8a/libflutter.so", "classes.dex")))
		val hit = facts.detections.single { it.toolkit == Toolkit.FLUTTER }
		assertEquals(Confidence.CERTAIN, hit.confidence)
		assertEquals("lib/arm64-v8a/libflutter.so", hit.evidence)
		assertEquals(listOf("arm64-v8a"), facts.abis)
	}

	@Test
	fun keepsTheStrongestEvidencePerToolkit() {
		val facts = ApkInspector.inspect(
			listOf(fakeApk("assets/flutter_assets/x", "lib/x86_64/libflutter.so"))
		)
		assertEquals(Confidence.CERTAIN, facts.detections.single { it.toolkit == Toolkit.FLUTTER }.confidence)
	}

	@Test
	fun readsSplitApksAsOneApp() {
		val base = fakeApk("classes.dex", "META-INF/androidx.compose.ui_ui.version")
		val split = fakeApk("lib/arm64-v8a/libunity.so")
		val facts = ApkInspector.inspect(listOf(base, split))
		assertTrue(facts.detections.any { it.toolkit == Toolkit.UNITY })
		assertTrue(facts.detections.any { it.toolkit == Toolkit.COMPOSE })
	}

	@Test
	fun listsEveryNativeLibraryOnceAcrossAbis() {
		val facts = ApkInspector.inspect(
			listOf(
				fakeApk(
					"lib/arm64-v8a/libflutter.so",
					"lib/armeabi-v7a/libflutter.so",
					"lib/arm64-v8a/libsqlite.so",
					"classes.dex",
				)
			)
		)
		assertEquals(listOf("libflutter.so", "libsqlite.so"), facts.nativeLibraries)
		assertEquals(listOf("arm64-v8a", "armeabi-v7a"), facts.abis)
	}

	@Test
	fun hasNoLibrariesWhenTheApkShipsNoNativeCode() {
		val facts = ApkInspector.inspect(listOf(fakeApk("classes.dex", "res/layout/main.xml")))
		assertTrue(facts.nativeLibraries.isEmpty())
	}

	@Test
	fun reportsUnreadableWhenNoFileOpens() {
		val facts = ApkInspector.inspect(listOf("/does/not/exist.apk"))
		assertTrue(!facts.readable)
	}

	@Test
	fun sortsCertainDetectionsFirst() {
		val facts = ApkInspector.inspect(
			listOf(
				fakeApkWith(
					"META-INF/androidx.appcompat_appcompat.version" to "1.7.0",
					"lib/arm64-v8a/libflutter.so" to "x",
				)
			)
		)
		assertEquals(Toolkit.FLUTTER, facts.detections.first().toolkit)
	}

	@Test
	fun aLayoutFolderAloneIsNotEvidenceOfViews() {
		val facts = ApkInspector.inspect(listOf(fakeApk("res/layout/custom_dialog.xml", "classes.dex")))
		assertTrue(facts.detections.none { it.toolkit == Toolkit.VIEWS })
	}

	@Test
	fun readsLibraryVersionsFromMetaInf() {
		val facts = ApkInspector.inspect(
			listOf(
				fakeApkWith(
					"META-INF/androidx.compose.ui_ui.version" to "1.8.3",
					"META-INF/androidx.appcompat_appcompat.version" to "1.7.0",
					"META-INF/androidx.compose.material_material-icons-core.version" to "1.7.8",
				)
			)
		)
		assertEquals("1.8.3", facts.libraries["androidx.compose.ui:ui"])
		assertEquals("1.7.0", facts.libraries["androidx.appcompat:appcompat"])
		assertEquals("1.7.8", facts.libraries["androidx.compose.material:material-icons-core"])
	}

	@Test
	fun trimsWhitespaceAroundAVersion() {
		val padded = """
			1.9.0
		"""
		val facts = ApkInspector.inspect(
			listOf(fakeApkWith("META-INF/androidx.compose.ui_ui.version" to padded))
		)
		assertEquals("1.9.0", facts.libraries["androidx.compose.ui:ui"])
	}

	@Test
	fun ignoresBrokenGradlePlaceholderVersions() {
		val junk = "task ':arch:core:core-runtime:writeVersionFile' property 'version'"
		val facts = ApkInspector.inspect(
			listOf(fakeApkWith("META-INF/androidx.arch.core_core-runtime.version" to junk))
		)
		assertTrue(facts.libraries.isEmpty())
	}

	@Test
	fun putsTheLibraryVersionOnTheToolkitItProves() {
		val facts = ApkInspector.inspect(
			listOf(
				fakeApkWith(
					"META-INF/androidx.compose.ui_ui.version" to "1.8.3",
					"META-INF/androidx.appcompat_appcompat.version" to "1.7.0",
				)
			)
		)
		assertEquals(
			"Jetpack Compose 1.8.3",
			facts.detections.single { it.toolkit == Toolkit.COMPOSE }.label,
		)
		assertEquals(
			"Android Views 1.7.0",
			facts.detections.single { it.toolkit == Toolkit.VIEWS }.label,
		)
	}

	@Test
	fun readsTheBuildToolingThatMadeTheApk() {
		val properties = """
			appMetadataVersion=1.1
			androidGradlePluginVersion=8.11.1
		""".trimIndent()
		val kotlinJson = """{"buildSystemVersion":"8.13","buildPluginVersion":"2.1.20"}"""
		val facts = ApkInspector.inspect(
			listOf(
				fakeApkWith(
					"META-INF/com/android/build/gradle/app-metadata.properties" to properties,
					"kotlin-tooling-metadata.json" to kotlinJson,
				)
			)
		)
		assertEquals("8.11.1", facts.agpVersion)
		assertEquals("2.1.20", facts.kotlinVersion)
		assertEquals("8.13", facts.gradleVersion)
		assertTrue(facts.usesKotlin)
	}

	@Test
	fun countsDexFilesAndSpotsABaselineProfile() {
		val facts = ApkInspector.inspect(
			listOf(fakeApk("classes.dex", "classes2.dex", "classes3.dex", "assets/dexopt/baseline.prof"))
		)
		assertEquals(3, facts.dexCount)
		assertTrue(facts.hasBaselineProfile)
	}

	@Test
	fun spotsKotlinWithoutToolingMetadata() {
		val facts = ApkInspector.inspect(listOf(fakeApk("META-INF/app_release.kotlin_module")))
		assertTrue(facts.usesKotlin)
	}

	@Test
	fun detectsComposeMultiplatformFromSkiko() {
		val facts = ApkInspector.inspect(listOf(fakeApk("lib/arm64-v8a/libskiko-android-arm64.so")))
		assertEquals(Toolkit.COMPOSE_MULTIPLATFORM, facts.detections.first().toolkit)
	}

	@Test
	fun tellsMauiApartFromXamarinForms() {
		val maui = ApkInspector.inspect(
			listOf(fakeApk("assemblies/Microsoft.Maui.Controls.dll", "lib/arm64-v8a/libmonodroid.so"))
		)
		assertTrue(maui.detections.any { it.toolkit == Toolkit.MAUI })
		val xamarin = ApkInspector.inspect(
			listOf(fakeApk("assemblies/Xamarin.Forms.Core.dll", "lib/arm64-v8a/libmonodroid.so"))
		)
		assertTrue(xamarin.detections.any { it.toolkit == Toolkit.XAMARIN })
	}

	@Test
	fun detectsTheNewerGameEngines() {
		val cases = mapOf(
			"lib/arm64-v8a/libcocos2djs.so" to Toolkit.COCOS2D,
			"lib/arm64-v8a/libgdx.so" to Toolkit.LIBGDX,
			"lib/arm64-v8a/libSDL2.so" to Toolkit.SDL,
			"lib/arm64-v8a/libcorona.so" to Toolkit.SOLAR2D,
			"lib/arm64-v8a/libdmengine.so" to Toolkit.DEFOLD,
		)
		cases.forEach { (entry, toolkit) ->
			val facts = ApkInspector.inspect(listOf(fakeApk(entry)))
			assertTrue("$entry should give $toolkit", facts.detections.any { it.toolkit == toolkit })
		}
	}
}
