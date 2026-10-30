package com.markvoronin.networkswitch.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.markvoronin.networkswitch.BuildConfig
import com.markvoronin.networkswitch.R
import com.markvoronin.networkswitch.domain.model.CompatibilityState
import com.markvoronin.networkswitch.presentation.theme.NetworkSwitchTheme
import com.markvoronin.networkswitch.presentation.ui.composable.CompatibilityCard
import com.markvoronin.networkswitch.presentation.ui.composable.NetworkStatsCard
import com.markvoronin.networkswitch.presentation.ui.composable.NetworkToggleCard
import com.markvoronin.networkswitch.presentation.ui.composable.QuickSettingsHintCard
import com.markvoronin.networkswitch.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Poll speed and listen for signal changes only while the screen is visible
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.observeNetworkStats()
            }
        }
        
        setContent {
            NetworkSwitchTheme {
                MainScreen(
                    viewModel = viewModel,
                    onSettingsClick = {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    },
                    onNetworkModeConfigClick = {
                        startActivity(Intent(this@MainActivity, NetworkModeConfigActivity::class.java))
                    }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshAllData()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    viewModel: MainViewModel,
    onSettingsClick: () -> Unit,
    onNetworkModeConfigClick: () -> Unit
) {
    val compatibilityState = viewModel.compatibilityState
    val snackbarHostState = remember { SnackbarHostState() }

    if (viewModel.switchFailed) {
        val message = stringResource(R.string.error_switch_failed)
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissSwitchFailure()
        }
    }

    if (viewModel.speedTestFailed) {
        val message = stringResource(R.string.error_speed_test_failed)
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissSpeedTestFailure()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNetworkModeConfigClick) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.network_mode_config)
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Compatibility Status Card
            CompatibilityCard(
                compatibilityState = compatibilityState,
                currentControlMethod = viewModel.selectedMethod,
                onRetryClick = { viewModel.retryCompatibilityCheck() }
            )
            
            // Network Toggle Card (show if compatible)
            if (compatibilityState is CompatibilityState.Compatible) {
                NetworkToggleCard(
                    currentMode = viewModel.currentNetworkMode,
                    toggleButtonText = stringResource(
                        R.string.switch_to,
                        viewModel.toggleModeConfig.getNextMode().displayName
                    ),
                    isLoading = viewModel.isLoading,
                    onToggleClick = { viewModel.toggleNetworkMode() }
                )
            }
            
            NetworkStatsCard(
                stats = viewModel.networkStats,
                speedTestResults = viewModel.speedTestResults,
                isSpeedTestRunning = viewModel.isSpeedTestRunning,
                canRunSpeedTest = viewModel.currentNetworkMode != null,
                onRunSpeedTestClick = { viewModel.runSpeedTest() }
            )

            // Quick Settings Tip Card
            QuickSettingsHintCard()

            // Version display
            Text(
                text = "v${BuildConfig.VERSION_NAME.removeSuffix("-debug")}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
