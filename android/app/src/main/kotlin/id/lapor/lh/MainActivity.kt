package id.lapor.lh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import id.lapor.lh.core.ui.LaporLHTheme
import id.lapor.lh.navigation.LaporLHNavHost
import id.lapor.lh.navigation.SessionViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { sessionViewModel.isAuthenticated.value == null }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaporLHTheme {
                LaporLHNavHost()
            }
        }
    }
}
