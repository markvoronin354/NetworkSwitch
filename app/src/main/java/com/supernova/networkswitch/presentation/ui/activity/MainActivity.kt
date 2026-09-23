package com.supernova.networkswitch.presentation.ui.activity

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
import androidx.compose.ui.unit.dp
import com.supernova.networkswitch.R
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.presentation.theme.NetworkSwitchTheme
import com.supernova.networkswitch.presentation.ui.composable.CompatibilityCard
import com.supernova.networkswitch.presentation.ui.composable.NetworkToggleCard
import com.supernova.networkswitch.presentation.ui.composable.QuickSettingsHintCard
import com.supernova.networkswitch.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
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
    
    Scaffold(
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
                            contentDescription = "Network Mode Configuration"
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                    toggleButtonText = viewModel.getToggleButtonText(),
                    isLoading = viewModel.isLoading,
                    onToggleClick = { viewModel.toggleNetworkMode() }
                )
            }
            
            // Quick Settings Tip Card
            QuickSettingsHintCard()
        }
    }
}
