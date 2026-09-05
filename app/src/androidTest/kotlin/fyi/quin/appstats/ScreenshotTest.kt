package fyi.quin.appstats

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fyi.quin.appstats.ui.AppRow
import fyi.quin.appstats.ui.RowActions
import fyi.quin.appstats.ui.Sort
import fyi.quin.appstats.ui.theme.AppStatsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
	@get:Rule
	val rule = createComposeRule()

	private fun shoot(name: String, dark: Boolean, content: @Composable () -> Unit) {
		rule.setContent {
			AppStatsTheme(dark = dark) {
				Surface(color = MaterialTheme.colorScheme.surface) {
					Column(Modifier.verticalScroll(rememberScrollState())) { content() }
				}
			}
		}
		val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
		val dir = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
		File(dir, "$name.png").outputStream().use {
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
		}
	}

	@Test
	fun darkRows() {
		shoot("rows_dark", dark = true) {
			AppRow(flutterApp, expanded = true, sort = Sort.NAME, actions = noop)
			AppRow(nativeApp, expanded = false, sort = Sort.SIZE, actions = noop)
			AppRow(composeApp, expanded = false, sort = Sort.UPDATED, actions = noop)
		}
	}

	@Test
	fun lightRows() {
		shoot("rows_light", dark = false) {
			AppRow(flutterApp, expanded = true, sort = Sort.NAME, actions = noop)
			AppRow(nativeApp, expanded = false, sort = Sort.SIZE, actions = noop)
			AppRow(composeApp, expanded = false, sort = Sort.UPDATED, actions = noop)
		}
	}
}

private val noop = RowActions(toggle = {}, open = {}, copy = {}, info = {})
