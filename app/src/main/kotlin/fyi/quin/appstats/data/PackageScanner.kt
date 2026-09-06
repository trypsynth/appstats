package fyi.quin.appstats.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import fyi.quin.appstats.model.ApkFacts
import fyi.quin.appstats.model.AppRecord
import java.io.File
import java.security.MessageDigest

private const val INFO_FLAGS = PackageManager.GET_PERMISSIONS or
	PackageManager.GET_ACTIVITIES or
	PackageManager.GET_SERVICES or
	PackageManager.GET_RECEIVERS or
	PackageManager.GET_PROVIDERS

class PackageScanner(context: Context) {
	private val pm = context.packageManager

	fun launchablePackages(): List<String> {
		val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
		return pm.queryIntentActivities(intent, 0)
			.mapNotNull { it.activityInfo?.packageName }
			.distinct()
	}

	fun allPackages(): List<Pair<String, Boolean>> {
		return pm.getInstalledApplications(0).map {
			it.packageName to (it.flags and ApplicationInfo.FLAG_SYSTEM != 0)
		}
	}

	fun record(packageName: String): AppRecord? {
		val info = try {
			pm.getPackageInfo(packageName, INFO_FLAGS or signingFlag())
		} catch (_: Exception) {
			return null
		}
		val app = info.applicationInfo ?: return null
		return AppRecord(
			packageName = packageName,
			label = pm.getApplicationLabel(app).toString(),
			versionName = info.versionName ?: "unknown",
			versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				info.longVersionCode
			} else {
				@Suppress("DEPRECATION") info.versionCode.toLong()
			},
			minSdk = app.minSdkVersion,
			targetSdk = app.targetSdkVersion,
			firstInstall = info.firstInstallTime,
			lastUpdate = info.lastUpdateTime,
			installer = installerOf(packageName),
			isSystem = app.flags and ApplicationInfo.FLAG_SYSTEM != 0,
			isDebuggable = app.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
			apkBytes = apkPaths(app).sumOf { File(it).length() },
			splitCount = app.splitSourceDirs?.size ?: 0,
			permissions = info.requestedPermissions?.toList().orEmpty().sorted(),
			activities = info.activities?.size ?: 0,
			services = info.services?.size ?: 0,
			receivers = info.receivers?.size ?: 0,
			providers = info.providers?.size ?: 0,
			certSha256 = certificateOf(info),
			facts = null,
		)
	}

	fun apkPaths(app: ApplicationInfo): List<String> {
		val splits = app.splitSourceDirs?.toList().orEmpty()
		return listOf(app.sourceDir) + splits
	}

	fun facts(packageName: String): ApkFacts {
		val app = try {
			pm.getApplicationInfo(packageName, 0)
		} catch (_: Exception) {
			return ApkFacts()
		}
		return ApkInspector.withExtra(
			ApkInspector.inspect(apkPaths(app)),
			ResourceProbe.probe(pm, app),
		)
	}

	private fun signingFlag(): Int {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			PackageManager.GET_SIGNING_CERTIFICATES
		} else {
			@Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
		}
	}

	private fun installerOf(packageName: String): String {
		return try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				pm.getInstallSourceInfo(packageName).installingPackageName ?: "not recorded"
			} else {
				@Suppress("DEPRECATION")
				pm.getInstallerPackageName(packageName) ?: "not recorded"
			}
		} catch (_: Exception) {
			"not recorded"
		}
	}

	private fun certificateOf(info: PackageInfo): String {
		val bytes = try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
			} else {
				@Suppress("DEPRECATION") info.signatures?.firstOrNull()?.toByteArray()
			}
		} catch (_: Exception) {
			null
		} ?: return "unavailable"
		val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
		return digest.joinToString("") { "%02X".format(it) }
	}
}
