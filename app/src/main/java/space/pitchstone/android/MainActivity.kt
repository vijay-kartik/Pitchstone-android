package space.pitchstone.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import space.pitchstone.android.presentation.ask.AskScreen
import space.pitchstone.android.presentation.ask.AskViewModel
import space.pitchstone.android.presentation.budgets.BudgetsScreen
import space.pitchstone.android.presentation.budgets.BudgetsViewModel
import space.pitchstone.android.presentation.capture.CaptureScreen
import space.pitchstone.android.presentation.capture.CaptureViewModel
import space.pitchstone.android.presentation.confirm.ConfirmScreen
import space.pitchstone.android.presentation.confirm.ConfirmViewModel
import space.pitchstone.android.presentation.detail.TransactionDetailScreen
import space.pitchstone.android.presentation.detail.TransactionDetailViewModel
import space.pitchstone.android.presentation.home.HomeScreen
import space.pitchstone.android.presentation.home.HomeViewModel
import space.pitchstone.android.presentation.list.TransactionListScreen
import space.pitchstone.android.presentation.list.TransactionListViewModel
import space.pitchstone.android.presentation.navigation.AskRoute
import space.pitchstone.android.presentation.navigation.BudgetsRoute
import space.pitchstone.android.presentation.navigation.CaptureRoute
import space.pitchstone.android.presentation.navigation.ConfirmRoute
import space.pitchstone.android.presentation.navigation.HomeRoute
import space.pitchstone.android.presentation.navigation.SettingsRoute
import space.pitchstone.android.presentation.navigation.TransactionDetailRoute
import space.pitchstone.android.presentation.navigation.TransactionListRoute
import space.pitchstone.android.presentation.settings.SettingsScreen
import space.pitchstone.android.presentation.settings.SettingsViewModel
import space.pitchstone.android.ui.theme.PitchstoneTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PitchstoneTheme {
                PitchstoneAppNavigation()
            }
        }
    }
}

@Composable
fun PitchstoneAppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = Modifier.fillMaxSize()
    ) {
        composable<HomeRoute> {
            val vm: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = vm,
                onNavigateToCapture = { navController.navigate(CaptureRoute) },
                onNavigateToAsk = { navController.navigate(AskRoute) },
                onNavigateToBudgets = { navController.navigate(BudgetsRoute) },
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
                onNavigateToDetail = { id -> navController.navigate(TransactionDetailRoute(id)) }
            )
        }

        composable<CaptureRoute> {
            val vm: CaptureViewModel = hiltViewModel()
            CaptureScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onExtractSuccess = { replyText ->
                    navController.navigate(ConfirmRoute(replyText)) {
                        popUpTo(CaptureRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<AskRoute> {
            val vm: AskViewModel = hiltViewModel()
            AskScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable<BudgetsRoute> {
            val vm: BudgetsViewModel = hiltViewModel()
            BudgetsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable<SettingsRoute> {
            val vm: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<ConfirmRoute> {
            val vm: ConfirmViewModel = hiltViewModel()
            ConfirmScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() },
                onSavedSuccess = {
                    navController.navigate(HomeRoute) {
                        popUpTo(HomeRoute) { inclusive = true }
                    }
                }
            )
        }

        // Kept but unreachable from nav — compiles cleanly via TransactionListRoute
        composable<TransactionListRoute> {
            val vm: TransactionListViewModel = hiltViewModel()
            TransactionListScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() },
                onTransactionClick = { id -> navController.navigate(TransactionDetailRoute(id)) }
            )
        }

        composable<TransactionDetailRoute> {
            val vm: TransactionDetailViewModel = hiltViewModel()
            TransactionDetailScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
