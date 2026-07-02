package space.pitchstone.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import space.pitchstone.android.presentation.confirm.ConfirmScreen
import space.pitchstone.android.presentation.confirm.ConfirmViewModel
import space.pitchstone.android.presentation.detail.TransactionDetailScreen
import space.pitchstone.android.presentation.detail.TransactionDetailViewModel
import space.pitchstone.android.presentation.extraction.ExtractionCoordinator
import space.pitchstone.android.presentation.extraction.ExtractionNotifier
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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var extractionCoordinator: ExtractionCoordinator

    /** Reply text delivered by a tapped "extraction complete" notification, pending navigation. */
    private val pendingExtractionReply = MutableStateFlow<String?>(null)

    private var pendingShareRequest: ShareRequest? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // The extraction proceeds regardless of the outcome — the notification
            // is a progress companion, not a prerequisite.
            startPendingShareExtraction()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            PitchstoneTheme {
                val extractionReply by pendingExtractionReply.collectAsState()
                PitchstoneAppNavigation(
                    pendingExtractionReply = extractionReply,
                    onPendingReplyConsumed = { pendingExtractionReply.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ExtractionNotifier.ACTION_VIEW_EXTRACTION_RESULT -> {
                intent.getStringExtra(ExtractionNotifier.EXTRA_REPLY_TEXT)?.let { replyText ->
                    extractionCoordinator.consumeResult()
                    pendingExtractionReply.value = replyText
                }
            }

            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> handleShareIntent(intent)
        }
    }

    private fun handleShareIntent(intent: Intent) {
        val uris = extractSharedUris(intent)
        if (uris.isEmpty()) return
        pendingShareRequest = ShareRequest(
            userInput = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty(),
            uris = uris
        )
        if (shouldRequestNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startPendingShareExtraction()
        }
    }

    private fun extractSharedUris(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_SEND -> listOfNotNull(
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        )

        Intent.ACTION_SEND_MULTIPLE ->
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                .orEmpty()

        else -> emptyList()
    }

    private fun startPendingShareExtraction() {
        val request = pendingShareRequest ?: return
        pendingShareRequest = null
        val started = extractionCoordinator.start(request.userInput, request.uris)
        val messageRes = if (started) {
            R.string.share_processing_started
        } else {
            R.string.share_extraction_already_running
        }
        Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show()
    }

    private fun shouldRequestNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

    private data class ShareRequest(val userInput: String, val uris: List<Uri>)
}

@Composable
fun PitchstoneAppNavigation(
    pendingExtractionReply: String?,
    onPendingReplyConsumed: () -> Unit
) {
    val navController = rememberNavController()

    LaunchedEffect(pendingExtractionReply) {
        if (pendingExtractionReply != null) {
            navController.navigate(ConfirmRoute(pendingExtractionReply))
            onPendingReplyConsumed()
        }
    }

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
