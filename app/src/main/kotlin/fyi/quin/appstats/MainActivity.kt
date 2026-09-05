package fyi.quin.appstats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import fyi.quin.appstats.ui.AppListScreen
import fyi.quin.appstats.ui.theme.AppStatsTheme

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			AppStatsTheme {
				Surface(color = MaterialTheme.colorScheme.background) {
					AppListScreen()
				}
			}
		}
	}
}
