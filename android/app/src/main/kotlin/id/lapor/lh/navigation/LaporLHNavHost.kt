package id.lapor.lh.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.lapor.lh.core.ui.LaporLHMotion
import id.lapor.lh.feature.auth.ui.AccountRoute
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

    NavHost(
        navController = navController,
        startDestination = LoginDestination,
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        enterTransition = { slideInHorizontally(LaporLHMotion.spatial()) { it / 8 } + fadeInThrough() },
        exitTransition = { slideOutHorizontally(LaporLHMotion.spatial()) { -it / 8 } + fadeOutThrough() },
        popEnterTransition = { slideInHorizontally(LaporLHMotion.spatial()) { -it / 8 } + fadeInThrough() },
        popExitTransition = { slideOutHorizontally(LaporLHMotion.spatial()) { it / 8 } + fadeOutThrough() },
    ) {
        composable<LoginDestination>(
            enterTransition = { fadeInThrough() },
            exitTransition = { fadeOutThrough() },
        ) { LoginRoute() }
        composable<ReportListDestination>(
            enterTransition = { if (initialState.isLogin()) fadeInThrough() else null },
            exitTransition = { if (targetState.isForm()) fadeOutThrough() else null },
            popEnterTransition = { if (initialState.isForm()) fadeInThrough() else null },
        ) {
            ReportListRoute(
                onCreateReport = { navController.navigate(ReportFormDestination(UUID.randomUUID().toString())) },
                onOpenAccount = { navController.navigate(AccountDestination) },
                onOpenReport = { report ->
                    if (report.status == ReportStatus.DRAFT) {
                        navController.navigate(ReportFormDestination(report.id))
                    } else {
                        navController.navigate(ReportDetailDestination(report.id))
                    }
                },
            )
        }
        composable<AccountDestination> {
            AccountRoute(onBack = { navController.popBackStack() })
        }
        composable<ReportFormDestination>(
            enterTransition = { slideInVertically(LaporLHMotion.spatial()) { it / 10 } + fadeInThrough() },
            popExitTransition = { slideOutVertically(LaporLHMotion.spatial()) { it / 10 } + fadeOutThrough() },
        ) {
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
        val signedIn = isAuthenticated ?: return@LaunchedEffect
        val onLogin = navController.currentDestination?.hasRoute(LoginDestination::class) ?: true
        if (signedIn && onLogin) {
            navController.navigate(ReportListDestination) { popUpTo<LoginDestination> { inclusive = true } }
        } else if (!signedIn && !onLogin) {
            navController.navigate(LoginDestination) { popUpTo(0) { inclusive = true } }
        }
    }
}

private fun fadeInThrough(): EnterTransition = fadeIn(tween(durationMillis = 210, delayMillis = 90, easing = LinearOutSlowInEasing))

private fun fadeOutThrough(): ExitTransition = fadeOut(tween(durationMillis = 90, easing = FastOutLinearInEasing))

private fun NavBackStackEntry.isLogin(): Boolean = destination.hasRoute(LoginDestination::class)

private fun NavBackStackEntry.isForm(): Boolean = destination.hasRoute(ReportFormDestination::class)
