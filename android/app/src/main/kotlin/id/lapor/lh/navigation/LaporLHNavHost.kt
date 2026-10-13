package id.lapor.lh.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.lapor.lh.feature.auth.ui.LoginRoute
import id.lapor.lh.feature.report.domain.ReportStatus
import id.lapor.lh.feature.report.ui.ReportDetailRoute
import id.lapor.lh.feature.report.ui.ReportFormRoute
import id.lapor.lh.feature.report.ui.ReportListRoute
import java.util.UUID

@Composable
fun LaporLHNavHost(navController: NavHostController = rememberNavController()) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val isAuthenticated by sessionViewModel.isAuthenticated.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = LoginDestination) {
        composable<LoginDestination> { LoginRoute() }
        composable<ReportListDestination> {
            ReportListRoute(
                onCreateReport = { navController.navigate(ReportFormDestination(UUID.randomUUID().toString())) },
                onOpenReport = { report ->
                    if (report.status == ReportStatus.DRAFT) {
                        navController.navigate(ReportFormDestination(report.id))
                    } else {
                        navController.navigate(ReportDetailDestination(report.id))
                    }
                },
            )
        }
        composable<ReportFormDestination> {
            ReportFormRoute(onExit = { navController.popBackStack() })
        }
        composable<ReportDetailDestination> {
            ReportDetailRoute(
                onBack = { navController.popBackStack() },
                onEdit = { id ->
                    navController.navigate(ReportFormDestination(id)) {
                        popUpTo<ReportListDestination>()
                    }
                },
            )
        }
    }

    LaunchedEffect(isAuthenticated) {
        val onLogin = navController.currentDestination?.hasRoute(LoginDestination::class) ?: true
        if (isAuthenticated && onLogin) {
            navController.navigate(ReportListDestination) { popUpTo<LoginDestination> { inclusive = true } }
        } else if (!isAuthenticated && !onLogin) {
            navController.navigate(LoginDestination) { popUpTo(0) { inclusive = true } }
        }
    }
}
