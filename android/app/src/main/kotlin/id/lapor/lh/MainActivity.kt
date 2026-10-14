package id.lapor.lh

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import id.lapor.lh.core.ui.LaporLHTheme
import id.lapor.lh.core.ui.ThemeMode
import id.lapor.lh.feature.sync.domain.SyncRepository
import id.lapor.lh.navigation.LaporLHNavHost
import id.lapor.lh.navigation.SessionViewModel
import id.lapor.lh.navigation.ThemeViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private val lightNavigationScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val darkNavigationScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var syncRepository: SyncRepository

    private val sessionViewModel: SessionViewModel by viewModels()

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition {
            sessionViewModel.isAuthenticated.value == null || themeViewModel.themeMode.value == null
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme =
                when (themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
                }
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(lightNavigationScrim, darkNavigationScrim) { darkTheme },
                )
                onDispose {}
            }
            LaporLHTheme(darkTheme = darkTheme) {
                LaporLHNavHost()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { syncRepository.requestSync() }
    }
}
