package fyi.quin.appstats

import fyi.quin.appstats.data.PermissionNames
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionNamesTest {
	@Test
	fun turnsAConstantIntoWords() {
		assertEquals(
			"Access fine location",
			PermissionNames.pretty("android.permission.ACCESS_FINE_LOCATION"),
		)
	}

	@Test
	fun handlesASingleWord() {
		assertEquals("Internet", PermissionNames.pretty("android.permission.INTERNET"))
	}

	@Test
	fun handlesAVendorPermission() {
		assertEquals(
			"Read settings",
			PermissionNames.pretty("com.example.launcher.permission.READ_SETTINGS"),
		)
	}

	@Test
	fun capitalisesABareName() {
		assertEquals("Weird", PermissionNames.pretty("weird"))
	}
}
