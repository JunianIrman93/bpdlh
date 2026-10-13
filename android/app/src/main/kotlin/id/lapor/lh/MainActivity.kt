package id.lapor.lh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import id.lapor.lh.core.ui.LaporLHTheme
import id.lapor.lh.feature.sync.domain.SyncRepository
import id.lapor.lh.navigation.LaporLHNavHost
import id.lapor.lh.navigation.SessionViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var syncRepository: SyncRepository

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

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { syncRepository.requestSync() }
    }
}
