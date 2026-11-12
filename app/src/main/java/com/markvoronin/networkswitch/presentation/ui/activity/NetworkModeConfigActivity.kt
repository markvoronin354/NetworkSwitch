package com.markvoronin.networkswitch.presentation.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markvoronin.networkswitch.R
import com.markvoronin.networkswitch.presentation.theme.NetworkSwitchTheme
import com.markvoronin.networkswitch.presentation.ui.components.SectionHeader
import com.markvoronin.networkswitch.presentation.ui.composable.NetworkModeSelector
import com.markvoronin.networkswitch.presentation.viewmodel.NetworkModeConfigViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NetworkModeConfigActivity : ComponentActivity() {
    
    private val viewModel: NetworkModeConfigViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            NetworkSwitchTheme {
                NetworkModeConfigScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NetworkModeConfigScreen(
    viewModel: NetworkModeConfigViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentConfig by viewModel.currentConfig.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val configSaved by viewModel.configSaved.collectAsState()
    val saveFailed by viewModel.saveFailed.collectAsState()
    val savedMessage = stringResource(R.string.configuration_saved)
    val saveFailedMessage = stringResource(R.string.configuration_save_failed)
    
    // Show toast when configuration is saved
    LaunchedEffect(configSaved) {
        if (configSaved) {
            Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
            viewModel.resetSavedState()
        }
    }
    
    LaunchedEffect(saveFailed) {
        if (saveFailed) {
            Toast.makeText(context, saveFailedMessage, Toast.LENGTH_LONG).show()
            viewModel.resetSaveFailedState()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val buttonHeight = ButtonDefaults.MediumContainerHeight

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.network_mode_config),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = stringResource(R.string.network_mode),
                description = stringResource(R.string.network_mode_config_desc)
            )

            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                // Mode A Selection
                NetworkModeSelector(
                    label = stringResource(R.string.mode_a_label),
                    selectedMode = currentConfig.modeA,
                    onModeSelected = { mode ->
                        viewModel.updateModeA(mode)
                    },
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2)
                )

                // Mode B Selection
                NetworkModeSelector(
                    label = stringResource(R.string.mode_b_label),
                    selectedMode = currentConfig.modeB,
                    onModeSelected = { mode ->
                        viewModel.updateModeB(mode)
                    },
                    shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2)
                )
            }

            // Warning if both modes are the same
            AnimatedVisibility(
                visible = currentConfig.modeA == currentConfig.modeB,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
                exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.warning),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.modes_must_differ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Save Button
            Button(
                onClick = { viewModel.saveConfiguration() },
                shapes = ButtonDefaults.shapes(),
                enabled = !isLoading && currentConfig.modeA != currentConfig.modeB,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .heightIn(min = buttonHeight),
                contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight)
            ) {
                if (isLoading) {
                    LoadingIndicator(
                        modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight)),
                        color = LocalContentColor.current
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.iconSpacingFor(buttonHeight)))
                }
                Text(
                    text = stringResource(R.string.save_configuration),
                    style = ButtonDefaults.textStyleFor(buttonHeight)
                )
            }
        }
    }
}
