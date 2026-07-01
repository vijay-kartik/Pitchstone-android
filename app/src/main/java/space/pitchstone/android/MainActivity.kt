package space.pitchstone.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import space.pitchstone.android.presentation.confirm.ConfirmScreen
import space.pitchstone.android.presentation.confirm.ConfirmViewModel
import space.pitchstone.android.presentation.detail.TransactionDetailScreen
import space.pitchstone.android.presentation.detail.TransactionDetailViewModel
import space.pitchstone.android.presentation.home.HomeScreen
import space.pitchstone.android.presentation.home.HomeViewModel
import space.pitchstone.android.presentation.list.TransactionListScreen
import space.pitchstone.android.presentation.list.TransactionListViewModel
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
        // Home Screen (Extraction input)
        composable<HomeRoute> {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
                onNavigateToTransactions = { navController.navigate(TransactionListRoute) },
                onExtractionSuccess = { replyText -> navController.navigate(ConfirmRoute(replyText)) }
            )
        }

        // Settings Screen
        composable<SettingsRoute> {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Confirm Screen
        composable<ConfirmRoute> {
            val confirmViewModel: ConfirmViewModel = hiltViewModel()
            ConfirmScreen(
                viewModel = confirmViewModel,
                onBackClick = { navController.popBackStack() },
                onSavedSuccess = {
                    navController.navigate(TransactionListRoute) {
                        popUpTo(HomeRoute) { saveState = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Transactions List Screen
        composable<TransactionListRoute> {
            val listViewModel: TransactionListViewModel = hiltViewModel()
            TransactionListScreen(
                viewModel = listViewModel,
                onBackClick = { navController.popBackStack() },
                onTransactionClick = { transactionId ->
                    navController.navigate(TransactionDetailRoute(transactionId))
                }
            )
        }

        // Transaction Detail Screen
        composable<TransactionDetailRoute> {
            val detailViewModel: TransactionDetailViewModel = hiltViewModel()
            TransactionDetailScreen(
                viewModel = detailViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}