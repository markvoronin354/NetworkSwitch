package com.markvoronin.networkswitch.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.markvoronin.networkswitch.R
import com.markvoronin.networkswitch.domain.model.CompatibilityState
import com.markvoronin.networkswitch.domain.model.ControlMethod
import com.markvoronin.networkswitch.presentation.theme.NetworkSwitchTheme
import com.markvoronin.networkswitch.presentation.ui.components.SectionHeader
import com.markvoronin.networkswitch.presentation.ui.components.ShapeBadge
import com.markvoronin.networkswitch.presentation.ui.components.segmentedItemColors
import com.markvoronin.networkswitch.presentation.ui.composable.WidgetCustomizationCard
import com.markvoronin.networkswitch.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            NetworkSwitchTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.retryCompatibilityCheck()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val controlMethod by viewModel.controlMethod.collectAsState()
    val widgetCustomization by viewModel.widgetCustomization.collectAsState()
    val automationEnabled by viewModel.automationEnabled.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
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
            // Control Method Selection
            ControlMethodCard(
                selectedMethod = controlMethod,
                onMethodSelected = { viewModel.updateControlMethod(it) },
                rootCompatibility = viewModel.rootCompatibility,
                shizukuCompatibility = viewModel.shizukuCompatibility,
                onRetryClick = { viewModel.retryCompatibilityCheck() }
            )

            // Automation Card
            AutomationCard(
                enabled = automationEnabled,
                onEnabledChange = { viewModel.updateAutomationEnabled(it) }
            )

            // Widget Customization Card
            WidgetCustomizationCard(
                config = widgetCustomization,
                onConfigChanged = { viewModel.updateWidgetCustomization(it) }
            )
            
            // About Section
            AboutCard()

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ControlMethodCard(
    selectedMethod: ControlMethod,
    onMethodSelected: (ControlMethod) -> Unit,
    rootCompatibility: CompatibilityState,
    shizukuCompatibility: CompatibilityState,
    onRetryClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = stringResource(R.string.control_method),
            description = stringResource(R.string.control_method_desc),
            action = {
                IconButton(onClick = onRetryClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.refresh_compatibility),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            // Option 1: Root Method
            ControlMethodOption(
                title = stringResource(R.string.method_root),
                subtitle = stringResource(R.string.method_root_desc),
                isSelected = selectedMethod == ControlMethod.ROOT,
                compatibilityState = rootCompatibility,
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                onClick = { onMethodSelected(ControlMethod.ROOT) }
            )

            // Option 2: Shizuku Method
            ControlMethodOption(
                title = stringResource(R.string.method_shizuku),
                subtitle = stringResource(R.string.method_shizuku_desc),
                isSelected = selectedMethod == ControlMethod.SHIZUKU,
                compatibilityState = shizukuCompatibility,
                shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                onClick = { onMethodSelected(ControlMethod.SHIZUKU) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ControlMethodOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    compatibilityState: CompatibilityState,
    shapes: ListItemShapes,
    onClick: () -> Unit
) {
    SegmentedListItem(
        selected = isSelected,
        onClick = onClick,
        shapes = shapes,
        colors = segmentedItemColors(),
        leadingContent = { RadioButton(selected = isSelected, onClick = null) },
        trailingContent = {
            // Status indicator
            when (compatibilityState) {
                is CompatibilityState.Pending -> {
                    LoadingIndicator(modifier = Modifier.size(24.dp))
                }
                is CompatibilityState.Compatible -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.compatible),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                is CompatibilityState.PermissionDenied, is CompatibilityState.Incompatible -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = stringResource(R.string.incompatible_or_denied),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        supportingContent = { Text(subtitle) },
        content = { Text(text = title, fontWeight = FontWeight.SemiBold) }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AutomationCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            title = stringResource(R.string.automation),
            description = stringResource(R.string.automation_desc)
        )

        SegmentedListItem(
            onClick = { onEnabledChange(!enabled) },
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
            colors = segmentedItemColors(),
            trailingContent = { Switch(checked = enabled, onCheckedChange = null) },
            supportingContent = { Text(stringResource(R.string.automation_allow_desc)) },
            content = { Text(text = stringResource(R.string.automation_allow), fontWeight = FontWeight.SemiBold) }
        )
    }
}

private val licenses = listOf(
    "Shizuku Service API" to "https://github.com/RikkaApps/Shizuku",
    "libsu Framework" to "https://github.com/topjohnwu/libsu",
    "Dagger Hilt" to "https://github.com/google/dagger",
    "AndroidHiddenApiBypass" to "https://github.com/LSPosed/AndroidHiddenApiBypass",
    "Android Jetpack & Compose" to "https://android.googlesource.com/platform/frameworks/support"
)

@Composable
private fun AboutCard() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = stringResource(R.string.about_title))

        LinkItem(
            title = stringResource(R.string.about_source_code),
            subtitle = "https://github.com/markvoronin354/NetworkSwitch",
            link = "https://github.com/markvoronin354/NetworkSwitch",
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
            icon = Icons.Default.Code
        )

        SectionHeader(title = stringResource(R.string.about_licenses))

        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            licenses.forEachIndexed { index, (title, link) ->
                LinkItem(
                    title = title,
                    subtitle = "Apache License 2.0",
                    link = link,
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = licenses.size)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkItem(
    title: String,
    subtitle: String,
    link: String,
    shapes: ListItemShapes,
    icon: ImageVector? = null
) {
    val context = LocalContext.current

    SegmentedListItem(
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri())) },
        shapes = shapes,
        colors = segmentedItemColors(),
        leadingContent = icon?.let {
            {
                ShapeBadge(shape = MaterialShapes.Cookie9Sided.toShape()) {
                    Icon(imageVector = it, contentDescription = null)
                }
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = stringResource(R.string.open_link),
                modifier = Modifier.size(20.dp)
            )
        },
        supportingContent = { Text(subtitle) },
        content = { Text(title) }
    )
}
