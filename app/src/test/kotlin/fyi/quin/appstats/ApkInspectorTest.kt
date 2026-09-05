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
	private fun fakeApk(vararg entries: String): String {
		val file = File.createTempFile("fake", ".apk")
		file.deleteOnExit()
		ZipOutputStream(file.outputStream()).use { zip ->
			entries.forEach {
				zip.putNextEntry(ZipEntry(it))
				zip.write(byteArrayOf(1))
				zip.closeEntry()
			}
		}
		return file.absolutePath
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
			listOf(fakeApk("res/layout/main.xml", "lib/arm64-v8a/libflutter.so"))
		)
		assertEquals(Toolkit.FLUTTER, facts.detections.first().toolkit)
	}
}
