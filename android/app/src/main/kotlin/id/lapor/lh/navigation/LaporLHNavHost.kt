package id.lapor.lh.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.lapor.lh.feature.auth.ui.LoginRoute

private object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
}

@Composable
fun LaporLHNavHost(navController: NavHostController = rememberNavController()) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val isAuthenticated by sessionViewModel.isAuthenticated.collectAsState()
    val currentUser by sessionViewModel.currentUser.collectAsState()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) { LoginRoute() }
        composable(Routes.HOME) { HomePlaceholder(fullName = currentUser?.fullName) }
    }

    LaunchedEffect(isAuthenticated) {
        val target = if (isAuthenticated) Routes.HOME else Routes.LOGIN
        if (navController.currentDestination?.route != target) {
            navController.navigate(target) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}

@Composable
private fun HomePlaceholder(fullName: String?) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (fullName != null) "Halo, $fullName" else "Berhasil masuk",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
